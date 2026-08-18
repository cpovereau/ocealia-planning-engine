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
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
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
 * <h3>Et une sélection, quand le pivot ne suffit pas</h3>
 * <p>{@code creneauxAjustables} restreint l'après-pivot aux créneaux désignés — lot O5, arbitrage
 * du 2026-08-18. La conjonction est une <strong>intersection</strong> : la liste ne peut que
 * rétrécir la zone remaniable, jamais l'étendre au passé. <em>La date pivot dit jusqu'où le moteur
 * a le droit d'aller, la liste dit ce qu'il a le droit de toucher à l'intérieur.</em> Absente, rien
 * ne change de ce qui précède.</p>
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

        // §5.5 — la sélection restreint l'après-pivot, elle ne rouvre jamais le passé. null =
        // aucune restriction ; ensemble vide = plus rien n'est ajustable. Deux demandes opposées
        // que « absent » et « vide » portent séparément, et que la préparation doit distinguer.
        Set<String> selection = parametres.selection();
        bornerLaSelection(selection, request, datePivot);

        CollecteurAlertes alertes = new CollecteurAlertes("SC-04");

        Set<String> creneauxFiges = new LinkedHashSet<>();
        Set<String> creneauxAjustables = new LinkedHashSet<>();
        Map<String, String> ressourceAvantParCreneau = new LinkedHashMap<>();
        Set<String> designesRencontres = new LinkedHashSet<>();
        Set<String> designesAvantPivot = new LinkedHashSet<>();
        Set<String> futursNonCouvertsGeles = new LinkedHashSet<>();

        PolitiqueAffectationCreneau politique = (dto, creneau, ressourcesParId) -> {
            String ressourceId = dto.getRessourceAffecteeId();
            boolean duPasse = creneau.getDate() != null && creneau.getDate().isBefore(datePivot);
            boolean designe = selection == null || selection.contains(creneau.getId());

            if (selection != null && designe) {
                designesRencontres.add(creneau.getId());
                if (duPasse) {
                    // Sans effet sous l'intersection, et souvent le signe d'un pivot mal placé.
                    designesAvantPivot.add(creneau.getId());
                }
            }

            // L'intersection, en une ligne : postérieur au pivot ET désigné.
            boolean ajustable = !duPasse && designe;

            if (ajustable && ressourceId != null && !ressourceId.isBlank()) {
                // Qui tenait quoi avant. Un créneau rouvert perd son affectation en devenant une
                // variable de décision : sans cette carte, la réponse ne pourrait dire que
                // l'après, alors que SC-04 doit expliciter gains ET régressions.
                ressourceAvantParCreneau.put(creneau.getId(), ressourceId);
            }

            if (ajustable) {
                creneauxAjustables.add(creneau.getId());
                return;
            }

            creneauxFiges.add(creneau.getId());

            if (ressourceId == null || ressourceId.isBlank()) {
                if (!duPasse) {
                    // Futur, découvert, non désigné : ne pas lister, c'est renoncer à couvrir. Le
                    // trou reste dans le planning mais sort de creneauxNonCouverts, qui ne compte
                    // que les ajustables — il faut donc le dire, sans quoi il devient invisible.
                    futursNonCouvertsGeles.add(creneau.getId());
                }
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

        signalerPivotSansEffet(datePivot, selection, creneauxAjustables, creneauxFiges, alertes);
        signalerSelectionIntrouvable(selection, designesRencontres, alertes);
        signalerSelectionAvantPivot(designesAvantPivot, datePivot, alertes);
        signalerFutursNonCouvertsGeles(futursNonCouvertsGeles, alertes);

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
                                               Set<String> selection,
                                               Set<String> ajustables,
                                               Set<String> figes,
                                               CollecteurAlertes alertes) {
        if (ajustables.isEmpty()) {
            // Même constat, trois causes possibles depuis O5 : les nommer distinctement évite à
            // l'appelant de corriger la date pivot quand c'est sa liste qui est en cause.
            String cause;
            if (selection == null) {
                cause = "La date pivot " + datePivot + " est postérieure à tous les créneaux "
                        + "transmis : ";
            } else if (selection.isEmpty()) {
                cause = "La liste creneauxAjustables a été transmise vide — ce qui demande de ne "
                        + "rien rouvrir, là où l'omettre aurait rouvert tout l'après-pivot : ";
            } else {
                cause = "Aucun des créneaux désignés par creneauxAjustables n'est postérieur à la "
                        + "date pivot " + datePivot + " : ";
            }
            alertes.signaler(AlertCode.AUCUN_CRENEAU_AJUSTABLE, AlertSeverity.WARNING,
                    cause + "rien n'est ajustable, et l'optimisation ne peut rien changer. Le "
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
    /**
     * La sélection tient-elle dans un mois ?
     *
     * <h3>Pourquoi un refus, quand le moteur ne refuse pas</h3>
     * <p>« Le moteur ne refuse pas » vaut pour ce qu'il ne sait pas <em>planifier</em> : une
     * demande impossible est rendue visible, pas rejetée. Une liste qui déborde le mois n'est pas
     * un planning impossible, c'est une requête <strong>mal formée</strong> — au même titre qu'une
     * {@code datePivot} absente. Tronquer en silence serait pire que refuser : l'appelant lirait un
     * résultat partiel comme un résultat complet.</p>
     *
     * <h3>Glissant, non calendaire</h3>
     * <p>La borne se lit du premier au dernier créneau désignés, et vaut un mois <em>glissant</em>
     * (métier, 2026-08-18) : un mois calendaire obligerait à scinder toute demande à cheval sur une
     * fin de mois, et produirait des contournements plutôt que de la clarté. Le découpage
     * calendaire de {@code DecoupageTemporel} est un autre objet — il dit comment on rend compte,
     * pas ce qu'on autorise à bouger.</p>
     *
     * <h3>Ce qui est mesuré : la zone remaniable, non la liste</h3>
     * <p>L'amplitude se lit sur les seuls créneaux désignés qui <strong>bougeront</strong> — donc
     * postérieurs au pivot et dans l'horizon. Un identifiant inconnu du dataset ne porte aucune
     * date ; un créneau désigné avant le pivot ou hors horizon ne bougera pas. Aucun des trois n'a
     * à gonfler l'amplitude, et chacun a déjà son alerte. Sans cette précision, un appelant qui
     * transmet un dataset de trois mois avec un horizon de deux semaines se verrait refuser une
     * demande que le moteur aurait de toute façon restreinte à deux semaines.</p>
     */
    private static void bornerLaSelection(Set<String> selection, Sc04ScenarioRequestDTO request,
                                          LocalDate datePivot) {
        if (selection == null || selection.isEmpty() || request.getDataSet().getCreneaux() == null) {
            return;
        }
        LocalDate finHorizon = request.getPlanningContext() == null
                || request.getPlanningContext().getHorizon() == null
                ? null : request.getPlanningContext().getHorizon().getDateFin();

        LocalDate premier = null;
        LocalDate dernier = null;
        for (CreneauInputDTO dto : request.getDataSet().getCreneaux()) {
            if (dto == null || dto.getDate() == null || !selection.contains(dto.getId())) {
                continue;
            }
            // Ce qui est borné, c'est la zone effectivement remaniable : un créneau désigné mais
            // antérieur au pivot ou hors horizon ne bougera pas, et n'a donc pas à gonfler
            // l'amplitude. Ses propres alertes le nomment déjà.
            if (dto.getDate().isBefore(datePivot)
                    || (finHorizon != null && dto.getDate().isAfter(finHorizon))) {
                continue;
            }
            if (premier == null || dto.getDate().isBefore(premier)) {
                premier = dto.getDate();
            }
            if (dernier == null || dto.getDate().isAfter(dernier)) {
                dernier = dto.getDate();
            }
        }
        if (premier == null || dernier.isBefore(premier.plusMonths(1))) {
            return;
        }
        throw new IllegalArgumentException(
                "[SC-04] scenarioParameters.creneauxAjustables ouvre une zone remaniable qui "
                        + "s'étale du " + premier + " au " + dernier + ", soit plus d'un mois. La zone remaniable est bornée à un mois "
                        + "glissant : on juge large, on ne remanie qu'étroit. Dernier jour "
                        + "désignable à partir du " + premier + " : "
                        + premier.plusMonths(1).minusDays(1) + ".");
    }

    /**
     * Les identifiants désignés que le dataset ne porte pas.
     *
     * <p>La sélection est transmise et jamais déduite : le moteur ne peut pas deviner ce que
     * l'appelant visait. Une liste dont la moitié des identifiants ne correspond à rien produirait
     * une optimisation bien plus étroite que demandée, sans que personne ne voie pourquoi.</p>
     */
    private static void signalerSelectionIntrouvable(Set<String> selection,
                                                     Set<String> rencontres,
                                                     CollecteurAlertes alertes) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        Set<String> manquants = new LinkedHashSet<>(selection);
        manquants.removeAll(rencontres);
        if (manquants.isEmpty()) {
            return;
        }
        alertes.signaler(AlertCode.CRENEAU_AJUSTABLE_INTROUVABLE, AlertSeverity.WARNING,
                manquants.size() + " créneau(x) désigné(s) par creneauxAjustables sont absents du "
                        + "dataset, ou en ont été écartés avant résolution : "
                        + String.join(", ", manquants) + ". Ils ne seront pas ajustés — "
                        + "l'optimisation porte sur un périmètre plus étroit que demandé.");
    }

    /**
     * Les identifiants désignés qui tombent avant le pivot.
     *
     * <p>La conjonction des deux champs est une <strong>intersection</strong> : ces créneaux
     * restent figés, et la demande est sans effet sur eux. Le moteur ne s'en offusque pas —
     * désigner un créneau passé est une erreur de bonne foi — mais il ne l'applique pas en
     * silence, car c'est le plus souvent le pivot qui est mal placé.</p>
     */
    private static void signalerSelectionAvantPivot(Set<String> avantPivot,
                                                    LocalDate datePivot,
                                                    CollecteurAlertes alertes) {
        if (avantPivot.isEmpty()) {
            return;
        }
        alertes.signaler(AlertCode.CRENEAU_AJUSTABLE_ANTERIEUR_AU_PIVOT, AlertSeverity.WARNING,
                avantPivot.size() + " créneau(x) désigné(s) par creneauxAjustables sont antérieurs "
                        + "à la date pivot " + datePivot + " et restent figés : la liste restreint "
                        + "l'après-pivot, elle ne rouvre pas le passé. Si ces créneaux devaient "
                        + "bouger, c'est la date pivot qu'il faut avancer.");
    }

    /**
     * Les besoins futurs que la sélection laisse gelés sans titulaire.
     *
     * <p>C'est la seule régression que la liste explicite peut introduire, et elle est sournoise :
     * toute la restitution se compte sur les créneaux ajustables, donc ces besoins
     * <strong>sortent du décompte {@code creneauxNonCouverts}</strong>. Le trou reste dans le
     * planning ; le compte cesse de le voir. Ne pas lister, ici, c'est renoncer à couvrir — une
     * décision recevable, qui doit se lire dans la réponse et non se découvrir sur le terrain.</p>
     */
    private static void signalerFutursNonCouvertsGeles(Set<String> geles,
                                                       CollecteurAlertes alertes) {
        if (geles.isEmpty()) {
            return;
        }
        alertes.signaler(AlertCode.CRENEAU_FUTUR_NON_COUVERT_GELE, AlertSeverity.WARNING,
                geles.size() + " créneau(x) postérieur(s) au pivot ne sont couverts par personne et "
                        + "ne figurent pas dans creneauxAjustables : ils restent découverts, et "
                        + "n'apparaissent pas dans optimisation.creneauxNonCouverts, qui ne compte "
                        + "que les créneaux ajustables. Les désigner permettrait au moteur de "
                        + "tenter de les pourvoir.");
    }
}
