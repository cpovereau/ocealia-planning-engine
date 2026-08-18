package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * SerieSalarieDTO — ce que l'optimisation a fait d'un salarié, tranche par tranche.
 *
 * <p>Les compteurs {@code repris} et {@code cedes} disent le mouvement ; les {@code tranches}
 * disent ce qu'il a produit, semaine par semaine puis mois par mois, et enfin sur la période. Un
 * salarié peut très bien être stable sur la période et avoir vu deux de ses semaines s'inverser :
 * c'est exactement le genre de chose qu'un total masque.</p>
 *
 * @param ressourceId identifiant du salarié
 * @param repris      créneaux qui lui ont été confiés et qu'il n'avait pas
 * @param cedes       créneaux qu'il tenait et qui sont passés à quelqu'un d'autre
 * @param tranches    la charge avant/après, dans l'ordre du découpage : semaines, mois, période
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SerieSalarieDTO(
        String ressourceId,
        int repris,
        int cedes,
        List<TrancheChargeDTO> tranches) {
}
