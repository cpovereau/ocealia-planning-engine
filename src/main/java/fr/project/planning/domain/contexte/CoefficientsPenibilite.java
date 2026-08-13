package fr.project.planning.domain.contexte;

import fr.project.planning.scoring.PenibiliteType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CoefficientsPenibilite — ce que vaut une heure, selon quand elle est travaillée (équité L1).
 *
 * <h3>Pourquoi ces coefficients existent</h3>
 * <p>Arbitrage métier : <em>on ne peut juger l'équité qu'à pénibilité équivalente</em>. Huit
 * heures un mardi et huit heures un dimanche ne sont pas la même chose, et ne doivent pas entrer
 * dans la même addition sans être ramenées à une unité commune. L'heure ordinaire est cette
 * unité — son coefficient vaut 1 par définition, il ne se paramètre pas.</p>
 *
 * <h3>Ils ne se décrètent pas, ils se calibrent</h3>
 * <p>L'ordre de pénibilité est un <strong>rang</strong> ; l'addition exige une <strong>échelle</strong>.
 * Une heure de nuit vaut-elle 1,5 heure ordinaire, ou 3 ? Aucune valeur <em>a priori</em> n'est
 * défendable : le choix décide qui est jugé le plus sollicité. C'est pourquoi ils sont transmis
 * par l'appelant et calibrés par simulation sur des cas réels (lot L3).</p>
 *
 * <h3>Le défaut est neutre, et c'est délibéré</h3>
 * <p>Absents, <strong>tous les coefficients valent 1</strong> : la mesure pondérée dégénère alors
 * en heures brutes, et ne prétend rien de plus. Choisir un autre défaut reviendrait à inventer
 * l'échelle que l'arbitrage interdit précisément de deviner. Le moteur signale qu'il applique le
 * défaut plutôt que de laisser croire à une pondération.</p>
 *
 * <p>Une seule catégorie s'applique à une minute donnée — celle que la dominance retient — de
 * sorte qu'aucun coefficient ne se multiplie par un autre.</p>
 */
public final class CoefficientsPenibilite implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Coefficient de l'heure ordinaire. Unité de la mesure : il ne se paramètre pas. */
    public static final double ORDINAIRE = 1.0;

    private final double nuit;
    private final double dimanche;
    private final double ferie;

    public CoefficientsPenibilite(Double nuit, Double dimanche, Double ferie) {
        this.nuit = valide(nuit, "nuit");
        this.dimanche = valide(dimanche, "dimanche");
        this.ferie = valide(ferie, "ferie");
    }

    /** Aucune pondération : la mesure pondérée vaut alors les heures brutes. */
    public static CoefficientsPenibilite neutres() {
        return new CoefficientsPenibilite(null, null, null);
    }

    /** {@code true} si aucun coefficient ne pondère quoi que ce soit. */
    public boolean sontNeutres() {
        return nuit == ORDINAIRE && dimanche == ORDINAIRE && ferie == ORDINAIRE;
    }

    public double pour(PenibiliteType type) {
        return switch (type) {
            case NUIT -> nuit;
            case DIMANCHE -> dimanche;
            case FERIE -> ferie;
        };
    }

    public double getNuit() {
        return nuit;
    }

    public double getDimanche() {
        return dimanche;
    }

    public double getFerie() {
        return ferie;
    }

    /**
     * [Équité L3] Les couples où la dominance retenue pèse moins qu'une pénibilité qu'elle absorbe.
     *
     * <h3>Pourquoi cette vérification existe</h3>
     * <p>Deux règles se rencontrent ici, et elles ne viennent pas du même endroit. La
     * <strong>dominance</strong> décide à quelle catégorie appartient une minute qui en cumule
     * plusieurs ; les <strong>coefficients</strong> décident ce que cette catégorie pèse. Rien ne
     * les oblige à s'accorder, et quand elles se contredisent le résultat est absurde plutôt que
     * faux : une minute qui cumule deux pénibilités pèse alors <em>moins</em> qu'une minute qui n'en
     * porte qu'une. Le cumul devient un avantage.</p>
     *
     * <p>Exemple, avec la dominance par défaut {@code NUIT > DIMANCHE > FERIE} et des coefficients
     * {@code dimanche = 1,2 ; ferie = 2,0} : une minute travaillée un dimanche férié est attribuée à
     * DIMANCHE et pèse 1,2, quand la même minute un jour férié ordinaire pèse 2,0.</p>
     *
     * <p>La condition tient en une phrase : <strong>les coefficients doivent être non croissants le
     * long de l'ordre de dominance</strong>. C'est la contrainte que toute calibration doit
     * respecter — voir 92_CALIBRATION_PENIBILITE.md.</p>
     *
     * @return les couples {@code (dominante, absorbée)} fautifs, vide si l'échelle est cohérente
     */
    public List<Inversion> inversionsSelon(DominancePenibilites dominance) {
        List<PenibiliteType> ordre = dominance.getOrdreDominance();
        List<Inversion> inversions = new ArrayList<>();

        for (int rang = 0; rang < ordre.size(); rang++) {
            for (int suivant = rang + 1; suivant < ordre.size(); suivant++) {
                PenibiliteType dominante = ordre.get(rang);
                PenibiliteType absorbee = ordre.get(suivant);
                if (pour(dominante) < pour(absorbee)) {
                    inversions.add(new Inversion(dominante, absorbee,
                            pour(dominante), pour(absorbee)));
                }
            }
        }
        return inversions;
    }

    /** Une minute cumulant les deux pénibilités est attribuée à {@code dominante}, qui pèse moins. */
    public record Inversion(PenibiliteType dominante, PenibiliteType absorbee,
                            double coefficientDominante, double coefficientAbsorbee) {
    }

    /**
     * Un coefficient absent vaut 1 — pas de pondération.
     *
     * <p>Une valeur négative est refusée : elle ferait d'une pénibilité un avantage, ce qui ne
     * décrit rien. Un coefficient inférieur à 1 reste accepté — c'est un choix d'échelle, pas une
     * incohérence.</p>
     */
    private static double valide(Double valeur, String nom) {
        if (valeur == null) {
            return ORDINAIRE;
        }
        if (valeur < 0) {
            throw new IllegalArgumentException(
                    "Le coefficient de pénibilité '" + nom + "' ne peut pas être négatif : "
                            + valeur + ". Une pénibilité ne s'allège pas.");
        }
        return valeur;
    }
}
