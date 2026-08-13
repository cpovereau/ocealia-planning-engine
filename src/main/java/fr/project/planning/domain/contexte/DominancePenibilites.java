package fr.project.planning.domain.contexte;

import fr.project.planning.scoring.PenibiliteType;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Représente l'ordre de dominance des pénibilités appliqué dans le scoring.
 *
 * Principe :
 * - Une minute pouvant appartenir à plusieurs catégories (NUIT, DIMANCHE, FERIE),
 *   le moteur applique une règle de dominance pour éviter la double-pondération.
 * - L'ordre est fourni par le PlanningContext (donc paramétrable côté client).
 * - Cette classe est immutable et validée à la construction.
 */
public final class DominancePenibilites implements Serializable { 

    private static final long serialVersionUID = 1L;

    /**
     * Ordre de dominance décroissant.
     * L'élément en position 0 est le plus dominant.
     *
     * Exemple par défaut :
     * [NUIT, DIMANCHE, FERIE]
     */
    private final List<PenibiliteType> ordreDominance;

    public DominancePenibilites(List<PenibiliteType> ordreDominance) {
        Objects.requireNonNull(
            ordreDominance,
            "ordreDominance ne doit pas être null."
        );

        this.ordreDominance = List.copyOf(ordreDominance);

        if (ordreDominance.isEmpty()) {
            throw new IllegalArgumentException(
                    "ordreDominance ne doit pas être vide."
            );
        }

        // Vérification des doublons et null
        Set<PenibiliteType> seen = EnumSet.noneOf(PenibiliteType.class);
        for (PenibiliteType type : ordreDominance) {
            if (type == null) {
                throw new IllegalArgumentException(
                        "ordreDominance ne doit pas contenir de valeur null."
                );
            }
            if (!seen.add(type)) {
                throw new IllegalArgumentException(
                        "ordreDominance ne doit pas contenir de doublons : " + type
                );
            }
        }

        // Variante stricte : exiger que toutes les pénibilités soient présentes
        if (!seen.containsAll(EnumSet.allOf(PenibiliteType.class))) {
            throw new IllegalArgumentException(
                    "ordreDominance doit contenir toutes les valeurs : "
                            + EnumSet.allOf(PenibiliteType.class)
            );
        }
    }

    /**
     * L'ordre appliqué faute d'ordre transmis : {@code NUIT > DIMANCHE > FERIE}.
     *
     * <p>Aucun scénario ne transmet cet ordre à ce jour ; c'est donc celui de toutes les demandes.
     * Il vivait en double — ici en commentaire, dans {@code PlanningContext} en dur — et le lot L3
     * en a eu besoin ailleurs pour vérifier qu'une échelle de coefficients ne le contredit pas
     * (voir {@link CoefficientsPenibilite#inversionsSelon(DominancePenibilites)}).</p>
     */
    public static DominancePenibilites parDefaut() {
        return new DominancePenibilites(
                List.of(PenibiliteType.NUIT, PenibiliteType.DIMANCHE, PenibiliteType.FERIE));
    }

    public List<PenibiliteType> getOrdreDominance() {
        return ordreDominance;
    }

    /**
     * Retourne la pénibilité dominante parmi celles fournies.
     *
     * Exemple :
     * si ordre = [NUIT, DIMANCHE, FERIE]
     * et candidats = [DIMANCHE, FERIE]
     * => retourne DIMANCHE
     */
    public PenibiliteType getDominante(Set<PenibiliteType> candidats) {
        if (candidats == null || candidats.isEmpty()) {
            return null;
        }

        for (PenibiliteType type : ordreDominance) {
            if (candidats.contains(type)) {
                return type;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "DominancePenibilites{" +
                "ordreDominance=" + ordreDominance +
                '}';
    }
}
