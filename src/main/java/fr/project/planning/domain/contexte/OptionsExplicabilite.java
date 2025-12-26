package fr.project.planning.domain.contexte;

import java.io.Serializable;

/**
 * OptionsExplicabilite
 *
 * Décrit le niveau de traçabilité et d'explication attendu
 * en sortie de résolution.
 *
 * Ces options n'influencent PAS le score,
 * mais conditionnent les informations produites
 * et retournées à l'appelant.
 */
public final class OptionsExplicabilite implements Serializable {

    /**
     * Indique si les violations de contraintes
     * doivent être tracées et restituées.
     */
    private final boolean tracerViolations;

    /**
     * Indique si les affectations rejetées
     * ou remplacées doivent être conservées
     * à des fins d'analyse.
     */
    private final boolean conserverAffectationsRejetees;

    /**
     * Indique si des indicateurs RH doivent être produits
     * (ETP manquant, surcharge, tensions).
     */
    private final boolean produireIndicateursRH;

    public OptionsExplicabilite(
            boolean tracerViolations,
            boolean conserverAffectationsRejetees,
            boolean produireIndicateursRH
    ) {
        this.tracerViolations = tracerViolations;
        this.conserverAffectationsRejetees = conserverAffectationsRejetees;
        this.produireIndicateursRH = produireIndicateursRH;
    }

    public boolean isTracerViolations() {
        return tracerViolations;
    }

    public boolean isConserverAffectationsRejetees() {
        return conserverAffectationsRejetees;
    }

    public boolean isProduireIndicateursRH() {
        return produireIndicateursRH;
    }
}
