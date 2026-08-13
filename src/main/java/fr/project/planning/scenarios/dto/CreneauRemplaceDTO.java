package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CreneauRemplaceDTO — le sort d'un créneau libéré par l'absence : avant, après, et de qui.
 *
 * <p>Un créneau apparaît ici <strong>quel que soit son sort</strong>, y compris lorsqu'il n'a
 * trouvé personne. Un remplacement qui n'a pas eu lieu est une information, pas un silence.</p>
 *
 * <h3>[S4] Les heures sont au format {@code HH:mm}, comme partout ailleurs dans la réponse</h3>
 * <p>Sérialisées par défaut, elles sortaient en {@code HH:mm:ss} — le bloc {@code planning} de la
 * même réponse, lui, produit {@code HH:mm}. Deux formats pour la même grandeur dans un seul
 * document obligeraient l'appelant à savoir lequel s'applique où. Corrigé pendant que SC-02 n'est
 * pas encore intégré, plutôt que d'imposer plus tard une seconde migration de contrat.</p>
 *
 * @param creneauId        identifiant du créneau, restitué tel qu'il a été reçu
 * @param creneauOrigineId créneau dont celui-ci est un morceau, ou {@code null} lorsqu'il n'a pas
 *                         été découpé — la clé est alors omise. Il existe pour que le rattachement
 *                         ne repose jamais sur l'analyse d'une chaîne de caractères
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
        @JsonFormat(pattern = "HH:mm") LocalTime heureDebut,
        @JsonFormat(pattern = "HH:mm") LocalTime heureFin,
        int dureeMinutes,
        String ressourceAvantId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ressourceApresId,
        NatureCouverture nature) {
}
