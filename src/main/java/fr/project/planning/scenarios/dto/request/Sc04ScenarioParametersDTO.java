package fr.project.planning.scenarios.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * <h3>Et une liste, quand le pivot ne suffit pas</h3>
 * <p>{@code creneauxAjustables} est venu <em>à côté</em> de la date pivot, jamais à sa place (lot
 * O5, arbitrage du 2026-08-18). Les deux champs n'ont pas la même nature, et c'est pour cela qu'ils
 * se composent au lieu de se concurrencer : <strong>la date pivot dit jusqu'où le moteur a le droit
 * d'aller, la liste dit ce qu'il a le droit de toucher à l'intérieur.</strong></p>
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

    /**
     * Identifiants des créneaux du {@code dataSet} que le solveur a le droit de déplacer —
     * <strong>optionnel</strong>.
     *
     * <h3>Absent, vide, renseigné : trois choses différentes</h3>
     * <ul>
     *   <li><strong>absent</strong> — tout l'après-pivot est ajustable. C'est le comportement de
     *       SC-04 depuis son écriture, et il n'a pas changé ;</li>
     *   <li><strong>renseigné</strong> — est ajustable ce qui est <em>à la fois</em> postérieur au
     *       pivot <em>et</em> présent ici. La liste ne peut que <strong>restreindre</strong> : elle
     *       n'atteint jamais le passé, car rouvrir un créneau passé que personne n'avait couvert
     *       ferait inventer au moteur une histoire qui n'a pas eu lieu ;</li>
     *   <li><strong>vide</strong> — plus rien n'est ajustable. Ce n'est pas un refus : le planning
     *       est rendu tel quel avec ses indicateurs, et l'alerte {@code AUCUN_CRENEAU_AJUSTABLE} le
     *       dit. C'est pourquoi ce champ ne porte pas de {@code @NotEmpty} : « absent » et « vide »
     *       n'expriment pas la même demande.</li>
     * </ul>
     *
     * <h3>Un mois glissant au plus</h3>
     * <p>Les créneaux désignés ne peuvent s'étaler sur plus d'un mois, mesuré du premier au dernier
     * — un mois <em>glissant</em>, non calendaire, pour qu'une demande à cheval sur une fin de mois
     * n'ait pas à être scindée. La borne dit une intention : <strong>on juge large, on ne remanie
     * qu'étroit</strong> — l'horizon garde sa profondeur et la restitution ses trois granularités,
     * seule la zone modifiable est resserrée.</p>
     *
     * <p>Un dépassement est <strong>refusé</strong>. Le moteur ne refuse pas ce qu'il ne sait pas
     * planifier ; il refuse ce qu'il ne sait pas lire, et une liste hors borne est une requête mal
     * formée. Tronquer en silence serait pire : l'appelant lirait un résultat partiel comme un
     * résultat complet.</p>
     *
     * <h3>Ne pas lister, c'est renoncer à couvrir</h3>
     * <p>Un créneau postérieur au pivot, non couvert et non désigné, reste un trou définitif — et
     * comme toute la restitution se compte sur les ajustables, il sort du décompte
     * {@code creneauxNonCouverts}. Le trou resterait dans le planning pendant que le compte
     * cesserait de le voir : l'alerte {@code CRENEAU_FUTUR_NON_COUVERT_GELE} existe pour que
     * personne ne le découvre sur le terrain.</p>
     */
    private List<String> creneauxAjustables;

    public LocalDate getDatePivot() {
        return datePivot;
    }

    public void setDatePivot(LocalDate datePivot) {
        this.datePivot = datePivot;
    }

    public List<String> getCreneauxAjustables() {
        return creneauxAjustables;
    }

    public void setCreneauxAjustables(List<String> creneauxAjustables) {
        this.creneauxAjustables = creneauxAjustables;
    }

    /**
     * La sélection sous la forme où le moteur la lit, ou {@code null} si l'appelant n'en a pas
     * transmis.
     *
     * <p>{@code null} et ensemble vide portent deux demandes opposées — « je ne restreins pas » et
     * « je ne rouvre rien » — et la préparation doit pouvoir les distinguer. Les identifiants nuls
     * ou blancs sont écartés ici : ce sont des artefacts de sérialisation, pas une demande.</p>
     */
    public Set<String> selection() {
        if (creneauxAjustables == null) {
            return null;
        }
        Set<String> selection = new LinkedHashSet<>();
        for (String id : creneauxAjustables) {
            if (id != null && !id.isBlank()) {
                selection.add(id);
            }
        }
        return selection;
    }
}
