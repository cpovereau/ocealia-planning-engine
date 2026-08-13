package fr.project.planning.scenarios.service;

import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.repos.CalendrierReposHebdomadaire;
import fr.project.planning.domain.reglementaire.CalendrierJoursFeries;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.dto.Sc06ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import fr.project.planning.scenarios.dto.request.BesoinCreneauDTO;
import fr.project.planning.scenarios.dto.request.BesoinDTO;
import fr.project.planning.scenarios.mapper.ToleranceEquiteMapper;
import fr.project.planning.scenarios.mapper.CoefficientsPenibiliteMapper;
import fr.project.planning.scenarios.mapper.ScenarioCreneauMapper;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.mapper.ScenarioRegulatoryParametersMapper;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import fr.project.planning.scoring.StrategieScoring;
import fr.project.planning.solution.PlanningProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ScenarioSc06PreparationService — validation et construction du problème SC-06 (lot S4).
 *
 * <p>Deux populations de créneaux, de statuts opposés :</p>
 * <ul>
 *   <li>{@code dataSet.creneaux} — le planning existant, <strong>intégralement figé</strong> ;</li>
 *   <li>{@code scenarioParameters.besoin.creneaux} — la question posée, seules variables de
 *       décision du problème.</li>
 * </ul>
 *
 * <p>Les garde-fous de ce service ne sont pas défensifs par principe : chacun protège d'un
 * résultat <em>faux mais crédible</em>, bien plus coûteux qu'une erreur franche.</p>
 */
