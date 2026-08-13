package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.dto.Sc02ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ScenarioSc02PreparationService — prépare le remplacement d'un salarié absent.
 *
 * <h3>Le principe, en une phrase</h3>
 * <p>Seuls les créneaux du salarié absent qui tombent pendant son absence sont rendus au solveur.
 * <strong>Tout le reste du planning transmis est épinglé</strong> : le solveur le voit, les
 * contraintes le mesurent, mais aucune affectation existante ne peut être déplacée pour faire de
 * la place.</p>
 *
 * <p>Le prix est assumé : le moteur répondra « à pourvoir » là où un échange entre deux collègues
 * aurait suffi. C'est voulu — remanier le planning des présents est une décision d'encadrement,
 * pas une décision de moteur. Voir {@code 92_CADRAGE_SCENARIO_SC-02.md} §3 et §4.1.</p>
 *
 * <h3>D'où vient l'absence</h3>
 * <p>De {@code dataSet.indisponibilites}, déjà au contrat et déjà tenu par une contrainte HARD.
 * {@code salarieAbsentId} ne fait que désigner <em>de qui</em> il s'agit — aucun champ nouveau
 * n'était nécessaire pour décrire l'absence elle-même.</p>
 */
@Service
public class ScenarioSc02PreparationService {

    private final ScenarioDatasetPreparationService preparationCommune;
    private final ScenarioResourceMapper resourceMapper;

    public ScenarioSc02PreparationService(ScenarioDatasetPreparationService preparationCommune,
                                          ScenarioResourceMapper resourceMapper) {
        this.preparationCommune = preparationCommune;
        this.resourceMapper = resourceMapper;
    }

    public PreparedSc02Scenario prepare(Sc02ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        if (!"SC-02".equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-02 est supporté par cet endpoint.");
        }
        if (request.getScenarioParameters() == null
                || request.getScenarioParameters().getSalarieAbsentId() == null
                || request.getScenarioParameters().getSalarieAbsentId().isBlank()) {
            throw new IllegalArgumentException(
                    "[SC-02] scenarioParameters.salarieAbsentId est requis : sans lui, le moteur ne "
                            + "sait pas de quelle absence il doit tirer les conséquences.");
        }
        if (request.getDataSet() == null) {
            throw new IllegalArgumentException("dataSet est requis.");
        }

        String salarieAbsentId = request.getScenarioParameters().getSalarieAbsentId();
        CollecteurAlertes alertes = new CollecteurAlertes("SC-02");

        // 1. Les périodes d'absence du salarié désigné, telles que le dataset les déclare.
        List<Indisponibilite> absences = resourceMapper
                .toIndisponibilites(request.getDataSet().getIndisponibilites())
                .stream()
                .filter(i -> salarieAbsentId.equals(i.getRessourceId()))
                .toList();

        if (absences.isEmpty()) {
            alertes.signaler(AlertCode.AUCUNE_ABSENCE_DECLAREE, AlertSeverity.WARNING,
                    "Le salarié '" + salarieAbsentId + "' ne porte aucune indisponibilité au "
                            + "dataset : le moteur ne sait pas quand il est absent et ne libère "
                            + "aucun créneau. Un vide ne vaut pas absence sur tout l'horizon.");
        }

        // 2. La politique : ce que SC-02 fait de l'affectation portée par chaque créneau d'entrée.
        Set<String> creneauxLiberes = new LinkedHashSet<>();
        PolitiqueAffectationCreneau politique = (dto, creneau, ressourcesParId) -> {
            String ressourceId = dto.getRessourceAffecteeId();

            // Un créneau sans affectation est un besoin nu : il l'était déjà, il le reste.
            if (ressourceId == null || ressourceId.isBlank()) {
                return;
            }

            if (salarieAbsentId.equals(ressourceId) && pendantUneAbsence(creneau, absences)) {
                creneauxLiberes.add(creneau.getId());
                return;
            }

            Ressource ressource = ressourcesParId.get(ressourceId);
            if (ressource == null) {
                throw new IllegalArgumentException(
                        "[SC-02] créneau id='" + dto.getId() + "' : ressource affectée introuvable "
                                + "dans le dataset : " + ressourceId);
            }
            creneau.figerSur(ressource);
        };

        PreparedDatasetScenario base = preparationCommune.preparer(request, "SC-02", politique);

        // 3. Constats que seul SC-02 peut faire, une fois le problème construit.
        boolean salarieConnu = base.planningRequest().ressources().stream()
                .anyMatch(r -> r instanceof SalarieReel && salarieAbsentId.equals(r.getId()));
        if (!salarieConnu) {
            alertes.signaler(AlertCode.SALARIE_ABSENT_INTROUVABLE, AlertSeverity.ERROR,
                    "Le salarié '" + salarieAbsentId + "' n'est pas dans le dataset : aucun créneau "
                            + "ne peut lui être repris.");
        } else if (creneauxLiberes.isEmpty() && !absences.isEmpty()) {
            alertes.signaler(AlertCode.AUCUN_CRENEAU_A_REMPLACER, AlertSeverity.WARNING,
                    "L'absence de '" + salarieAbsentId + "' ne recouvre aucun de ses créneaux : "
                            + "il n'y a rien à remplacer.");
        }

        // 4. [S2] Découpage des créneaux libérés aux frontières de disponibilité.
        PlanningRequest apresDecoupage = decouper(base.planningRequest(), creneauxLiberes,
                salarieAbsentId, alertes);

        // 5. Le poste virtuel ne s'invite jamais de lui-même.
        PlanningRequest problemeSoumis = request.getScenarioParameters().estPosteVirtuelAutorise()
                ? apresDecoupage
                : sansPostesVirtuels(apresDecoupage, alertes);

        List<ScenarioAlertDTO> toutesLesAlertes = new ArrayList<>(base.alerts());
        toutesLesAlertes.addAll(alertes.versDto());

        return new PreparedSc02Scenario(
                base, problemeSoumis, salarieAbsentId, creneauxLiberes, toutesLesAlertes);
    }

