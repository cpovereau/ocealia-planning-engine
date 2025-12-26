package fr.project.planning.domain.contexte;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * PlanningContext
 *
 * Décrit l'intention de résolution du solveur.
 * Ce n'est PAS un scénario, ni une règle.
 *
 * - Immuable pendant la résolution
 * - Utilisé par les contraintes pour adapter leur sévérité
 * - Fournit par l'appelant (WebDev)
 */
public final class PlanningContext implements Serializable {

    private final ObjectifResolution objectif;
    private final StrategieScoring strategieScoring;
    private final HorizonTemporel horizonTemporel;

    private final StrategieCouverture strategieCouverture;
    private final SeuilsDeTolerance seuilsDeTolerance;
    private final Penalites penalites;
    private final OptionsExplicabilite optionsExplicabilite;

    public PlanningContext(
            ObjectifResolution objectif,
            StrategieScoring strategieScoring,
            HorizonTemporel horizonTemporel,
            StrategieCouverture strategieCouverture,
            SeuilsDeTolerance seuilsDeTolerance,
            Penalites penalites,
            OptionsExplicabilite optionsExplicabilite
    ) {
        this.objectif = Objects.requireNonNull(objectif);
        this.strategieScoring = Objects.requireNonNull(strategieScoring);
        this.horizonTemporel = Objects.requireNonNull(horizonTemporel);
        this.strategieCouverture = Objects.requireNonNull(strategieCouverture);
        this.seuilsDeTolerance = Objects.requireNonNull(seuilsDeTolerance);
        this.penalites = Objects.requireNonNull(penalites);
        this.optionsExplicabilite = Objects.requireNonNull(optionsExplicabilite);
    }

    public ObjectifResolution getObjectif() {
        return objectif;
    }

    public StrategieScoring getStrategieScoring() {
        return strategieScoring;
    }

    public HorizonTemporel getHorizonTemporel() {
        return horizonTemporel;
    }

    public StrategieCouverture getStrategieCouverture() {
        return strategieCouverture;
    }

    public SeuilsDeTolerance getSeuilsDeTolerance() {
        return seuilsDeTolerance;
    }

    public Penalites getPenalites() {
        return penalites;
    }

    public OptionsExplicabilite getOptionsExplicabilite() {
        return optionsExplicabilite;
    }
}