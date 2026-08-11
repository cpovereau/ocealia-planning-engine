package fr.project.planning.scenarios.alerte;

import fr.project.planning.scenarios.dto.ScenarioAlertDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CollecteurAlertes — lot S8.4
 *
 * <p>Ce que la préparation d'un scénario a décidé, ou constaté, et que l'appelant doit savoir.</p>
 *
 * <h3>Un seul geste, deux destinataires</h3>
 * <p>Jusqu'ici les deux canaux vivaient séparément : un {@code log.warn} pour l'exploitant, et une
 * liste d'alertes que seul SC-01 alimentait. Les constats de SC-03 et SC-06 n'atteignaient donc que
 * le journal du serveur — invisibles du logiciel de Planning, qui est pourtant le seul à pouvoir
 * agir dessus.</p>
 *
 * <p>{@link #signaler} écrit dans les deux à la fois. C'est volontairement le seul moyen d'ajouter
 * une alerte : on ne peut pas en émettre une sans qu'elle soit tracée, ni tracer un constat de
 * préparation sans que le client l'apprenne.</p>
 */
public final class CollecteurAlertes {

    private static final Logger log = LoggerFactory.getLogger(CollecteurAlertes.class);

    private final String scenario;
    private final List<ScenarioAlert> alertes = new ArrayList<>();

    /**
     * @param scenario préfixe des journaux, p. ex. {@code "SC-03"}
     */
    public CollecteurAlertes(String scenario) {
        this.scenario = scenario;
    }

    /**
     * Alerte portant sur la configuration ou sur le dataset dans son ensemble — sans date.
     */
    public void signaler(AlertCode code, AlertSeverity severity, String message) {
        signaler(code, severity, null, message);
    }

    /**
     * Alerte ancrée sur un jour.
     */
    public void signaler(AlertCode code, AlertSeverity severity, LocalDate date, String message) {
        alertes.add(new ScenarioAlert(code, severity, date, message));

        String trace = "[{}] {} — {}";
        switch (severity) {
            case ERROR -> log.error(trace, scenario, code, message);
            case WARNING -> log.warn(trace, scenario, code, message);
            case INFO -> log.info(trace, scenario, code, message);
        }
    }

    /** Reprend des alertes produites ailleurs — le builder SC-01, notamment. */
    public void reprendre(List<ScenarioAlert> produites) {
        if (produites != null) {
            alertes.addAll(produites);
        }
    }

    public boolean estVide() {
        return alertes.isEmpty();
    }

    public List<ScenarioAlert> alertes() {
        return List.copyOf(alertes);
    }

    /** Forme restituée par l'API. */
    public List<ScenarioAlertDTO> versDto() {
        return alertes.stream()
                .map(a -> new ScenarioAlertDTO(
                        a.code().name(), a.severity().name(), a.date(), a.message()))
                .toList();
    }
}
