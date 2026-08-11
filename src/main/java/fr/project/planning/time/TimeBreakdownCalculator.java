package fr.project.planning.time;

import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public final class TimeBreakdownCalculator {

    public TimeBreakdown compute(Creneau creneau, RegulatoryParameters rp, boolean compteDansCharge) {
        Objects.requireNonNull(creneau, "creneau");
        Objects.requireNonNull(rp, "regulatoryParameters");

        if (!compteDansCharge) {
            return zero();
        }

        long minutesTravaillees = creneau.getDuree();

        LocalTime debutNuit = debutNuitEffectif(creneau, rp);
        LocalTime finNuit = finNuitEffectif(creneau, rp);

        // Répartition jour0 / jour1 sans modifier Creneau
        Split split = splitJour0Jour1(creneau.getHeureDebut(), creneau.getHeureFin());

        LocalDate d0 = creneau.getDate();
        LocalDate d1 = d0.plusDays(1);

        boolean d0Dimanche = (d0.getDayOfWeek() == DayOfWeek.SUNDAY);
        boolean d1Dimanche = (split.minutesJour1 > 0) && (d1.getDayOfWeek() == DayOfWeek.SUNDAY);

        boolean d0Ferie = rp.estJourFerie(d0);
        boolean d1Ferie = (split.minutesJour1 > 0) && rp.estJourFerie(d1);

        long minutesDimanche = (d0Dimanche ? split.minutesJour0 : 0) + (d1Dimanche ? split.minutesJour1 : 0);
        long minutesFerie = (d0Ferie ? split.minutesJour0 : 0) + (d1Ferie ? split.minutesJour1 : 0);

        // Minutes de nuit par jour civil (pour intersections fiables)
        long nuitJour0 = minutesNuitDansSegment(split.jour0Debut, split.jour0Fin, debutNuit, finNuit);
        long nuitJour1 = (split.minutesJour1 > 0)
                ? minutesNuitDansSegment(LocalTime.MIDNIGHT, split.jour1Fin, debutNuit, finNuit)
                : 0;

        long minutesNuit = nuitJour0 + nuitJour1;

        long minutesNuitEtDimanche = (d0Dimanche ? nuitJour0 : 0) + (d1Dimanche ? nuitJour1 : 0);
        long minutesNuitEtFerie = (d0Ferie ? nuitJour0 : 0) + (d1Ferie ? nuitJour1 : 0);

        long minutesDimancheEtFerie =
                (d0Dimanche && d0Ferie ? split.minutesJour0 : 0)
                        + (d1Dimanche && d1Ferie ? split.minutesJour1 : 0);

        long minutesNuitEtDimancheEtFerie =
                (d0Dimanche && d0Ferie ? nuitJour0 : 0)
                        + (d1Dimanche && d1Ferie ? nuitJour1 : 0);

        return new TimeBreakdown(
                minutesTravaillees,
                minutesNuit,
                minutesDimanche,
                minutesFerie,
                minutesNuitEtDimanche,
                minutesNuitEtFerie,
                minutesDimancheEtFerie,
                minutesNuitEtDimancheEtFerie
        );
    }

    /**
     * Début de la plage de nuit applicable à ce créneau (lot S8.1).
     *
     * <p>Celui du salarié affecté s'il en déclare un, sinon le paramètre réglementaire global.
     * La plage de nuit est une donnée de la <strong>personne</strong> autant que du cadre : un
     * salarié veilleur et un salarié faisant du travail de nuit occasionnel ne relèvent pas
     * nécessairement des mêmes horaires.</p>
     *
     * <p>Conséquence assumée : les mêmes heures ne produisent pas les mêmes minutes de nuit selon
     * qui les exécute. C'est la traduction fidèle du contrat, et cela fait dépendre la pénibilité
     * d'un créneau de son affectation.</p>
     */
    static LocalTime debutNuitEffectif(Creneau creneau, RegulatoryParameters rp) {
        return creneau.getRessourceAffectee() instanceof SalarieReel salarie
                ? salarie.heureDebutNuitEffective(rp.getHeureDebutNuit())
                : rp.getHeureDebutNuit();
    }

    /** Fin de la plage de nuit applicable à ce créneau. Voir {@link #debutNuitEffectif}. */
    static LocalTime finNuitEffectif(Creneau creneau, RegulatoryParameters rp) {
        return creneau.getRessourceAffectee() instanceof SalarieReel salarie
                ? salarie.heureFinNuitEffective(rp.getHeureFinNuit())
                : rp.getHeureFinNuit();
    }

    private TimeBreakdown zero() {
        return new TimeBreakdown(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Calcule la part du créneau sur le jour de départ (jour0) et sur le jour suivant (jour1),
     * sans LocalDateTime et sans modifier Creneau.
     */
    // Paramètres : (debutJour0, finJour0, minutesJour0, minutesJour1, finJour1)
    private Split splitJour0Jour1(LocalTime heureDebut, LocalTime heureFin) {
        if (!traverseMinuit(heureDebut, heureFin)) {
            long minutes = minutesBetween(heureDebut, heureFin);
            return new Split(heureDebut, heureFin, minutes,0, LocalTime.MIDNIGHT);
        }

        // Traverse minuit : jour0 = [debut, 24:00), jour1 = [00:00, fin)
        long minutesJour0 = minutesBetween(heureDebut, LocalTime.MIDNIGHT); // 24:00 représenté par MIDNIGHT du lendemain
        long minutesJour1 = minutesBetween(LocalTime.MIDNIGHT, heureFin);

        // jour0Fin = MIDNIGHT (fin de journée)
        return new Split(heureDebut, LocalTime.MIDNIGHT, minutesJour0, minutesJour1, heureFin);
    }

    private boolean traverseMinuit(LocalTime debut, LocalTime fin) {
        return fin.isBefore(debut);
    }

    private long minutesBetween(LocalTime start, LocalTime end) {
        // start inclusive, end exclusive conceptuellement
        int startMin = start.getHour() * 60 + start.getMinute();
        int endMin = end.getHour() * 60 + end.getMinute();
        if (endMin >= startMin) return endMin - startMin;
        // ne devrait pas arriver ici si on appelle correctement, mais safe
        return (24 * 60 - startMin) + endMin;
    }

    /**
     * Minutes de nuit dans un segment horaire (sur un seul jour civil),
     * en gérant le fait que la plage nuit peut traverser minuit (22:00-06:00).
     */
    private long minutesNuitDansSegment(
            LocalTime segmentDebut,
            LocalTime segmentFin,
            LocalTime debutNuit,
            LocalTime finNuit
    ) {
        if (segmentDebut.equals(segmentFin)) return 0;

        // Cas simple : plage nuit ne traverse pas minuit
        if (!finNuit.isBefore(debutNuit)) {
            return intersectMinutes(segmentDebut, segmentFin, debutNuit, finNuit);
        }

        // Plage nuit traverse minuit (ex 22:00->06:00)
        // Sur un jour civil, la nuit se compose de :
        // - [00:00, finNuit)  (partie "matin")
        // - [debutNuit, 24:00) (partie "soir")
        long partMatin = intersectMinutes(segmentDebut, segmentFin, LocalTime.MIDNIGHT, finNuit);
        long partSoir = intersectMinutes(segmentDebut, segmentFin, debutNuit, LocalTime.MIDNIGHT);
        return partMatin + partSoir;
    }

    /**
     * Intersection en minutes de deux intervalles [aStart, aEnd) et [bStart, bEnd) sur un jour civil.
     * Note : aEnd ou bEnd peuvent être à minuit pour représenter la fin de journée, mais pas les deux en même temps.
     */
    private long intersectMinutes(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        int aS = toMinuteIndex(aStart);
        int aE = toMinuteIndexEnd(aStart, aEnd);

        int bS = toMinuteIndex(bStart);
        int bE = toMinuteIndexEnd(bStart, bEnd);

        int start = Math.max(aS, bS);
        int end = Math.min(aE, bE);
        return Math.max(0, end - start);
    }

    private int toMinuteIndex(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    /**
    * Convertit une borne de fin en minute-index, avec gestion explicite du cas "fin de journée".
    * Règle : si end == MIDNIGHT et start != MIDNIGHT => on considère que end = 24:00 (1440).
    * Sinon end est simplement son index (00:00 => 0).
    */
    private int toMinuteIndexEnd(LocalTime start, LocalTime end) {
        if (end.equals(LocalTime.MIDNIGHT) && !start.equals(LocalTime.MIDNIGHT)) {
            return 24 * 60;
        }
        return toMinuteIndex(end);
    }   

    private static final class Split {
        final LocalTime jour0Debut;
        final LocalTime jour0Fin;      // MIDNIGHT = fin de journée
        final long minutesJour1;       // 0 si pas de traversée
        final long minutesJour0;
        final LocalTime jour1Fin;      // fin réelle sur jour1, MIDNIGHT si 0

        Split(LocalTime jour0Debut, LocalTime jour0Fin,
              long minutesJour0, long minutesJour1, LocalTime jour1Fin) {
            this.jour0Debut = jour0Debut;
            this.jour0Fin = jour0Fin;
            this.minutesJour0 = minutesJour0;
            this.minutesJour1 = minutesJour1;
            this.jour1Fin = jour1Fin;
        }
    }
}