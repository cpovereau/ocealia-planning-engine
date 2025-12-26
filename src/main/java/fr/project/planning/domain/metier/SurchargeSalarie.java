package fr.project.planning.domain.metier;

import java.time.LocalDate;
import java.util.Set;

public class SurchargeSalarie {

    public enum NiveauSurcharge {
        AUCUNE,
        ALERTE,
        SOFT,
        HARD
    }

    public enum CauseSurcharge {
        DEPASSEMENT_JOURS_CONSECUTIFS,
        TROP_DE_NUITS_CONSECUTIVES,
        TRAVAIL_REPOS_HEBDOMADAIRE,
        TRAVAIL_FREQUENT_DIMANCHE,
        DETTE_REPOS_ELEVEE,
        DESEQUILIBRE_COLLECTIF
    }

    private String salarieId;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;

    private NiveauSurcharge niveau;

    private int heuresTravaillees;
    private int heuresNuit;
    private int heuresJourFerie;
    private int heuresReposHebdoTravaille;
    private int heuresSupplementaires;
    private int heuresComplementaires;
    private int detteReposCompensateur;

    private Set<CauseSurcharge> causes;

    private boolean suggerePosteVirtuel;

    private String commentaireSynthese;

    // getters uniquement
}
