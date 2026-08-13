package fr.project.planning.domain.contexte;

/**
 * SeuilsSurcharge — ce que l'appelant juge acceptable de faire porter à un remplaçant.
 *
 * <h3>Une borne de confort, pas une borne légale</h3>
 * <p>Les bornes légales sont individuelles, portées par le salarié
 * ({@code contraintesReglementaires}) et tenues par leurs propres contraintes. Ce seuil-ci est
 * <strong>propre à la demande</strong> : il dit jusqu'où l'encadrement accepte d'aller pour couvrir
 * une absence, ce jour-là, dans cette situation.</p>
 *
 * <p>Il est donc traité comme le plafond de dimanches arbitré au lot S7.1 : le dépassement est
 * <strong>signalé, jamais éliminatoire</strong>. Le moteur ne refuse pas — il rend visible ce qu'il
 * en coûte, et laisse l'arbitrage à qui de droit.</p>
 *
 * <h3>Une borne absente n'est pas une borne à zéro</h3>
 * <p>{@code null} signifie « aucun seuil déclaré », donc aucun dépassement possible de ce côté.
 * C'est la même lecture que partout ailleurs dans le moteur.</p>
 */
public final class SeuilsSurcharge {

    private final Double heuresMaximumParJour;
    private final Double heuresMaximumParSemaine;

    public SeuilsSurcharge(Double heuresMaximumParJour, Double heuresMaximumParSemaine) {
        this.heuresMaximumParJour = heuresMaximumParJour;
        this.heuresMaximumParSemaine = heuresMaximumParSemaine;
    }

    public Double getHeuresMaximumParJour() {
        return heuresMaximumParJour;
    }

    public Double getHeuresMaximumParSemaine() {
        return heuresMaximumParSemaine;
    }

    /** Aucun seuil déclaré : l'objet n'a rien à dire, et aucune contrainte ne s'en saisit. */
    public boolean estVide() {
        return heuresMaximumParJour == null && heuresMaximumParSemaine == null;
    }

    /** Seuil journalier en minutes, ou {@code null} s'il n'est pas déclaré. */
    public Integer minutesParJour() {
        return heuresMaximumParJour == null ? null : (int) (heuresMaximumParJour * 60);
    }

    /** Seuil hebdomadaire en minutes, ou {@code null} s'il n'est pas déclaré. */
    public Integer minutesParSemaine() {
        return heuresMaximumParSemaine == null ? null : (int) (heuresMaximumParSemaine * 60);
    }
}
