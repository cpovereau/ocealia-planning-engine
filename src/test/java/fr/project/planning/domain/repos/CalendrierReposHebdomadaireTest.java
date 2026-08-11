package fr.project.planning.domain.repos;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CalendrierReposHebdomadaireTest — lot S7.9b
 *
 * <p>Deux sources de repos, dans cet ordre : les marqueurs déclarés par l'appelant, puis le repli
 * samedi/dimanche. Le repli s'applique <strong>par salarié et par semaine</strong> — c'est le
 * point le plus facile à casser d'une future évolution, et celui qui décide si une semaine
 * oubliée devient silencieusement travaillable.</p>
 */
class CalendrierReposHebdomadaireTest {

    /** Lundi 11 mai 2026. Semaine 1 : 11→17. Semaine 2 : 18→24. */
    private static final LocalDate LUNDI_S1 = LocalDate.of(2026, 5, 11);
    private static final LocalDate LUNDI_S2 = LUNDI_S1.plusWeeks(1);

    private static final String CODE_RH = "10450";
    private static final String CODE_RHD = "10451";
    private static final String SAL = "SAL-2001";
    private static final String AUTRE = "SAL-2002";

    private static final HorizonTemporel SEMAINE_1 =
            new HorizonTemporel(LUNDI_S1, LUNDI_S1.plusDays(6));
    private static final HorizonTemporel DEUX_SEMAINES =
            new HorizonTemporel(LUNDI_S1, LUNDI_S2.plusDays(6));

    // =====================================================================
    // Reconnaissance des marqueurs
    // =====================================================================

    @Nested
    @DisplayName("Reconnaissance du code de repos")
    class Reconnaissance {

        @Test
        void codesDeclares_sontReconnusAvecLeurNature() {
            ReferentielComptabiliteActivite ref = referentiel(CODE_RH, CODE_RHD);

            assertEquals(QualificationJour.RH, ref.natureRepos(CODE_RH));
            assertEquals(QualificationJour.RHD, ref.natureRepos(CODE_RHD));
            assertTrue(ref.estReposHebdomadaire(CODE_RH));
        }

        @Test
        void codesNonDeclares_aucunCreneauNEstUnMarqueur() {
            // Le moteur ne connaît aucun code en dur : « RH » n'est pas un mot réservé.
            ReferentielComptabiliteActivite ref = referentiel(null, null);

            assertFalse(ref.estReposHebdomadaire("RH"));
            assertFalse(ref.estReposHebdomadaire(CODE_RH));
            assertFalse(ref.declareUnCodeDeRepos());
        }

        @Test
        void codeBlanc_vautNonDeclare() {
            ReferentielComptabiliteActivite ref = referentiel("   ", "");

            assertFalse(ref.declareUnCodeDeRepos());
            assertFalse(ref.estReposHebdomadaire(""));
        }

        @Test
        void marqueurSansRessourceAffectee_estIgnore() {
            // Sans salarié, le créneau ne dit le repos de personne.
            ReferentielComptabiliteActivite ref = referentiel(CODE_RH, CODE_RHD);
            Creneau orphelin = creneau(LUNDI_S1.plusDays(1), CODE_RH, null);

            assertTrue(CalendrierReposHebdomadaire.depuisLesMarqueurs(List.of(orphelin), ref).isEmpty());
        }
    }

    // =====================================================================
    // Construction du calendrier
    // =====================================================================

    @Nested
    @DisplayName("Déclaration et repli")
    class Construction {

        @Test
        void aucuneDeclaration_repliSamediDimanche() {
            List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                    List.of(SAL), List.of(), SEMAINE_1);

            assertEquals(Set.of(LUNDI_S1.plusDays(5), LUNDI_S1.plusDays(6)), dates(calendrier));
            assertEquals(QualificationJour.RH, natureDe(calendrier, LUNDI_S1.plusDays(5)));
            assertEquals(QualificationJour.RHD, natureDe(calendrier, LUNDI_S1.plusDays(6)));
            assertTrue(calendrier.stream().noneMatch(ReposHebdomadaire::estDeclare));
        }

        @Test
        void reposDeclareUnMardi_remplaceLeRepliDeSaSemaine() {
            // Le repos peut tomber n'importe quel jour : c'est tout l'intérêt du marqueur.
            ReposHebdomadaire mardi =
                    new ReposHebdomadaire(SAL, LUNDI_S1.plusDays(1), QualificationJour.RH, true);

            List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                    List.of(SAL), List.of(mardi), SEMAINE_1);

