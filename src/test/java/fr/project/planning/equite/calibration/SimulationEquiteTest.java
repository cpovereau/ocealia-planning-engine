package fr.project.planning.equite.calibration;

import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.scoring.PenibiliteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimulationEquiteTest — lot L3 du chantier équité (V1, calcul pur).
 *
 * <h3>Le cas de démonstration</h3>
 * <p>Trois personnes, une semaine, et des pénibilités volontairement contrastées — c'est la seule
 * façon de rendre visible ce que le harnais sait faire :</p>
 * <ul>
 *   <li><strong>SAL-NUIT</strong>, contrat 35 h : deux nuits de 8 h, un jour ordinaire ;</li>
 *   <li><strong>SAL-DIMANCHE</strong>, contrat 28 h : un dimanche, un férié, un jour ordinaire ;</li>
 *   <li><strong>SAL-JOUR</strong>, contrat 35 h : quatre jours ordinaires, et rien d'autre.</li>
 * </ul>
 *
 * <p>À l'échelle neutre, c'est SAL-JOUR qui ressort le plus sollicité — il a simplement fait le
 * plus d'heures. Toute la question de la calibration tient dans le fait que ce classement n'est pas
 * une vérité : il est ce que dit une échelle où une nuit vaut une heure de bureau.</p>
 *
 * <p>⚠️ Ce cas <strong>ne calibre rien</strong>. Il est construit pour éprouver l'instrument, et un
 * cas construit ne peut pas trancher un arbitrage qu'il a été dessiné pour illustrer. La calibration
 * se fait sur des plannings réels — voir 92_CALIBRATION_PENIBILITE.md.</p>
 */
class SimulationEquiteTest {

    private static final double MINUTES_35H = 35 * 60.0;
    private static final double MINUTES_28H = 28 * 60.0;

    private static final CasDeCalibration CAS = new CasDeCalibration("demonstration", List.of(
            new ChargeObservee("SAL-NUIT", 960, 0, 0, 480, MINUTES_35H),
            new ChargeObservee("SAL-DIMANCHE", 0, 480, 480, 480, MINUTES_28H),
            new ChargeObservee("SAL-JOUR", 0, 0, 0, 1920, MINUTES_35H)));

    private static final SimulationEquite SIMULATION = new SimulationEquite(CAS);

    // ---------------------------------------------------------
    // Le classement
    // ---------------------------------------------------------

    @Test
    @DisplayName("À l'échelle neutre, le classement est celui des heures brutes — et c'est le problème")
    void aLEchelleNeutre_leClassementEstCeluiDesHeuresBrutes() {
        List<SimulationEquite.LigneClassement> classement =
                SIMULATION.classement(CoefficientsPenibilite.neutres());

        assertEquals(List.of("SAL-JOUR", "SAL-DIMANCHE", "SAL-NUIT"),
                classement.stream().map(SimulationEquite.LigneClassement::ressourceId).toList(),
                "Sans pondération, celui qui fait deux nuits passe derrière celui qui fait "
                        + "quatre jours de bureau. C'est exactement ce que la calibration doit "
                        + "permettre de discuter.");

        assertEquals(-8.57, classement.get(0).ecartPourcent(), 0.01);
        assertEquals(-31.43, classement.get(2).ecartPourcent(), 0.01);
    }

    @Test
    @DisplayName("Une nuit à 2, et celui qui les fait devient le plus sollicité")
    void unePonderationForte_renverseLeClassement() {
        List<SimulationEquite.LigneClassement> classement =
                SIMULATION.classement(new CoefficientsPenibilite(2.0, null, null));

        assertEquals("SAL-NUIT", classement.get(0).ressourceId());
        assertEquals(14.29, classement.get(0).ecartPourcent(), 0.01,
                "480 minutes ordinaires plus 960 minutes de nuit comptées double, sur 2100 "
                        + "minutes attendues.");
    }

    @Test
    @DisplayName("Sans contrat déclaré, la personne n'est pas classée plutôt que classée au hasard")
    void sansContrat_lapersonneNEstPasClassee() {
        CasDeCalibration avecUnPosteVirtuel = new CasDeCalibration("avec-interimaire", List.of(
                new ChargeObservee("SAL-NUIT", 960, 0, 0, 480, MINUTES_35H),
                new ChargeObservee("PV-001", 0, 0, 0, 960, null)));

        List<SimulationEquite.LigneClassement> classement =
                new SimulationEquite(avecUnPosteVirtuel).classement(CoefficientsPenibilite.neutres());

        assertEquals(List.of("SAL-NUIT"),
                classement.stream().map(SimulationEquite.LigneClassement::ressourceId).toList(),
                "Rien ne rend comparable qui n'a pas de référence. Lui donner une place "
                        + "reviendrait à en inventer une.");
    }

