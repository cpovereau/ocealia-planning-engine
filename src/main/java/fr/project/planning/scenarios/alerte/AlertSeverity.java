package fr.project.planning.scenarios.alerte;

/**
 * Niveau de gravité d'une alerte de pré-résolution.
 *
 * <ul>
 *   <li>{@code INFO} — configuration atypique mais valide, aucune action requise ;</li>
 *   <li>{@code WARNING} — configuration acceptée mais dégradée, ou décision prise à la place
 *       de l'appelant ;</li>
 *   <li>{@code ERROR} — configuration incohérente, résultat à considérer avec réserve.</li>
 * </ul>
 *
 * <p>Le client filtre sur la gravité, jamais sur le code : un {@code INFO} n'est pas une anomalie
 * et ne doit pas être restitué comme un défaut.</p>
 */
public enum AlertSeverity {
    INFO,
    WARNING,
    ERROR
}
