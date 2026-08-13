package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.dto.CoefficientsPenibiliteDTO;
import fr.project.planning.scenarios.dto.PlanningContextDTO;

/**
 * CoefficientsPenibiliteMapper — lit les coefficients de pénibilité, ou dit qu'il n'y en a pas.
 *
 * <h3>Quand le moteur se tait, et quand il parle</h3>
 * <p>Sans coefficients, la mesure pondérée vaut les heures brutes. Le moteur applique ce neutre —
 * il n'invente pas une échelle que seule la simulation peut établir — et <strong>ne le signale
 * pas</strong> : tant que les coefficients ne sont pas calibrés (lot L3), c'est le cas de toutes
 * les demandes, et une alerte sur cent pour cent des réponses n'est pas un signal, c'est du bruit
 * dans un canal que ce projet garde utile. L'information est portée là où elle est vérifiable :
 * {@code heuresPonderees} vaut alors exactement {@code heuresTravaillees}, et le contrat le dit.</p>
 *
 * <p>Il parle en revanche quand l'appelant a <strong>transmis un bloc qui ne pondère rien</strong> :
 * là, quelqu'un a cru configurer quelque chose. C'est rare, c'est actionnable, et c'est le seul cas
 * où le silence tromperait.</p>
 */
public final class CoefficientsPenibiliteMapper {

    private CoefficientsPenibiliteMapper() {
    }

    /**
     * @param contexte bloc {@code planningContext} reçu, éventuellement {@code null}
     * @param scenario code du scénario, pour le message
     * @param alertes  collecteur du scénario ; une alerte {@code INFO} y est déposée si un bloc de
     *                 coefficients est transmis sans rien pondérer
     */
    public static CoefficientsPenibilite depuis(PlanningContextDTO contexte, String scenario,
                                                CollecteurAlertes alertes) {
        CoefficientsPenibiliteDTO dto =
                contexte == null ? null : contexte.getCoefficientsPenibilite();

        if (dto == null) {
            return CoefficientsPenibilite.neutres();
        }

        CoefficientsPenibilite coefficients =
                new CoefficientsPenibilite(dto.getNuit(), dto.getDimanche(), dto.getFerie());

        if (coefficients.sontNeutres() && alertes != null) {
            alertes.signaler(AlertCode.COEFFICIENTS_PENIBILITE_SANS_EFFET, AlertSeverity.INFO,
                    "[" + scenario + "] Un bloc coefficientsPenibilite est transmis, mais aucun "
                            + "coefficient ne pondère quoi que ce soit : les heures pondérées "
                            + "valent les heures brutes, et une heure de nuit y pèse autant qu'une "
                            + "heure ordinaire. Ce n'est pas une comparaison à pénibilité "
                            + "équivalente.");
        }

        return coefficients;
    }
}
