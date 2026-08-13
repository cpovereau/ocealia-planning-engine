package fr.project.planning.scenarios.dto;

/**
 * EquiteDTO — à partir de quel écart au contrat l'encadrement estime qu'il y a inéquité (L5).
 *
 * <h3>Mesurer n'est pas sanctionner</h3>
 * <p>Les lots L1 et L2 ont livré la mesure — {@code heuresPonderees}, {@code ecartContratPourcent}
 * — qui <em>décrit</em> une répartition sans statuer qu'elle est mauvaise. Un écart de 5 %
 * gêne-t-il ? de 20 % ? Le moteur n'en sait rien, et la réponse n'est pas la même d'un client à
 * l'autre.</p>
 *
 * <p><strong>Bloc facultatif, et il l'est pour de bon</strong> : absent, la contrainte d'équité ne
 * pèse rien et le score est exactement celui d'avant le lot L5. C'est le cas de toutes les demandes
 * à ce jour — les coefficients de pénibilité qui pondèrent cet écart ne sont pas encore calibrés,
 * et peser une mesure dont l'échelle reste à établir reviendrait à deviner deux fois.</p>
 */
public class EquiteDTO {

    /**
     * Écart au volume contractuel, en points de pourcentage, au-delà duquel le moteur pénalise.
     *
     * <p>La <strong>valeur absolue</strong> de l'écart est comparée à cette tolérance : la
     * sous-charge compte autant que la surcharge, sans quoi le moteur éviterait de surcharger sans
     * jamais rééquilibrer.</p>
     *
     * <p>Une valeur négative est refusée — une tolérance décrit une marge, pas une exigence.
     * {@code 0} est accepté et signifie « tout écart compte », ce qui est une configuration
     * extrême mais cohérente.</p>
     */
    private Double ecartTolerePourcent;

    public Double getEcartTolerePourcent() {
        return ecartTolerePourcent;
    }

    public void setEcartTolerePourcent(Double ecartTolerePourcent) {
        this.ecartTolerePourcent = ecartTolerePourcent;
    }
}
