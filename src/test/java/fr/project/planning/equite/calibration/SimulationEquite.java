package fr.project.planning.equite.calibration;

import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.domain.workmetrics.EcartAuContrat;
import fr.project.planning.scoring.PenibiliteType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SimulationEquite — rejouer une même charge sous plusieurs échelles, et voir ce qui change.
 *
 * <h3>Le problème que le harnais résout</h3>
 * <p>L'arbitrage métier dit que les coefficients de pénibilité <em>se calibrent, ils ne se
 * décrètent pas</em> : une heure de nuit vaut-elle 1,5 heure ordinaire, ou 3 ? Le choix décide qui
 * est jugé le plus sollicité, et aucune valeur <em>a priori</em> n'est défendable. Sans instrument,
 * « calibrer » veut dire deviner deux fois — une fois la valeur, une fois la justification.</p>
 *
 * <h3>Ce que le harnais rend, et ce qu'il ne rend pas</h3>
 * <p>Il ne produit pas de coefficient. Il transforme une question à laquelle personne ne peut
 * répondre — <em>combien vaut une heure de nuit ?</em> — en une question à laquelle le métier peut
 * répondre : <em>sur ce planning, Paul et Sophie basculent à 1,375 ; lequel des deux vous paraît le
 * plus sollicité ?</em></p>
 *
 * <h3>Pourquoi les bascules se calculent au lieu de se chercher</h3>
 * <p>Balayer une grille — 1,0 puis 1,5 puis 2,0 — ne dit jamais où l'ordre a changé, seulement
 * qu'il a changé entre deux essais. Or l'écart au contrat est <strong>affine</strong> en chaque
 * coefficient : à répartition fixée, {@code écart(c) = (K + c·m) / A − 1}. Deux personnes forment
 * donc deux droites, et leur intersection est une valeur exacte — celle où le classement
 * s'inverse. Deux droites parallèles ne se croisent jamais : ce coefficient-là ne départage pas
 * ces deux personnes, quelle que soit sa valeur, et c'est une information au même titre.</p>
 */
public final class SimulationEquite {

    /** En deçà, deux pentes sont tenues pour égales : les droites ne se croisent pas. */
    private static final double PENTES_EGALES = 1e-9;

    private final CasDeCalibration cas;

    public SimulationEquite(CasDeCalibration cas) {
        this.cas = cas;
    }

    /**
     * Le classement sous une échelle donnée, du plus sollicité au moins sollicité.
     *
     * <p>Les personnes sans contrat déclaré en sont absentes : rien ne les rend comparables, et
     * leur inventer une place serait pire qu'une absence de place.</p>
     */
    public List<LigneClassement> classement(CoefficientsPenibilite coefficients) {
        List<LigneClassement> lignes = new ArrayList<>();

        for (ChargeObservee charge : cas.charges()) {
            if (!charge.estComparable()) {
                continue;
            }
            double ponderees = charge.minutesPondereesPar(coefficients);
            lignes.add(new LigneClassement(
                    charge.ressourceId(),
                    ponderees,
                    EcartAuContrat.ecartPourcent(ponderees, charge.minutesAttendues())));
        }

        lignes.sort(Comparator.comparingDouble(LigneClassement::ecartPourcent).reversed()
                .thenComparing(LigneClassement::ressourceId));
        return lignes;
    }

    /**
     * Les valeurs du coefficient {@code variable} où deux personnes échangent leur place.
     *
     * @param variable la catégorie dont on fait varier le coefficient
     * @param base     les coefficients des autres catégories, qui ne bougent pas
     * @return les bascules de valeur positive, par valeur croissante ; une échelle négative ne
     *         décrivant rien, une intersection hors de ce domaine signifie que l'ordre des deux
     *         personnes ne change jamais
     */
    public List<Bascule> bascules(PenibiliteType variable, CoefficientsPenibilite base) {
        List<ChargeObservee> comparables = cas.charges().stream()
                .filter(ChargeObservee::estComparable)
                .sorted(Comparator.comparing(ChargeObservee::ressourceId))
                .toList();

        List<Bascule> bascules = new ArrayList<>();

        for (int i = 0; i < comparables.size(); i++) {
            for (int j = i + 1; j < comparables.size(); j++) {
                intersection(comparables.get(i), comparables.get(j), variable, base)
                        .ifPresent(bascules::add);
            }
        }

        bascules.sort(Comparator.comparingDouble(Bascule::valeur));
        return bascules;
    }

    /**
     * L'intersection des deux droites {@code écart(c)}, si elle existe dans le domaine admissible.
     *
     * <p>{@code écart_i(c) = (K_i + c·m_i) / A_i − 1}. Les deux sont égaux quand
     * {@code c · (m_A/A_A − m_B/A_B) = K_B/A_B − K_A/A_A}. Le dénominateur est la différence des
     * pentes : nul, les deux personnes réagissent identiquement au coefficient et leur ordre est
     * fixé une fois pour toutes.</p>
     */
    private static java.util.Optional<Bascule> intersection(ChargeObservee a, ChargeObservee b,
                                                            PenibiliteType variable,
                                                            CoefficientsPenibilite base) {
        double penteA = a.minutes(variable) / a.minutesAttendues();
        double penteB = b.minutes(variable) / b.minutesAttendues();

        double denominateur = penteA - penteB;
        if (Math.abs(denominateur) < PENTES_EGALES) {
            return java.util.Optional.empty();
        }

        double constanteA = a.minutesPondereesHors(variable, base) / a.minutesAttendues();
        double constanteB = b.minutesPondereesHors(variable, base) / b.minutesAttendues();

        double valeur = (constanteB - constanteA) / denominateur;
        if (valeur < 0) {
            return java.util.Optional.empty();
        }

        // Au-dessus de l'intersection, c'est la pente la plus forte qui domine.
        String auDessus = penteA > penteB ? a.ressourceId() : b.ressourceId();
        String enDessous = penteA > penteB ? b.ressourceId() : a.ressourceId();

        return java.util.Optional.of(
                new Bascule(variable, valeur, enDessous, auDessus));
    }

    /** Une place au classement : ce que la personne a pesé, et de combien elle s'écarte du contrat. */
    public record LigneClassement(String ressourceId, double minutesPonderees, double ecartPourcent) {
    }

    /**
     * Une valeur de coefficient où deux personnes échangent leur place au classement.
     *
     * @param plusSollicitEnDessous celle jugée la plus sollicitée tant que le coefficient reste
     *                              sous {@code valeur}
     * @param plusSollicitAuDessus  celle qui le devient au-delà
     */
    public record Bascule(PenibiliteType coefficient, double valeur,
                          String plusSollicitEnDessous, String plusSollicitAuDessus) {
    }
}
