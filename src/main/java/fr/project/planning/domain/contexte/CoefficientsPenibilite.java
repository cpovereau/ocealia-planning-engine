package fr.project.planning.domain.contexte;

import fr.project.planning.scoring.PenibiliteType;

import java.io.Serializable;

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
