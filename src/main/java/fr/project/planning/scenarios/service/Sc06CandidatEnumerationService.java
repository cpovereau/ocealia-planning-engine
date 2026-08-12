package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.dto.MotifCandidat;
import fr.project.planning.scenarios.dto.NatureCandidat;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.solution.PlanningProblem;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolutionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sc06CandidatEnumerationService — le cœur de SC-06 (lot S4).
 *
 * <h2>Énumération, pas optimisation</h2>
 * <p>SC-06 ne cherche pas <em>une</em> bonne solution : il <strong>classe</strong> les manières
 * de couvrir un besoin. Une recherche heuristique rendrait un résultat unique, non reproductible
 * et sans garantie de couverture. Ce service énumère au contraire les candidats éligibles et
 * évalue chacun par {@link SolutionManager#explain}, sans lancer aucune recherche.</p>
 *
 * <p>Il en résulte trois propriétés qu'aucun {@code solve()} n'offre : même entrée ⇒ même podium,
 * aucun candidat éligible oublié, et un motif attaché à chaque rang.</p>
 *
 * <h2>Mesure relative</h2>
 * <p>Un candidat n'est jamais jugé sur son score absolu, mais sur ce qu'il <strong>ajoute</strong>
 * à la situation de référence — le besoin non couvert. Le planning existant étant figé, il peut
 * porter des violations préexistantes : les imputer aux candidats les disqualifierait tous
 * indifféremment et rendrait l'indicateur inutilisable.</p>
 *
 * <h2>Trois passes</h2>
 * <ol>
 *   <li><strong>Mono-ressource</strong> — chaque salarié éligible prend tout le besoin ;</li>
 *   <li><strong>Composée</strong> — déclenchée seulement si la passe 1 ne suffit pas ;</li>
 *   <li><strong>Repli</strong> — poste virtuel, à défaut ressource à pourvoir.</li>
 * </ol>
 */
@Service
public class Sc06CandidatEnumerationService {

    private static final Logger log = LoggerFactory.getLogger(Sc06CandidatEnumerationService.class);

    /** Taille du podium — fixée au §4.3 du cadrage, non paramétrable. */
    public static final int NB_SOLUTIONS = 3;

    /** Options retenues par créneau lors de la composition. */
    private static final int MAX_OPTIONS_PAR_CRENEAU = 3;

    /** Combinaisons composées réellement évaluées. */
    private static final int MAX_COMBINAISONS_EVALUEES = 8;

    /** Garde-fou sur l'explosion combinatoire avant tout tri. */
    private static final int MAX_COMBINAISONS_GENEREES = 512;

    private final SolutionManager<PlanningProblem, HardSoftScore> solutionManager;

    public Sc06CandidatEnumerationService(SolutionManager<PlanningProblem, HardSoftScore> solutionManager) {
        this.solutionManager = solutionManager;
    }

    // =========================================================
    // Point d'entrée
    // =========================================================

    /**
     * Énumère, évalue et classe les manières de couvrir le besoin.
     *
     * @return au plus {@value #NB_SOLUTIONS} candidats, du plus favorable au moins favorable
     */
    public List<Candidat> enumerer(PreparedSc06Scenario prepared) {

        Evaluation reference = evaluer(prepared, Map.of());

        List<SalarieReel> eligiblesTousCreneaux = new ArrayList<>();
        for (SalarieReel salarie : prepared.salaries()) {
            if (estEligiblePourTout(salarie, prepared)) {
                eligiblesTousCreneaux.add(salarie);
            }
        }

        List<Candidat> candidats = new ArrayList<>(passeMonoRessource(prepared, reference, eligiblesTousCreneaux));

        long conformes = candidats.stream().filter(Candidat::conforme).count();
        if (conformes < NB_SOLUTIONS) {
            candidats.addAll(passeComposee(prepared, reference));
        }

        candidats.addAll(passeRepli(prepared, reference, candidats));

        candidats.sort(COMPARATEUR);

        if (candidats.isEmpty()) {
            log.warn("[SC-06] aucun candidat produit — dataset sans ressource exploitable");
            return List.of();
        }
        return candidats.subList(0, Math.min(NB_SOLUTIONS, candidats.size()));
    }

    // =========================================================
    // Classement — paliers lexicographiques (§4.4 du cadrage)
    // =========================================================

    /**
     * Ordre de préférence, palier par palier. Chaque palier ne départage que les ex æquo du
     * précédent.
     *
     * <p>Le choix d'un ordre lexicographique plutôt que d'un score unique est délibéré : les
     * pondérations du moteur ont été calibrées pour optimiser un planning, pas pour choisir une
     * personne. Les réemployer telles quelles produirait un classement défendable en théorie et
     * injustifiable devant l'utilisateur. Ici, chaque rang se lit ligne à ligne.</p>
     */
    static final Comparator<Candidat> COMPARATEUR =
            Comparator.comparing(Candidat::conforme).reversed()                     // 1. conformité
                    .thenComparing(Comparator.comparing(Candidat::couvertureComplete).reversed()) // 2. couverture
                    .thenComparing(c -> c.nature().ordinal())                       // 3. mono avant composée
                    .thenComparingInt(Candidat::nbRessourcesRappelees)              // 4. déjà en poste
                    .thenComparing(Comparator.comparingInt(Candidat::softScore).reversed()) // 5. score SOFT
                    .thenComparingDouble(Candidat::ratioCharge);                    // 6. charge relative

    // =========================================================
    // Passe 1 — mono-ressource
    // =========================================================

    private List<Candidat> passeMonoRessource(PreparedSc06Scenario prepared,
                                              Evaluation reference,
                                              List<SalarieReel> eligibles) {
        List<Candidat> candidats = new ArrayList<>();
        for (SalarieReel salarie : eligibles) {
            Map<String, Ressource> affectations = new LinkedHashMap<>();
            for (Creneau creneau : prepared.creneauxBesoin()) {
                affectations.put(creneau.getId(), salarie);
            }
            candidats.add(construire(prepared, reference, affectations, NatureCandidat.MONO_RESSOURCE));
        }
        log.info("[SC-06] passe 1 — {} salarié(s) éligible(s) sur l'intégralité du besoin", candidats.size());
        return candidats;
    }

    // =========================================================
    // Passe 2 — solutions composées
    // =========================================================

    private List<Candidat> passeComposee(PreparedSc06Scenario prepared, Evaluation reference) {

        List<Creneau> besoin = prepared.creneauxBesoin();
        if (besoin.size() < 2) {
            return List.of();   // un besoin d'un seul créneau ne se compose pas
        }

        // Meilleures options créneau par créneau, chacune évaluée isolément.
        List<List<SalarieReel>> optionsParCreneau = new ArrayList<>(besoin.size());
        for (Creneau creneau : besoin) {
            List<SalarieReel> options = prepared.salaries().stream()
                    .filter(s -> estEligible(s, creneau, prepared))
                    .map(s -> Map.entry(s, evaluer(prepared, Map.of(creneau.getId(), s)).score().softScore()))
                    .sorted(Map.Entry.<SalarieReel, Integer>comparingByValue().reversed())
                    .limit(MAX_OPTIONS_PAR_CRENEAU)
                    .map(Map.Entry::getKey)
                    .toList();
            optionsParCreneau.add(options);
        }

        List<Map<String, Ressource>> combinaisons = produitCartesien(besoin, optionsParCreneau);

        List<Candidat> candidats = new ArrayList<>();
        int evaluees = 0;
        for (Map<String, Ressource> combinaison : combinaisons) {
            if (evaluees >= MAX_COMBINAISONS_EVALUEES) {
                log.info("[SC-06] passe 2 — {} combinaison(s) retenue(s) sur {} générée(s) : "
                        + "les suivantes ne sont pas évaluées", evaluees, combinaisons.size());
                break;
            }
            if (nbRessourcesDistinctes(combinaison) < 2) {
                continue;       // déjà couvert par la passe 1
            }
            if (aUnConflitInterne(prepared, combinaison)) {
                continue;       // une même personne sur deux créneaux du besoin qui se chevauchent
            }
            candidats.add(construire(prepared, reference, combinaison, NatureCandidat.COMPOSEE));
            evaluees++;
        }

        log.info("[SC-06] passe 2 — {} solution(s) composée(s) évaluée(s)", candidats.size());
        return candidats;
    }

    /**
     * Produit cartésien des options, ordonné du plus prometteur au moins prometteur.
     *
     * <p>Le tri porte sur la somme des rangs individuels : une combinaison qui retient partout
     * le meilleur candidat de son créneau passe avant celle qui en dégrade un.</p>
     */
    private List<Map<String, Ressource>> produitCartesien(List<Creneau> besoin,
                                                          List<List<SalarieReel>> optionsParCreneau) {
        List<Map<String, Ressource>> combinaisons = new ArrayList<>();
        List<Integer> rangs = new ArrayList<>();
        construireCombinaisons(besoin, optionsParCreneau, 0, new LinkedHashMap<>(), 0, combinaisons, rangs);

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < combinaisons.size(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator.comparingInt(rangs::get));

        return indices.stream().map(combinaisons::get).toList();
    }

    private void construireCombinaisons(List<Creneau> besoin,
                                        List<List<SalarieReel>> optionsParCreneau,
                                        int index,
                                        Map<String, Ressource> courante,
                                        int sommeRangs,
                                        List<Map<String, Ressource>> resultats,
                                        List<Integer> rangs) {
        if (resultats.size() >= MAX_COMBINAISONS_GENEREES) {
            return;
        }
        if (index == besoin.size()) {
            resultats.add(new LinkedHashMap<>(courante));
            rangs.add(sommeRangs);
            return;
        }

        List<SalarieReel> options = optionsParCreneau.get(index);
        String creneauId = besoin.get(index).getId();

        if (options.isEmpty()) {
            // Aucun candidat pour ce créneau : il restera à pourvoir.
            courante.put(creneauId, RessourceNonAffectee.INSTANCE);
            construireCombinaisons(besoin, optionsParCreneau, index + 1, courante,
                    sommeRangs + MAX_OPTIONS_PAR_CRENEAU, resultats, rangs);
            courante.remove(creneauId);
            return;
        }

        for (int rang = 0; rang < options.size(); rang++) {
            courante.put(creneauId, options.get(rang));
            construireCombinaisons(besoin, optionsParCreneau, index + 1, courante,
                    sommeRangs + rang, resultats, rangs);
            courante.remove(creneauId);
        }
    }

    // =========================================================
    // Passe 3 — repli
    // =========================================================

    /**
     * Complète le podium lorsque les ressources réelles ne suffisent pas : poste virtuel si le
     * dataset en propose un, sinon ressource à pourvoir.
     *
     * <p>Cette solution est toujours produite quand le besoin n'est pas intégralement couvert
     * par ailleurs : mieux vaut restituer « personne ne peut, voici ce qu'il reste à pourvoir »
     * qu'une liste vide, qui n'apprend rien.</p>
     */
    private List<Candidat> passeRepli(PreparedSc06Scenario prepared,
                                      Evaluation reference,
                                      List<Candidat> dejaTrouves) {

        boolean couvertureExistante = dejaTrouves.stream()
                .anyMatch(c -> c.conforme() && c.couvertureComplete());
        if (couvertureExistante && dejaTrouves.size() >= NB_SOLUTIONS) {
            return List.of();
        }

        Map<String, Ressource> affectations = new LinkedHashMap<>();
        for (Creneau creneau : prepared.creneauxBesoin()) {
            Ressource repli = posteVirtuelDeRepli(prepared, creneau);
            affectations.put(creneau.getId(), repli != null ? repli : RessourceNonAffectee.INSTANCE);
        }
        return List.of(construire(prepared, reference, affectations, NatureCandidat.RESSOURCE_A_POURVOIR));
    }

    /**
     * Poste virtuel proposé en repli pour ce créneau, ou {@code null} si le dataset n'en contient
     * aucun.
     *
     * <h3>[Lot S8.3] Un poste virtuel n'est pas soumis à la règle d'activité</h3>
     * <p>Arbitrage rendu : {@code activitesCompatibles} exprime ce qu'une <strong>personne</strong>
     * sait faire, et deviendra une contrainte HARD avec le lot des contraintes personnelles. Un
     * poste virtuel n'est pas une personne — il représente le poste qu'il resterait à pourvoir.
     * Le filtrer sur l'activité revenait à retirer le remplaçant au motif qu'il ne fait pas déjà
     * le travail.</p>
     *
     * <p>La méthode rendait {@code null} quand aucun poste virtuel ne déclarait l'activité
     * demandée : le créneau retombait alors en {@code RessourceNonAffectee} et SC-06 restituait
     * « personne, rien à pourvoir » alors qu'un poste à pourvoir existait bel et bien. Elle rend
     * désormais toujours un poste virtuel dès qu'il en existe un.</p>
     *
     * <p>La déclaration d'activité reste lue, mais comme une <em>préférence</em> : à plusieurs
     * postes virtuels, celui qui annonce l'activité demandée est le plus parlant à restituer.
     * Elle n'écarte plus personne.</p>
     */
    private Ressource posteVirtuelDeRepli(PreparedSc06Scenario prepared, Creneau creneau) {
        PosteVirtuel premier = null;
        for (Ressource ressource : prepared.problem().getRessources()) {
            if (ressource instanceof PosteVirtuel pv) {
                if (premier == null) {
                    premier = pv;
                }
                Set<String> activites = pv.getActivitesAutorisees();
                if (activites == null || activites.isEmpty()
                        || activites.contains(creneau.getCodeActiviteEffectif())) {
                    return pv;
                }
            }
        }
        return premier;
    }

    // =========================================================
    // Construction et qualification d'un candidat
    // =========================================================

    private Candidat construire(PreparedSc06Scenario prepared,
                                Evaluation reference,
                                Map<String, Ressource> affectations,
                                NatureCandidat nature) {

        Evaluation evaluation = evaluer(prepared, affectations);

        List<MotifCandidat> motifs = new ArrayList<>();
        boolean conforme = true;

        // Un motif n'est levé que si le candidat aggrave la situation de référence.
        for (Map.Entry<PenaliteKey, Integer> entree : evaluation.penalites().entrySet()) {
            int referencePenalite = reference.penalites().getOrDefault(entree.getKey(), 0);
            if (entree.getValue() >= referencePenalite) {
                continue;   // pénalité inchangée ou allégée : rien à imputer au candidat
            }
            MotifCandidat motif = MotifCandidat.parPenalite(entree.getKey());
            if (motif == null) {
                continue;   // contrainte sans motif catalogué : volontairement muette
            }
            motifs.add(motif);
            if (motif.isEliminatoire()) {
                conforme = false;
            }
        }

        boolean couvertureComplete = affectations.values().stream()
                .noneMatch(RessourceNonAffectee.class::isInstance);
        if (!couvertureComplete) {
            motifs.add(MotifCandidat.BESOIN_PARTIELLEMENT_COUVERT);
        }

        int rappelees = compterRessourcesRappelees(prepared, affectations);
        if (rappelees > 0) {
            motifs.add(MotifCandidat.RAPPEL_SUR_REPOS);
        }

        return new Candidat(
                nature,
                affectations,
                conforme,
                couvertureComplete,
                rappelees,
                pireRatioCharge(prepared, affectations),
                evaluation.score().softScore(),
                List.copyOf(motifs)
        );
    }

    /**
     * Nombre de ressources mobilisées qui ne travaillaient pas le jour du besoin.
     *
     * <p>Palier 4 du classement : à conformité et forme égales, on préfère prolonger une journée
     * déjà commencée plutôt que de rappeler quelqu'un sur son repos.</p>
     */
    private int compterRessourcesRappelees(PreparedSc06Scenario prepared,
                                           Map<String, Ressource> affectations) {
        Set<String> mobilisees = new HashSet<>();
        for (Ressource ressource : affectations.values()) {
            if (ressource instanceof SalarieReel salarie) {
                mobilisees.add(salarie.getId());
            }
        }

        Set<String> enPosteCeJour = new HashSet<>();
        for (Creneau creneau : prepared.problem().getCreneaux()) {
            if (creneau.isFige()
                    && prepared.dateBesoin().equals(creneau.getDate())
                    && creneau.getRessourceAffectee() != null) {
                enPosteCeJour.add(creneau.getRessourceAffectee().getId());
            }
        }

        return (int) mobilisees.stream().filter(id -> !enPosteCeJour.contains(id)).count();
    }

    /**
     * Charge hebdomadaire résultante rapportée au volume habituel, la plus défavorable parmi les
     * ressources mobilisées.
     *
     * <p>Palier 6 — dernier départage. Une ressource dont le contrat ne déclare pas de volume
     * hebdomadaire habituel obtient {@link Double#MAX_VALUE} et se retrouve donc classée en
     * dernier à égalité par ailleurs : à information égale, on préfère la personne dont on peut
     * mesurer l'impact. WinDev est tenu de toujours transmettre ce volume — le cas traduit donc
     * un défaut d'intégration, que ce classement rend visible plutôt que d'absorber.</p>
     */
    private double pireRatioCharge(PreparedSc06Scenario prepared, Map<String, Ressource> affectations) {
        double pire = 0.0;
        Set<String> deja = new HashSet<>();

        for (Ressource ressource : affectations.values()) {
            if (!(ressource instanceof SalarieReel salarie) || !deja.add(salarie.getId())) {
                continue;
            }
            if (salarie.getContrat() == null
                    || salarie.getContrat().getHeuresHebdomadairesHabituelles() == null
                    || salarie.getContrat().getHeuresHebdomadairesHabituelles() <= 0) {
                return Double.MAX_VALUE;
            }

            // Même calcul que celui restitué dans impacts[].heuresSemaine : le classement et la
            // réponse ne peuvent pas diverger, ils passent par les mêmes primitives.
            List<Creneau> apres = Sc06ChargeCalculator.avecLeBesoin(
                    Sc06ChargeCalculator.planningFige(prepared, salarie.getId()),
                    Sc06ChargeCalculator.partDuBesoin(prepared, affectations, salarie.getId()));

            int minutes = Sc06ChargeCalculator.minutesTravaillees(apres, prepared.referentiel());
            double ratio = minutes / (salarie.getContrat().getHeuresHebdomadairesHabituelles() * 60.0);
            pire = Math.max(pire, ratio);
        }
        return pire;
    }

    // =========================================================
    // Éligibilité
    // =========================================================

    private boolean estEligiblePourTout(SalarieReel salarie, PreparedSc06Scenario prepared) {
        for (Creneau creneau : prepared.creneauxBesoin()) {
            if (!estEligible(salarie, creneau, prepared)) {
                return false;
            }
        }
        // Deux créneaux du besoin qui se chevauchent ne peuvent pas revenir à la même personne.
        return !seChevauchentEntreEux(prepared.creneauxBesoin());
    }

    /**
     * Filtre d'éligibilité — sans aucun coût solveur.
     *
     * <p>Écarte les ressources dont l'activité n'est pas compatible, celles indisponibles ce
     * jour-là, et celles dont un créneau déjà affecté chevauche le besoin.</p>
     *
     * <p><strong>Le lieu n'entre pas dans ce filtre</strong> : {@code sitesAutorises} est
     * transporté et mappé, mais aucune contrainte ni aucun filtre ne l'exploite aujourd'hui.
     * C'est la posture actuelle du moteur, assumée, que SC-06 ne modifie pas.</p>
     */
    boolean estEligible(SalarieReel salarie, Creneau besoin, PreparedSc06Scenario prepared) {

        Set<String> activites = salarie.getActivitesCompatibles();
        if (activites != null && !activites.isEmpty()
                && !activites.contains(besoin.getCodeActiviteEffectif())) {
            return false;
        }

        // [Lot S0 de SC-02] Le besoin est comparé par son intervalle effectif, et non plus par la
        // seule date du scénario : un besoin de nuit qui déborde sur le lendemain rendait le
        // salarié éligible alors qu'il y est déclaré absent. Même règle que la contrainte HARD.
        for (Indisponibilite indisponibilite : prepared.indisponibilites()) {
            if (salarie.getId().equals(indisponibilite.getRessourceId())
                    && besoin.chevauchePeriode(
                            indisponibilite.getDateDebut(), indisponibilite.getDateFin())) {
                return false;
            }
        }

        for (Creneau existant : prepared.problem().getCreneaux()) {
            if (existant.isFige()
                    && existant.getRessourceAffectee() != null
                    && salarie.getId().equals(existant.getRessourceAffectee().getId())
                    && seChevauchent(existant, besoin)) {
                return false;
            }
        }

        return true;
    }

    /** Nombre de ressources réelles distinctes mobilisées par une affectation. */
    private int nbRessourcesDistinctes(Map<String, Ressource> affectations) {
        Set<String> ids = new HashSet<>();
        for (Ressource ressource : affectations.values()) {
            if (ressource instanceof SalarieReel salarie) {
                ids.add(salarie.getId());
            }
        }
        return ids.size();
    }

    private boolean aUnConflitInterne(PreparedSc06Scenario prepared, Map<String, Ressource> affectations) {
        List<Creneau> besoin = prepared.creneauxBesoin();
        for (int i = 0; i < besoin.size(); i++) {
            for (int j = i + 1; j < besoin.size(); j++) {
                Ressource a = affectations.get(besoin.get(i).getId());
                Ressource b = affectations.get(besoin.get(j).getId());
                if (a instanceof SalarieReel && a == b && seChevauchent(besoin.get(i), besoin.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean seChevauchentEntreEux(List<Creneau> creneaux) {
        for (int i = 0; i < creneaux.size(); i++) {
            for (int j = i + 1; j < creneaux.size(); j++) {
                if (seChevauchent(creneaux.get(i), creneaux.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Chevauchement de deux créneaux, en temps absolu.
     *
     * <p>Un créneau dont l'heure de fin n'est pas postérieure à son heure de début se termine le
     * lendemain : comparer les seules heures ferait passer une nuit et le matin suivant pour
     * disjoints.</p>
     */
    static boolean seChevauchent(Creneau a, Creneau b) {
        LocalDateTime debutA = LocalDateTime.of(a.getDate(), a.getHeureDebut());
        LocalDateTime finA = LocalDateTime.of(a.getDate(), a.getHeureFin());
        if (!finA.isAfter(debutA)) {
            finA = finA.plusDays(1);
        }
        LocalDateTime debutB = LocalDateTime.of(b.getDate(), b.getHeureDebut());
        LocalDateTime finB = LocalDateTime.of(b.getDate(), b.getHeureFin());
        if (!finB.isAfter(debutB)) {
            finB = finB.plusDays(1);
        }
        return debutA.isBefore(finB) && debutB.isBefore(finA);
    }

    // =========================================================
    // Évaluation
    // =========================================================

    /**
     * Applique une affectation aux créneaux du besoin et mesure le résultat.
     *
     * <p>Aucune recherche n'est lancée : {@link SolutionManager#explain} calcule le score de la
     * solution telle quelle. Les créneaux du besoin non affectés reçoivent
     * {@code A_AFFECTER} — jamais {@code null}, conformément à l'invariant du moteur.</p>
     */
    private Evaluation evaluer(PreparedSc06Scenario prepared, Map<String, Ressource> affectations) {
        for (Creneau creneau : prepared.creneauxBesoin()) {
            Ressource affectee = affectations.get(creneau.getId());
            creneau.setRessourceAffectee(affectee != null ? affectee : RessourceNonAffectee.INSTANCE);
        }

        ScoreExplanation<PlanningProblem, HardSoftScore> explanation =
                solutionManager.explain(prepared.problem());

        Map<PenaliteKey, Integer> penalites = new EnumMap<>(PenaliteKey.class);
        for (ConstraintMatchTotal<HardSoftScore> total : explanation.getConstraintMatchTotalMap().values()) {
            PenaliteKey key;
            try {
                key = PenaliteKey.valueOf(total.getConstraintName());
            } catch (IllegalArgumentException e) {
                continue;   // contrainte hors catalogue : ignorée, comme dans ScoreBreakdownFactory
            }
            // Les contraintes HARD et SOFT sont ramenées à une grandeur unique : seule leur
            // dégradation relative nous intéresse, pas leur unité.
            int contribution = total.getScore().hardScore() != 0
                    ? total.getScore().hardScore()
                    : total.getScore().softScore();
            penalites.merge(key, contribution, Integer::sum);
        }

        return new Evaluation(explanation.getScore(), penalites);
    }

    /**
     * Résultat d'une évaluation : le score global et la contribution de chaque contrainte.
     */
    private record Evaluation(HardSoftScore score, Map<PenaliteKey, Integer> penalites) {
    }
}
