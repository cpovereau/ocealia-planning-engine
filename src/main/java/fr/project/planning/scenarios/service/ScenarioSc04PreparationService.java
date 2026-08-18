package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.workmetrics.TrancheTemporelle;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsParTranche;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.dto.Sc04ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.dto.request.Sc04ScenarioParametersDTO;
import fr.project.planning.solution.PlanningProblem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ScenarioSc04PreparationService — prépare l'optimisation d'un planning existant.
 *
 * <h3>Le principe, en une phrase</h3>
 * <p>Ce qui précède la <strong>date pivot</strong> est épinglé ; ce qui la suit est rendu au
 * solveur. C'est la mécanique de SC-02 et SC-05 — libérer, épingler le reste, laisser le score
 * décider — avec un déclencheur qui n'en est pas un : SC-04 ne réagit à rien, il relit une période.
 * </p>
 *
 * <h3>Le passé ne se réécrit pas, même quand il est vide</h3>
 * <p>Un créneau antérieur au pivot que <strong>personne n'avait couvert</strong> est un cas qu'on
 * pourrait croire sans importance. Le laisser libre reviendrait pourtant à laisser le solveur y
 * placer quelqu'un : inventer une histoire qui n'a pas eu lieu. Il est donc épinglé sur
 * {@link RessourceNonAffectee}, ce qui dit exactement ce qui s'est passé — ce besoin n'a été
 * couvert par personne, et il ne le sera pas rétroactivement.</p>
 *
 * <h3>Ce que la préparation ne décide pas</h3>
 * <p>Elle rouvre ; elle n'optimise pas. C'est le <strong>score</strong> qui répartit — les mêmes
 * contraintes que pour tous les autres scénarios, aux mêmes poids. §5.6 : la pondération n'est pas
 * réglable par l'appelant, et à poids fixes SC-04 reste SC-04.</p>
 */
@Service
public class ScenarioSc04PreparationService {

    private final ScenarioDatasetPreparationService preparationCommune;
    private final WorkMetricsParTranche metriquesParTranche = new WorkMetricsParTranche();

    public ScenarioSc04PreparationService(ScenarioDatasetPreparationService preparationCommune) {
        this.preparationCommune = preparationCommune;
    }

    public PreparedSc04Scenario prepare(Sc04ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        if (!"SC-04".equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-04 est supporté par cet endpoint.");
        }
        if (request.getDataSet() == null) {
            throw new IllegalArgumentException("dataSet est requis.");
        }
        Sc04ScenarioParametersDTO parametres = request.getScenarioParameters();
        if (parametres == null || parametres.getDatePivot() == null) {
            throw new IllegalArgumentException(
                    "[SC-04] scenarioParameters.datePivot est requis : sans elle, le moteur ne sait "
                            + "pas ce qui a le droit de bouger, et rouvrir tout un planning "
                            + "existant n'est jamais ce qu'on lui demande par défaut.");
        }
        LocalDate datePivot = parametres.getDatePivot();

        CollecteurAlertes alertes = new CollecteurAlertes("SC-04");

        Set<String> creneauxFiges = new LinkedHashSet<>();
        Set<String> creneauxAjustables = new LinkedHashSet<>();
        Map<String, String> ressourceAvantParCreneau = new LinkedHashMap<>();

        PolitiqueAffectationCreneau politique = (dto, creneau, ressourcesParId) -> {
            String ressourceId = dto.getRessourceAffecteeId();
            boolean duPasse = creneau.getDate() != null && creneau.getDate().isBefore(datePivot);

            if (ressourceId != null && !ressourceId.isBlank() && !duPasse) {
                // Qui tenait quoi avant. Un créneau rouvert perd son affectation en devenant une
                // variable de décision : sans cette carte, la réponse ne pourrait dire que
                // l'après, alors que SC-04 doit expliciter gains ET régressions.
                ressourceAvantParCreneau.put(creneau.getId(), ressourceId);
            }

            if (!duPasse) {
                creneauxAjustables.add(creneau.getId());
                return;
            }

            creneauxFiges.add(creneau.getId());

            if (ressourceId == null || ressourceId.isBlank()) {
                creneau.figerSur(RessourceNonAffectee.INSTANCE);
                return;
            }

            Ressource ressource = ressourcesParId.get(ressourceId);
            if (ressource == null) {
                throw new IllegalArgumentException(
                        "[SC-04] créneau id='" + dto.getId() + "' : ressource affectée introuvable "
                                + "dans le dataset : " + ressourceId);
            }
            creneau.figerSur(ressource);
        };

        PreparedDatasetScenario base = preparationCommune.preparer(request, "SC-04", politique);

        signalerPivotSansEffet(datePivot, creneauxAjustables, creneauxFiges, alertes);

        PlanningRequest problemeSoumis = base.planningRequest();

        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> metriquesAvant =
                mesurerAvant(problemeSoumis, ressourceAvantParCreneau);

        List<ScenarioAlertDTO> toutesLesAlertes = new ArrayList<>(base.alerts());
        toutesLesAlertes.addAll(alertes.versDto());

        return new PreparedSc04Scenario(
                base, problemeSoumis, datePivot, creneauxFiges, creneauxAjustables,
                ressourceAvantParCreneau, metriquesAvant, toutesLesAlertes);
    }

