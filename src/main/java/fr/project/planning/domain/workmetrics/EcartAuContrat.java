package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.ressource.ContratSalarie;

import java.time.temporal.ChronoUnit;

/**
 * EcartAuContrat — de combien cette personne est-elle au-dessus, ou en dessous, de son contrat.
 *
 * <h3>Pourquoi le contrat et non la moyenne du groupe</h3>
 * <p>Comparer à la moyenne collective en heures brutes mettrait un salarié à 50 % perpétuellement
 * « sous la moyenne », et le moteur passerait son temps à vouloir le charger. La référence est
 * donc <strong>le contrat de chacun</strong> : deux personnes sont comparables par ce qu'elles
 * font rapporté à ce qu'elles doivent, jamais par leurs volumes absolus.</p>
 *
 * <p>Illustration de l'arbitrage : un salarié à 30 h à qui l'on demande 35 h est à +16,7 % ; un
 * salarié à 35 h à qui l'on demande 40 h est à +14,3 %. Le second est le moins sollicité, bien
 * qu'il travaille cinq heures de plus.</p>
 *
 * <h3>L'écart est signé</h3>
 * <p>La sous-charge compte autant que la surcharge. Sans signe, le moteur se contenterait d'éviter
 * de surcharger : il ne rééquilibrerait jamais, et l'équité ne se produirait pas — elle serait
 * seulement moins violée.</p>
 *
 * <h3>La référence est proratisée sur la fenêtre observée, absences déduites</h3>
 * <p>Le contrat s'exprime par semaine, la mesure porte sur ce que l'appelant a transmis. La
 * référence vaut donc {@code heuresHebdomadaires × jours disponibles / 7}.</p>
 *
 * <p><strong>Jours disponibles, et non jours de la fenêtre</strong> — rang 14. Proratiser sur
 * l'horizon entier ferait lire une absence déclarée comme du temps disponible non travaillé : le
 * salarié revenant de congé paraîtrait sous son contrat, donc préférable, et le moteur lui
 * rattraperait son absence de part et d'autre. Le congé est un fait, pas un déficit. Voir
 * {@link JoursDisponibles}.</p>
 *
 * <p>C'est aussi ce qui traite l'annualisation sans cas particulier : pour un salarié annualisé,
 * {@code heuresHebdomadairesHabituelles} décrit une <strong>moyenne</strong>, et une semaine
 * au-dessus n'est pas une anomalie mais l'objet même de l'annualisation. Lue sur toute la fenêtre,
 * la mesure ne le pénalise pas d'avoir travaillé comme son contrat le prévoit. Plus la fenêtre est
 * large, plus le chiffre a de sens — pour lui comme pour les autres.</p>
 */
public final class EcartAuContrat {

    private EcartAuContrat() {
    }

    /**
     * Volume contractuel attendu sur la fenêtre entière, en minutes — <strong>sans déduire les
     * absences</strong>.
     *
     * <p>⚠️ Ne convient qu'à un salarié dont on sait qu'il était disponible tout du long, ou à un
     * calcul qui ne compare personne. Toute mesure d'équité doit passer par la surcharge à
     * {@code joursDisponibles} : voir {@link JoursDisponibles} et le rang 14.</p>
     *
     * @return {@code null} si le contrat ne déclare pas de volume hebdomadaire — auquel cas rien
     *         n'est comparable, et le moteur préfère ne rien dire plutôt que de supposer
     */
    public static Double minutesAttendues(ContratSalarie contrat, HorizonTemporel horizon) {
        if (horizon == null || horizon.getDateDebut() == null || horizon.getDateFin() == null) {
            return null;
        }
        return minutesAttendues(contrat, joursObserves(horizon));
    }

    /**
     * Volume contractuel attendu sur les seuls jours où le salarié était disponible, en minutes.
     *
     * <p>C'est la forme à employer dès qu'on compare deux personnes. Proratiser sur l'horizon
     * entier ferait lire une absence comme du temps disponible non travaillé, et le moteur
     * rattraperait le congé au lieu de le respecter.</p>
     *
     * @param joursDisponibles jours de la fenêtre hors indisponibilité, de {@link JoursDisponibles}
     * @return {@code null} si le contrat ne déclare pas de volume hebdomadaire, ou si
     *         {@code joursDisponibles} est nul — un salarié absent toute la fenêtre n'est pas à
     *         −100 % de son contrat, il est <strong>hors de comparaison</strong>
     */
    public static Double minutesAttendues(ContratSalarie contrat, long joursDisponibles) {
        if (contrat == null || contrat.getHeuresHebdomadairesHabituelles() == null) {
            return null;
        }
        double heuresHebdo = contrat.getHeuresHebdomadairesHabituelles();
        if (heuresHebdo <= 0 || joursDisponibles <= 0) {
            return null;
        }
        return heuresHebdo * 60.0 * joursDisponibles / 7.0;
    }

    /**
     * Part d'un volume dans le contrat attendu, en pourcentage — ou {@code null} sans référence.
     *
     * <p>Sert aussi bien aux pénibilités qu'à la charge : rapporter les nuits au contrat les rend
     * comparables entre un temps plein et un mi-temps, exactement comme les heures.</p>
     */
    public static Double pourcentageDuContrat(double minutes, Double minutesAttendues) {
        if (minutesAttendues == null || minutesAttendues <= 0) {
            return null;
        }
        return arrondi(minutes / minutesAttendues * 100.0);
    }

    /** Écart <strong>signé</strong> au contrat, en pourcentage — ou {@code null} sans référence. */
    public static Double ecartPourcent(double minutesRealisees, Double minutesAttendues) {
        if (minutesAttendues == null || minutesAttendues <= 0) {
            return null;
        }
        return arrondi((minutesRealisees - minutesAttendues) / minutesAttendues * 100.0);
    }

    /** Jours calendaires de la fenêtre, bornes comprises. */
    public static long joursObserves(HorizonTemporel horizon) {
        return ChronoUnit.DAYS.between(horizon.getDateDebut(), horizon.getDateFin()) + 1;
    }

    private static double arrondi(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }
}
