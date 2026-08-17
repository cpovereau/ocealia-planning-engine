package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * DecoupageTemporel — découper l'horizon en semaines, en mois, et en lui-même (lot O1 de SC-04).
 *
 * <h3>Ce que le découpage sert</h3>
 * <p>Un chiffre unique sur un trimestre dit <em>combien</em>, jamais <em>quand</em>. Le
 * regroupement à la semaine et au mois montre où un déséquilibre s'est installé — c'est ce que
 * SC-04 apporte et qu'aucun autre scénario n'apporte.</p>
 *
 * <h3>Les semaines sont ISO, les mois sont calendaires</h3>
 * <p>La semaine commence le <strong>lundi</strong>, comme partout ailleurs dans le moteur — le
 * repos hebdomadaire, les heures minimum par semaine et les bornes hebdomadaires le supposent
 * déjà. Un découpage qui partirait du premier jour transmis produirait des « semaines » que rien
 * d'autre dans le moteur ne reconnaît.</p>
 *
 * <h3>Les bords ne débordent jamais</h3>
 * <p>Une semaine ou un mois à cheval sur le bord de l'horizon est <strong>tronqué</strong>, jamais
 * étendu : le moteur ne juge pas hors de ce qu'il a reçu. La tranche se déclare alors
 * {@link TrancheTemporelle#partielle()}.</p>
 */
public final class DecoupageTemporel {

    private DecoupageTemporel() {
    }

    /**
     * L'horizon découpé en semaines, puis en mois, puis en une tranche unique.
     *
     * <p>Ordre du plus fin au plus large — celui dans lequel on lit un déséquilibre : la semaine
     * le montre, le mois le confirme, la période le juge.</p>
     */
    public static List<TrancheTemporelle> decouper(HorizonTemporel horizon) {
        // HorizonTemporel garantit ses deux bornes : seule son absence est à traiter ici.
        if (horizon == null) {
            return List.of();
        }
        LocalDate debut = horizon.getDateDebut();
        LocalDate fin = horizon.getDateFin();
        if (fin.isBefore(debut)) {
            return List.of();
        }

        List<TrancheTemporelle> tranches = new ArrayList<>();
        tranches.addAll(semaines(debut, fin));
        tranches.addAll(mois(debut, fin));
        tranches.add(new TrancheTemporelle(TrancheTemporelle.Granularite.PERIODE, debut, fin));
        return List.copyOf(tranches);
    }

    private static List<TrancheTemporelle> semaines(LocalDate debut, LocalDate fin) {
        List<TrancheTemporelle> semaines = new ArrayList<>();
        LocalDate curseur = debut;
        while (!curseur.isAfter(fin)) {
            LocalDate dimanche = curseur.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            LocalDate borne = dimanche.isAfter(fin) ? fin : dimanche;
            semaines.add(new TrancheTemporelle(
                    TrancheTemporelle.Granularite.SEMAINE, curseur, borne));
            curseur = borne.plusDays(1);
        }
        return semaines;
    }

    private static List<TrancheTemporelle> mois(LocalDate debut, LocalDate fin) {
        List<TrancheTemporelle> mois = new ArrayList<>();
        LocalDate curseur = debut;
        while (!curseur.isAfter(fin)) {
            LocalDate dernierDuMois = curseur.with(TemporalAdjusters.lastDayOfMonth());
            LocalDate borne = dernierDuMois.isAfter(fin) ? fin : dernierDuMois;
            mois.add(new TrancheTemporelle(TrancheTemporelle.Granularite.MOIS, curseur, borne));
            curseur = borne.plusDays(1);
        }
        return mois;
    }
}
