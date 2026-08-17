package fr.project.planning.domain.workmetrics;

/**
 * JoursDisponiblesSalarie — les jours disponibles d'un salarié, sous forme de fait de problème.
 *
 * <h3>Pourquoi ce fait existe</h3>
 * <p>{@link JoursDisponibles} suffit partout où l'on tient le {@code PlanningProblem} : la mesure
 * et le classement de SC-06 l'appellent directement. Un <strong>flux de contraintes</strong>, lui,
 * ne voit que des faits — et il ne peut pas joindre {@code Indisponibilite} sans se multiplier par
 * le nombre d'absences de chacun, ce qui multiplierait la pénalité d'équité d'autant.</p>
 *
 * <p>Ce fait porte donc le compte <strong>déjà agrégé</strong> : un tuple par salarié, jamais
 * plus.</p>
 *
 * <h3>Dérivé, jamais fourni</h3>
 * <p>{@code PlanningProblem} le calcule depuis ses ressources, son horizon et ses
 * indisponibilités. Aucun service de préparation ne le pose, et c'est le point : une jointure est
 * <em>interne</em>, et un salarié sans fait correspondant disparaîtrait de la contrainte — l'équité
 * serait alors silencieusement désactivée pour lui, ce qui est pire que le défaut qu'on corrige.
 * Le dériver le rend structurellement impossible à oublier.</p>
 */
public final class JoursDisponiblesSalarie {

    private final String salarieId;
    private final long jours;

    public JoursDisponiblesSalarie(String salarieId, long jours) {
        this.salarieId = salarieId;
        this.jours = jours;
    }

    public String getSalarieId() {
        return salarieId;
    }

    /** Jours de l'horizon où ce salarié n'était pas déclaré indisponible. Peut valoir 0. */
    public long getJours() {
        return jours;
    }

    @Override
    public String toString() {
        return "JoursDisponiblesSalarie{" + salarieId + " → " + jours + "}";
    }
}
