package fr.project.planning.domain.creneau;

/**
 * CodeActivite — la règle de résolution du code activité d'un créneau (lot S7.8)
 *
 * <p>Un créneau porte deux champs concurrents pour désigner son activité :
 * {@code codeActiviteId}, la clé du contrat courant, et {@code activite}, le libellé hérité
 * conservé pour les clients qui n'ont pas encore migré. La règle d'arbitrage est unique :
 * <strong>{@code codeActiviteId} prioritaire, repli sur {@code activite}, {@code null} si aucun
 * des deux n'est exploitable.</strong></p>
 *
 * <h3>Pourquoi une classe pour trois lignes</h3>
 * <p>Cette règle était réimplémentée à l'identique sur onze sites — contraintes, calcul des
 * métriques, services de préparation. Le chantier S7 a montré ce que coûte une règle dupliquée :
 * six contraintes lisaient {@code getActivite()} seul et sont restées muettes en production
 * pendant que la suite de tests, qui alimentait ce champ, les déclarait vertes. Une règle
 * dupliquée ne diverge pas bruyamment ; elle diverge en silence.</p>
 *
 * <p>Les deux porteurs — l'entité {@link Creneau} et le DTO d'entrée — exposent chacun un
 * {@code getCodeActiviteEffectif()} qui délègue ici. Un test de garde vérifie qu'aucune classe
 * de production ne réécrit l'expression en ligne.</p>
 */
public final class CodeActivite {

    private CodeActivite() {
        // Utility class
    }

    /**
     * Résout le code activité effectif à partir des deux champs concurrents.
     *
     * <p>Une chaîne vide ou blanche est traitée comme une absence : elle ne peut désigner
     * aucune entrée du référentiel, et la laisser passer ferait porter à l'appelant un
     * {@code isBlank()} de plus. Le {@code null} de sortie signifie « aucune activité
     * exploitable », jamais « activité quelconque » — un vide ne suppose pas que la chose
     * est possible.</p>
     *
     * @param codeActiviteId clé du contrat courant, prioritaire
     * @param activite       libellé hérité, utilisé en repli
     * @return le code effectif, ou {@code null} si les deux champs sont vides
     */
    public static String effectif(String codeActiviteId, String activite) {
        if (codeActiviteId != null && !codeActiviteId.isBlank()) {
            return codeActiviteId;
        }
        return (activite != null && !activite.isBlank()) ? activite : null;
    }
}
