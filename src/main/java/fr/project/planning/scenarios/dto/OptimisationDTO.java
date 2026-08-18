package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

/**
 * OptimisationDTO — ce que SC-04 a fait du planning transmis, et ce que cela a coûté à chacun.
 *
 * <h3>Pourquoi ce bloc existe en plus de {@code planning}</h3>
 * <p>{@code planning} ne restitue que l'<strong>après</strong>. Il ne dit ni d'où l'on vient, ni ce
 * que l'optimisation a coûté — or le contrat demande explicitement « gains / régressions
 * explicitées ; indicateurs comparatifs », et un planning remanié sur une période entière doit
 * pouvoir se justifier devant ceux qu'il déplace. Même raison que le bloc {@code arbitrage} de
 * SC-05, à une échelle plus large.</p>
 *
 * <h3>La date pivot est restituée</h3>
 * <p>Elle est le contrat de cette optimisation : ce qui la précède n'a pas pu bouger. La rendre
 * évite à l'appelant de recouper sa propre demande pour interpréter le résultat, et rend la réponse
 * lisible seule — un fichier d'{@code outbox} relu six mois plus tard porte alors sa propre clé.</p>
 *
 * @param datePivot          premier jour ajustable, tel qu'appliqué
 * @param creneauxFiges      créneaux épinglés parce qu'antérieurs au pivot
 * @param creneauxAjustables créneaux rendus au solveur
 * @param creneauxDeplaces   créneaux ayant effectivement changé de main
 * @param creneauxNonCouverts créneaux ajustables que personne ne couvre à l'arrivée
 * @param acceptable         aucun motif éliminatoire. ⚠️ au regard de ce que le moteur sait : les
 *                           préférences individuelles lui échappent (rang 10)
 * @param motifs             ce qui disqualifie ce planning, ou le décrit
 * @param parSalarie         le mouvement et les séries de chaque salarié concerné
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OptimisationDTO(
        LocalDate datePivot,
        int creneauxFiges,
        int creneauxAjustables,
        int creneauxDeplaces,
        int creneauxNonCouverts,
        boolean acceptable,
        List<MotifArbitrageDTO> motifs,
        List<SerieSalarieDTO> parSalarie) {
}