    // ---------------------------------------------------------
    // Les points de bascule
    // ---------------------------------------------------------

    @Test
    @DisplayName("La bascule est une valeur exacte, pas un intervalle trouvé à tâtons")
    void laBasculeEstUneValeurExacte() {
        List<SimulationEquite.Bascule> bascules =
                SIMULATION.bascules(PenibiliteType.NUIT, CoefficientsPenibilite.neutres());

        // SAL-NUIT rattrape SAL-DIMANCHE à 1,375 puis SAL-JOUR à 1,5. Balayer une grille aurait
        // dit « quelque part entre 1 et 2 » — ce dont personne ne peut décider.
        assertEquals(2, bascules.size());

        assertEquals(1.375, bascules.get(0).valeur(), 1e-9);
        assertEquals("SAL-DIMANCHE", bascules.get(0).plusSollicitEnDessous());
        assertEquals("SAL-NUIT", bascules.get(0).plusSollicitAuDessus());

        assertEquals(1.5, bascules.get(1).valeur(), 1e-9);
        assertEquals("SAL-JOUR", bascules.get(1).plusSollicitEnDessous());
        assertEquals("SAL-NUIT", bascules.get(1).plusSollicitAuDessus());
    }

    @Test
    @DisplayName("La bascule annoncée est bien celle où le classement s'inverse")
    void laBasculeAnnonceeEstCelleOuLeClassementSinverse() {
        double bascule = SIMULATION.bascules(PenibiliteType.NUIT, CoefficientsPenibilite.neutres())
                .get(1).valeur();

        assertEquals("SAL-JOUR", premier(bascule - 0.01),
                "Juste en dessous, l'ordre annoncé doit encore tenir.");
        assertEquals("SAL-NUIT", premier(bascule + 0.01),
                "Juste au-dessus, il doit avoir changé — sinon la valeur ne veut rien dire.");
    }

    @Test
    @DisplayName("Deux personnes que le coefficient touche pareil ne basculent jamais")
    void deuxPersonnesInsensiblesAuCoefficient_neBasculentJamais() {
        // Ni SAL-DIMANCHE ni SAL-JOUR n'ont d'heures de nuit : le coefficient de nuit les
        // déplace tous les deux de zéro. Aucune valeur ne les départage — et c'est une réponse,
        // pas une absence de réponse.
        List<SimulationEquite.Bascule> bascules =
                SIMULATION.bascules(PenibiliteType.NUIT, CoefficientsPenibilite.neutres());

        assertTrue(bascules.stream().noneMatch(b ->
                        List.of(b.plusSollicitEnDessous(), b.plusSollicitAuDessus())
                                .containsAll(List.of("SAL-DIMANCHE", "SAL-JOUR"))),
                "Deux droites parallèles ne se croisent pas.");
    }

    @Test
    @DisplayName("Une bascule sous 1 est écartée : elle ne dit rien du domaine où l'on choisit")
    void uneBasculeHorsDomaine_estEcartee() {
        // SAL-DIMANCHE dépasse SAL-NUIT dès que le coefficient dimanche atteint 0,4 — soit bien
        // avant le neutre. Aucune échelle admissible ne se trouve de l'autre côté.
        List<SimulationEquite.Bascule> bascules =
                SIMULATION.bascules(PenibiliteType.DIMANCHE, CoefficientsPenibilite.neutres());

        assertEquals(List.of(0.4, 1.2),
                bascules.stream().map(b -> Math.round(b.valeur() * 1000) / 1000.0).toList(),
                "Les deux intersections positives sont rendues, y compris celle qui tombe sous "
                        + "le neutre : c'est au lecteur de voir qu'elle est hors du domaine "
                        + "qu'il explore, pas au harnais de la taire.");
    }

    @Test
    @DisplayName("Un cas sans pénibilité est muet, ce qui n'est pas la même chose que neutre")
    void unCasSansPenibilite_estMuet() {
        CasDeCalibration quePourDuJour = new CasDeCalibration("que-du-jour", List.of(
                new ChargeObservee("SAL-A", 0, 0, 0, 1920, MINUTES_35H),
                new ChargeObservee("SAL-B", 0, 0, 0, 1440, MINUTES_35H)));

        assertTrue(quePourDuJour.estMuet(),
                "Aucune échelle ne change ce classement. Conclure de là que les coefficients "
                        + "sont indifférents serait l'erreur que ce cas rend possible.");
        assertTrue(!CAS.estMuet());
    }

    private static String premier(double coefficientNuit) {
        return SIMULATION.classement(new CoefficientsPenibilite(coefficientNuit, null, null))
                .get(0).ressourceId();
    }
}
