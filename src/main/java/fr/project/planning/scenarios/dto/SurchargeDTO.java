package fr.project.planning.scenarios.dto;

import java.time.LocalDate;

/**
 * SurchargeDTO — ce qu'un remplacement change à la charge de celui qui l'assure, un jour donné.
 *
 * <h3>Deux grandeurs, parce que l'encadrement ne juge pas les deux de la même manière</h3>
 * <p>La surcharge s'exprime <strong>en heures par jour</strong> quand elle porte sur une journée,
 * et <strong>en heures par semaine</strong> quand la semaine est également touchée. Une longue
 * journée dans une semaine creuse n'est pas une semaine chargée faite de journées ordinaires :
 * les deux mesures sont donc rendues côte à côte, et chacune avec son propre seuil.</p>
 *
 * <p>Une entrée par salarié réel et par jour où il reprend quelque chose. La mesure hebdomadaire
 * y est répétée telle quelle pour deux jours d'une même semaine : chaque ligne se lit seule,
 * « ce jour-là, il passe à tant d'heures dans la journée et tant dans la semaine ».</p>
 *
 * <p>{@link ImpactMesureDTO} est repris tel quel de SC-06. La grandeur restituée pour un candidat
 * et celle restituée pour un remplaçant sont la même grandeur ; elles doivent rester le même
 * objet, sans quoi elles finiraient par diverger.</p>
 *
 * <p><strong>Un dépassement signalé décrit une conséquence.</strong> Il ne dit pas qu'une règle
 * l'interdit : le seuil de surcharge est une borne de confort, pesée dans le score et jamais
 * éliminatoire. Les bornes légales, elles, ont leurs propres contraintes.</p>
 *
 * @param ressourceId   salarié qui assure le remplacement
 * @param date          jour concerné
 * @param heuresJour    charge de la journée, avant / après / delta, avec le seuil journalier
 * @param heuresSemaine charge de la semaine calendaire lundi → dimanche contenant ce jour
 */
public record SurchargeDTO(
        String ressourceId,
        LocalDate date,
        ImpactMesureDTO heuresJour,
        ImpactMesureDTO heuresSemaine) {
}