@Service
public class ScenarioSc06PreparationService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc06PreparationService.class);

    private static final String SCENARIO_TYPE = "SC-06";

    private final ScenarioResourceMapper resourceMapper;
    private final ScenarioCreneauMapper creneauMapper;

    private final ScenarioRegulatoryParametersMapper regulatoryMapper =
            new ScenarioRegulatoryParametersMapper();

    public ScenarioSc06PreparationService(ScenarioResourceMapper resourceMapper,
                                          ScenarioCreneauMapper creneauMapper) {
        this.resourceMapper = resourceMapper;
        this.creneauMapper = creneauMapper;
    }

    public PreparedSc06Scenario prepare(Sc06ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        if (!SCENARIO_TYPE.equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-06 est supporté par cet endpoint.");
        }

        BesoinDTO besoin = exigerBesoin(request);
        LocalDate dateBesoin = besoin.getDate();
        LocalDate lundi = verifierSemainePleine(request, dateBesoin);

        if (request.getDataSet() == null) {
            throw new IllegalArgumentException("[SC-06] dataSet est requis.");
        }
        if (request.getDataSet().getReferentiels() == null) {
            throw new IllegalArgumentException("[SC-06] dataSet.referentiels est requis.");
        }
        if (request.getDataSet().getRessources() == null
                || request.getDataSet().getRessources().getSalaries() == null
                || request.getDataSet().getRessources().getSalaries().isEmpty()) {
            throw new IllegalArgumentException(
                    "[SC-06] dataSet.ressources.salaries est requis et ne peut pas être vide : "
                            + "sans candidat, la question n'a pas de réponse.");
        }

        // 1. Référentiel d'activités
        ReferentielComptabiliteActivite referentiel =
                resourceMapper.toReferentiel(request.getDataSet().getReferentiels());

        // 2. Ressources — instances uniques, partagées entre le value range et les affectations figées
        List<Ressource> ressources = resourceMapper.toRessources(request.getDataSet());
        Map<String, Ressource> ressourcesParId = new HashMap<>();
        for (Ressource r : ressources) {
            ressourcesParId.put(r.getId(), r);
        }

        List<SalarieReel> salaries = ressources.stream()
                .filter(SalarieReel.class::isInstance)
                .map(SalarieReel.class::cast)
                .toList();

        // 3. Planning existant — figé, et exigé complet
        List<CreneauInputDTO> creneauxExistants = request.getDataSet().getCreneaux() != null
                ? request.getDataSet().getCreneaux() : List.of();
        verifierAffectationsRenseignees(creneauxExistants);
        verifierDansHorizon(creneauxExistants, lundi);

        List<Creneau> planningFigeComplet =
                creneauMapper.toCreneauxFiges(creneauxExistants, ressourcesParId);

        // 3 bis. [S7.9b] Les marqueurs de repos sortent du problème : un repos n'est ni un besoin
        //        à pourvoir ni une charge. Ils ne sont pas restitués non plus — la réponse SC-06
        //        ne contient que les créneaux du besoin, pas le planning complet.
        List<Creneau> marqueursRepos = planningFigeComplet.stream()
                .filter(c -> CalendrierReposHebdomadaire.estMarqueurDeRepos(c, referentiel))
                .toList();
        List<Creneau> planningFige = planningFigeComplet.stream()
                .filter(c -> !CalendrierReposHebdomadaire.estMarqueurDeRepos(c, referentiel))
                .toList();

        // 4. Créneaux du besoin — seules variables de décision
        List<Creneau> creneauxBesoin = construireCreneauxBesoin(besoin, referentiel);

        // 5. Indisponibilités
        List<Indisponibilite> indisponibilites =
                resourceMapper.toIndisponibilites(request.getDataSet().getIndisponibilites());

        // 6. Contexte
        StrategieScoring strategieScoring = StrategieScoring.valueOf(
                request.getPlanningContext().getStrategieScoring());

        // [S8.4] SC-06 refuse plutôt qu'il n'ignore : ses garde-fous lèvent des exceptions, et
        //        aucun créneau n'est écarté en silence. Restait le cadre réglementaire, seul
        //        endroit où le moteur décide à la place de l'appelant — c'est ce que ce collecteur
        //        rend visible.
        CollecteurAlertes alertes = new CollecteurAlertes("SC-06");

        PlanningContext planningContext = new PlanningContext(
                ObjectifResolution.ANALYSER_LE_MANQUE,
                strategieScoring,
                request.getPlanningContext().getHorizon().getDateDebut(),
                request.getPlanningContext().getHorizon().getDateFin(),
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE,
                // [Équité L1] Ce que vaut une heure selon quand elle est travaillée.
                CoefficientsPenibiliteMapper.depuis(request.getPlanningContext(), "SC-06", alertes),
                // [Équité L5] Absente, la contrainte d'équité ne pèse rien.
                ToleranceEquiteMapper.depuis(request.getPlanningContext())
        );

        List<Creneau> tousCreneaux = new ArrayList<>(planningFige.size() + creneauxBesoin.size());
        tousCreneaux.addAll(planningFige);
        tousCreneaux.addAll(creneauxBesoin);

        // [S8.0] Le calendrier déclaré au contrat fait autorité ; à défaut, les fériés restent
        // ceux que les créneaux déclarent, planning figé et besoin confondus (S7.9a).
        PlanningProblem problem = new PlanningProblem(
                planningContext,
                regulatoryMapper.toRegulatoryParameters(
                        request.getPlanningContext().getRegulatoryParameters(),
                        CalendrierJoursFeries.declaresParLesCreneaux(tousCreneaux),
                        "SC-06",
                        alertes),
                referentiel,
                ressources,
                tousCreneaux,
                indisponibilites
        );

        // [S7.9b] Calendrier de repos — déclarations du planning figé, puis repli par semaine.
        problem.setReposHebdomadaires(CalendrierReposHebdomadaire.construire(
                salaries.stream().map(SalarieReel::getId).filter(Objects::nonNull).toList(),
                CalendrierReposHebdomadaire.depuisLesMarqueurs(marqueursRepos, referentiel),
                planningContext.getHorizonTemporel()));

        log.info("[SC-06] Besoin du {} : {} créneau(x) à couvrir, {} créneau(x) figés, "
                        + "{} marqueur(s) de repos, {} salarié(s) au dataset",
                dateBesoin, creneauxBesoin.size(), planningFige.size(),
                marqueursRepos.size(), salaries.size());

        return new PreparedSc06Scenario(
                problem, creneauxBesoin, salaries, indisponibilites,
                referentiel, dateBesoin, lundi, request.getScenarioType(),
                alertes.versDto());
    }

    // =========================================================
    // Garde-fous
    // =========================================================

    private BesoinDTO exigerBesoin(Sc06ScenarioRequestDTO request) {
        if (request.getScenarioParameters() == null
                || request.getScenarioParameters().getBesoin() == null) {
            throw new IllegalArgumentException("[SC-06] scenarioParameters.besoin est requis.");
        }
        BesoinDTO besoin = request.getScenarioParameters().getBesoin();
        if (besoin.getDate() == null) {
            throw new IllegalArgumentException("[SC-06] scenarioParameters.besoin.date est requise.");
        }
        if (besoin.getCreneaux() == null || besoin.getCreneaux().isEmpty()) {
            throw new IllegalArgumentException(
                    "[SC-06] scenarioParameters.besoin.creneaux est requis et ne peut pas être vide.");
        }
        return besoin;
    }

    /**
     * Vérifie que l'horizon couvre exactement la semaine calendaire lundi → dimanche contenant
     * le besoin.
     *
     * <p>Ce contrôle n'est pas une formalité. La durée hebdomadaire maximale se mesure sur ce
     * que le moteur reçoit : une semaine tronquée produit un total sous-évalué, donc aucun
     * dépassement détecté, donc des candidats déclarés conformes à tort. Mieux vaut refuser la
     * demande que répondre faux.</p>
     *
     * @return le lundi de la semaine du besoin
     */
    private LocalDate verifierSemainePleine(Sc06ScenarioRequestDTO request, LocalDate dateBesoin) {
        if (request.getPlanningContext() == null || request.getPlanningContext().getHorizon() == null) {
            throw new IllegalArgumentException("[SC-06] planningContext.horizon est requis.");
        }
        LocalDate debut = request.getPlanningContext().getHorizon().getDateDebut();
        LocalDate fin = request.getPlanningContext().getHorizon().getDateFin();
        if (debut == null || fin == null) {
            throw new IllegalArgumentException(
                    "[SC-06] planningContext.horizon.dateDebut et dateFin sont requises.");
        }

        LocalDate lundi = dateBesoin.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate dimanche = lundi.plusDays(6);

        if (!debut.equals(lundi) || !fin.equals(dimanche)) {
            throw new IllegalArgumentException(
                    "[SC-06] planningContext.horizon doit couvrir la semaine complète du besoin, "
                            + "du " + lundi + " au " + dimanche + " — reçu du " + debut + " au " + fin
                            + ". Sans semaine pleine, la durée hebdomadaire maximale est invérifiable.");
        }
        return lundi;
    }

    /**
     * Exige que chaque créneau du planning existant déclare la ressource qui le sert.
     *
     * <p>Un créneau de planning sans affectation n'a pas de sens en SC-06 : le laisser passer
     * en ferait une variable de décision silencieuse, que le solveur affecterait librement —
     * exactement ce que le scénario s'interdit.</p>
     */
    private void verifierAffectationsRenseignees(List<CreneauInputDTO> creneaux) {
        for (CreneauInputDTO dto : creneaux) {
            if (dto.getRessourceAffecteeId() == null || dto.getRessourceAffecteeId().isBlank()) {
                throw new IllegalArgumentException(
                        "[SC-06] créneau id='" + dto.getId() + "' : ressourceAffecteeId est requis. "
                                + "Tout créneau du dataSet appartient au planning existant et doit "
                                + "déclarer la ressource qui le sert.");
            }
        }
    }

    private void verifierDansHorizon(List<CreneauInputDTO> creneaux, LocalDate lundi) {
        LocalDate dimanche = lundi.plusDays(6);
        for (CreneauInputDTO dto : creneaux) {
            if (dto.getDate() == null) {
                throw new IllegalArgumentException(
                        "[SC-06] créneau id='" + dto.getId() + "' : date est requise.");
            }
            if (dto.getDate().isBefore(lundi) || dto.getDate().isAfter(dimanche)) {
                throw new IllegalArgumentException(
                        "[SC-06] créneau id='" + dto.getId() + "' : date " + dto.getDate()
                                + " hors de la semaine du besoin [" + lundi + " — " + dimanche + "].");
            }
        }
    }

    // =========================================================
    // Construction des créneaux du besoin
    // =========================================================

    private List<Creneau> construireCreneauxBesoin(BesoinDTO besoin,
                                                   ReferentielComptabiliteActivite referentiel) {
        List<Creneau> creneaux = new ArrayList<>(besoin.getCreneaux().size());

        for (BesoinCreneauDTO dto : besoin.getCreneaux()) {
            if (referentiel.getByCode(dto.getCodeActiviteId()) == null) {
                throw new IllegalArgumentException(
                        "[SC-06] besoin id='" + dto.getId() + "' : activité '" + dto.getCodeActiviteId()
                                + "' absente du référentiel transmis. Une activité inconnue ne pèse "
                                + "sur aucune règle : le besoin serait couvert sans être évalué.");
            }

            Creneau creneau = new Creneau(
                    dto.getId(),
                    besoin.getDate(),
                    dto.getHeureDebut(),
                    dto.getHeureFin(),
                    ScenarioCreneauMapper.dureeMinutes(dto.getHeureDebut(), dto.getHeureFin()),
                    dto.getLieu(),
                    dto.getCodeActiviteId(),
                    null,                       // activite (libellé déprécié) — jamais alimenté en SC-06
                    dto.getPosteComptable(),
                    null,                       // priorite — non exploitée
                    TypeCreneau.IMPOSE,
                    Boolean.TRUE.equals(dto.getSegmentNuit()) ? TypePlageHoraire.NUIT : TypePlageHoraire.JOUR,
                    Boolean.TRUE.equals(dto.getIsJourFerie()),
                    QualificationJour.OUVRE
            );

            // Jamais null : l'absence d'affectation se représente par A_AFFECTER (invariant moteur).
            creneau.setRessourceAffectee(RessourceNonAffectee.INSTANCE);
            creneaux.add(creneau);
        }
        return creneaux;
    }
}
