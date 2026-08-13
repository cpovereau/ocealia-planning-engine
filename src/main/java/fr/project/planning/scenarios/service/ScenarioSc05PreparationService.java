package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.contexte.PerimetreArbitre;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.dto.Sc05ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.dto.request.Sc05ScenarioParametersDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ScenarioSc05PreparationService — prépare l'arbitrage d'un périmètre entre deux salariés.
 *
 * <h3>Le principe, en une phrase</h3>
 * <p>Seuls les créneaux <strong>du périmètre désigné</strong> et <strong>tenus par l'un des
 * salariés de l'arbitrage</strong> sont rendus au solveur. Tout le reste du planning transmis est
 * épinglé : le solveur le voit, les contraintes le mesurent, mais rien d'autre ne bouge.</p>
 *
 * <p>C'est la mécanique de SC-02 — libérer, épingler le reste, laisser le solveur décider — avec un
 * déclencheur différent : SC-02 réagit à une absence, SC-05 à un déséquilibre.</p>
 *
 * <h3>Le périmètre est transmis, jamais déduit</h3>
 * <p>§5.1 du cadrage. Le moteur ne fabrique pas le périmètre d'un arbitrage : un périmètre déduit
 * trop large déplacerait des créneaux que personne ne voulait bouger ; trop étroit, il rendrait
 * l'arbitrage sans effet.</p>
 *
 * <h3>Ce que la préparation ne décide pas</h3>
 * <p>Elle libère ; elle ne répartit pas. C'est le <strong>score</strong> qui arbitre — la contrainte
 * d'équité du lot L5 — exactement comme pour SC-02. Et c'est la contrainte HARD du lot A0 qui
 * garantit que les créneaux libérés ne partiront pas ailleurs.</p>
 */
@Service
public class ScenarioSc05PreparationService {

    private final ScenarioDatasetPreparationService preparationCommune;

    public ScenarioSc05PreparationService(ScenarioDatasetPreparationService preparationCommune) {
        this.preparationCommune = preparationCommune;
    }

