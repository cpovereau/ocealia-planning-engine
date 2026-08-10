package fr.project.planning.scenarios.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Sc06ScenarioParametersDTO — paramètres du scénario SC-06 (lot S4).
 *
 * <p>SC-06 désigne, parmi les ressources dont le planning est transmis, celles les plus à même
 * de couvrir un besoin. Un seul paramètre : le besoin lui-même.</p>
 *
 * <p>Le nombre de solutions restituées n'est pas paramétrable — trois, fixé au §4.3 du cadrage.</p>
 *
 * <p>Contrat strict : un champ inconnu est rejeté.</p>
 */
public class Sc06ScenarioParametersDTO {

    @NotNull(message = "scenarioParameters.besoin est obligatoire")
    @Valid
    private BesoinDTO besoin;

    public BesoinDTO getBesoin() { return besoin; }
    public void setBesoin(BesoinDTO besoin) { this.besoin = besoin; }
}
