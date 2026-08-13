package fr.project.planning.domain.contexte;

/**
 * ToleranceEquite — à partir de quel écart au contrat l'encadrement estime qu'il y a inéquité.
 *
 * <h3>Mesurer n'est pas sanctionner</h3>
 * <p>Les lots L1 et L2 ont livré la <strong>mesure</strong> : {@code heuresPonderees} et
 * {@code ecartContratPourcent} décrivent une répartition, sans statuer qu'elle est mauvaise. Un
 * écart de 5 % gêne-t-il ? de 20 % ? Le moteur n'en sait rien, et la réponse n'est pas la même
 * d'un client à l'autre. <strong>C'est l'encadrement qui dit à partir de quand un écart gêne</strong>,
 * exactement comme le seuil de surcharge de SC-02 dit jusqu'où l'on accepte d'aller.</p>
 *
 * <h3>Absente, la contrainte n'existe pas</h3>
 * <p>Aucune tolérance transmise, aucun fait au problème, et la jointure de
 * {@code EquiteChargeAuContrat} ne produit rien : le score est <strong>exactement</strong> celui
 * d'avant le lot L5. Ce n'est pas de la prudence, c'est la même règle que partout ailleurs — une
 * borne absente n'est pas une borne à zéro, et le moteur n'invente pas un seuil que personne ne
 * lui a donné.</p>
 *
 * <p>La retenue vaut ici plus qu'ailleurs pour une raison supplémentaire : les coefficients de
 * pénibilité qui pondèrent cet écart <strong>ne sont pas encore calibrés</strong> (lot L3). Peser
 * au score une mesure dont l'échelle reste à établir reviendrait à deviner deux fois.</p>
 */
public final class ToleranceEquite {

    private final Double ecartTolerePourcent;

    public ToleranceEquite(Double ecartTolerePourcent) {
        if (ecartTolerePourcent != null && ecartTolerePourcent < 0) {
            throw new IllegalArgumentException(
                    "La tolérance d'équité ne peut pas être négative : " + ecartTolerePourcent
                            + ". Une tolérance décrit une marge, pas une exigence.");
        }
        this.ecartTolerePourcent = ecartTolerePourcent;
    }

    public Double getEcartTolerePourcent() {
        return ecartTolerePourcent;
    }

    /** Aucune tolérance déclarée : l'objet n'a rien à dire, et aucune contrainte ne s'en saisit. */
    public boolean estVide() {
        return ecartTolerePourcent == null;
    }

    /**
     * Points de pourcentage d'écart <strong>au-delà</strong> de la tolérance, ou 0.
     *
     * <p>L'écart est pris en valeur absolue : <strong>la sous-charge compte autant que la
     * surcharge</strong>. Sans cela, le moteur se contenterait d'éviter de surcharger et ne
     * rééquilibrerait jamais — l'équité ne se produirait pas, elle serait seulement moins violée.</p>
     */
    public int pointsExcedentaires(double ecartPourcent) {
        if (ecartTolerePourcent == null) {
            return 0;
        }
        return (int) Math.max(0, Math.round(Math.abs(ecartPourcent) - ecartTolerePourcent));
    }
}
