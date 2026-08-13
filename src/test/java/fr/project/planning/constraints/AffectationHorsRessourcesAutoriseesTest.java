package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.AffectationHorsRessourcesAutorisees;
import fr.project.planning.domain.contexte.PerimetreArbitre;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.TypePosteVirtuel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AffectationHorsRessourcesAutoriseesTest — lot A0 de SC-05.
 *
 * <h3>Ce que la règle dit</h3>
 * <p>Un créneau remis en jeu par un arbitrage ne peut être confié qu'à l'une des ressources entre
 * lesquelles on arbitre.</p>
 *
 * <h3>Ce que ces tests protègent, et qui n'est pas évident</h3>
 * <ul>
 *   <li><strong>Le reste du planning reste libre.</strong> SC-05 exige le planning complet de la
 *       période ; borner au-delà du périmètre le rendrait entièrement fautif.</li>
 *   <li><strong>Le problème reste soluble.</strong> Le créneau garde toujours une issue — rester à
 *       pourvoir. Une contrainte HARD qui ne laisse aucune sortie ne rend pas visible l'impossible,
 *       elle le cache.</li>
 *   <li><strong>L'épinglé n'est pas jugé</strong>, et c'est ce qui rend l'arbitrage §5.2 tenable :
 *       le créneau tenu par un tiers est épinglé et signalé, pas repris.</li>
 *   <li><strong>Aucun {@code if (deux)}.</strong> §5.5 — l'ouverture à N est attendue à brève
 *       échéance. Un ensemble de trois se comporte exactement comme un ensemble de deux.</li>
 *   <li><strong>Les segments restent bornés.</strong> Joindre sur l'identifiant du créneau plutôt
 *       que sur celui du besoin ferait échapper tout créneau découpé, sans qu'aucun point HARD ne
 *       le signale.</li>
 * </ul>
 */
class AffectationHorsRessourcesAutoriseesTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);

    private static final String CRENEAU_ARBITRE = "PLN-001";
    private static final String CRENEAU_HORS_PERIMETRE = "PLN-999";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    private final SalarieReel salarieA = TestPlanningRequestFactory.buildSalarie("SAL-2001");
    private final SalarieReel salarieB = TestPlanningRequestFactory.buildSalarie("SAL-2002");
    private final SalarieReel tiers = TestPlanningRequestFactory.buildSalarie("SAL-3000");

    // ---------------------------------------------------------
    // L'interdiction
    // ---------------------------------------------------------

    @Test
    @DisplayName("Un créneau arbitré confié hors des ressources autorisées : interdit")
    void horsDesRessourcesAutorisees_estInterdit() {
        verifier().given(faits(
                        List.of(creneau(CRENEAU_ARBITRE, tiers)),
                        perimetre(salarieA, salarieB)))
                .penalizesBy(1);
    }

    @Test
    @DisplayName("L'interdiction se compte par créneau")
    void deuxCreneauxFautifs_comptentDeuxFois() {
        Creneau premier = creneau("PLN-001", tiers);
        Creneau second = creneau("PLN-002", tiers);

        verifier().given(faits(
                        List.of(premier, second),
                        new PerimetreArbitre(Set.of("PLN-001", "PLN-002"),
                                Set.of(salarieA.getId(), salarieB.getId()))))
                .penalizesBy(2);
    }

    @Test
    @DisplayName("Un segment d'un créneau arbitré reste borné — la jointure porte sur le besoin")
    void segmentDUnCreneauArbitre_resteBorne() {
        // La préparation peut découper un créneau : les morceaux reçoivent des identifiants
        // fabriqués et gardent celui de leur origine. Joindre sur getId() les ferait tous échapper
        // à l'arbitrage — sans qu'aucun point HARD ne le signale.
        Creneau segment = creneau("PLN-001-SEG-1", tiers);
        segment.setCreneauOrigineId(CRENEAU_ARBITRE);

        verifier().given(faits(List.of(segment), perimetre(salarieA, salarieB)))
                .penalizesBy(1);
    }

    // ---------------------------------------------------------
    // Ce que la règle autorise
    // ---------------------------------------------------------

    @Test
    @DisplayName("Chacune des ressources autorisées peut tenir le créneau")
    void ressourcesAutorisees_sontLibres() {
        for (SalarieReel autorise : List.of(salarieA, salarieB)) {
            verifier().given(faits(
                            List.of(creneau(CRENEAU_ARBITRE, autorise)),
                            perimetre(salarieA, salarieB)))
                    .penalizesBy(0);
        }
    }

    @Test
    @DisplayName("Un ensemble de trois se comporte comme un ensemble de deux — aucun if (deux)")
    void ensembleDeTrois_seComporteIdentiquement() {
        // §5.5 du cadrage : l'ouverture à N est attendue à brève échéance. La limite à deux est une
        // règle du contrat d'entrée, pas une hypothèse de calcul — ce test le vérifie.
        PerimetreArbitre aTrois = new PerimetreArbitre(
                Set.of(CRENEAU_ARBITRE),
                Set.of(salarieA.getId(), salarieB.getId(), tiers.getId()));

        verifier().given(faits(List.of(creneau(CRENEAU_ARBITRE, tiers)), aTrois))
                .penalizesBy(0);

        SalarieReel quatrieme = TestPlanningRequestFactory.buildSalarie("SAL-4000");
        verifier().given(faits(List.of(creneau(CRENEAU_ARBITRE, quatrieme)), aTrois))
                .penalizesBy(1);
    }

    @Test
    @DisplayName("Le reste du planning reste libre : hors périmètre, aucune borne")
    void horsDuPerimetre_aucuneBorne() {
        // SC-05 exige le planning complet de la période, sans quoi les bornes hebdomadaires sont
        // invérifiables. Borner au-delà du périmètre le rendrait entièrement fautif.
        verifier().given(faits(
                        List.of(creneau(CRENEAU_HORS_PERIMETRE, tiers)),
                        perimetre(salarieA, salarieB)))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Aucun périmètre transmis : la contrainte ne se déclenche jamais")
    void aucunPerimetre_neDeclencheRien() {
        // L'état de tous les scénarios livrés à ce jour. Un vide ne suppose jamais que la chose est
        // possible : il dit qu'aucun arbitrage n'est demandé.
        verifier().given(creneau(CRENEAU_ARBITRE, tiers))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Ce qui garantit que le problème reste soluble
    // ---------------------------------------------------------

    @Test
    @DisplayName("Rester à pourvoir n'est jamais fautif")
    void resteAPourvoir_nEstPasJuge() {
        verifier().given(faits(
                        List.of(creneau(CRENEAU_ARBITRE, RessourceNonAffectee.INSTANCE)),
                        perimetre(salarieA, salarieB)))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un poste virtuel n'est pas une ressource à autoriser")
    void posteVirtuel_nEstPasJuge() {
        PosteVirtuel posteVirtuel = new PosteVirtuel(
                "PV-001", TypePosteVirtuel.POTENTIEL, 1, Set.of(), Set.of(), Set.of());

        verifier().given(faits(
                        List.of(creneau(CRENEAU_ARBITRE, posteVirtuel)),
                        perimetre(salarieA, salarieB)))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un créneau épinglé sur un tiers n'est pas jugé — sans quoi §5.2 serait insoluble")
    void creneauEpingleSurUnTiers_nEstPasJuge() {
        // Arbitrage §5.2 : le créneau du périmètre tenu par un tiers est épinglé et signalé, jamais
        // repris. Le juger produirait un point HARD que le solveur ne peut pas défaire, et
        // « épingler et signaler » deviendrait « épingler et rendre insoluble ».
        Creneau tenuParUnTiers = creneau(CRENEAU_ARBITRE, tiers);
        tenuParUnTiers.figerSur(tiers);

        verifier().given(faits(List.of(tenuParUnTiers), perimetre(salarieA, salarieB)))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Le fait d'entrée refuse ce qui n'est pas un arbitrage
    // ---------------------------------------------------------

    @Test
    @DisplayName("Un arbitrage sans créneau ou sans personne est refusé à la construction")
    void perimetreVide_estRefuse() {
        // Un ensemble autorisé vide interdirait à quiconque de tenir les créneaux du périmètre, et
        // l'appelant lirait le résultat comme une impossibilité métier au lieu d'une demande mal
        // formée.
        assertThatThrownBy(() -> new PerimetreArbitre(Set.of(), Set.of(salarieA.getId())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PerimetreArbitre(Set.of(CRENEAU_ARBITRE), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat((p, factory) ->
                AffectationHorsRessourcesAutorisees.affectationHorsRessourcesAutorisees(factory));
    }

    private static Object[] faits(List<Creneau> creneaux, PerimetreArbitre perimetre) {
        List<Object> faits = new ArrayList<>(creneaux);
        faits.add(perimetre);
        return faits.toArray();
    }

    private static PerimetreArbitre perimetre(SalarieReel a, SalarieReel b) {
        return new PerimetreArbitre(Set.of(CRENEAU_ARBITRE), Set.of(a.getId(), b.getId()));
    }

    private static Creneau creneau(String id, Ressource ressource) {
        Creneau c = new Creneau(
                id, LUNDI, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                "SITE-A", "ACT-SOIN", null, "PC-001",
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE);
        c.setRessourceAffectee(ressource);
        return c;
    }
}
