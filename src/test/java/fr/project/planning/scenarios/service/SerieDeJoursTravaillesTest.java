package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ComptabiliteActivite.TypeImpactActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.fixtures.TestCreneauFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SerieDeJoursTravaillesTest — lot L4 du chantier équité (V1, calcul pur).
 *
 * <h3>La décision que ces tests fixent</h3>
 * <p>Le critère métier est <em>ne pas rappeler qui enchaîne</em>. Ce qui compte est donc la série
 * à laquelle le besoin rattacherait la personne — pas la plus longue série de la fenêtre. Une
 * série survenue ailleurs est acquise : aucun choix ne la change, et s'en servir ferait perdre le
 * candidat pour un enchaînement dont il n'est pas question.</p>
 *
 * <p>La différence est invisible tant qu'on ne construit pas le cas exprès, ce que fait le premier
 * test.</p>
 */
class SerieDeJoursTravaillesTest {

    /** Le créneau produit par la fabrique porte {@code ACT1} ; le référentiel doit le connaître. */
    private static final String ACTIVITE = "ACT1";

    private static final ReferentielComptabiliteActivite REFERENTIEL =
            new ReferentielComptabiliteActivite(Map.of(ACTIVITE, new ComptabiliteActivite(
                    ACTIVITE, true, false, false, false, TypeImpactActivite.CHARGE_STANDARD)));

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate JEUDI = LUNDI.plusDays(3);

    @Test
    @DisplayName("C'est la série qui contient le jour du besoin, pas la plus longue de la fenêtre")
    void laSerieQuiContientLeJour_etNonLaPlusLongue() {
        // Lundi, mardi, mercredi d'affilée — puis un jour de repos, puis le vendredi seul.
        // La plus longue série de la semaine vaut 3 ; celle du vendredi en vaut 1, et c'est
        // celle-là que le besoin du vendredi prolongerait.
        List<Creneau> semaine = joursTravailles(0, 1, 2, 4);

        assertEquals(1, Sc06ChargeCalculator.joursConsecutifsAutour(
                        semaine, REFERENTIEL, LUNDI.plusDays(4)),
                "Prendre le maximum de la fenêtre ferait perdre cette personne pour trois jours "
                        + "d'affilée qui sont derrière elle et qu'aucun choix ne change.");

        assertEquals(3, Sc06ChargeCalculator.joursConsecutifsAutour(
                semaine, REFERENTIEL, LUNDI.plusDays(2)));
    }

    @Test
    @DisplayName("La série se compte des deux côtés du jour")
    void laSerieSeCompteDesDeuxCotes() {
        assertEquals(5, Sc06ChargeCalculator.joursConsecutifsAutour(
                        joursTravailles(1, 2, 3, 4, 5), REFERENTIEL, LUNDI.plusDays(3)),
                "Un jour encadré de deux jours travaillés appartient à une série qui compte "
                        + "avant lui comme après.");
    }

    @Test
    @DisplayName("Qui ne travaille pas ce jour-là n'a aucune série à prolonger")
    void quiNeTravaillePasCeJourLa_naPasDeSerie() {
        assertEquals(0, Sc06ChargeCalculator.joursConsecutifsAutour(
                        joursTravailles(0, 1, 2), REFERENTIEL, JEUDI),
                "Zéro, et non la série voisine : la personne serait rappelée, ce dont le palier 4 "
                        + "décide déjà — pas celui-ci.");
    }

    @Test
    @DisplayName("Une journée sans créneau comptant dans la charge ne prolonge aucune série")
    void unJourHorsChargeNeProlongeRien() {
        ReferentielComptabiliteActivite horsCharge =
                new ReferentielComptabiliteActivite(Map.of(ACTIVITE, new ComptabiliteActivite(
                        ACTIVITE, false, false, false, false, TypeImpactActivite.CHARGE_STANDARD)));

        assertEquals(0, Sc06ChargeCalculator.joursConsecutifsAutour(
                        joursTravailles(0, 1, 2), horsCharge, LUNDI),
                "La série mesure le travail, pas la présence au planning.");
    }

    private static List<Creneau> joursTravailles(int... decalages) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int decalage : decalages) {
            creneaux.add(TestCreneauFactory.jour(LUNDI.plusDays(decalage), 8, 0, 16, 0));
        }
        return creneaux;
    }
}
