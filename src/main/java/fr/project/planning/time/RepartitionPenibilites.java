package fr.project.planning.time;

import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.domain.contexte.DominancePenibilites;
import fr.project.planning.scoring.PenibiliteType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RepartitionPenibilites — à quelle catégorie appartient chaque minute travaillée.
 *
 * <h3>Le problème que ça résout</h3>
 * <p>Une minute peut être à la fois de nuit, un dimanche et un jour férié. La compter dans les
 * trois la pénaliserait trois fois pour une seule pénibilité subie. Le moteur applique donc une
 * <strong>dominance</strong> — {@code NUIT > DIMANCHE > FERIE} par défaut, paramétrable — et
 * attribue chaque minute à sa catégorie dominante, et à elle seule.</p>
 *
 * <p>Ce calcul existait, mais enfoui dans la construction du score. Le chantier équité en a besoin
 * pour une tout autre raison — pondérer des heures afin de les rendre comparables — et deux
 * implémentations de la même dominance auraient fini par diverger. C'est le genre d'écart que ce
 * projet passe son temps à réparer ; il est extrait ici, une fois.</p>
 *
 * <h3>Ce que la répartition rend</h3>
 * <p>Les minutes par catégorie, plus les <strong>minutes ordinaires</strong> — celles qui ne
 * relèvent d'aucune pénibilité. Leur somme vaut exactement les minutes travaillées : toute minute
 * est quelque part, et n'est nulle part deux fois.</p>
 */
public final class RepartitionPenibilites {

    private final Map<PenibiliteType, Long> parCategorie;
    private final long minutesOrdinaires;

    private RepartitionPenibilites(Map<PenibiliteType, Long> parCategorie, long minutesOrdinaires) {
        this.parCategorie = parCategorie;
        this.minutesOrdinaires = minutesOrdinaires;
    }

    /** Minutes attribuées à cette catégorie après application de la dominance. */
    public long minutes(PenibiliteType type) {
        return parCategorie.getOrDefault(type, 0L);
    }

    /** Minutes ne relevant d'aucune pénibilité. */
    public long minutesOrdinaires() {
        return minutesOrdinaires;
    }

    /**
     * [Équité L3] Les minutes ramenées à l'unité commune de l'heure ordinaire.
     *
     * <p>Une seule implémentation de « pondérer », parce que deux consommateurs en ont besoin : le
     * moteur, qui mesure, et le harnais de calibration, qui rejoue la même mesure sous d'autres
     * coefficients. Si le harnais recalculait de son côté, il calibrerait quelque chose que le
     * moteur ne calcule pas — et personne ne le verrait.</p>
     *
     * <p>Fractionnaire par nature : un coefficient de 1,5 sur 47 minutes ne tombe pas juste. La
     * mesure sert à comparer, pas à facturer.</p>
     */
    public double minutesPondereesPar(CoefficientsPenibilite coefficients) {
        double ponderees = minutesOrdinaires * CoefficientsPenibilite.ORDINAIRE;
        for (PenibiliteType type : PenibiliteType.values()) {
            ponderees += minutes(type) * coefficients.pour(type);
        }
        return ponderees;
    }

    /**
     * [Équité L3] Une répartition dont la dominance est <strong>déjà tranchée</strong>.
     *
     * <p>Le harnais de calibration part des volumes que le moteur a restitués, catégorie par
     * catégorie : la dominance y a été appliquée une fois pour toutes, et la rejouer n'aurait aucun
     * sens. Rejouer la <em>pondération</em>, en revanche, est tout l'objet de la calibration.</p>
     */
    public static RepartitionPenibilites deMinutesDejaReparties(
            long minutesNuit, long minutesDimanche, long minutesFerie, long minutesOrdinaires) {

        Map<PenibiliteType, Long> minutes = new EnumMap<>(PenibiliteType.class);
        minutes.put(PenibiliteType.NUIT, Math.max(0, minutesNuit));
        minutes.put(PenibiliteType.DIMANCHE, Math.max(0, minutesDimanche));
        minutes.put(PenibiliteType.FERIE, Math.max(0, minutesFerie));

        return new RepartitionPenibilites(minutes, Math.max(0, minutesOrdinaires));
    }

