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
    private final ResolutionType resolutionType;
    private final HypotheseHistorique hypotheseHistorique;
    private final HorizonTemporel horizonTemporel; 

    private final StrategieCouverture strategieCouverture;
    private final SeuilsDeTolerance seuilsDeTolerance;
    private final Penalites penalites;
    private final OptionsExplicabilite optionsExplicabilite;

    public PlanningContext(
            ObjectifResolution objectif,
            StrategieScoring strategieScoring,
            ResolutionType resolutionType,
            HypotheseHistorique hypotheseHistorique,
            HorizonTemporel horizonTemporel,
            StrategieCouverture strategieCouverture,
            SeuilsDeTolerance seuilsDeTolerance,
            Penalites penalites,
            OptionsExplicabilite optionsExplicabilite
    ) {
        this.objectif = Objects.requireNonNull(objectif);
        this.strategieScoring = Objects.requireNonNull(strategieScoring);
        this.resolutionType = Objects.requireNonNull(resolutionType);
        this.hypotheseHistorique = Objects.requireNonNull(hypotheseHistorique);
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

    public ResolutionType getResolutionType() {
        return resolutionType;
    }  

    public HypotheseHistorique getHypotheseHistorique() {
        return hypotheseHistorique;
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

// ======================================================
// Valeurs par défaut neutres (socle test / scénarios)
// ======================================================

private static StrategieCouverture defaultStrategieCouverture() {
    // Cadre permissif : aucune interdiction implicite
    return new StrategieCouverture(
            true,  // autoriserPosteVirtuel
            true,  // autoriserNonAffectation
            true   // prefererSalarieReel
    );
}

private static SeuilsDeTolerance defaultSeuilsDeTolerance() {
    // Seuils volontairement larges pour ne pas biaiser les tests
    return new SeuilsDeTolerance(
            Integer.MAX_VALUE, // surchargeMaxParSalarie
            Integer.MAX_VALUE, // violationsLegalesMax
            Integer.MAX_VALUE  // violationsMetierMax
    );
}

private static Penalites defaultPenalites() {
    // Ordres de grandeur neutres, sans écraser le score
    return new Penalites(
            1,        // violationPhysique
            10_000,   // violationLegale
            1_000,    // violationMetier
            100,      // violationService
            10,       // violationPersonnelle
            500,      // affectationPosteVirtuel
            2_000,    // nonAffectation
            5_000,    // detteRepos
            5_000,    // nuitsConsecutives
            5_000     // dimanchesTravailles
    );
}

private static OptionsExplicabilite defaultOptionsExplicabilite() {
    // Actif mais non intrusif
    return new OptionsExplicabilite(
            true,  // tracerViolations
            true,  // conserverAffectationsRejetees
            true   // produireIndicateursRH
    );
}

    // ============================================================
    // Constructeur simplifié avec valeurs par défaut
    // ============================================================

public PlanningContext(
        ObjectifResolution objectif,
        StrategieScoring strategieScoring,
        LocalDate dateDebut,
        LocalDate dateFin,
        ResolutionType resolutionType,
        HypotheseHistorique hypotheseHistorique
) {
    this(
            objectif,
            strategieScoring,
            resolutionType,
            hypotheseHistorique,
            new HorizonTemporel(dateDebut, dateFin),
            defaultStrategieCouverture(),
            defaultSeuilsDeTolerance(),
            defaultPenalites(),
            defaultOptionsExplicabilite()
    );
}
}