    /**
     * La charge de chacun <strong>avant</strong> optimisation, tranche par tranche.
     *
     * <h3>Pourquoi les créneaux sont modifiés puis remis en l'état</h3>
     * <p>Le calculateur lit une solution, et l'état d'avant n'existe nulle part sous cette forme :
     * les créneaux rouverts ont perdu leur affectation en devenant des variables de décision. On la
     * leur rend le temps de la mesure, puis on la retire — même mécanique qu'au lot A2 de SC-05, et
     * pour la même raison : <strong>mesurer ne doit pas modifier le problème</strong>. Les rendre au
     * solveur avec cette affectation lui donnerait un point de départ qui dépendrait d'un calcul de
     * restitution, et un couplage entre la mesure et la recherche n'a pas à exister.</p>
     *
     * <p>Le découpage vient de {@code WorkMetricsParTranche}, donc du calculateur de production :
     * l'avant et l'après passent par le même chemin, sans quoi une différence de méthode se lirait
     * comme un mouvement du planning.</p>
     */
    private Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> mesurerAvant(
            PlanningRequest requete, Map<String, String> ressourceAvantParCreneau) {

        Map<String, Ressource> parId = new LinkedHashMap<>();
        for (Ressource r : requete.ressources()) {
            if (r.getId() != null) {
                parId.putIfAbsent(r.getId(), r);
            }
        }

        List<Creneau> aRestaurer = new ArrayList<>();
        for (Creneau creneau : requete.creneaux()) {
            // Les créneaux figés portent déjà leur titulaire d'avant : seuls les rouverts sont nus.
            if (creneau.getRessourceAffectee() != null) {
                continue;
            }
            Ressource avant = parId.get(ressourceAvantParCreneau.get(creneau.getId()));
            if (avant != null) {
                creneau.setRessourceAffectee(avant);
                aRestaurer.add(creneau);
            }
        }

        PlanningProblem etatAvant = new PlanningProblem(
                requete.planningContext(),
                requete.regulatoryParameters(),
                requete.referentielComptabiliteActivite(),
                requete.ressources(),
                requete.creneaux(),
                requete.indisponibilites());
        etatAvant.setReposHebdomadaires(requete.reposHebdomadaires());

        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> mesure =
                metriquesParTranche.calculer(etatAvant);

        for (Creneau creneau : aRestaurer) {
            creneau.setRessourceAffectee(null);
        }
        return mesure;
    }

    /**
     * Le pivot laisse-t-il quelque chose à faire, et laisse-t-il quelque chose debout ?
     *
     * <p>Les deux bords sont acceptés — <em>le moteur ne refuse pas</em> — mais aucun n'est ce que
     * l'appelant croit demander. Un pivot postérieur à l'horizon rend une optimisation qui ne peut
     * rien changer ; un pivot antérieur à son début rouvre le planning entier, quand SC-04 promet
     * précisément d'améliorer <strong>sans reconstruire</strong>.</p>
     */
    private static void signalerPivotSansEffet(LocalDate datePivot,
                                               Set<String> ajustables,
                                               Set<String> figes,
                                               CollecteurAlertes alertes) {
        if (ajustables.isEmpty()) {
            alertes.signaler(AlertCode.AUCUN_CRENEAU_AJUSTABLE, AlertSeverity.WARNING,
                    "La date pivot " + datePivot + " est postérieure à tous les créneaux transmis : "
                            + "rien n'est ajustable, et l'optimisation ne peut rien changer. Le "
                            + "planning est rendu tel quel, avec ses indicateurs.");
            return;
        }
        if (figes.isEmpty()) {
            alertes.signaler(AlertCode.PLANNING_ENTIEREMENT_REOUVERT, AlertSeverity.WARNING,
                    "La date pivot " + datePivot + " est antérieure à tous les créneaux transmis : "
                            + "le planning est rouvert en entier. SC-04 améliore normalement un "
                            + "planning existant sans le reconstruire — un pivot dans la période "
                            + "évite un remaniement que personne n'a demandé.");
        }
    }
}
