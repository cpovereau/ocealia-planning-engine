package fr.project.planning.constraints;

import fr.project.planning.constraints.physiques.LimitePhysique;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ChevauchementMinuitTest — lot S8.3
 *
 * <p>{@code LimitePhysique.pasDeChevauchement} appariait les créneaux <strong>de même date</strong>
 * puis comparait des {@link LocalTime} nus. Un salarié pouvait donc être à deux endroits à la fois
 * de part et d'autre de minuit sans qu'aucun point HARD ne le signale — sur les nuits, précisément
 * là où le risque est le plus élevé.</p>
 *
 * <p>Les deux premiers tests décrivent les deux formes de la cécité. Les suivants verrouillent ce
 * que la correction ne doit pas casser : la comparaison reste stricte, elle reste cantonnée aux
 * salariés réels, et elle n'apparie pas deux personnes différentes.</p>
 */
class ChevauchementMinuitTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate MARDI = LUNDI.plusDays(1);

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // -------------------------------------------------------
    // Ce que la règle ne voyait pas
    // -------------------------------------------------------

    @Test
    @DisplayName("Une nuit qui déborde sur le lendemain recouvre le créneau du matin")
    void nuitDebordante_etCreneauDuLendemain_doitLeverUnPointHard() {
        // Lundi 22:00 → mardi 06:00, puis mardi 02:00 → 10:00 : quatre heures en double.
        // Dates différentes : les deux créneaux n'étaient jamais appariés.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");

        constraintVerifier
                .verifyThat((provider, factory) -> LimitePhysique.pasDeChevauchement(factory))
                .given(
                        creneau("C-NUIT", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0), salarie),
                        creneau("C-MATIN", MARDI, LocalTime.of(2, 0), LocalTime.of(10, 0), salarie))
                .penalizesBy(1);
    }

    @Test
    @DisplayName("Deux créneaux du même jour, dont une nuit débordante, se recouvrent aussi")
    void nuitDebordante_etCreneauDuSoir_memeDate_doitLeverUnPointHard() {
        // Lundi 22:00 → mardi 06:00 et lundi 23:00 → 23:30. Le test historique lisait
        // « 23:00 est-il avant 06:00 ? » — non — et concluait à l'absence de chevauchement,
        // alors que le second créneau est intégralement inclus dans le premier.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");

        constraintVerifier
                .verifyThat((provider, factory) -> LimitePhysique.pasDeChevauchement(factory))
                .given(
                        creneau("C-NUIT", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0), salarie),
                        creneau("C-SOIR", LUNDI, LocalTime.of(23, 0), LocalTime.of(23, 30), salarie))
                .penalizesBy(1);
    }

    // -------------------------------------------------------
    // Ce que la correction ne doit pas casser
    // -------------------------------------------------------

    @Test
    @DisplayName("Deux créneaux jointifs ne se chevauchent pas")
    void creneauxJointifs_doitNeRienPenaliser() {
        // 08:00–12:00 puis 12:00–16:00 : la comparaison reste stricte, comme avant.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-JOUR");

        constraintVerifier
                .verifyThat((provider, factory) -> LimitePhysique.pasDeChevauchement(factory))
                .given(
                        creneau("C-MATIN", LUNDI, LocalTime.of(8, 0), LocalTime.of(12, 0), salarie),
                        creneau("C-APREM", LUNDI, LocalTime.of(12, 0), LocalTime.of(16, 0), salarie))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Une nuit s'achevant à 06:00 laisse place au créneau de 06:00")
    void nuitDebordante_puisCreneauQuiEnchaine_doitNeRienPenaliser() {
        // Borne du premier test : mardi 06:00–14:00 commence là où la nuit se termine.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");

        constraintVerifier
                .verifyThat((provider, factory) -> LimitePhysique.pasDeChevauchement(factory))
                .given(
                        creneau("C-NUIT", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0), salarie),
                        creneau("C-MATIN", MARDI, LocalTime.of(6, 0), LocalTime.of(14, 0), salarie))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Deux salariés différents ne se chevauchent jamais entre eux")
    void deuxSalaries_surLeMemeHoraire_doitNeRienPenaliser() {
        SalarieReel premier = TestPlanningRequestFactory.buildSalarie("SAL-A");
        SalarieReel second = TestPlanningRequestFactory.buildSalarie("SAL-B");

        constraintVerifier
                .verifyThat((provider, factory) -> LimitePhysique.pasDeChevauchement(factory))
                .given(
                        creneau("C-A", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0), premier),
                        creneau("C-B", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0), second))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Deux créneaux non affectés qui se recouvrent ne sont pas une violation physique")
    void creneauxNonAffectes_doitNeRienPenaliser() {
        // La règle porte sur les personnes. Deux besoins simultanés que personne ne couvre
        // relèvent de la couverture, pas de l'ubiquité.
        constraintVerifier
                .verifyThat((provider, factory) -> LimitePhysique.pasDeChevauchement(factory))
                .given(
                        creneau("C-X", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0),
                                RessourceNonAffectee.INSTANCE),
                        creneau("C-Y", LUNDI, LocalTime.of(23, 0), LocalTime.of(23, 30),
                                RessourceNonAffectee.INSTANCE))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Le comptage porte sur les paires en conflit, pas sur les créneaux")
    void nuitRecouvrantDeuxAutresCreneaux_doitLeverDeuxPointsHard() {
        // La nuit recouvre les deux autres ; ceux-ci ne se recouvrent pas entre eux
        // (lundi 23:30 précède mardi 02:00). Deux paires en conflit, donc deux points.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");

        constraintVerifier
                .verifyThat((provider, factory) -> LimitePhysique.pasDeChevauchement(factory))
                .given(
                        creneau("C-1", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0), salarie),
                        creneau("C-2", LUNDI, LocalTime.of(23, 0), LocalTime.of(23, 30), salarie),
                        creneau("C-3", MARDI, LocalTime.of(2, 0), LocalTime.of(10, 0), salarie))
                .penalizesBy(2);
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    private Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                            Ressource ressource) {
        Creneau c = new Creneau(
                id, date, debut, fin, 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }
}
