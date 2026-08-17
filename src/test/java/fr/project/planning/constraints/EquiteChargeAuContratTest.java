package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.EquiteChargeAuContrat;
import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.contexte.ToleranceEquite;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.ContratSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.JoursDisponiblesSalarie;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.scoring.StrategieScoring;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EquiteChargeAuContratTest — lot L5 du chantier équité (V3, contrainte isolée).
 *
 * <h3>Ce que la règle répare</h3>
 * <p>Toutes les autres contraintes vérifient une personne contre <em>sa</em> borne. Conséquence :
 * un planning où l'un fait 48 h et l'autre 25 h obtenait exactement le même score qu'un planning à
 * 35 h chacun. C'est la première règle du moteur à rendre un déséquilibre coûteux.</p>
 *
 * <h3>Ce que ces tests gardent, et qui n'est pas évident</h3>
 * <ul>
 *   <li><strong>Sans tolérance transmise, elle ne pèse rien.</strong> C'est le cas de toutes les
 *       demandes à ce jour, et c'est délibéré : les coefficients qui pondèrent cet écart ne sont
 *       pas calibrés. Peser une mesure dont l'échelle reste à établir reviendrait à deviner deux
 *       fois. Ce test-là est le plus important du lot.</li>
 *   <li><strong>La sous-charge coûte autant que la surcharge.</strong> Sans cela le moteur
 *       éviterait de surcharger sans jamais rééquilibrer.</li>
 *   <li><strong>Le salarié qui ne travaille rien est pénalisé aussi.</strong> Il n'apparaît dans
 *       aucune jointure : sans le second volet, la personne que l'équité désigne le plus resterait
 *       invisible au moteur.</li>
 * </ul>
 *
 * <p>Horizon d'une semaine, contrat de 35 h : <strong>2 100 minutes attendues</strong>. Tous les
 * créneaux sont des journées ordinaires — ni nuit, ni dimanche, ni férié — de sorte que les minutes
 * pondérées valent les minutes travaillées et que chaque nombre se vérifie à la main.</p>
 */
class EquiteChargeAuContratTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final long JOURS_DE_LA_SEMAINE = 7;
    private static final String ACTIVITE = "ACT-SOIN";
    private static final String ACTIVITE_HORS_CHARGE = "ACT-FORMATION";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // Sans tolérance, la règle n'existe pas
    // ---------------------------------------------------------

    @Test
    @DisplayName("Sans tolérance transmise, aucun écart ne coûte quoi que ce soit")
    void sansTolerance_aucunEcartNeCouteRien() {
        // Le score doit rester exactement celui d'avant le lot L5. C'est ce qui permet de livrer
        // la contrainte avant que les coefficients qui la pondèrent ne soient calibrés.
        SalarieReel salarie = salarie("SAL-A", 35.0);

        volet1().given(faits(sansTolerance(), salarie, minutes(salarie, 12 * 60)))
                .penalizesBy(0);
        volet2().given(faits(sansTolerance(), salarie("SAL-B", 35.0)))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Volet 1 — le salarié travaille
    // ---------------------------------------------------------

    @Test
    @DisplayName("Au-delà de la tolérance, chaque point d'écart coûte")
    void auDelaDeLaTolerance_chaquePointCoute() {
        // 42 h sur 35 attendues : +20 %. Tolérance de 10 → 10 points excédentaires, à 10 chacun.
        SalarieReel salarie = salarie("SAL-SURCHARGE", 35.0);

        volet1().given(faits(tolerance(10.0), salarie, minutes(salarie, 42 * 60)))
                .penalizesBy(100);
    }

    @Test
    @DisplayName("La sous-charge coûte autant que la surcharge — sinon rien ne se rééquilibre")
    void laSousChargeCouteAutantQueLaSurcharge() {
        // 28 h sur 35 attendues : −20 %, soit le même écart en valeur absolue que le test
        // précédent, et le même coût. Sans cela le moteur éviterait de surcharger sans jamais
        // rééquilibrer : l'équité ne se produirait pas, elle serait seulement moins violée.
        SalarieReel salarie = salarie("SAL-SOUS-CHARGE", 35.0);

        volet1().given(faits(tolerance(10.0), salarie, minutes(salarie, 28 * 60)))
                .penalizesBy(100);
    }

    @Test
    @DisplayName("Dans la tolérance, rien n'est dû")
    void dansLaTolerance_rienNestDu() {
        SalarieReel salarie = salarie("SAL-JUSTE", 35.0);

        volet1().given(faits(tolerance(10.0), salarie, minutes(salarie, 32 * 60)))
                .penalizesBy(0);   // −8,57 %, en deçà des 10 points tolérés
    }

    @Test
    @DisplayName("Sans volume contractuel déclaré, rien n'est comparable et rien n'est pesé")
    void sansContrat_rienNestPese() {
        SalarieReel salarie = salarie("SAL-SANS-CONTRAT", null);

        volet1().given(faits(tolerance(10.0), salarie, minutes(salarie, 60 * 60)))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Une semaine entière hors charge ne passe pas entre les deux volets")
    void uneSemaineEntiereHorsCharge_nePassePasEntreLesDeuxVolets() {
        // Même périmètre que les bornes individuelles : ces heures-là ne sont pas du travail. Le
        // salarié n'a donc aucune charge, et c'est le volet 2 qui doit le rattraper. Si celui-ci
        // se contentait de constater « aucun créneau », une semaine entière de formation
        // échapperait aux deux volets à la fois — pénalisée nulle part.
        SalarieReel salarie = salarie("SAL-FORMATION", 35.0);
        Creneau formation = travail("C-F", LUNDI, ACTIVITE_HORS_CHARGE, 8 * 60, salarie);

        volet1().given(faits(tolerance(10.0), salarie, List.of(formation)))
                .penalizesBy(0);
        volet2().given(faits(tolerance(10.0), salarie, List.of(formation)))
                .penalizesBy(900);   // écart de −100 %, moins 10 tolérés, à 10 le point
    }

    // ---------------------------------------------------------
    // Volet 2 — le salarié ne travaille rien
    // ---------------------------------------------------------

    @Test
    @DisplayName("Le salarié sans aucun créneau est pénalisé — c'est celui que l'équité désigne")
    void leSalarieSansAucunCreneau_estPenalise() {
        // Il n'apparaît dans aucune jointure : sans ce volet, son écart de −100 % serait invisible
        // et le moteur n'aurait aucune raison de lui donner du travail.
        SalarieReel salarie = salarie("SAL-INOCCUPE", 35.0);

        volet2().given(faits(tolerance(10.0), salarie)).penalizesBy(900);
    }

    @Test
    @DisplayName("Un salarié qui travaille n'est pas pénalisé deux fois")
    void unSalarieQuiTravaille_nEstPasPenaliseDeuxFois() {
        SalarieReel salarie = salarie("SAL-OCCUPE", 35.0);

        volet2().given(faits(tolerance(10.0), salarie, minutes(salarie, 8 * 60)))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // [Rang 14] Une absence n'est pas du temps disponible non travaillé
    // ---------------------------------------------------------

    @Test
    @DisplayName("Absent quatre jours, il fait son contrat en trois : il ne coûte rien")
    void absentQuatreJours_faireSonContratEnTroisNeCouteRien() {
        // 35 h par semaine proratisées sur 3 jours disponibles : 900 minutes attendues.
        // Il en fait 900. Sur l'horizon nu il serait lu à −57 %, soit 470 points de pénalité —
        // et le moteur le désignerait pour rattraper son propre congé.
        SalarieReel salarie = salarie("SAL-A", 35.0);

        volet1().given(faits(tolerance(10.0), salarie, minutes(salarie, 15 * 60), 3))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Absent toute la semaine, il n'est pas le salarié le plus sous-chargé")
    void absentTouteLaSemaine_nEstPasLePlusSousCharge() {
        // Sans jour disponible, rien n'est comparable. Le noter −100 % en ferait le premier que
        // l'équité désigne : il se verrait rattraper son arrêt maladie dès le créneau suivant.
        SalarieReel salarie = salarie("SAL-B", 35.0);

        volet2().given(faits(tolerance(10.0), salarie, List.of(), 0))
                .penalizesBy(0);
        volet1().given(faits(tolerance(10.0), salarie, minutes(salarie, 8 * 60), 0))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Absent une partie seulement, celui qui ne travaille rien reste jugé")
    void absentUnePartie_celuiQuiNeTravailleRienResteJuge() {
        // Il avait trois jours pour travailler et n'a rien fait : c'est exactement ce que
        // l'équité doit voir. La déduction ne doit pas devenir une excuse générale.
        SalarieReel salarie = salarie("SAL-C", 35.0);

        volet2().given(faits(tolerance(10.0), salarie, List.of(), 3))
                .penalizesBy(900);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private SingleConstraintVerification<PlanningProblem> volet1() {
        return constraintVerifier.verifyThat(
                (p, factory) -> EquiteChargeAuContrat.equiteChargeAuContrat(factory));
    }

    private SingleConstraintVerification<PlanningProblem> volet2() {
        return constraintVerifier.verifyThat(
                (p, factory) -> EquiteChargeAuContrat.equiteSalarieSansAffectation(factory));
    }

    private static Object[] faits(PlanningContext contexte, SalarieReel salarie,
                                  List<Creneau> creneaux) {
        return faits(contexte, salarie, creneaux, JOURS_DE_LA_SEMAINE);
    }

    /**
     * [Rang 14] {@code JoursDisponiblesSalarie} est un fait <strong>dérivé</strong> : en
     * production {@code PlanningProblem} le produit, mais {@code ConstraintVerifier} ne connaît
     * que les faits qu'on lui donne. L'omettre ferait sortir le salarié de la jointure et la
     * contrainte ne pèserait plus rien — un test vert pour la mauvaise raison.
     */
    private static Object[] faits(PlanningContext contexte, SalarieReel salarie,
                                  List<Creneau> creneaux, long joursDisponibles) {
        List<Object> faits = new ArrayList<>();
        faits.add(contexte);
        faits.add(referentiel());
        faits.add(RegulatoryParameters.neutre());
        faits.add(salarie);
        faits.add(new JoursDisponiblesSalarie(salarie.getId(), joursDisponibles));
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static Object[] faits(PlanningContext contexte, SalarieReel salarie) {
        return faits(contexte, salarie, List.of());
    }

    /** Une charge donnée, découpée en journées ordinaires successives à partir du lundi. */
    private static List<Creneau> minutes(SalarieReel salarie, int minutesTotales) {
        List<Creneau> creneaux = new ArrayList<>();
        int reste = minutesTotales;
        int jour = 0;
        while (reste > 0) {
            int duree = Math.min(reste, 8 * 60);
            creneaux.add(travail("C-" + salarie.getId() + "-" + jour,
                    LUNDI.plusDays(jour % 5L), ACTIVITE, duree, salarie));
            reste -= duree;
            jour++;
        }
        return creneaux;
    }

    private static Creneau travail(String id, LocalDate date, String codeActivite, int duree,
                                   SalarieReel salarie) {
        Creneau creneau = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), duree,
                "SITE-A", codeActivite, null, "PC-001",
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE);
        creneau.setRessourceAffectee(salarie);
        return creneau;
    }

    private static SalarieReel salarie(String id, Double heuresHebdomadaires) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContrat(new ContratSalarie(null, heuresHebdomadaires, null, null));
        return salarie;
    }

    private static PlanningContext sansTolerance() {
        return contexte(new ToleranceEquite(null));
    }

    private static PlanningContext tolerance(Double ecartTolerePourcent) {
        return contexte(new ToleranceEquite(ecartTolerePourcent));
    }

    private static PlanningContext contexte(ToleranceEquite tolerance) {
        return new PlanningContext(
                ObjectifResolution.ANALYSER_LE_MANQUE,
                StrategieScoring.ANALYSE_RH,
                LUNDI,
                LUNDI.plusDays(6),
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE,
                CoefficientsPenibilite.neutres(),
                tolerance);
    }

    private static ReferentielComptabiliteActivite referentiel() {
        return new ReferentielComptabiliteActivite(Map.of(
                ACTIVITE, new ComptabiliteActivite(ACTIVITE, true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD),
                ACTIVITE_HORS_CHARGE, new ComptabiliteActivite(ACTIVITE_HORS_CHARGE, false, false,
                        false, false, ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }
}
