package fr.project.planning.scenarios.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sc05ScenarioParametersDTO — paramètres propres à l'arbitrage de répartition.
 *
 * <h3>Trois champs, et aucun de plus</h3>
 * <p>Le contrat annoncé de longue date en portait deux autres, tous deux supprimés par les
 * arbitrages du 13/08 (§5.3 et §5.4 du cadrage) :</p>
 * <ul>
 *   <li>{@code objectif} — il se déduit de ce que l'appelant transmet. Un enum qui double des
 *       paramètres existants finit par les contredire. 🔁 Il reviendra <strong>au rang 10</strong>,
 *       avec la seule valeur qui ne se déduira pas d'un paramètre : le respect des préférences ;</li>
 *   <li>{@code autoriserDesequilibre} — remplacé par
 *       {@code planningContext.equite.ecartTolerePourcent}, qui dit <em>de combien</em> là où un
 *       booléen ne disait pas jusqu'où. « Équité stricte » se transmet comme une tolérance à zéro.</li>
 * </ul>
 *
 * <h3>Deux salariés aujourd'hui, N demain</h3>
 * <p>§5.5 : l'ouverture à N est attendue à brève échéance, et c'est <strong>ce bloc</strong> qui
 * devra alors changer — le moteur, lui, raisonne déjà sur un ensemble. Le passage à N sera un
 * élargissement du contrat d'entrée, pas une réécriture du calcul.</p>
 */
public class Sc05ScenarioParametersDTO {

    /** Premier des deux salariés entre lesquels arbitrer. */
    @NotBlank(message = "scenarioParameters.salarieAId est obligatoire")
    private String salarieAId;

    /** Second des deux salariés entre lesquels arbitrer. */
    @NotBlank(message = "scenarioParameters.salarieBId est obligatoire")
    private String salarieBId;

    /**
     * Identifiants des créneaux du {@code dataSet} remis en jeu par l'arbitrage.
     *
     * <p>Le périmètre est <strong>transmis, jamais déduit</strong> (§5.1). Un périmètre déduit trop
     * large déplacerait des créneaux que personne ne voulait bouger ; trop étroit, il rendrait
     * l'arbitrage sans effet — et dans les deux cas l'appelant ne verrait pas pourquoi.</p>
     */
    @NotEmpty(message = "scenarioParameters.creneauxArbitres est obligatoire et ne peut pas être vide")
    private List<String> creneauxArbitres;

    public String getSalarieAId() {
        return salarieAId;
    }

    public void setSalarieAId(String salarieAId) {
        this.salarieAId = salarieAId;
    }

    public String getSalarieBId() {
        return salarieBId;
    }

    public void setSalarieBId(String salarieBId) {
        this.salarieBId = salarieBId;
    }

    public List<String> getCreneauxArbitres() {
        return creneauxArbitres;
    }

    public void setCreneauxArbitres(List<String> creneauxArbitres) {
        this.creneauxArbitres = creneauxArbitres;
    }

    /**
     * Les salariés entre lesquels arbitrer, en un ensemble.
     *
     * <p>C'est sous cette forme que le moteur les reçoit, et il ne sait pas qu'ils sont deux —
     * §5.5. Le « deux » ne vit que dans ce bloc.</p>
     */
    public Set<String> ressourcesAutorisees() {
        Set<String> autorisees = new LinkedHashSet<>();
        if (salarieAId != null && !salarieAId.isBlank()) {
            autorisees.add(salarieAId);
        }
        if (salarieBId != null && !salarieBId.isBlank()) {
            autorisees.add(salarieBId);
        }
        return autorisees;
    }
}