            assertEquals(Set.of(LUNDI_S1.plusDays(1)), dates(calendrier));
            assertTrue(calendrier.get(0).estDeclare());
        }

        @Test
        void semaineSansMarqueur_retombeSurLeRepli_memeSiLAutreSemaineEnDeclare() {
            // Le point décisif : la maille est la semaine, pas le salarié. Une semaine oubliée
            // ne devient jamais silencieusement travaillable.
            ReposHebdomadaire mardiS1 =
                    new ReposHebdomadaire(SAL, LUNDI_S1.plusDays(1), QualificationJour.RH, true);

            List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                    List.of(SAL), List.of(mardiS1), DEUX_SEMAINES);

            assertEquals(
                    Set.of(LUNDI_S1.plusDays(1),                       // semaine 1 : déclaré
                            LUNDI_S2.plusDays(5), LUNDI_S2.plusDays(6)), // semaine 2 : repli
                    dates(calendrier));
        }

        @Test
        void chaqueSalarieEstTraitePourLuiMeme() {
            ReposHebdomadaire mardiDuPremier =
                    new ReposHebdomadaire(SAL, LUNDI_S1.plusDays(1), QualificationJour.RH, true);

            List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                    List.of(SAL, AUTRE), List.of(mardiDuPremier), SEMAINE_1);

            assertEquals(Set.of(LUNDI_S1.plusDays(1)), datesDe(calendrier, SAL));
            assertEquals(Set.of(LUNDI_S1.plusDays(5), LUNDI_S1.plusDays(6)), datesDe(calendrier, AUTRE));
        }

        @Test
        void deuxMarqueursLaMemeSemaine_sontTousDeuxRetenus() {
            ReposHebdomadaire mardi =
                    new ReposHebdomadaire(SAL, LUNDI_S1.plusDays(1), QualificationJour.RH, true);
            ReposHebdomadaire jeudi =
                    new ReposHebdomadaire(SAL, LUNDI_S1.plusDays(3), QualificationJour.RHD, true);

            List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                    List.of(SAL), List.of(mardi, jeudi), SEMAINE_1);

            assertEquals(Set.of(LUNDI_S1.plusDays(1), LUNDI_S1.plusDays(3)), dates(calendrier));
        }

        @Test
        void repliHorsHorizon_nEstPasProduit() {
            // Horizon lundi→vendredi : samedi et dimanche n'existent pas, aucun repos n'est posé.
            List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                    List.of(SAL), List.of(),
                    new HorizonTemporel(LUNDI_S1, LUNDI_S1.plusDays(4)));

            assertTrue(calendrier.isEmpty());
        }

        @Test
        void marqueurHorsHorizon_estIgnoreEtNEmpechePasLeRepli() {
            ReposHebdomadaire horsHorizon =
                    new ReposHebdomadaire(SAL, LUNDI_S2.plusDays(1), QualificationJour.RH, true);

            List<ReposHebdomadaire> calendrier = CalendrierReposHebdomadaire.construire(
                    List.of(SAL), List.of(horsHorizon), SEMAINE_1);

            assertEquals(Set.of(LUNDI_S1.plusDays(5), LUNDI_S1.plusDays(6)), dates(calendrier));
        }

        @Test
        void aucunSalarie_calendrierVide() {
            assertTrue(CalendrierReposHebdomadaire.construire(List.of(), List.of(), SEMAINE_1).isEmpty());
        }
    }

    // =====================================================================
    // Extraction depuis les créneaux
    // =====================================================================

    @Nested
    @DisplayName("Extraction depuis les créneaux marqueurs")
    class Extraction {

        @Test
        void creneauPorteurDuCode_devientUnReposDeclare() {
            ReferentielComptabiliteActivite ref = referentiel(CODE_RH, CODE_RHD);
            SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(SAL);

            List<ReposHebdomadaire> declares = CalendrierReposHebdomadaire.depuisLesMarqueurs(
                    List.of(creneau(LUNDI_S1.plusDays(1), CODE_RH, salarie),
                            creneau(LUNDI_S1.plusDays(2), CODE_RHD, salarie)),
                    ref);

            assertEquals(2, declares.size());
            assertEquals(QualificationJour.RH, declares.get(0).getNature());
            assertEquals(QualificationJour.RHD, declares.get(1).getNature());
            assertTrue(declares.stream().allMatch(ReposHebdomadaire::estDeclare));
            assertTrue(declares.stream().allMatch(r -> SAL.equals(r.getSalarieId())));
        }

        @Test
        void creneauDeTravail_nEstPasUnMarqueur() {
            ReferentielComptabiliteActivite ref = referentiel(CODE_RH, CODE_RHD);
            SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(SAL);
            Creneau travail = creneau(LUNDI_S1, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);

            assertFalse(CalendrierReposHebdomadaire.estMarqueurDeRepos(travail, ref));
            assertTrue(CalendrierReposHebdomadaire.depuisLesMarqueurs(List.of(travail), ref).isEmpty());
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static Set<LocalDate> dates(List<ReposHebdomadaire> calendrier) {
        return calendrier.stream().map(ReposHebdomadaire::getDate).collect(Collectors.toSet());
    }

    private static Set<LocalDate> datesDe(List<ReposHebdomadaire> calendrier, String salarieId) {
        return calendrier.stream()
                .filter(r -> salarieId.equals(r.getSalarieId()))
                .map(ReposHebdomadaire::getDate)
                .collect(Collectors.toSet());
    }

    private static QualificationJour natureDe(List<ReposHebdomadaire> calendrier, LocalDate date) {
        return calendrier.stream()
                .filter(r -> r.getDate().equals(date))
                .findFirst().orElseThrow().getNature();
    }

    private static ReferentielComptabiliteActivite referentiel(String codeRh, String codeRhd) {
        return new ReferentielComptabiliteActivite(
                Map.of(TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        new ComptabiliteActivite(TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                                true, true, false, false,
                                ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)),
                codeRh, codeRhd);
    }

    private static Creneau creneau(LocalDate date, String codeActivite, SalarieReel salarie) {
        Creneau c = new Creneau(
                "C-" + date + "-" + codeActivite, date, LocalTime.of(0, 0), LocalTime.of(23, 59), 1439,
                "SITE-A", codeActivite, null, "PC-001",
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE);
        c.setRessourceAffectee(salarie);
        return c;
    }
}
