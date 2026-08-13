package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.ressource.ContratSalarie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EcartAuContratTest — lot L2 du chantier équité (V1, calcul pur).
 *
 * <p>La règle tient en une phrase : <strong>on compare au contrat de chacun, jamais à la moyenne
 * du groupe</strong>. Ces tests fixent les trois conséquences qui en découlent et qu'il serait
 * facile de perdre — l'exemple exact de l'arbitrage métier, le signe, et le silence quand rien
 * n'est comparable.</p>
 */
class EcartAuContratTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final HorizonTemporel UNE_SEMAINE = new HorizonTemporel(LUNDI, LUNDI.plusDays(6));

    @Test
    @DisplayName("L'exemple de l'arbitrage : +16,7 % contre +14,3 %, et le second est le moins sollicité")
    void lExempleDeLArbitrage() {
        // Un salarié à 30 h à qui l'on demande 35 h ; un salarié à 35 h à qui l'on demande 40 h.
        // Le second travaille cinq heures de plus, et il est pourtant le moins sollicité.
        Double attenduesA = EcartAuContrat.minutesAttendues(contrat(30.0), UNE_SEMAINE);
        Double attenduesB = EcartAuContrat.minutesAttendues(contrat(35.0), UNE_SEMAINE);

        double ecartA = EcartAuContrat.ecartPourcent(35 * 60.0, attenduesA);
        double ecartB = EcartAuContrat.ecartPourcent(40 * 60.0, attenduesB);

        assertEquals(16.67, ecartA, 0.01);
        assertEquals(14.29, ecartB, 0.01);
        assertTrue(ecartB < ecartA,
                "Comparer les volumes absolus donnerait la réponse inverse.");
    }

    @Test
    @DisplayName("L'écart est signé : la sous-charge se voit autant que la surcharge")
    void lEcartEstSigne() {
        Double attendues = EcartAuContrat.minutesAttendues(contrat(30.0), UNE_SEMAINE);

        // Sans signe, le moteur éviterait de surcharger sans jamais rééquilibrer.
        assertEquals(-33.33, EcartAuContrat.ecartPourcent(20 * 60.0, attendues), 0.01);
        assertEquals(0.0, EcartAuContrat.ecartPourcent(30 * 60.0, attendues), 0.01);
    }

    @Test
    @DisplayName("La référence est proratisée sur la fenêtre — c'est ce qui traite l'annualisation")
    void laReferenceEstProratiseeSurLaFenetre() {
        // Deux semaines transmises : le contrat attendu double. Pour un salarié annualisé, une
        // semaine au-dessus de la moyenne n'est plus une anomalie, elle se compense dans la
        // fenêtre — c'est l'objet même de l'annualisation.
        HorizonTemporel deuxSemaines = new HorizonTemporel(LUNDI, LUNDI.plusDays(13));

        assertEquals(35 * 60.0, EcartAuContrat.minutesAttendues(contrat(35.0), UNE_SEMAINE), 0.01);
        assertEquals(70 * 60.0, EcartAuContrat.minutesAttendues(contrat(35.0), deuxSemaines), 0.01);

        // 45 h puis 25 h sur deux semaines : à l'équilibre, alors que la première semaine seule
        // afficherait +28,6 %.
        assertEquals(0.0,
                EcartAuContrat.ecartPourcent(70 * 60.0,
                        EcartAuContrat.minutesAttendues(contrat(35.0), deuxSemaines)), 0.01);
        assertEquals(28.57,
                EcartAuContrat.ecartPourcent(45 * 60.0,
                        EcartAuContrat.minutesAttendues(contrat(35.0), UNE_SEMAINE)), 0.01);
    }

    @Test
    @DisplayName("Sans volume contractuel, le moteur ne dit rien plutôt que de supposer")
    void sansContrat_rienNEstComparable() {
        assertNull(EcartAuContrat.minutesAttendues(null, UNE_SEMAINE));
        assertNull(EcartAuContrat.minutesAttendues(contrat(null), UNE_SEMAINE));
        assertNull(EcartAuContrat.minutesAttendues(contrat(0.0), UNE_SEMAINE));

        assertNull(EcartAuContrat.ecartPourcent(35 * 60.0, null));
        assertNull(EcartAuContrat.pourcentageDuContrat(8 * 60.0, null));
    }

    @Test
    @DisplayName("Les pénibilités se rapportent au contrat comme les heures")
    void lesPenibilitesSeRapportentAuContrat() {
        // Huit heures de nuit : un cinquième du contrat d'un temps plein, deux cinquièmes de
        // celui d'un mi-temps. Le même volume ne pèse pas pareil.
        Double tempsPlein = EcartAuContrat.minutesAttendues(contrat(35.0), UNE_SEMAINE);
        Double miTemps = EcartAuContrat.minutesAttendues(contrat(17.5), UNE_SEMAINE);

        assertEquals(22.86, EcartAuContrat.pourcentageDuContrat(8 * 60.0, tempsPlein), 0.01);
        assertEquals(45.71, EcartAuContrat.pourcentageDuContrat(8 * 60.0, miTemps), 0.01);
    }

    @Test
    @DisplayName("La fenêtre se compte bornes comprises")
    void laFenetreSeCompteBornesComprises() {
        assertEquals(7, EcartAuContrat.joursObserves(UNE_SEMAINE));
        assertEquals(1, EcartAuContrat.joursObserves(new HorizonTemporel(LUNDI, LUNDI)));
    }

    private static ContratSalarie contrat(Double heuresHebdomadaires) {
        return new ContratSalarie(null, heuresHebdomadaires, null, null);
    }
}
