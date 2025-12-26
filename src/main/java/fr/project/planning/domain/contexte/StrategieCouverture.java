package fr.project.planning.domain.contexte;

import java.io.Serializable;

/**
 * StrategieCouverture
 *
 * Décrit les moyens acceptables pour couvrir un créneau.
 * Ce n'est PAS une règle, mais un cadre d'autorisation.
 *
 * Utilisé par les contraintes pour pondérer ou pénaliser
 * certains types d'affectation.
 */
public final class StrategieCouverture implements Serializable {

    /**
     * Autorise l'utilisation de postes virtuels
     * pour couvrir des créneaux.
     */
    private final boolean autoriserPosteVirtuel;

    /**
     * Autorise explicitement qu'un créneau reste non affecté.
     * (état "A_AFFECTER")
     */
    private final boolean autoriserNonAffectation;

    /**
     * Indique si l'affectation à un salarié réel
     * doit toujours être privilégiée lorsqu'elle est possible.
     */
    private final boolean prefererSalarieReel;

    public StrategieCouverture(
            boolean autoriserPosteVirtuel,
            boolean autoriserNonAffectation,
            boolean prefererSalarieReel
    ) {
        this.autoriserPosteVirtuel = autoriserPosteVirtuel;
        this.autoriserNonAffectation = autoriserNonAffectation;
        this.prefererSalarieReel = prefererSalarieReel;
    }

    public boolean isAutoriserPosteVirtuel() {
        return autoriserPosteVirtuel;
    }

    public boolean isAutoriserNonAffectation() {
        return autoriserNonAffectation;
    }

    public boolean isPrefererSalarieReel() {
        return prefererSalarieReel;
    }
}
