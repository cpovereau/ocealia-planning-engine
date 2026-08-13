package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * MouvementSalarieDTO — ce que l'arbitrage a changé pour l'un des salariés arbitrés.
 *
 * <h3>Deux lectures, et elles ne se déduisent pas l'une de l'autre</h3>
 * <p>Les <strong>compteurs de créneaux</strong> disent combien d'objets ont changé de main ; les
 * <strong>heures et l'écart</strong> disent ce que l'arbitrage a réellement déplacé. Reprendre un
 * créneau de huit heures et en céder un d'une heure fait « un repris, un cédé » et n'a rien d'un
 * échange équilibré.</p>
 *
 * <h3>Pourquoi l'écart au contrat, et pas seulement les heures</h3>
 * <p>C'est la seule des deux grandeurs qui soit <strong>comparable entre deux personnes</strong> :
 * elle rapporte ce que chacun fait à ce que son contrat prévoit. Deux salariés qui travaillent tous
 * deux 32 h ne sont pas dans la même situation si l'un est à 35 h de contrat et l'autre à 24 h. Le
 * chantier équité a livré cette mesure au lot L2, et c'est elle que la contrainte du lot L5 pèse.</p>
 *
 * <p>L'écart est mesuré <strong>avant et après par le même calcul</strong>. Deux mesures obtenues
 * autrement se compareraient mal, et l'appelant lirait comme un mouvement ce qui ne serait qu'une
 * différence de méthode.</p>
 *
 * @param ressourceId                identifiant du salarié
 * @param creneauxRepris             créneaux du périmètre qu'il tient au bout de l'arbitrage et
 *                                   qu'il ne tenait pas avant
 * @param creneauxCedes              créneaux du périmètre qu'il tenait et qu'il ne tient plus
 * @param heuresAvant                heures travaillées sur l'horizon avant arbitrage, en heures
 *                                   décimales — heures brutes, comme {@code workMetrics}
 * @param heuresApres                heures travaillées après arbitrage
 * @param ecartContratAvantPourcent  écart signé à son contrat avant arbitrage, en points de
 *                                   pourcentage. {@code null} si son contrat n'est pas connu —
 *                                   une borne absente n'est pas une borne à zéro
 * @param ecartContratApresPourcent  le même écart après arbitrage. C'est la comparaison des deux
 *                                   qui dit si l'arbitrage a rapproché ou éloigné
 */
public record MouvementSalarieDTO(
        String ressourceId,
        int creneauxRepris,
        int creneauxCedes,
        double heuresAvant,
        double heuresApres,
        @JsonInclude(JsonInclude.Include.NON_NULL) Double ecartContratAvantPourcent,
        @JsonInclude(JsonInclude.Include.NON_NULL) Double ecartContratApresPourcent) {
}
