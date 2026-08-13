package fr.project.planning.scenarios.dto;

import java.util.List;

/**
 * ArbitrageDTO — ce que SC-05 restitue en propre, absent des autres scénarios.
 *
 * <p>Il est à SC-05 ce que {@link RemplacementDTO} est à SC-02 : la réponse à <em>qu'est-ce qui a
 * bougé, et pour qui</em>. Comme lui, et comme {@code candidats[]} pour SC-06, cette clé
 * n'apparaît que dans la réponse du scénario qui la produit — une réponse SC-01 ou SC-03 ne doit
 * pas gagner un bloc vide qui laisserait croire à une capacité inexistante.</p>
 *
 * <h3>Ce que le bloc dit, et que le planning seul ne dit pas</h3>
 * <p>Le bloc {@code planning} restitue l'état d'<strong>après</strong>. Il ne dit ni d'où l'on
 * vient, ni ce que l'arbitrage a coûté à chacun. SC-05 rend une répartition dont le propre est
 * d'être <em>justifiable ligne à ligne devant les intéressés</em> : sans l'avant, la justification
 * manque de son terme de comparaison.</p>
 *
 * <h3>Les compteurs ne se recomposent pas comme ceux de SC-02</h3>
 * <p>Un créneau du périmètre peut être resté chez son titulaire, avoir changé de main, être resté
 * épinglé sur un tiers, ou n'avoir trouvé personne. Les trois compteurs ci-dessous découpent le
 * périmètre selon <strong>ce qui lui est arrivé</strong>, et un créneau inchangé n'entre dans aucun
 * des trois — c'est le cas le plus fréquent et le moins intéressant.</p>
 *
 * @param ressourcesArbitrees        salariés entre lesquels l'arbitrage a porté. Un
 *                                   <strong>ensemble</strong>, de taille deux aujourd'hui : le
 *                                   « deux » ne vit que dans le contrat d'entrée (§5.5 du cadrage)
 * @param creneauxArbitres           créneaux du périmètre <strong>effectivement soumis</strong> au
 *                                   solveur. Peut être inférieur au périmètre demandé lorsque
 *                                   celui-ci nomme des créneaux absents du dataset — une alerte
 *                                   {@code CRENEAU_ARBITRE_INTROUVABLE} le dit alors
 * @param creneauxDeplaces           ceux qui ont changé de main
 * @param creneauxEpinglesSurUnTiers ceux qui n'ont pas pu bouger parce qu'un tiers les tient
 *                                   (§5.2). Ce n'est pas un échec de l'arbitrage : c'est le
 *                                   respect du travail de quelqu'un qui n'a rien demandé
 * @param creneauxNonCouverts        ceux que personne ne tient au bout du compte. Le moteur ne
 *                                   refuse pas — il rend visible ce qu'il n'a pas su couvrir
 * @param parSalarie                 l'avant et l'après de chaque salarié arbitré
 * @param details                    le sort de chaque créneau du périmètre, y compris ceux que
 *                                   l'arbitrage n'a pas déplacés
 */
public record ArbitrageDTO(
        List<String> ressourcesArbitrees,
        int creneauxArbitres,
        int creneauxDeplaces,
        int creneauxEpinglesSurUnTiers,
        int creneauxNonCouverts,
        List<MouvementSalarieDTO> parSalarie,
        List<CreneauArbitreDTO> details) {

    public ArbitrageDTO {
        ressourcesArbitrees = ressourcesArbitrees == null ? List.of() : List.copyOf(ressourcesArbitrees);
        parSalarie = parSalarie == null ? List.of() : List.copyOf(parSalarie);
        details = details == null ? List.of() : List.copyOf(details);
    }
}
