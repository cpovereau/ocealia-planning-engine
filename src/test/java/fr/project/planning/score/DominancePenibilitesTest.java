package fr.project.planning.score;

import fr.project.planning.domain.contexte.*;
import fr.project.planning.scoring.PenibiliteType;
import fr.project.planning.scoring.StrategieScoring;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DominancePenibilitesTest {

    @Test
    void dominance_change_l_allocation_des_minutes_d_intersection() {
        // Cas : 60 minutes simultanément NUIT + DIMANCHE (donc 100% en intersection)
        long minutesNuit = 60;
        long minutesDimanche = 60;
        long minutesFerie = 0;

        long minutesNuitEtDimanche = 60;
        long minutesNuitEtFerie = 0;
        long minutesDimancheEtFerie = 0;
        long minutesNuitEtDimancheEtFerie = 0;

        // 1) Dominance : NUIT d’abord
        PlanningContext ctxNuitFirst = contexteAvecDominance(
                StrategieScoring.EXPLOITATION,
                List.of(PenibiliteType.NUIT, PenibiliteType.DIMANCHE, PenibiliteType.FERIE)
        );

        HardSoftScore scoreNuitFirst = ScoreUtils.penalitesLegalesAvecDominance(
                ctxNuitFirst,
                minutesNuit,
                minutesDimanche,
                minutesFerie,
                minutesNuitEtDimanche,
                minutesNuitEtFerie,
                minutesDimancheEtFerie,
                minutesNuitEtDimancheEtFerie
        );

        // EXPLOITATION : NUIT = 3, DIMANCHE = 2 (d’après ScoreWeights)
        // Ici, l’intersection va vers NUIT => 60 * 3 = 180 => score soft = -180
        assertEquals(HardSoftScore.ofSoft(-180), scoreNuitFirst);

        // 2) Dominance : DIMANCHE d’abord
        PlanningContext ctxDimancheFirst = contexteAvecDominance(
                StrategieScoring.EXPLOITATION,
                List.of(PenibiliteType.DIMANCHE, PenibiliteType.NUIT, PenibiliteType.FERIE)
        );

        HardSoftScore scoreDimancheFirst = ScoreUtils.penalitesLegalesAvecDominance(
                ctxDimancheFirst,
                minutesNuit,
                minutesDimanche,
                minutesFerie,
                minutesNuitEtDimanche,
                minutesNuitEtFerie,
                minutesDimancheEtFerie,
                minutesNuitEtDimancheEtFerie
        );

        // Intersection va vers DIMANCHE => 60 * 4 = 240 => score soft = -240
        assertEquals(HardSoftScore.ofSoft(-240), scoreDimancheFirst);

        // Vérification clé : changer la dominance change le score
        assertNotEquals(scoreNuitFirst, scoreDimancheFirst);
    }

    // ----------------------------------------------------------------
    // Helper : PlanningContext minimal mais "valide domaine"
    // (on suit le pattern déjà utilisé dans vos tests)
    // ----------------------------------------------------------------
    private PlanningContext contexteAvecDominance(
            StrategieScoring strategieScoring,
            List<PenibiliteType> ordreDominance
    ) {
        return new PlanningContext(
                ObjectifResolution.COUVRIR_A_TOUT_PRIX,
                strategieScoring,
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE,
                new HorizonTemporel(LocalDate.now(), LocalDate.now()),
                new StrategieCouverture(true, true, true),
                new SeuilsDeTolerance(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
                new Penalites(
                        1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1,
                        1, 1, 1
                ),
                new DominancePenibilites(ordreDominance),
                new OptionsExplicabilite(false, false, false)
        );
    }
}