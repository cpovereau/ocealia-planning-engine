package fr.project.planning.scenarios.alerte;

import java.time.LocalDate;

/**
 * Alerte de pré-résolution, telle que la produisent les couches de préparation.
 *
 * @param date jour concerné, ou {@code null} lorsque l'alerte porte sur la configuration ou sur le
 *             dataset dans son ensemble. La clé est alors omise de la réponse plutôt que
 *             sérialisée à {@code null}.
 */
public record ScenarioAlert(AlertCode code, AlertSeverity severity, LocalDate date, String message) {
}
