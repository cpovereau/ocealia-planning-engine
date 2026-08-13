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
 * <h3>La référence est proratisée sur la fenêtre observée</h3>
 * <p>Le contrat s'exprime par semaine, la mesure porte sur ce que l'appelant a transmis. La
 * référence vaut donc {@code heuresHebdomadaires × jours observés / 7}.</p>
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
     * Volume contractuel attendu sur la fenêtre, en minutes.
     *
     * @return {@code null} si le contrat ne déclare pas de volume hebdomadaire — auquel cas rien
     *         n'est comparable, et le moteur préfère ne rien dire plutôt que de supposer
     */
    public static Double minutesAttendues(ContratSalarie contrat, HorizonTemporel horizon) {
        if (contrat == null || contrat.getHeuresHebdomadairesHabituelles() == null
                || horizon == null || horizon.getDateDebut() == null
                || horizon.getDateFin() == null) {
            return null;
        }
        double heuresHebdo = contrat.getHeuresHebdomadairesHabituelles();
        if (heuresHebdo <= 0) {
            return null;
        }
        long jours = joursObserves(horizon);
        return heuresHebdo * 60.0 * jours / 7.0;
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
