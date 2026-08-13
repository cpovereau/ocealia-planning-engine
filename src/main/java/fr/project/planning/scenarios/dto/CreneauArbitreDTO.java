package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CreneauArbitreDTO — le sort d'un créneau du périmètre : à qui il était, à qui il est.
 *
 * <p>Décalque de {@link CreneauRemplaceDTO}, à une différence près qui tient au scénario : SC-02
 * part d'un absent, donc {@code ressourceAvantId} y désigne toujours la même personne. Ici chaque
 * créneau du périmètre a son propre titulaire d'avant, et c'est précisément ce que l'arbitrage
 * déplace.</p>
 *
 * <p>Un créneau du périmètre apparaît ici <strong>quel que soit son sort</strong> — déplacé,
 * inchangé, épinglé sur un tiers, ou resté à pourvoir. Un arbitrage qui n'a rien changé est une
 * information, pas un silence.</p>
 *
 * @param creneauId        identifiant du créneau, restitué tel qu'il a été reçu
 * @param creneauOrigineId créneau dont celui-ci est un morceau, ou {@code null} — clé alors omise
 * @param date             jour de début du créneau
 * @param heureDebut       heure de début
 * @param heureFin         heure de fin, le lendemain si elle n'est pas postérieure au début
 * @param dureeMinutes     durée telle que portée par le créneau, jamais recalculée
 * @param ressourceAvantId qui tenait le créneau dans le planning transmis, ou {@code null} si
 *                         personne — un besoin nu est aussi du travail à répartir
 * @param ressourceApresId qui le tient au bout de l'arbitrage, ou {@code null} si personne
 * @param nature           ce qui couvre le créneau au bout du compte
 * @param deplace          {@code true} si le créneau a changé de main. Le déduire de la comparaison
 *                         des deux identifiants obligerait l'appelant à traiter lui-même le cas du
 *                         créneau qui n'était à personne
 * @param tenuParUnTiers   {@code true} si le créneau est resté épinglé parce que tenu par quelqu'un
 *                         qui n'est pas de l'arbitrage (§5.2 du cadrage). Il n'a alors pas pu
 *                         bouger, et ce n'est pas un échec de l'arbitrage
 */
public record CreneauArbitreDTO(
        String creneauId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String creneauOrigineId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime heureDebut,
        @JsonFormat(pattern = "HH:mm") LocalTime heureFin,
        int dureeMinutes,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ressourceAvantId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ressourceApresId,
        NatureCouverture nature,
        boolean deplace,
        boolean tenuParUnTiers) {
}
