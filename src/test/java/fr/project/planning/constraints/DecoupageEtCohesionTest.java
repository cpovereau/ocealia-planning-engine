package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.BlocConfieTropCourt;
import fr.project.planning.constraints.metier.CohesionCreneauOrigine;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.scenarios.service.DecoupageAuxFrontieres;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * DecoupageEtCohesionTest — lot S2 de SC-02.
 *
 * <p>Deux niveaux dans un même fichier, parce qu'ils décrivent un même mécanisme : où l'on coupe
 * (V1, calcul pur) et ce qu'il en coûte (V3, contraintes isolées).</p>
 *
 * <p>L'arbitrage métier tient en deux phrases : <strong>un bloc confié ne fait jamais moins de
 * 30 minutes</strong>, et <strong>le reliquat non couvert n'a aucun minimum</strong>. Les tests
 * ci-dessous vérifient qu'on n'a pas glissé de l'un à l'autre.</p>
 */
class DecoupageEtCohesionTest {

    private static final LocalDate MARDI = LocalDate.of(2026, 5, 12);

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // -------------------------------------------------------
    // V1 — où l'on coupe
    // -------------------------------------------------------

    @Test
    @DisplayName("Sans frontière à l'intérieur, le créneau n'est pas touché — il garde son identité")
    void aucuneFrontiere_laisseLeCreneauIntact() {
        Creneau libere = creneau("BESOIN", LocalTime.of(13, 30), LocalTime.of(16, 0), null);

        List<Creneau> morceaux = DecoupageAuxFrontieres.decouper(
                libere, List.of(), List.of(), "SAL-ABSENT");

        assertEquals(1, morceaux.size());
        assertSame(libere, morceaux.get(0), "Aucune copie : le créneau reçu est le créneau rendu.");
        assertNull(morceaux.get(0).getCreneauOrigineId());
    }

    @Test
    @DisplayName("Le début du service d'un remplaçant coupe le créneau à cette heure-là")
    void priseDeServiceDUnRemplacant_produitUneCoupe() {
        Creneau libere = creneau("BESOIN", LocalTime.of(13, 30), LocalTime.of(16, 0), null);
        Creneau serviceDeX = fige("PLN-X", LocalTime.of(15, 30), LocalTime.of(20, 0), "SAL-X");

        List<Creneau> morceaux = DecoupageAuxFrontieres.decouper(
                libere, List.of(serviceDeX), List.of(), "SAL-ABSENT");

        assertEquals(2, morceaux.size());
        assertEquals("BESOIN#S1", morceaux.get(0).getId());
        assertEquals(LocalTime.of(13, 30), morceaux.get(0).getHeureDebut());
        assertEquals(LocalTime.of(15, 30), morceaux.get(0).getHeureFin());
        assertEquals(120, morceaux.get(0).getDuree());
        assertEquals("BESOIN", morceaux.get(0).getCreneauOrigineId());

        assertEquals(30, morceaux.get(1).getDuree(),
                "Le reliquat n'est soumis à aucun minimum au moment de couper.");
    }

    @Test
    @DisplayName("Une coupe à 13h45 est légitime — aucune grille de 30 minutes ne l'aurait produite")
    void coupeHorsGrille_estProduite() {
        Creneau libere = creneau("BESOIN", LocalTime.of(13, 30), LocalTime.of(16, 0), null);
        Creneau serviceDeY = fige("PLN-Y", LocalTime.of(13, 45), LocalTime.of(18, 0), "SAL-Y");

        List<Creneau> morceaux = DecoupageAuxFrontieres.decouper(
                libere, List.of(serviceDeY), List.of(), "SAL-ABSENT");

        assertEquals(2, morceaux.size());
        assertEquals(15, morceaux.get(0).getDuree());
        assertEquals(LocalTime.of(13, 45), morceaux.get(1).getHeureDebut());
    }

    @Test
    @DisplayName("Les frontières de l'absent lui-même ne comptent pas : il n'est pas son remplaçant")
    void frontieresDeLAbsent_sontIgnorees() {
        Creneau libere = creneau("BESOIN", LocalTime.of(13, 30), LocalTime.of(16, 0), null);
        Creneau autreCreneauDeLAbsent = fige("PLN-ABS", LocalTime.of(14, 0), LocalTime.of(18, 0),
                "SAL-ABSENT");

        List<Creneau> morceaux = DecoupageAuxFrontieres.decouper(
                libere, List.of(autreCreneauDeLAbsent), List.of(), "SAL-ABSENT");

        assertEquals(1, morceaux.size());
    }