    public PreparedSc05Scenario prepare(Sc05ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        if (!"SC-05".equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-05 est supporté par cet endpoint.");
        }
        if (request.getDataSet() == null) {
            throw new IllegalArgumentException("dataSet est requis.");
        }
        Sc05ScenarioParametersDTO parametres = request.getScenarioParameters();
        if (parametres == null) {
            throw new IllegalArgumentException("[SC-05] scenarioParameters est requis.");
        }

        Set<String> autorisees = parametres.ressourcesAutorisees();
        if (autorisees.size() < 2) {
            // Arbitrer quelqu'un avec lui-même n'a pas d'objet, et le moteur y répondrait par une
            // répartition inchangée que l'appelant lirait comme un refus silencieux.
            throw new IllegalArgumentException(
                    "[SC-05] salarieAId et salarieBId doivent désigner deux salariés distincts : "
                            + "un arbitrage se fait entre deux personnes.");
        }

        Set<String> perimetreDemande = new LinkedHashSet<>();
        for (String id : parametres.getCreneauxArbitres()) {
            if (id != null && !id.isBlank()) {
                perimetreDemande.add(id);
            }
        }
        if (perimetreDemande.isEmpty()) {
            throw new IllegalArgumentException(
                    "[SC-05] scenarioParameters.creneauxArbitres ne porte aucun identifiant "
                            + "exploitable : le périmètre d'un arbitrage est transmis, jamais déduit.");
        }

        CollecteurAlertes alertes = new CollecteurAlertes("SC-05");

        Set<String> creneauxLiberes = new LinkedHashSet<>();
        Set<String> tenusParUnTiers = new LinkedHashSet<>();
        Set<String> besoinsRencontres = new LinkedHashSet<>();

        // [A2] Qui tenait quoi avant l'arbitrage. Un créneau libéré perd son affectation en
        //      devenant une variable de décision : sans cette carte, l'état d'avant serait perdu
        //      et la réponse ne pourrait dire que l'après.
        Map<String, String> ressourceAvantParCreneau = new LinkedHashMap<>();

        // La politique : ce que SC-05 fait de l'affectation portée par chaque créneau d'entrée.
        PolitiqueAffectationCreneau politique = (dto, creneau, ressourcesParId) -> {
            String besoin = creneau.getIdBesoin();
            boolean duPerimetre = perimetreDemande.contains(besoin);
            if (duPerimetre) {
                besoinsRencontres.add(besoin);
            }

            String ressourceId = dto.getRessourceAffecteeId();
            if (duPerimetre && ressourceId != null && !ressourceId.isBlank()) {
                ressourceAvantParCreneau.put(creneau.getId(), ressourceId);
            }

            // Un créneau sans affectation est un besoin nu : il l'était déjà, il le reste. S'il est
            // du périmètre, l'arbitrage le couvre — c'est du travail à répartir comme un autre.
            if (ressourceId == null || ressourceId.isBlank()) {
                if (duPerimetre) {
                    creneauxLiberes.add(besoin);
                }
                return;
            }

            Ressource ressource = ressourcesParId.get(ressourceId);
            if (ressource == null) {
                throw new IllegalArgumentException(
                        "[SC-05] créneau id='" + dto.getId() + "' : ressource affectée introuvable "
                                + "dans le dataset : " + ressourceId);
            }

            if (!duPerimetre) {
                // Hors du périmètre, rien ne bouge — y compris les autres créneaux de A et de B.
                // Le planning complet est exigé pour que les bornes hebdomadaires soient
                // vérifiables, pas pour être remanié.
                creneau.figerSur(ressource);
                return;
            }

            if (autorisees.contains(ressourceId)) {
                // Le cas nominal : ce créneau est l'objet même de l'arbitrage.
                creneauxLiberes.add(besoin);
                return;
            }

            if (!(ressource instanceof SalarieReel)) {
                // Un poste virtuel n'est le travail de personne : c'est une capacité que le
                // planning n'a pas encore attribuée. §5.2 protège un tiers, pas un emplacement
                // vide — et l'épingler rendrait sans effet un créneau que l'appelant a désigné.
                creneauxLiberes.add(besoin);
                return;
            }

            // §5.2 — ni refus, ni reprise. Le tiers n'a rien demandé.
            tenusParUnTiers.add(creneau.getId());
            creneau.figerSur(ressource);
        };

        PreparedDatasetScenario base = preparationCommune.preparer(request, "SC-05", politique);

        signalerSalariesIntrouvables(base, autorisees, alertes);
        signalerCreneauxIntrouvables(perimetreDemande, besoinsRencontres, alertes);
        signalerCreneauxTenusParUnTiers(tenusParUnTiers, alertes);

        PerimetreArbitre perimetre = new PerimetreArbitre(perimetreDemande, autorisees);
        PlanningRequest problemeSoumis = avecLePerimetre(base.planningRequest(), perimetre);

        // [A2] Mesuré avant que le solveur ne touche à quoi que ce soit.
        Map<String, WorkMetrics> metriquesAvant =
                mesurerAvant(problemeSoumis, ressourceAvantParCreneau);

        List<ScenarioAlertDTO> toutesLesAlertes = new ArrayList<>(base.alerts());
        toutesLesAlertes.addAll(alertes.versDto());

        return new PreparedSc05Scenario(
                base, problemeSoumis, autorisees, creneauxLiberes, tenusParUnTiers,
                besoinsRencontres, ressourceAvantParCreneau, metriquesAvant, toutesLesAlertes);
    }

