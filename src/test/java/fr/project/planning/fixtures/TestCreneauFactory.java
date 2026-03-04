package fr.project.planning.fixtures;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class TestCreneauFactory {

    private static final String LIEU_DEFAUT = "L1";
    private static final String ACTIVITE_DEFAUT = "ACT1";
    private static final String POSTE_COMPTABLE_DEFAUT = "PC1";

    private TestCreneauFactory() {}

    // ----------------------------
    // API simple et lisible (V3-friendly)
    // ----------------------------

    /** Créneau "jour" simple. Ne porte aucune vérité réglementaire (nuit/férié/dimanche). */
    public static Creneau jour(LocalDate date, int hDebut, int mDebut, int hFin, int mFin) {
        return createNeutre(
                date,
                LocalTime.of(hDebut, mDebut),
                LocalTime.of(hFin, mFin),
                TypeCreneau.IMPOSE
        );
    }

    /** Variante "jour standard" pratique. */
    public static Creneau jourStandard(LocalDate date) {
        return jour(date, 9, 0, 17, 0);
    }

    /** Exemple : samedi 22:00 -> dimanche 06:00 (heureFin < heureDebut). */
    public static Creneau traverseMinuit(LocalDate dateDepart, int hDebut, int mDebut, int hFin, int mFin) {
        return createNeutre(
                dateDepart,
                LocalTime.of(hDebut, mDebut),
                LocalTime.of(hFin, mFin),
                TypeCreneau.IMPOSE
        );
    }

    /** Variante "nuit standard" (utile pour déclencher des intersections de nuit via TimeBreakdownCalculator). */
    public static Creneau nuitStandard(LocalDate dateDepart) {
        return traverseMinuit(dateDepart, 22, 0, 6, 0);
    }

    // ----------------------------
    // Créateurs centraux
    // ----------------------------

    /**
     * Créateur neutre : ne permet pas de "classer" un créneau en nuit/férié/dimanche via des indicateurs.
     *
     * NOTE V3 :
     * - La vérité réglementaire (nuit, dimanche, férié) doit venir de RegulatoryParameters + calcul d'intersection.
     * - Les champs TypePlageHoraire / QualificationJour (s'ils existent encore dans le modèle) doivent être traités
     *   comme indicatifs et ne doivent jamais servir de source de vérité dans les contraintes ni WorkMetrics.
     */
    public static Creneau createNeutre(
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin,
            TypeCreneau type
    ) {
        // Valeurs indicatives par défaut (NE DOIVENT PAS être utilisées comme vérité réglementaire)
        TypePlageHoraire typePlageHoraireIndicatif = TypePlageHoraire.JOUR;
        QualificationJour qualificationJourIndicative = QualificationJour.OUVRE;

        return createAvecIndicateurs(
                date,
                heureDebut,
                heureFin,
                type,
                typePlageHoraireIndicatif,
                qualificationJourIndicative
        );
    }

    /**
     * Créateur avancé : conservé pour compatibilité si des tests dépendent encore de ces champs.
     * À utiliser uniquement si vous testez explicitement un comportement "indicatif" (et non réglementaire).
     */
    public static Creneau createAvecIndicateurs(
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin,
            TypeCreneau type,
            TypePlageHoraire typePlageHoraire,
            QualificationJour qualificationJour
    ) {
        if (heureDebut.equals(heureFin)) {
            throw new IllegalArgumentException("Invariant fixture: heureDebut != heureFin");
        }

        int duree = (int) dureeMinutes(heureDebut, heureFin);

        // V3 : jourFerie sur Creneau n'est pas une source de vérité -> on force à false.
        // La source de vérité : RegulatoryParameters (jours fériés) + calcul d'intersection.
        boolean jourFerie = false;

        return new Creneau(
                idAuto(),
                date,
                heureDebut,
                heureFin,
                duree,
                LIEU_DEFAUT,
                ACTIVITE_DEFAUT,
                POSTE_COMPTABLE_DEFAUT,
                PrioriteCreneau.NORMALE,
                type,
                typePlageHoraire,
                jourFerie,
                qualificationJour
        );
    }

    private static String idAuto() {
        return "T-" + UUID.randomUUID();
    }

    /** Durée en minutes, gère la traversée minuit. */
    private static long dureeMinutes(LocalTime debut, LocalTime fin) {
        int start = debut.getHour() * 60 + debut.getMinute();
        int end = fin.getHour() * 60 + fin.getMinute();
        if (end > start) {
            return end - start;
        }
        // traverse minuit
        return (24 * 60 - start) + end;
    }
}