    @Test
    @DisplayName("Une frontière qui tombe sur une borne du créneau ne coupe rien")
    void frontiereSurLaBorne_neCoupePas() {
        Creneau libere = creneau("BESOIN", LocalTime.of(13, 30), LocalTime.of(16, 0), null);
        Creneau serviceDeX = fige("PLN-X", LocalTime.of(16, 0), LocalTime.of(20, 0), "SAL-X");

        List<Creneau> morceaux = DecoupageAuxFrontieres.decouper(
                libere, List.of(serviceDeX), List.of(), "SAL-ABSENT");

        assertEquals(1, morceaux.size(), "Couper à la fin reviendrait à produire un morceau vide.");
    }

    @Test
    @DisplayName("Le bord d'une indisponibilité coupe aussi — quelqu'un y redevient disponible")
    void bordDIndisponibilite_produitUneCoupe() {
        // Créneau de nuit 22:00 → 06:00 : minuit tombe à l'intérieur, et l'absence de X s'arrête
        // au mardi soir — il redevient disponible à 00:00.
        Creneau libere = creneau("BESOIN-NUIT", LocalTime.of(22, 0), LocalTime.of(6, 0), null);
        Indisponibilite absenceDeX = new Indisponibilite("SAL-X", MARDI.minusDays(2), MARDI, "CONGE");

        List<Creneau> morceaux = DecoupageAuxFrontieres.decouper(
                libere, List.of(), List.of(absenceDeX), "SAL-ABSENT");

        assertEquals(2, morceaux.size());
        assertEquals(120, morceaux.get(0).getDuree(), "22:00 → minuit.");
        assertEquals(MARDI.plusDays(1), morceaux.get(1).getDate(),
                "Le second morceau commence le lendemain : sa date de début le dit.");
        assertEquals(360, morceaux.get(1).getDuree(), "minuit → 06:00.");
    }

    // -------------------------------------------------------
    // V3 — ce qu'il en coûte
    // -------------------------------------------------------

    @Test
    @DisplayName("Un bloc de quinze minutes confié à un salarié lève une violation HARD")
    void blocDeQuinzeMinutes_estRefuse() {
        SalarieReel x = TestPlanningRequestFactory.buildSalarie("SAL-X");
        Creneau segment = segment("BESOIN", 1, LocalTime.of(13, 30), LocalTime.of(13, 45), x);

        constraintVerifier
                .verifyThat((provider, factory) -> BlocConfieTropCourt.blocConfieTropCourt(factory))
                .given(segment)
                .penalizesBy(1);
    }