    /**
     * [A2] Les indicateurs de chacun <strong>avant</strong> l'arbitrage.
     *
     * <h3>Pourquoi le même calculateur, et pas un calcul dédié</h3>
     * <p>L'avant et l'après servent à être <strong>comparés</strong> : « SAL-A passe de −8,6 % à
     * +2,9 % ». Deux calculs différents feraient passer une divergence de méthode pour un
     * mouvement — et la pondération par la pénibilité, la fenêtre d'observation et le filtre
     * {@code compteDansCharge} offrent trois occasions de diverger. {@link WorkMetricsCalculator}
     * est donc appelé deux fois sur deux états, jamais réécrit.</p>
     *
     * <h3>Pourquoi les créneaux sont modifiés puis remis en l'état</h3>
     * <p>Le calculateur lit une solution, et l'état d'avant n'existe nulle part sous cette forme :
     * les créneaux libérés ont perdu leur affectation en devenant des variables de décision. On la
     * leur rend le temps de la mesure, puis on la retire. La fenêtre est étroite — ces créneaux
     * n'ont pas encore été confiés au solveur — et l'alternative, remapper tout le dataset une
     * seconde fois, coûterait davantage pour le même résultat.</p>
     *
     * <p>⚠️ La remise en l'état garantit que <strong>mesurer ne modifie pas le problème</strong> :
     * les rendre au solveur avec cette affectation lui donnerait un point de départ différent de
     * celui que le lot A1 définit, et ce point de départ dépendrait alors d'un calcul de
     * restitution. Le résultat n'en changerait pas forcément — sur le jeu d'essai du projet il ne
     * change pas, et <em>aucun test bout-en-bout ne discrimine donc cette ligne</em> —, mais un
     * couplage entre la mesure et la recherche n'a pas à exister. C'est
     * {@code ScenarioSc05PreparationServiceTest} qui le vérifie, en constatant directement l'état
     * du créneau libéré au sortir de la préparation.</p>
     */
    private static Map<String, WorkMetrics> mesurerAvant(PlanningRequest requete,
                                                         Map<String, String> ressourceAvantParCreneau) {
        Map<String, Ressource> parId = new LinkedHashMap<>();
        for (Ressource r : requete.ressources()) {
            if (r.getId() != null) {
                parId.putIfAbsent(r.getId(), r);
            }
        }

        List<Creneau> aRestaurer = new ArrayList<>();
        for (Creneau creneau : requete.creneaux()) {
            // Les créneaux épinglés portent déjà leur titulaire d'avant : seuls les libérés sont nus.
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

        Map<String, WorkMetrics> metriques = new LinkedHashMap<>();
        new WorkMetricsCalculator().compute(etatAvant)
                .forEach((ressource, wm) -> metriques.put(ressource.getId(), wm));

        for (Creneau creneau : aRestaurer) {
            creneau.setRessourceAffectee(null);
        }
        return metriques;
    }

    /**
     * Un salarié de l'arbitrage absent du dataset ne peut rien recevoir.
     *
     * <p>Le scénario s'exécute — il ne refuse pas — mais l'arbitrage se fait alors entre moins de
     * personnes que demandé. Même lecture que {@code SALARIE_ABSENT_INTROUVABLE} pour SC-02.</p>
     */
    private static void signalerSalariesIntrouvables(PreparedDatasetScenario base,
                                                     Set<String> autorisees,
                                                     CollecteurAlertes alertes) {
        Set<String> connus = new LinkedHashSet<>();
        for (Ressource r : base.planningRequest().ressources()) {
            if (r instanceof SalarieReel && r.getId() != null) {
                connus.add(r.getId());
            }
        }
        for (String id : autorisees) {
            if (!connus.contains(id)) {
                alertes.signaler(AlertCode.SALARIE_ARBITRE_INTROUVABLE, AlertSeverity.ERROR,
                        "Le salarié '" + id + "' n'est pas dans le dataset : aucun créneau du "
                                + "périmètre ne peut lui revenir, et l'arbitrage se fait sans lui.");
            }
        }
    }

    /**
     * Le périmètre désigne des créneaux que le problème ne contient pas.
     *
     * <p>Soit ils sont absents du dataset, soit ils en ont été écartés avant le solveur — activité
     * inconnue, date hors horizon, marqueur de repos. Dans les trois cas l'arbitrage porte sur
     * moins que ce qui a été demandé, et le taire le rendrait inexplicable.</p>
     */
    private static void signalerCreneauxIntrouvables(Set<String> perimetreDemande,
                                                     Set<String> besoinsRencontres,
                                                     CollecteurAlertes alertes) {
        Set<String> manquants = new LinkedHashSet<>(perimetreDemande);
        manquants.removeAll(besoinsRencontres);
        if (manquants.isEmpty()) {
            return;
        }
        alertes.signaler(AlertCode.CRENEAU_ARBITRE_INTROUVABLE, AlertSeverity.WARNING,
                manquants.size() + " créneau(x) du périmètre sont absents du problème soumis au "
                        + "solveur — absents du dataset, ou écartés avant résolution : "
                        + String.join(", ", manquants) + ". L'arbitrage porte sur les autres.");
    }

    /** §5.2 — l'arbitrage porte sur ce qui reste, et l'appelant apprend ce qui a été écarté. */
    private static void signalerCreneauxTenusParUnTiers(Set<String> tenusParUnTiers,
                                                        CollecteurAlertes alertes) {
        if (tenusParUnTiers.isEmpty()) {
            return;
        }
        alertes.signaler(AlertCode.CRENEAU_ARBITRE_TENU_PAR_UN_TIERS, AlertSeverity.WARNING,
                tenusParUnTiers.size() + " créneau(x) du périmètre sont tenus par quelqu'un qui "
                        + "n'est pas de l'arbitrage : " + String.join(", ", tenusParUnTiers)
                        + ". Ils sont épinglés — on ne retire pas son travail à un tiers pour "
                        + "équilibrer deux autres personnes — et l'arbitrage porte sur ce qui reste.");
    }

    /** Rattache le périmètre d'arbitrage au problème, sans rien changer d'autre. */
    private static PlanningRequest avecLePerimetre(PlanningRequest requete,
                                                   PerimetreArbitre perimetre) {
        return new PlanningRequest(
                requete.planningContext(),
                requete.regulatoryParameters(),
                requete.referentielComptabiliteActivite(),
                requete.ressources(),
                requete.creneaux(),
                requete.indisponibilites(),
                requete.reposHebdomadaires(),
                requete.seuilsSurcharge(),
                perimetre);
    }
}
