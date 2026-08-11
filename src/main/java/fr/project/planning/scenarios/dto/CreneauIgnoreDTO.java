package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * Un créneau signalé par la préparation, et ce que le moteur en a fait — lot S8.4.
 *
 * <p>{@code ignoredCreneaux} ne restituait que trois entiers. Un appelant qui transmet quatre-vingts
 * créneaux et en retrouve soixante-dix-sept savait qu'il en manquait trois, sans jamais pouvoir
 * dire lesquels : la seule trace nominative partait dans un {@code log.warn} côté serveur.</p>
 *
 * @param creneauId identifiant reçu, restitué tel quel — c'est la clé qui permet de retrouver le
 *                  créneau dans la demande d'origine
 * @param date      jour du créneau, omis s'il n'en portait pas
 * @param motif     voir {@link MotifCreneauIgnore}
 * @param exclu     {@code true} si le créneau n'a pas atteint le solveur et ne figure pas dans la
 *                  réponse. Le nom du bloc, hérité, laisse croire que tout ce qu'il compte est
 *                  perdu : c'est faux en SC-01, qui mesure sans écarter, et faux d'un des trois
 *                  compteurs en SC-03. Le champ le dit plutôt que de laisser le nom le suggérer
 * @param message   phrase lisible, reprenant la valeur en cause
 */
public record CreneauIgnoreDTO(
        String creneauId,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate date,
        String motif,
        boolean exclu,
        String message) {

    /** Créneau réellement écarté : il n'atteint pas le solveur et ne figure pas dans la réponse. */
    public static CreneauIgnoreDTO exclu(String creneauId, LocalDate date,
                                         MotifCreneauIgnore motif, String message) {
        return new CreneauIgnoreDTO(creneauId, date, motif.name(), true, message);
    }

    /** Créneau signalé mais conservé : il part au solveur et sera restitué. */
    public static CreneauIgnoreDTO conserve(String creneauId, LocalDate date,
                                            MotifCreneauIgnore motif, String message) {
        return new CreneauIgnoreDTO(creneauId, date, motif.name(), false, message);
    }
}
