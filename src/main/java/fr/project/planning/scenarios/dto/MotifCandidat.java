package fr.project.planning.scenarios.dto;

import fr.project.planning.scoring.PenaliteKey;

import java.util.EnumMap;
import java.util.Map;

/**
 * MotifCandidat — catalogue des raisons attachées à un candidat SC-06 (lot S4).
 *
 * <p>Chaque motif déclare sa sévérité et son caractère éliminatoire. Un motif éliminatoire
 * entraîne {@code conforme = false} : la solution reste restituée, mais classée en dernier —
 * l'invariant du contrat veut que le moteur rende visible l'impossible plutôt que de le taire
 * (§4.5 du cadrage SC-06).</p>
 *
 * <h3>Éliminatoire ≠ contrainte HARD</h3>
 * <p>Le caractère éliminatoire est une notion du <strong>classement SC-06</strong>, pas du
 * solveur. `ReposQuotidienMinimum` et `HeuresMaximumParSemaine` sont des contraintes SOFT ;
 * elles disqualifient néanmoins un candidat parce qu'aucune recommandation ne doit conduire à
 * une situation illégale. Voir §7.1 du cadrage.</p>
 *
 * <h3>Détection par delta</h3>
 * <p>Un motif rattaché à une {@link PenaliteKey} est levé lorsque la pénalité du candidat est
 * <em>plus lourde que celle de la situation de référence</em> — besoin non couvert. Cette
 * mesure relative est indispensable : le planning existant est figé, et une violation qu'il
 * porte déjà ne doit être imputée à aucun candidat.</p>
 */
public enum MotifCandidat {

    // ---- Éliminatoires ----

    REPOS_QUOTIDIEN_INSUFFISANT(Severite.ERROR, true,
            PenaliteKey.LEGAL_SOFT_REPOS_QUOTIDIEN_MINIMUM,
            "Le repos quotidien minimum ne serait plus respecté"),

    HEURES_HEBDO_DEPASSEES(Severite.ERROR, true,
            PenaliteKey.LEGAL_SOFT_HEURES_MAX_PAR_SEMAINE,
            "La durée hebdomadaire maximale serait dépassée"),

    JOUR_FERIE_NON_AUTORISE(Severite.ERROR, true,
            PenaliteKey.METIER_HARD_JOUR_FERIE_REFUSE,
            "Le salarié n'est pas autorisé à travailler les jours fériés"),

    INDISPONIBILITE(Severite.ERROR, true,
            PenaliteKey.METIER_HARD_INDISPONIBILITE,
            "Le salarié est indisponible ce jour-là"),

    CHEVAUCHEMENT(Severite.ERROR, true,
            PenaliteKey.PHYSIQUE_HARD_CHEVAUCHEMENT_CRENEAUX,
            "Le créneau chevauche une affectation existante"),

    // ---- Signalements non éliminatoires ----

    AMPLITUDE_DEPASSEE(Severite.WARNING, false,
            PenaliteKey.PHYSIQUE_SOFT_AMPLITUDE_JOURNALIERE,
            "L'amplitude journalière maximale serait dépassée"),

    JOURS_CONSECUTIFS_DEPASSES(Severite.WARNING, false,
            PenaliteKey.LEGAL_SOFT_JOURS_CONSECUTIFS_MAX,
            "Le nombre de jours travaillés consécutifs serait dépassé"),

    NUIT_SALARIE_NON_NUIT(Severite.WARNING, false,
            PenaliteKey.METIER_SOFT_NUIT_SALARIE_NON_NUIT,
            "Créneau de nuit confié à un salarié non déclaré travailleur de nuit"),

    // ---- Motifs sans contrainte associée ----

    RAPPEL_SUR_REPOS(Severite.INFO, false, null,
            "Le salarié ne travaillait pas ce jour-là"),

    BESOIN_PARTIELLEMENT_COUVERT(Severite.WARNING, false, null,
            "Une partie du besoin reste non couverte"),

    AUCUNE_RESSOURCE_ELIGIBLE(Severite.ERROR, false, null,
            "Aucune ressource du dataset ne peut prendre ce besoin en charge");

    /** Sévérités exposées, alignées sur celles de {@code diagnostics.alerts[]}. */
    public static final class Severite {
        public static final String INFO = "INFO";
        public static final String WARNING = "WARNING";
        public static final String ERROR = "ERROR";

        private Severite() {
        }
    }

    /** Index inverse : quelle pénalité lève quel motif. */
    private static final Map<PenaliteKey, MotifCandidat> PAR_PENALITE = new EnumMap<>(PenaliteKey.class);

    static {
        for (MotifCandidat motif : values()) {
            if (motif.penaliteKey != null) {
                PAR_PENALITE.put(motif.penaliteKey, motif);
            }
        }
    }

    private final String severite;
    private final boolean eliminatoire;
    private final PenaliteKey penaliteKey;
    private final String message;

    MotifCandidat(String severite, boolean eliminatoire, PenaliteKey penaliteKey, String message) {
        this.severite = severite;
        this.eliminatoire = eliminatoire;
        this.penaliteKey = penaliteKey;
        this.message = message;
    }

    public String getSeverite() { return severite; }

    public boolean isEliminatoire() { return eliminatoire; }

    public PenaliteKey getPenaliteKey() { return penaliteKey; }

    public String getMessage() { return message; }

    /**
     * Retourne le motif associé à une pénalité, ou {@code null} si cette pénalité n'en porte
     * aucun. Toutes les contraintes du moteur n'ont pas vocation à expliquer un rang : celles
     * qui ne sont pas cataloguées sont volontairement muettes.
     */
    public static MotifCandidat parPenalite(PenaliteKey key) {
        return PAR_PENALITE.get(key);
    }

    public MotifCandidatDTO toDto() {
        return new MotifCandidatDTO(name(), severite, message);
    }
}
