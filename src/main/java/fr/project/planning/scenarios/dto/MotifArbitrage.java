package fr.project.planning.scenarios.dto;

/**
 * MotifArbitrage — ce qui disqualifie la répartition rendue par SC-05 (lot A3).
 *
 * <h3>Arbitrage métier du 13/08, §5.6 du cadrage</h3>
 * <blockquote>Sans répartition acceptable, la moins mauvaise est rendue. Jamais une erreur. La
 * répartition est restituée <strong>avec les motifs qui la disqualifient</strong>.</blockquote>
 *
 * <p>C'est l'invariant du projet — <em>le moteur ne refuse pas, il rend visible l'impossible</em> —
 * et le même traitement qu'en SC-06 §4.5, où les solutions non conformes sont restituées et jamais
 * masquées. L'appelant voit l'impasse au lieu de la deviner.</p>
 *
 * <h3>Éliminatoire ≠ contrainte HARD</h3>
 * <p>Comme pour {@link MotifCandidat}, le caractère éliminatoire est une notion de
 * <strong>restitution</strong>, pas du solveur. Une inéquité résiduelle ne viole aucune contrainte
 * HARD — la contrainte d'équité est SOFT — et disqualifie néanmoins la répartition au regard de ce
 * que l'appelant demandait.</p>
 *
 * <h3>⚠️ « Acceptable » veut dire : au regard de ce que le moteur sait</h3>
 * <p>Et le moteur ne connaît pas les <strong>préférences des deux intéressés</strong> — c'est le
 * rang 10 du backlog, en attente de la Production. Or deux salariés désignés est précisément le cas
 * où elles comptent le plus : une répartition parfaitement équitable et contraire au souhait des
 * deux est la façon habituelle dont ce type d'arbitrage se fait rejeter sur le terrain (§9.2 du
 * cadrage). L'absence de motif ne vaut donc pas accord des intéressés.</p>
 */
public enum MotifArbitrage {

    // ---- Éliminatoires ----

    /**
     * La répartition viole au moins une contrainte HARD.
     *
     * <p>Elle est rendue malgré tout : c'est la moins mauvaise que le solveur ait trouvée, et la
     * taire laisserait l'appelant sans rien. Le détail des violations est dans
     * {@code solverResult.scoreBreakdown}.</p>
     */
    REPARTITION_NON_CONFORME(Severite.ERROR, true,
            "La répartition viole au moins une contrainte impérative — voir scoreBreakdown"),

    /**
     * L'écart au contrat d'au moins un salarié arbitré reste au-delà de la tolérance déclarée.
     *
     * <p>L'arbitrage a fait ce qu'il pouvait, et ce n'était pas assez : il n'y avait pas, dans le
     * périmètre remis en jeu, de quoi ramener tout le monde dans la marge. Ce n'est pas un défaut
     * du moteur — c'est un constat sur le périmètre, et l'élargir est une décision d'encadrement.</p>
     *
     * <p>Sans tolérance déclarée, ce motif ne peut pas être levé : <em>une borne absente n'est pas
     * une borne à zéro</em>.</p>
     */
    INEQUITE_RESIDUELLE(Severite.WARNING, true,
            "L'écart au contrat reste au-delà de la tolérance déclarée après arbitrage"),

    /**
     * Un créneau du périmètre que quelqu'un assurait ne trouve plus personne.
     *
     * <p>À distinguer d'un créneau qui n'était déjà à personne : celui-là n'a rien perdu. Ici, du
     * travail assuré a disparu du planning, ce qu'aucun arbitrage n'est censé produire.</p>
     */
    CRENEAU_ARBITRE_PERDU(Severite.ERROR, true,
            "Un créneau du périmètre que quelqu'un assurait ne trouve plus personne"),

    // ---- Signalements non éliminatoires ----

    /**
     * Aucun créneau n'a changé de main.
     *
     * <p>La répartition transmise était déjà la meilleure que le moteur sache produire sur ce
     * périmètre. Sans ce signalement, l'appelant recevrait son propre planning et chercherait ce
     * qui a échoué.</p>
     */
    ARBITRAGE_SANS_EFFET(Severite.INFO, false,
            "Aucun créneau n'a changé de main : la répartition transmise n'a pas pu être améliorée");

    /** Sévérités exposées, alignées sur celles de {@code diagnostics.alerts[]}. */
    public static final class Severite {
        public static final String INFO = "INFO";
        public static final String WARNING = "WARNING";
        public static final String ERROR = "ERROR";

        private Severite() {
        }
    }

    private final String severite;
    private final boolean eliminatoire;
    private final String message;

    MotifArbitrage(String severite, boolean eliminatoire, String message) {
        this.severite = severite;
        this.eliminatoire = eliminatoire;
        this.message = message;
    }

    public String getSeverite() {
        return severite;
    }

    public boolean isEliminatoire() {
        return eliminatoire;
    }

    public String getMessage() {
        return message;
    }

    public MotifArbitrageDTO toDto() {
        return new MotifArbitrageDTO(name(), severite, message);
    }
}
