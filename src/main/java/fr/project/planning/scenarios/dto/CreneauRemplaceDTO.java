package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CreneauRemplaceDTO — le sort d'un créneau libéré par l'absence : avant, après, et de qui.
 *
 * <p>Un créneau apparaît ici <strong>quel que soit son sort</strong>, y compris lorsqu'il n'a
 * trouvé personne. Un remplacement qui n'a pas eu lieu est une information, pas un silence.</p>
 *
 * @param creneauId        identifiant du créneau, restitué tel qu'il a été reçu
 * @param creneauOrigineId créneau dont celui-ci est un morceau. Toujours {@code null} au lot S1,
 *                         qui ne découpe pas : le champ existe pour que le contrat ne change pas
 *                         quand le découpage arrivera (lot S2), et il est omis tant qu'il est nul
 * @param date             jour de début du créneau
 * @param heureDebut       heure de début
 * @param heureFin         heure de fin, le lendemain si elle n'est pas postérieure au début
 * @param dureeMinutes     durée telle que portée par le créneau, jamais recalculée
 * @param ressourceAvantId salarié absent à qui le créneau était affecté
 * @param ressourceApresId qui le reprend, ou {@code null} si personne
 * @param nature           ce qui couvre le créneau au bout du compte
 */
public record CreneauRemplaceDTO(
        String creneauId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String creneauOrigineId,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        int dureeMinutes,
        String ressourceAvantId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ressourceApresId,
        NatureCouverture nature) {
}