    @Test
    @DisplayName("Trente minutes pile passent : le seuil est un minimum, pas une marge")
    void blocDeTrenteMinutes_estAccepte() {
        SalarieReel x = TestPlanningRequestFactory.buildSalarie("SAL-X");
        Creneau segment = segment("BESOIN", 1, LocalTime.of(15, 30), LocalTime.of(16, 0), x);

        constraintVerifier
                .verifyThat((provider, factory) -> BlocConfieTropCourt.blocConfieTropCourt(factory))
                .given(segment)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Deux segments courts mais contigus forment un bloc valide")
    void deuxSegmentsContigus_formentUnSeulBloc() {
        // C'est le cœur de l'arbitrage : la personne travaille une demi-heure d'une traite.
        // Mesurer segment par segment aurait refusé une situation parfaitement acceptable.
        SalarieReel x = TestPlanningRequestFactory.buildSalarie("SAL-X");
        Creneau premier = segment("BESOIN", 1, LocalTime.of(13, 30), LocalTime.of(13, 45), x);
        Creneau second = segment("BESOIN", 2, LocalTime.of(13, 45), LocalTime.of(14, 0), x);

        constraintVerifier
                .verifyThat((provider, factory) -> BlocConfieTropCourt.blocConfieTropCourt(factory))
                .given(premier, second)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Deux passages courts et séparés comptent pour deux violations")
    void deuxBlocsSeparesTropCourts_comptentDeuxFois() {
        SalarieReel x = TestPlanningRequestFactory.buildSalarie("SAL-X");
        Creneau matin = segment("BESOIN", 1, LocalTime.of(8, 0), LocalTime.of(8, 15), x);
        Creneau soir = segment("BESOIN", 3, LocalTime.of(17, 0), LocalTime.of(17, 15), x);

        constraintVerifier
                .verifyThat((provider, factory) -> BlocConfieTropCourt.blocConfieTropCourt(factory))
                .given(matin, soir)
                .penalizesBy(2);
    }

    @Test
    @DisplayName("Un créneau reçu tel quel n'est pas jugé sur sa durée")
    void creneauNonDecoupe_echappeAuSeuil() {
        // Sa durée est un fait d'entrée. Le moteur n'a pas à refuser un besoin de dix minutes
        // que l'appelant lui transmet : il n'en est pas l'auteur.
        SalarieReel x = TestPlanningRequestFactory.buildSalarie("SAL-X");
        Creneau court = creneau("BESOIN-COURT", LocalTime.of(13, 30), LocalTime.of(13, 40), x);

        constraintVerifier
                .verifyThat((provider, factory) -> BlocConfieTropCourt.blocConfieTropCourt(factory))
                .given(court)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un besoin éclaté entre deux personnes coûte un cran de cohésion")
    void besoinPartageEntreDeuxPersonnes_estPenalise() {
        SalarieReel x = TestPlanningRequestFactory.buildSalarie("SAL-X");
        SalarieReel y = TestPlanningRequestFactory.buildSalarie("SAL-Y");
        Creneau premier = segment("BESOIN", 1, LocalTime.of(8, 0), LocalTime.of(12, 0), x);
        Creneau second = segment("BESOIN", 2, LocalTime.of(12, 0), LocalTime.of(16, 0), y);

        constraintVerifier
                .verifyThat((provider, factory) -> CohesionCreneauOrigine.cohesionCreneauOrigine(factory))
                .given(premier, second, contexte())
                .penalizesBy(contexte().getPenalites().getFragmentationCreneauOrigine());
    }

    @Test
    @DisplayName("Une part laissée à pourvoir n'est pas de l'éparpillement")
    void partNonCouverte_neComptePasCommeFragmentation() {
        // Elle a déjà son propre coût. La compter deux fois pousserait le solveur à préférer
        // un éparpillement à une couverture partielle.
        SalarieReel x = TestPlanningRequestFactory.buildSalarie("SAL-X");
        Creneau couvert = segment("BESOIN", 1, LocalTime.of(8, 0), LocalTime.of(12, 0), x);
        Creneau nonCouvert = segment("BESOIN", 2, LocalTime.of(12, 0), LocalTime.of(16, 0),
                RessourceNonAffectee.INSTANCE);

        constraintVerifier
                .verifyThat((provider, factory) -> CohesionCreneauOrigine.cohesionCreneauOrigine(factory))
                .given(couvert, nonCouvert, contexte())
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private PlanningContext contexte() {
        return TestPlanningRequestFactory.buildPlanningContextSc01();
    }

    private static Creneau creneau(String id, LocalTime debut, LocalTime fin, Ressource ressource) {
        Creneau c = new Creneau(
                id, MARDI, debut, fin, minutes(debut, fin),
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE);
        c.setRessourceAffectee(ressource);
        return c;
    }

    private static Creneau fige(String id, LocalTime debut, LocalTime fin, String salarieId) {
        Creneau c = creneau(id, debut, fin, null);
        c.figerSur(TestPlanningRequestFactory.buildSalarie(salarieId));
        return c;
    }

    private static Creneau segment(String origine, int rang, LocalTime debut, LocalTime fin,
                                   Ressource ressource) {
        Creneau c = creneau(origine + "#S" + rang, debut, fin, ressource);
        c.setCreneauOrigineId(origine);
        return c;
    }

    private static int minutes(LocalTime debut, LocalTime fin) {
        int d = debut.getHour() * 60 + debut.getMinute();
        int f = fin.getHour() * 60 + fin.getMinute();
        return f > d ? f - d : f + 24 * 60 - d;
    }
}
