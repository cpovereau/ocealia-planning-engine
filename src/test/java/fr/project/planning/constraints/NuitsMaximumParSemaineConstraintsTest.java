package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.NuitsMaximumParSemaine;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NuitsMaximumParSemaineConstraintsTest — lot S7.7
 *
 * <p>Valide le plafond hebdomadaire de nuits travaillées : un <strong>volume</strong> sur la
 * semaine calendaire, à distinguer de {@code NuitsConsecutivesMax} qui borne un
 * <strong>enchaînement</strong>. Trois nuits lundi, mercredi et vendredi ne violent aucun
 * enchaînement mais peuvent dépasser un volume de deux.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Plafond non transmis → contrainte inactive</li>
 *   <li>Plafond à 0 → aucune nuit autorisée — le cas de SAL-2001 dans SC-03</li>
 *   <li>Volume sous le plafond → pas de pénalité</li>
 *   <li>Volume exactement au plafond → pas de pénalité</li>
 *   <li>Volume dépassé → pénalité par nuit excédentaire</li>
 *   <li>Nuits non consécutives → comptées quand même, c'est un volume</li>
 *   <li>Deux créneaux la même nuit → une seule nuit</li>
 *   <li>Nuits réparties sur deux semaines → chaque semaine compte pour elle-même</li>
 *   <li>Créneau de jour → non compté</li>
 *   <li>Activité hors charge → non comptée</li>
 * </ol>
 */
class NuitsMaximumParSemaineConstraintsTest {

    /** Lundi 11 mai 2026. */
    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final int PENALITE = 5_000;
    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-2. Activation
    // ---------------------------------------------------------

    @Test
    void plafondNonTransmis_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NS-01");

        verifier().given(faits(salarie, nuits(salarie, 0, 1, 2, 3, 4))).penalizesBy(0);
    }

    @Test
    void plafondAZero_aucuneNuitAutorisee() {
        // C'est la situation de SAL-2001 dans le jeu de référence SC-03 : le champ est
        // renseigné à 0 pour dire « ce salarié ne travaille pas de nuit ».
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-02", 0);

        verifier().given(faits(salarie, nuits(salarie, 0))).penalizesBy(PENALITE);
    }

    // ---------------------------------------------------------
    // 3-5. Comparaison au plafond
    // ---------------------------------------------------------

    @Test
    void volumeSousLePlafond_pasDePenalite() {
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-03", 3);

        verifier().given(faits(salarie, nuits(salarie, 0, 2))).penalizesBy(0);
    }

    @Test
    void volumeExactementAuPlafond_pasDePenalite() {
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-04", 3);

        verifier().given(faits(salarie, nuits(salarie, 0, 2, 4))).penalizesBy(0);
    }

    @Test
    void volumeDepasse_penaliteParNuitExcedentaire() {
        // 5 nuits pour un plafond de 3 → 2 en excédent.
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-05", 3);

        verifier().given(faits(salarie, nuits(salarie, 0, 1, 2, 3, 4))).penalizesBy(PENALITE * 2);
    }

    // ---------------------------------------------------------
    // 6-8. Ce qu'un « volume » veut dire
    // ---------------------------------------------------------

    @Test
    void nuitsNonConsecutives_comptentQuandMeme() {
        // Lundi, mercredi, vendredi : aucun enchaînement, mais trois nuits sur la semaine.
        // C'est précisément ce que NuitsConsecutivesMax ne voit pas.
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-06", 2);

        verifier().given(faits(salarie, nuits(salarie, 0, 2, 4))).penalizesBy(PENALITE);
    }

    @Test
    void deuxCreneauxLaMemeNuit_comptentPourUne() {
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-07", 1);

        List<Creneau> creneaux = List.of(
                nuit("C-NS-07A", LUNDI, salarie),
                nuit("C-NS-07B", LUNDI, salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(0);
    }

    @Test
    void nuitsRepartiesSurDeuxSemaines_chaqueSemaineComptePourElleMeme() {
        // 2 nuits la première semaine, 2 la seconde, plafond de 2 : aucune violation,
        // alors qu'un comptage sur l'horizon entier en verrait quatre.
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-08", 2);

        verifier().given(faits(salarie, nuits(salarie, 0, 1, 7, 8))).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 9-10. Périmètre
    // ---------------------------------------------------------

    @Test
    void creneauDeJour_nonCompte() {
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-09", 0);

        List<Creneau> creneaux = List.of(journee("C-NS-09", LUNDI, salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(0);
    }

    @Test
    void activiteHorsCharge_nonComptee() {
        SalarieReel salarie = salarieAvecPlafond("SAL-NS-10", 0);

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielMixte()));
        faits.add(nuitHorsCharge("C-NS-10", LUNDI, salarie));

        verifier().given(faits.toArray()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> NuitsMaximumParSemaine.nuitsMaximumParSemaine(factory));
    }

    /** La contrainte ne consulte pas le contexte : la semaine se déduit de la date. */
    private static Object[] faits(SalarieReel salarie, List<Creneau> creneaux) {
        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static SalarieReel salarieAvecPlafond(String id, int plafond) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, plafond, null,
                null, null, null, null, null));
        return salarie;
    }

    /** Une nuit pour chaque décalage donné, à partir du lundi. */
    private static List<Creneau> nuits(SalarieReel salarie, int... decalages) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int d : decalages) {
            creneaux.add(nuit("C-NS-" + d, LUNDI.plusDays(d), salarie));
        }
        return creneaux;
    }

    private static Creneau nuit(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(22, 0), LocalTime.of(6, 0), TypePlageHoraire.NUIT,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau nuitHorsCharge(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(22, 0), LocalTime.of(6, 0), TypePlageHoraire.NUIT,
                ACTIVITE_HORS_CHARGE, salarie);
    }

    private static Creneau journee(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), TypePlageHoraire.JOUR,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                   TypePlageHoraire plage, String codeActiviteId, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, 480,
                TestRessourceFactory.SITE_CANON,
                codeActiviteId,
                null,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, plage,
                false, QualificationJour.OUVRE);
        c.setRessourceAffectee(salarie);
        return c;
    }

    private static ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }

    private static ReferentielComptabiliteActivite referentielMixte() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD),
                ACTIVITE_HORS_CHARGE,
                new ComptabiliteActivite(
                        ACTIVITE_HORS_CHARGE,
                        false, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }
}
