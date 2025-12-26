package fr.project.planning.domain.ressource;

/**
 * Ressource spéciale représentant l'état "à affecter".
 * Utilisée pour éviter toute affectation nulle.
 */
public final class RessourceNonAffectee extends Ressource {

    public static final RessourceNonAffectee INSTANCE =
            new RessourceNonAffectee();

    private RessourceNonAffectee() {
        super("A_AFFECTER");
    }
}