    /**
     * Remplace chaque créneau libéré par ses segments, coupés aux frontières de disponibilité des
     * remplaçants possibles (lot S2).
     *
     * <p>Un créneau qu'aucune frontière ne traverse est laissé <strong>tel quel</strong> : il
     * garde son identifiant, et le cas courant — un remplaçant prend la journée entière — reste
     * exactement ce qu'il était au lot S1.</p>
     */
    private static PlanningRequest decouper(PlanningRequest requete,
                                            Set<String> creneauxLiberes,
                                            String salarieAbsentId,
                                            CollecteurAlertes alertes) {
        if (creneauxLiberes.isEmpty()) {
            return requete;
        }

        List<Creneau> apres = new ArrayList<>(requete.creneaux().size());
        int decoupes = 0;
        int segments = 0;

        for (Creneau creneau : requete.creneaux()) {
            if (!creneauxLiberes.contains(creneau.getId())) {
                apres.add(creneau);
                continue;
            }
            List<Creneau> morceaux = DecoupageAuxFrontieres.decouper(
                    creneau, requete.creneaux(), requete.indisponibilites(), salarieAbsentId);
            if (morceaux.size() > 1) {
                decoupes++;
                segments += morceaux.size();
            }
            apres.addAll(morceaux);
        }

        if (decoupes > 0) {
            alertes.signaler(AlertCode.CRENEAUX_DECOUPES, AlertSeverity.INFO,
                    decoupes + " créneau(x) libéré(s) découpé(s) en " + segments + " segments, aux "
                            + "heures où la disponibilité des remplaçants change. Les segments "
                            + "contigus confiés à la même personne sont recombinés à la restitution.");
        }

        return new PlanningRequest(
                requete.planningContext(),
                requete.regulatoryParameters(),
                requete.referentielComptabiliteActivite(),
                requete.ressources(),
                apres,
                requete.indisponibilites(),
                requete.reposHebdomadaires());
    }

    /** Le créneau tombe-t-il, ne serait-ce qu'en partie, dans l'une des absences déclarées ? */
    private static boolean pendantUneAbsence(Creneau creneau, List<Indisponibilite> absences) {
        for (Indisponibilite absence : absences) {
            if (creneau.chevauchePeriode(absence.getDateDebut(), absence.getDateFin())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retire les postes virtuels du domaine de la variable de décision.
     *
     * <p>Un poste virtuel encore affecté par le planning existant est <strong>conservé</strong> :
     * l'existant ne se réécrit pas, et le créneau qui le porte est épinglé. L'appelant l'apprend
     * par une alerte plutôt que de le découvrir dans la réponse.</p>
     */
    private static PlanningRequest sansPostesVirtuels(PlanningRequest requete,
                                                      CollecteurAlertes alertes) {
        Set<String> virtuelsEnUsage = new LinkedHashSet<>();
        for (Creneau c : requete.creneaux()) {
            if (c.isFige() && c.getRessourceAffectee() instanceof PosteVirtuel pv) {
                virtuelsEnUsage.add(pv.getId());
            }
        }

        List<Ressource> retenues = requete.ressources().stream()
                .filter(r -> !(r instanceof PosteVirtuel) || virtuelsEnUsage.contains(r.getId()))
                .toList();

        if (retenues.size() == requete.ressources().size() && !virtuelsEnUsage.isEmpty()) {
            alertes.signaler(AlertCode.POSTE_VIRTUEL_REFUSE_MAIS_PRESENT, AlertSeverity.WARNING,
                    "Le poste virtuel n'est pas autorisé, mais le planning existant en affecte "
                            + virtuelsEnUsage.size() + " : conservé(s) pour ne pas réécrire "
                            + "l'existant, et donc encore mobilisable(s).");
        }

        return new PlanningRequest(
                requete.planningContext(),
                requete.regulatoryParameters(),
                requete.referentielComptabiliteActivite(),
                retenues,
                requete.creneaux(),
                requete.indisponibilites(),
                requete.reposHebdomadaires());
    }
}
