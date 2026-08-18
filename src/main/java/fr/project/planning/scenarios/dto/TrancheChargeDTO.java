package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * TrancheChargeDTO — la charge d'un salarié sur une tranche, avant et après optimisation.
 *
 * <p>C'est l'unité de lecture de SC-04 : « sur la semaine du 18, SAL-A passe de −60 % à −5 % ».
 * Un total sur la période dirait <em>combien</em> ; la tranche dit <strong>quand</strong>, et c'est
 * ce que ce scénario apporte et qu'aucun autre n'apporte.</p>
 *
 * <p>Les deux états sont mesurés par le <strong>même calculateur</strong> sur la même fenêtre :
 * sans cela, une différence de méthode se lirait comme un mouvement du planning.</p>
 *
 * @param granularite              {@code SEMAINE}, {@code MOIS} ou {@code PERIODE}
 * @param debut                    premier jour de la tranche, jamais avant l'horizon
 * @param fin                      dernier jour de la tranche, jamais après l'horizon
 * @param partielle                la tranche ne couvre pas toute sa granularité — une semaine
 *                                 tronquée par le bord de l'horizon. Ses <strong>volumes bruts</strong>
 *                                 ne se comparent pas à ceux d'une tranche pleine ; son écart au
 *                                 contrat, lui, se compare, puisqu'il proratise
 * @param joursDisponiblesAvant    jours de la tranche où le salarié n'était pas indisponible
 * @param joursDisponiblesApres    identique par construction — une optimisation ne déplace pas les
 *                                 congés — et restitué pour que l'appelant puisse le vérifier
 * @param heuresAvant              heures travaillées avant, décimales
 * @param heuresApres              heures travaillées après
 * @param ecartContratAvantPourcent écart signé au contrat avant, ou {@code null} si rien n'est
 *                                 comparable — pas de volume contractuel déclaré, ou aucun jour
 *                                 disponible sur la tranche
 * @param ecartContratApresPourcent écart signé au contrat après
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrancheChargeDTO(
        String granularite,
        LocalDate debut,
        LocalDate fin,
        boolean partielle,
        int joursDisponiblesAvant,
        int joursDisponiblesApres,
        double heuresAvant,
        double heuresApres,
        Double ecartContratAvantPourcent,
        Double ecartContratApresPourcent) {
}
