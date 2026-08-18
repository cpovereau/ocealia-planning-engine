package fr.project.planning.scenarios.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Sc04ScenarioParametersDTO — paramètres propres à l'optimisation globale d'un planning existant.
 *
 * <h3>Un seul champ</h3>
 * <p>SC-04 juge une <strong>période</strong> — c'est son élément propre, et la période se transmet
 * déjà par {@code planningContext.horizon}. Le planning existant, les contrats, les
 * indisponibilités et le cadre réglementaire étaient au contrat et déjà lus. Ne manquait que la
 * règle qui désigne <strong>ce qui a le droit de bouger</strong>.</p>
 *
 * <h3>Pourquoi une date, et non une liste de créneaux</h3>
 * <p>§5.5 du cadrage, tranché le 2026-08-17. Tous les scénarios livrés décident <em>pour</em>
 * l'appelant de ce qui est épinglé ; SC-04 est le premier où ce choix lui revient, et c'est lui qui
 * décide si le résultat est exploitable ou un remaniement que personne ne voulait.</p>
 *
 * <p>La date pivot dit la seule chose que SC-04 a besoin de savoir : <strong>corriger la suite au
 * vu du passé</strong>. Un seul champ, déductible, cohérent avec une période qui couvre du passé et
 * du futur.</p>
 *
 * <p>🔁 La <strong>liste explicite</strong> de créneaux ajustables est <strong>tranchée
 * depuis le 2026-08-18</strong> et <strong>pas encore livrée</strong> — lot O5. Elle s'ajoutera
 * <em>à côté</em> de ce champ, jamais à sa place : champ optionnel {@code creneauxAjustables}, en
 * <strong>intersection</strong> avec le pivot — ajustable = postérieur au pivot <em>et</em> désigné
 * — et borné à un mois. Un appelant qui ne la transmet pas retombe sur le pivot seul, tel que
 * décrit ici.</p>
 *
 * <h3>Ce que le contrat annonçait et qui n'est pas ici</h3>
 * <p>{@code 50_SCENARIO_CONTRACT.md} §3.4 annonçait aussi des « priorités d'optimisation » et une
 * « pondération des règles ». <strong>Reportées</strong> (§5.6) : le moteur tient qu'on ne pondère
 * pas une mesure dont l'échelle n'est pas calibrée, et les coefficients de pénibilité ne le sont
 * pas. À poids fixes, SC-04 reste SC-04.</p>
 */
public class Sc04ScenarioParametersDTO {

    /**
     * Premier jour <strong>ajustable</strong> du planning. Tout ce qui précède est figé.
     *
     * <p>Bornes : le jour du pivot lui-même est ajustable — « à partir de », non « après ». Un
     * pivot antérieur au début de l'horizon rouvre tout le planning ; un pivot postérieur à sa fin
     * n'en rouvre rien. Les deux sont acceptés et <strong>signalés</strong> : le moteur ne refuse
     * pas, il rend visible ce que la demande implique.</p>
     */
    @NotNull(message = "scenarioParameters.datePivot est obligatoire pour SC-04")
    private LocalDate datePivot;

    public LocalDate getDatePivot() {
        return datePivot;
    }

    public void setDatePivot(LocalDate datePivot) {
        this.datePivot = datePivot;
    }
}