    /** Répartit un découpage temporel selon l'ordre de dominance donné. */
    public static RepartitionPenibilites de(TimeBreakdown breakdown, DominancePenibilites dominance) {
        RepartitionPenibilites repartition = de(
                breakdown.minutesNuit(),
                breakdown.minutesDimanche(),
                breakdown.minutesFerie(),
                breakdown.minutesNuitEtDimanche(),
                breakdown.minutesNuitEtFerie(),
                breakdown.minutesDimancheEtFerie(),
                breakdown.minutesNuitEtDimancheEtFerie(),
                dominance);

        long penibles = 0;
        for (PenibiliteType type : PenibiliteType.values()) {
            penibles += repartition.minutes(type);
        }
        return new RepartitionPenibilites(repartition.parCategorie,
                Math.max(0, breakdown.minutesTravaillees() - penibles));
    }

    /**
     * Même répartition, à partir des volumes bruts et de leurs intersections.
     *
     * <p>Les minutes ordinaires ne sont pas calculables ici — le total travaillé n'est pas
     * transmis — et valent 0. Cette forme sert la construction du score, qui n'en a pas besoin.</p>
     */
    public static RepartitionPenibilites de(
            long minutesNuit,
            long minutesDimanche,
            long minutesFerie,
            long minutesNuitEtDimanche,
            long minutesNuitEtFerie,
            long minutesDimancheEtFerie,
            long minutesNuitEtDimancheEtFerie,
            DominancePenibilites dominance) {

        // Sécurité minimale : un volume négatif ne décrit rien.
        long n = Math.max(0, minutesNuit);
        long d = Math.max(0, minutesDimanche);
        long f = Math.max(0, minutesFerie);
        long ndf = Math.max(0, minutesNuitEtDimancheEtFerie);
        long nd = Math.max(0, Math.max(0, minutesNuitEtDimanche) - ndf);
        long nf = Math.max(0, Math.max(0, minutesNuitEtFerie) - ndf);
        long df = Math.max(0, Math.max(0, minutesDimancheEtFerie) - ndf);

        // Segments exclusifs, par inclusion-exclusion.
        long nSeul = Math.max(0, n - nd - nf - ndf);
        long dSeul = Math.max(0, d - nd - df - ndf);
        long fSeul = Math.max(0, f - nf - df - ndf);

        Map<PenibiliteType, Long> minutesFinales = new EnumMap<>(PenibiliteType.class);
        for (PenibiliteType type : PenibiliteType.values()) {
            minutesFinales.put(type, 0L);
        }

        ajouter(minutesFinales, PenibiliteType.NUIT, nSeul);
        ajouter(minutesFinales, PenibiliteType.DIMANCHE, dSeul);
        ajouter(minutesFinales, PenibiliteType.FERIE, fSeul);

        List<PenibiliteType> ordre = dominance.getOrdreDominance();
        auDominant(minutesFinales, ordre, EnumSet.of(PenibiliteType.NUIT, PenibiliteType.DIMANCHE), nd);
        auDominant(minutesFinales, ordre, EnumSet.of(PenibiliteType.NUIT, PenibiliteType.FERIE), nf);
        auDominant(minutesFinales, ordre, EnumSet.of(PenibiliteType.DIMANCHE, PenibiliteType.FERIE), df);
        auDominant(minutesFinales, ordre,
                EnumSet.of(PenibiliteType.NUIT, PenibiliteType.DIMANCHE, PenibiliteType.FERIE), ndf);

        return new RepartitionPenibilites(minutesFinales, 0);
    }

    private static void ajouter(Map<PenibiliteType, Long> minutes, PenibiliteType type, long volume) {
        if (volume <= 0) {
            return;
        }
        minutes.put(type, minutes.get(type) + volume);
    }

    private static void auDominant(Map<PenibiliteType, Long> minutes,
                                   List<PenibiliteType> ordreDominance,
                                   Set<PenibiliteType> candidats,
                                   long volume) {
        if (volume <= 0) {
            return;
        }
        for (PenibiliteType type : ordreDominance) {
            if (candidats.contains(type)) {
                ajouter(minutes, type, volume);
                return;
            }
        }
        // Inatteignable si DominancePenibilites exige les trois valeurs — repli sûr.
        ajouter(minutes, candidats.iterator().next(), volume);
    }
}
