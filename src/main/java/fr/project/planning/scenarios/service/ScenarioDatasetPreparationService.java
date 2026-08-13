package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.repos.CalendrierReposHebdomadaire;
import fr.project.planning.domain.repos.ReposHebdomadaire;
import fr.project.planning.domain.ressource.SalarieReel;
import java.util.HashMap;
import java.util.Map;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.CalendrierJoursFeries;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.dto.CreneauIgnoreDTO;
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;
import fr.project.planning.scenarios.dto.MotifCreneauIgnore;
import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioDatasetRequest;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import fr.project.planning.scenarios.dto.input.PosteVirtuelInputDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import fr.project.planning.scenarios.mapper.CoefficientsPenibiliteMapper;
import fr.project.planning.scenarios.mapper.ScenarioCreneauMapper;
import fr.project.planning.scenarios.mapper.ScenarioRegulatoryParametersMapper;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import fr.project.planning.scoring.StrategieScoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ScenarioDatasetPreparationService
 *
 * <p>Prépare toutes les données nécessaires avant l'appel au solveur pour un scénario
 * <strong>dataset-driven</strong> — un scénario dont les créneaux sont reçus, non générés.</p>
 *
 * <p>Différences vs SC-01 :</p>
 * <ul>
 *   <li>les créneaux viennent du dataSet (pas d'un builder SC-01) ;</li>
 *   <li>le référentiel est construit depuis dataSet.referentiels (pas hardcodé "travail") ;</li>
 *   <li>pas de ressource cible unique : toutes les ressources du dataSet sont incluses ;</li>
 *   <li>PlanningContext construit avec les defaults (idem SC-01).</li>
 * </ul>
 *
 * <p><strong>[Lot S1 de SC-02]</strong> Cette classe s'appelait {@code ScenarioSc03PreparationService}
 * et ne servait qu'à SC-03. Tout ce qu'elle fait — référentiel, partitions, marqueurs de repos,
 * cadre réglementaire, diagnostics — vaut à l'identique pour SC-02. Un seul point les sépare : ce
 * qu'ils font de l'affectation déjà portée par un créneau d'entrée, que chacun fournit sous forme
 * de {@link PolitiqueAffectationCreneau}. Le reste est écrit une fois.</p>
 */
@Service
public class ScenarioDatasetPreparationService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioDatasetPreparationService.class);

    private final ScenarioResourceMapper resourceMapper;
    private final ScenarioCreneauMapper creneauMapper;

    private final ScenarioRegulatoryParametersMapper regulatoryMapper =
            new ScenarioRegulatoryParametersMapper();

    public ScenarioDatasetPreparationService(ScenarioResourceMapper resourceMapper,
                                          ScenarioCreneauMapper creneauMapper) {
        this.resourceMapper = resourceMapper;
        this.creneauMapper  = creneauMapper;
    }

    /**
     * Prépare SC-03 : aucune affectation d'entrée n'est reprise, tout créneau reste une variable
     * de décision. Comportement historique du moteur, inchangé.
     */
    public PreparedDatasetScenario prepare(Sc03ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        // [Phase 1 visibilité] Signalement des champs SC-03 reçus mais non exploités
        if (request.getScenarioParameters() != null) {
            if (request.getScenarioParameters().getPrioriteCouverture() != null) {
                log.warn("[SC-03] prioriteCouverture='{}' reçu mais non exploité — champ ignoré (Phase 5 cible)",
                        request.getScenarioParameters().getPrioriteCouverture());
            }
            if (request.getScenarioParameters().getPeriode() != null) {
                log.warn("[SC-03] scenarioParameters.periode reçu mais non exploité — l'horizon de planningContext fait foi (Phase 5 cible)");
            }
        }

        if (!"SC-03".equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-03 est supporté par cet endpoint.");
        }

        return preparer(request, "SC-03", PolitiqueAffectationCreneau.AUCUNE);
    }

    /**
     * Prépare un scénario dataset-driven quelconque.
     *
     * @param request   contexte et jeu de données ; les paramètres propres au scénario restent
     *                  chez l'appelant
     * @param scenario  étiquette du scénario, utilisée dans les journaux et les alertes
     * @param politique ce que le scénario fait de l'affectation déjà portée par un créneau
     */
    public PreparedDatasetScenario preparer(ScenarioDatasetRequest request,
                                            String scenario,
                                            PolitiqueAffectationCreneau politique) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(politique, "politique");

        if (request.getPlanningContext() == null) {
            throw new IllegalArgumentException("planningContext est requis.");
        }

        if (request.getDataSet() == null) {
            throw new IllegalArgumentException("dataSet est requis.");
        }

        if (request.getDataSet().getCreneaux() == null || request.getDataSet().getCreneaux().isEmpty()) {
            throw new IllegalArgumentException("dataSet.creneaux est requis et ne peut pas être vide.");
        }

        // [Phase 7] Guards — horizon temporel
        //   Ces vérifications remplacent les NPE silencieuses qui se produiraient plus loin dans la méthode.
        //   dateDebut et dateFin sont déclarées ici pour être réutilisées par la partition Phase 3.
        if (request.getPlanningContext().getHorizon() == null) {
            throw new IllegalArgumentException("[" + scenario + "] planningContext.horizon est requis.");
        }
        LocalDate dateDebut = request.getPlanningContext().getHorizon().getDateDebut();
        LocalDate dateFin   = request.getPlanningContext().getHorizon().getDateFin();
        if (dateDebut == null) {
            throw new IllegalArgumentException("[" + scenario + "] planningContext.horizon.dateDebut est requise.");
        }
        if (dateFin == null) {
            throw new IllegalArgumentException("[" + scenario + "] planningContext.horizon.dateFin est requise.");
        }
        if (dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException(
                    "[" + scenario + "] planningContext.horizon incohérent : dateDebut (" + dateDebut + ") est postérieure à dateFin (" + dateFin + ").");
        }

        // [Phase 7] Guard — referentiels requis
        //   Sans référentiel, toReferentiel() retournerait neutre() silencieusement et tous les créneaux
        //   seraient exclus comme activité inconnue — comportement trompeur non signalé.
        if (request.getDataSet().getReferentiels() == null) {
            throw new IllegalArgumentException("[" + scenario + "] dataSet.referentiels est requis.");
        }

        // [S8.4] Ce que la préparation constate part désormais dans les deux canaux à la fois :
        //        le journal du serveur, et les `alerts` de la réponse. SC-03 émettait `List.of()`
        //        en dur — tout ce qui suit n'atteignait donc jamais l'appelant.
        CollecteurAlertes alertes = new CollecteurAlertes(scenario);
        List<CreneauIgnoreDTO> creneauxSignales = new ArrayList<>();

        // 1. Référentiel d'activités — construit en premier pour filtrer les créneaux avant solveur
        ReferentielComptabiliteActivite referentiel = resourceMapper.toReferentiel(
                request.getDataSet().getReferentiels()
        );

        // [Phase 7] WARN — référentiel vide
        if (request.getDataSet().getReferentiels().getActivites() == null
                || request.getDataSet().getReferentiels().getActivites().isEmpty()) {
            log.warn("[" + scenario + "] dataSet.referentiels.activites est vide — tous les créneaux seront exclus comme activité inconnue");
        }

        // 1 bis. [S7.9b] Marqueurs de repos hebdomadaire — extraits avant toute autre partition.
        //    Un repos n'est ni un besoin à pourvoir ni une charge : il ne doit pas devenir une
        //    variable de décision, et il n'a pas à traverser le contrôle « activité connue » —
        //    sa déclaration au bloc referentiels vaut déclaration. L'extraire ici évite aussi
        //    les guards horaires : un repos couvre la journée, pas une plage.
        List<CreneauInputDTO> marqueursRepos = new ArrayList<>();
        List<CreneauInputDTO> creneauxATraiter = new ArrayList<>();
        for (CreneauInputDTO dto : request.getDataSet().getCreneaux()) {
            if (referentiel.estReposHebdomadaire(dto.getCodeActiviteEffectif())) {
                marqueursRepos.add(dto);
            } else {
                creneauxATraiter.add(dto);
            }
        }

        // 2. [Phase 2] Partition des créneaux : valides (activité connue) vs exclus (activité inconnue)
        //    On collecte directement les DTOs valides pour éviter tout dépendance sur l'id du créneau.
        List<CreneauInputDTO> creneauxValides = new ArrayList<>();
        int activiteInconnue = 0;
        for (CreneauInputDTO dto : creneauxATraiter) {
            // [Phase 7] Guards — champs horaires requis (NPE garantie sinon dans calculerDureeMinutes)
            if (dto.getHeureDebut() == null) {
                throw new IllegalArgumentException(
                        "[" + scenario + "] créneau id='" + dto.getId() + "' : heureDebut est requise.");
            }
            if (dto.getHeureFin() == null) {
                throw new IllegalArgumentException(
                        "[" + scenario + "] créneau id='" + dto.getId() + "' : heureFin est requise.");
            }

            // [S7.8] La règle de repli vit dans CodeActivite ; ici on ne fait que la diagnostiquer.
            // Le repli s'est produit si le code retenu existe et n'est pas la clé du contrat.
            String codeUtilise = dto.getCodeActiviteEffectif();
            boolean estFallback = codeUtilise != null && !codeUtilise.equals(dto.getCodeActiviteId());

            // [Phase 7] WARN — discordance codeActiviteId vs activite
            if (!estFallback && codeUtilise != null
                    && dto.getActivite() != null && !dto.getActivite().isBlank()
                    && !codeUtilise.equals(dto.getActivite())) {
                log.warn("[" + scenario + "] créneau id='{}' : codeActiviteId='{}' et activite='{}' discordants — activite ignorée",
                        dto.getId(), codeUtilise, dto.getActivite());
            }

            if (estFallback) {
                log.warn("[" + scenario + "] créneau id='{}' : codeActiviteId absent — fallback sur activite='{}' utilisé comme clé référentiel",
                        dto.getId(), codeUtilise);
            }

            if (codeUtilise == null || referentiel.getByCode(codeUtilise) == null) {
                activiteInconnue++;
                log.warn("[" + scenario + "] créneau id='{}' : activité '{}' absente du référentiel — créneau exclu avant solveur",
                        dto.getId(), codeUtilise);
                creneauxSignales.add(CreneauIgnoreDTO.exclu(
                        dto.getId(), dto.getDate(), MotifCreneauIgnore.ACTIVITE_INCONNUE,
                        "Activité '" + codeUtilise + "' absente du référentiel — créneau exclu avant solveur."));
            } else {
                creneauxValides.add(dto);
            }
        }

        // 3. [Phase 3] Partition hors-horizon — sur creneauxValides (activité connue) uniquement.
        //    Ordre : activiteInconnue en premier (Phase 2), horsHorizon ensuite (Phase 3).
        //    Un créneau inconnu + hors-horizon est compté dans activiteInconnue uniquement.
        //    Cas limite : créneau à date null — ni compté, ni exclu (hors périmètre Phase 3).
        //    Note : dateDebut et dateFin sont déclarées dans les guards Phase 7 ci-dessus.
        List<CreneauInputDTO> creneauxDansHorizon = new ArrayList<>();
        int horsHorizon = 0;
        for (CreneauInputDTO dto : creneauxValides) {
            if (dto.getDate() != null && (dto.getDate().isBefore(dateDebut) || dto.getDate().isAfter(dateFin))) {
                horsHorizon++;
                log.warn("[" + scenario + "] créneau id='{}' : date '{}' hors horizon [{} — {}] — créneau exclu avant solveur",
                        dto.getId(), dto.getDate(), dateDebut, dateFin);
                creneauxSignales.add(CreneauIgnoreDTO.exclu(
                        dto.getId(), dto.getDate(), MotifCreneauIgnore.HORS_HORIZON,
                        "Date hors de l'horizon [" + dateDebut + " — " + dateFin + "]"
                                + " — créneau exclu avant solveur."));
            } else {
                creneauxDansHorizon.add(dto);
            }
        }

        // [S8.4] Des créneaux ont disparu entre la demande et la réponse : l'appelant l'apprend
        //        maintenant, et sait lesquels.
        if (activiteInconnue + horsHorizon > 0) {
            alertes.signaler(AlertCode.CRENEAUX_ECARTES_AVANT_SOLVEUR, AlertSeverity.WARNING,
                    (activiteInconnue + horsHorizon) + " créneau(x) écarté(s) avant résolution "
                            + "(activité inconnue : " + activiteInconnue + ", hors horizon : " + horsHorizon
                            + ") — le détail est dans diagnostics.ignoredCreneaux.details.");
        }

        // [Phase 7] WARN — zéro créneaux transmis au solveur après les deux partitions
        if (creneauxDansHorizon.isEmpty()) {
            alertes.signaler(AlertCode.AUCUN_CRENEAU_A_RESOUDRE, AlertSeverity.ERROR,
                    "Aucun créneau n'a survécu aux partitions (activité inconnue : " + activiteInconnue
                            + ", hors horizon : " + horsHorizon + ") — la résolution porte sur un "
                            + "problème vide.");
        }

        // 4. Ressources (salariés + postes virtuels + RessourceNonAffectee)
        //    [S1 de SC-02] Construites avant les créneaux : figer un créneau exige les instances
        //    du value range, une copie distincte sortirait du domaine de la variable de décision.
        List<Ressource> ressources = resourceMapper.toRessources(request.getDataSet());
        Map<String, Ressource> ressourcesParId = new HashMap<>();
        for (Ressource r : ressources) {
            if (r.getId() != null) {
                ressourcesParId.putIfAbsent(r.getId(), r);
            }
        }

        // 5. Créneaux — activité connue ET dans l'horizon.
        //    [S1 de SC-02] Chaque créneau passe ensuite sous la politique du scénario, seule à
        //    savoir ce qu'il faut faire de l'affectation déjà portée par l'entrée : SC-03 l'ignore,
        //    SC-02 fige l'existant et ne libère que les créneaux de l'absent.
        List<Creneau> creneaux = new ArrayList<>(creneauxDansHorizon.size());
        for (CreneauInputDTO dto : creneauxDansHorizon) {
            Creneau creneau = creneauMapper.toCreneau(dto);
            politique.appliquer(dto, creneau, ressourcesParId);
            creneaux.add(creneau);
        }


        // 6. Indisponibilités
        List<Indisponibilite> indisponibilites = resourceMapper.toIndisponibilites(
                request.getDataSet().getIndisponibilites()
        );

        // 7. Contexte planning
        StrategieScoring strategieScoring = StrategieScoring.valueOf(
                request.getPlanningContext().getStrategieScoring()
        );
        PlanningContext planningContext = new PlanningContext(
                ObjectifResolution.ANALYSER_LE_MANQUE,
                strategieScoring,
                request.getPlanningContext().getHorizon().getDateDebut(),
                request.getPlanningContext().getHorizon().getDateFin(),
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE,
                // [Équité L1] Ce que vaut une heure selon quand elle est travaillée.
                CoefficientsPenibiliteMapper.depuis(request.getPlanningContext(), scenario, alertes)
        );

        // 7. Paramètres réglementaires — [S8.0] le calendrier déclaré au contrat fait autorité ;
        //    à défaut, les dates retenues restent celles que les créneaux déclarent via
        //    isJourFerie (S7.9a).
        RegulatoryParameters regulatoryParameters = regulatoryMapper.toRegulatoryParameters(
                request.getPlanningContext().getRegulatoryParameters(),
                CalendrierJoursFeries.declaresParLesCreneaux(creneaux),
                scenario,
                alertes);

        // 7 bis. [S7.9b] Calendrier de repos hebdomadaire — repos déclarés par l'appelant,
        //        complétés semaine par semaine par le repli samedi/dimanche.
        ReposPrepares repos = preparerRepos(scenario, marqueursRepos, referentiel, ressources,
                planningContext.getHorizonTemporel(), creneauxSignales);

        // 7 ter. [Équité L0] Ce que le planning transmis contient déjà et que le moteur
        //        refuserait de produire.
        signalerReposDominicalDejaTravaille(creneaux, repos.calendrier(), alertes);

        // 8. Planning Request
        PlanningRequest planningRequest = new PlanningRequest(
                planningContext,
                regulatoryParameters,
                referentiel,
                ressources,
                creneaux,
                indisponibilites,
                repos.calendrier()
        );

        // 9. IDs postes virtuels (pour les diagnostics)
        //    Le bloc est facultatif : ne pas déclarer de poste virtuel dit qu'aucun n'est
        //    mobilisable, ce qui est une demande légitime. Le moteur y répondait 500 jusqu'au
        //    lot L4 — la garde existait déjà trois lignes plus bas, pour les salariés.
        Set<String> posteVirtuelIds =
                request.getDataSet().getRessources().getPostesVirtuels() == null
                        ? Set.of()
                        : request.getDataSet().getRessources().getPostesVirtuels()
                                .stream().map(PosteVirtuelInputDTO::getId).collect(Collectors.toSet());

        // 10. Comptage ignoredCreneaux (pré-résolution)
        //     horsHorizon est produit par la partition Phase 3 (cf. étape 3 ci-dessus).
        //     aucuneRessourceDansDataset est calculé sur creneauxDansHorizon uniquement :
        //     les créneaux exclus (activiteInconnue ou horsHorizon) ne gonflent pas ce compteur.
        List<SalarieInputDTO> salaries = request.getDataSet().getRessources() != null
                && request.getDataSet().getRessources().getSalaries() != null
                ? request.getDataSet().getRessources().getSalaries() : List.of();

        List<PosteVirtuelInputDTO> postesVirtuelsList = request.getDataSet().getRessources() != null
                && request.getDataSet().getRessources().getPostesVirtuels() != null
                ? request.getDataSet().getRessources().getPostesVirtuels() : List.of();

        // [Phase 7] Salariés sans id
        for (SalarieInputDTO sal : salaries) {
            if (sal.getId() == null || sal.getId().isBlank()) {
                alertes.signaler(AlertCode.RESSOURCE_SANS_ID, AlertSeverity.WARNING,
                        "Un salarié du dataset n'a pas d'identifiant — son comportement solveur "
                                + "n'est pas garanti et il ne sera pas retrouvable dans la réponse.");
            }
        }

        // [Phase 7] Aucune ressource réelle dans le dataset
        if (salaries.isEmpty() && postesVirtuelsList.isEmpty()) {
            alertes.signaler(AlertCode.AUCUNE_RESSOURCE_DANS_DATASET, AlertSeverity.ERROR,
                    "Ni salarié ni poste virtuel au dataset — tous les créneaux reviendront non "
                            + "affectés, quelle que soit la demande.");
        }

        // [S8.4] Le compteur reste un nombre ; le détail dit enfin de quels créneaux il s'agit.
        //        Ces créneaux ne sont PAS écartés : ils partent au solveur, qui rendra visible
        //        l'impossible plutôt que de refuser.
        int aucuneRessourceDansDataset = 0;
        for (CreneauInputDTO dto : creneauxDansHorizon) {
            if (!auMoinsUneRessourceCompatible(dto, salaries, postesVirtuelsList)) {
                aucuneRessourceDansDataset++;
                creneauxSignales.add(CreneauIgnoreDTO.conserve(
                        dto.getId(), dto.getDate(), MotifCreneauIgnore.AUCUNE_RESSOURCE_DANS_DATASET,
                        "Aucune ressource du dataset ne déclare l'activité '"
                                + dto.getCodeActiviteEffectif() + "'. Le créneau est conservé et "
                                + "soumis au solveur."));
            }
        }

        IgnoredCreneauxDTO ignoredCreneaux = new IgnoredCreneauxDTO(
                horsHorizon, aucuneRessourceDansDataset, activiteInconnue, creneauxSignales);

        return new PreparedDatasetScenario(
                planningRequest,
                request.getScenarioType(),
                posteVirtuelIds,
                ignoredCreneaux,
                repos.marqueurs(),
                alertes.versDto()
        );
    }

    /**
     * [S7.9b] Ce que produit l'extraction des marqueurs de repos.
     *
     * @param marqueurs  les créneaux de repos reçus, conservés pour être <strong>restitués</strong>
     *                   tels quels : l'appelant recharge la réponse pour réafficher son planning,
     *                   et un repos manquant y ferait un trou. Ils n'entrent jamais dans le
     *                   problème du solveur.
     * @param calendrier les faits lus par la contrainte, repli samedi/dimanche compris
     */
    private record ReposPrepares(List<Creneau> marqueurs, List<ReposHebdomadaire> calendrier) {}

    private ReposPrepares preparerRepos(
            String scenario,
            List<CreneauInputDTO> marqueursDto,
            ReferentielComptabiliteActivite referentiel,
            List<Ressource> ressources,
            HorizonTemporel horizon,
            List<CreneauIgnoreDTO> creneauxSignales) {

        Map<String, Ressource> ressourcesParId = new HashMap<>();
        for (Ressource r : ressources) {
            if (r.getId() != null) {
                ressourcesParId.putIfAbsent(r.getId(), r);
            }
        }

        List<Creneau> marqueurs = new ArrayList<>();
        for (CreneauInputDTO dto : marqueursDto) {
            String salarieId = dto.getRessourceAffecteeId();

            if (salarieId == null || salarieId.isBlank()) {
                log.warn("[" + scenario + "] créneau id='{}' : repos hebdomadaire sans ressourceAffecteeId — "
                        + "impossible de savoir de qui c'est le repos, marqueur ignoré", dto.getId());
                creneauxSignales.add(CreneauIgnoreDTO.exclu(
                        dto.getId(), dto.getDate(), MotifCreneauIgnore.MARQUEUR_REPOS_NON_RATTACHE,
                        "Repos hebdomadaire sans ressourceAffecteeId — impossible de savoir de qui "
                                + "c'est le repos. Marqueur écarté, et donc absent du planning restitué."));
                continue;
            }
            Ressource ressource = ressourcesParId.get(salarieId);
            if (!(ressource instanceof SalarieReel)) {
                log.warn("[" + scenario + "] créneau id='{}' : repos hebdomadaire rattaché à '{}', absent du "
                        + "dataset ou non salarié — marqueur ignoré", dto.getId(), salarieId);
                creneauxSignales.add(CreneauIgnoreDTO.exclu(
                        dto.getId(), dto.getDate(), MotifCreneauIgnore.MARQUEUR_REPOS_NON_RATTACHE,
                        "Repos hebdomadaire rattaché à '" + salarieId + "', absent du dataset ou non "
                                + "salarié. Marqueur écarté, et donc absent du planning restitué."));
                continue;
            }

            Creneau marqueur = creneauMapper.toCreneau(dto);
            marqueur.setRessourceAffectee(ressource);
            marqueurs.add(marqueur);
        }

        List<String> salarieIds = ressources.stream()
                .filter(SalarieReel.class::isInstance)
                .map(Ressource::getId)
                .filter(Objects::nonNull)
                .toList();

        List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                salarieIds,
                CalendrierReposHebdomadaire.depuisLesMarqueurs(marqueurs, referentiel),
                horizon);

        log.info("[" + scenario + "] repos hebdomadaire : {} marqueur(s) déclaré(s), {} jour(s) de repos au "
                + "calendrier pour {} salarié(s)", marqueurs.size(), calendrier.size(), salarieIds.size());

        return new ReposPrepares(marqueurs, calendrier);
    }

    /**
     * Signale le travail déjà posé sur un repos dominical par le planning transmis (équité L0).
     *
     * <p>Seuls les créneaux <strong>épinglés</strong> sont concernés : ce sont les seuls que le
     * solveur ne peut pas défaire, et donc les seuls dont la violation ne lui est pas imputable.
     * {@code ReposDominicalInviolable} interdit de <em>créer</em> la situation ; cette alerte dit
     * qu'elle <em>existe déjà</em>.</p>
     *
     * <p>Sans objet pour les scénarios qui n'épinglent rien — SC-01 et SC-03 n'ont pas de planning
     * existant à préserver, donc rien à constater.</p>
     */
    private static void signalerReposDominicalDejaTravaille(List<Creneau> creneaux,
                                                            List<ReposHebdomadaire> calendrier,
                                                            CollecteurAlertes alertes) {
        Set<String> dominicaux = new LinkedHashSet<>();
        for (ReposHebdomadaire repos : calendrier) {
            // Déclaré seulement — le repli fait de tout dimanche un RHD, et signaler chaque
            // dimanche travaillé du planning transmis noierait le constat sous le bruit.
            if (repos.getNature() == QualificationJour.RHD && repos.estDeclare()) {
                dominicaux.add(repos.getSalarieId() + "@" + repos.getDate());
            }
        }
        if (dominicaux.isEmpty()) {
            return;
        }

        for (Creneau creneau : creneaux) {
            if (!creneau.isFige()
                    || !(creneau.getRessourceAffectee() instanceof SalarieReel salarie)
                    || Boolean.TRUE.equals(creneau.getEstSegmentDePause())) {
                continue;
            }
            if (dominicaux.contains(salarie.getId() + "@" + creneau.getDate())) {
                alertes.signaler(AlertCode.REPOS_DOMINICAL_TRAVAILLE, AlertSeverity.WARNING,
                        creneau.getDate(),
                        "Le planning transmis fait travailler '" + salarie.getId() + "' le jour de "
                                + "son repos dominical (créneau '" + creneau.getId() + "'). Le "
                                + "moteur n'a pas défait cet existant — il est épinglé — mais il "
                                + "refuse d'en créer de semblable.");
            }
        }
    }

    /**
     * Retourne {@code true} si au moins une ressource du dataset peut potentiellement
     * couvrir le créneau d'après son activité déclarée.
     *
     * Règle : une ressource avec une liste d'activités vide ou nulle est considérée
     * comme non contrainte (peut couvrir toute activité). Une ressource contrainte
     * doit déclarer explicitement le code activité du créneau.
     *
     * Ce contrôle est structurel et pré-résolution — il ne fait intervenir ni le solveur
     * ni les contraintes OptaPlanner.
     */
    private boolean auMoinsUneRessourceCompatible(
            CreneauInputDTO creneau,
            List<SalarieInputDTO> salaries,
            List<PosteVirtuelInputDTO> postesVirtuels) {

        String activiteCode = creneau.getCodeActiviteEffectif();

        for (SalarieInputDTO sal : salaries) {
            Set<String> acts = sal.getActivitesCompatibles();
            if (acts == null || acts.isEmpty() || acts.contains(activiteCode)) return true;
        }
        for (PosteVirtuelInputDTO pv : postesVirtuels) {
            Set<String> acts = pv.getActivitesCompatibles();
            if (acts == null || acts.isEmpty() || acts.contains(activiteCode)) return true;
        }
        return false;
    }
}
