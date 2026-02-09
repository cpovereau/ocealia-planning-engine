package fr.project.planning.score;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.scoring.StrategieScoring;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoreDominanceTest {

    private PlanningContext context(StrategieScoring strategie) {
        PlanningContext ctx = mock(PlanningContext.class);
        when(ctx.getStrategieScoring()).thenReturn(strategie);
        return ctx;
    }

    @Test
    void exploitation_nonCouvert_domine_nuit() {
        PlanningContext ctx = context(StrategieScoring.EXPLOITATION);

        // 1 occurrence non couvert = 10_000
        HardSoftScore nonCouvert = ScoreUtils.soft(
            ctx,
            PenaliteKey.METIER_SOFT_CRENEAU_NON_COUVERT,
            1
        );

        // 3000 minutes de nuit = 3000 * 3 = 9000 (donc non couvert doit dominer)
        HardSoftScore nuit = ScoreUtils.soft(
            ctx,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
            3000
        );

        assertTrue(nonCouvert.getSoftScore() < nuit.getSoftScore(),
            "1 non couvert doit dominer 3000 minutes de nuit en EXPLOITATION");
    }

    @Test
    void exploitation_posteVirtuel_domine_ferie_sous_le_seuil() {
        PlanningContext ctx = context(StrategieScoring.EXPLOITATION);

        // 1 poste virtuel = 2_000
        HardSoftScore posteVirtuel = ScoreUtils.soft(
            ctx,
            PenaliteKey.METIER_SOFT_AFFECTATION_POSTE_VIRTUEL,
            1
        );

        // Seuil PV vs Férié = 2000 / 5 = 400 min.
        // Donc à 399 min: 399*5=1995 -> PV domine.
        HardSoftScore ferie399 = ScoreUtils.soft(
            ctx,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES,
            399
        );

        assertTrue(posteVirtuel.getSoftScore() < ferie399.getSoftScore(),
            "1 poste virtuel doit dominer 399 minutes férié en EXPLOITATION");
    }

    @Test
    void exploitation_ferie_domine_posteVirtuel_au_dessus_du_seuil() {
        PlanningContext ctx = context(StrategieScoring.EXPLOITATION);

        HardSoftScore posteVirtuel = ScoreUtils.soft(
            ctx,
            PenaliteKey.METIER_SOFT_AFFECTATION_POSTE_VIRTUEL,
            1
        );

        // À 401 min: 401*5=2005 -> Férié domine PV.
        HardSoftScore ferie401 = ScoreUtils.soft(
            ctx,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES,
            401
        );

        assertTrue(posteVirtuel.getSoftScore() > ferie401.getSoftScore(),
            "401 minutes férié doivent dominer 1 poste virtuel en EXPLOITATION");
    }

    @Test
    void exploitation_ferie_domine_nuit_a_volume_egal() {
        PlanningContext ctx = context(StrategieScoring.EXPLOITATION);

        // Poids férié/min = 5 > poids nuit/min = 3 => férié doit être plus pénalisant à volume égal
        HardSoftScore nuit = ScoreUtils.soft(
            ctx,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
            120
        );

        HardSoftScore ferie = ScoreUtils.soft(
            ctx,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES,
            120
        );

        assertTrue(ferie.getSoftScore() < nuit.getSoftScore(),
            "À volume égal, FÉRIÉ doit dominer NUIT en EXPLOITATION");
    }

    @Test
    void strategie_change_le_score_pour_la_meme_penalite() {
        PlanningContext exploitation = context(StrategieScoring.EXPLOITATION);
        PlanningContext analyseRh = context(StrategieScoring.ANALYSE_RH);

        HardSoftScore s1 = ScoreUtils.soft(
            exploitation,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
            120
        );

        HardSoftScore s2 = ScoreUtils.soft(
            analyseRh,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
            120
        );

        assertTrue(s1.getSoftScore() != s2.getSoftScore(),
            "Le score doit varier quand StrategieScoring change, à pénalité identique");
    }
}
