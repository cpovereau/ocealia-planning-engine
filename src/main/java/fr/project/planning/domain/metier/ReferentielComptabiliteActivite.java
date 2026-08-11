package fr.project.planning.domain.metier;

import fr.project.planning.domain.creneau.QualificationJour;

import java.util.Map;
import java.util.Collections;

public class ReferentielComptabiliteActivite {

    private final Map<String, ComptabiliteActivite> comptabilitesParCode;

    /**
     * Code activité désignant un repos hebdomadaire, et celui désignant un repos hebdomadaire
     * dominical (lot S7.9b).
     *
     * <p>Ces identifiants <strong>changent d'un client à l'autre</strong> : le moteur ne connaît
     * aucun code en dur, c'est l'appelant qui déclare lesquels valent repos, via
     * {@code dataSet.referentiels}. Non déclarés, aucun créneau n'est un marqueur de repos et
     * le calendrier retombe entièrement sur le repli samedi/dimanche.</p>
     */
    private final String codeReposHebdomadaire;
    private final String codeReposHebdomadaireDimanche;

    /** Référentiel sans déclaration de repos — conservé pour les appelants qui l'ignorent. */
    public ReferentielComptabiliteActivite(
            Map<String, ComptabiliteActivite> comptabilitesParCode
    ) {
        this(comptabilitesParCode, null, null);
    }

    public ReferentielComptabiliteActivite(
            Map<String, ComptabiliteActivite> comptabilitesParCode,
            String codeReposHebdomadaire,
            String codeReposHebdomadaireDimanche
    ) {
        this.comptabilitesParCode = comptabilitesParCode;
        this.codeReposHebdomadaire = normaliser(codeReposHebdomadaire);
        this.codeReposHebdomadaireDimanche = normaliser(codeReposHebdomadaireDimanche);
    }

    private static String normaliser(String code) {
        return (code == null || code.isBlank()) ? null : code;
    }

    /**
     * Nature du repos désigné par ce code activité, ou {@code null} si le code n'en désigne aucun.
     *
     * @return {@link QualificationJour#RH}, {@link QualificationJour#RHD}, ou {@code null}
     */
    public QualificationJour natureRepos(String codeActivite) {
        if (codeActivite == null) {
            return null;
        }
        if (codeActivite.equals(codeReposHebdomadaire)) {
            return QualificationJour.RH;
        }
        if (codeActivite.equals(codeReposHebdomadaireDimanche)) {
            return QualificationJour.RHD;
        }
        return null;
    }

    /** Vrai si ce code désigne un repos hebdomadaire, quelle qu'en soit la nature. */
    public boolean estReposHebdomadaire(String codeActivite) {
        return natureRepos(codeActivite) != null;
    }

    /** Vrai si l'appelant a déclaré au moins un code de repos. */
    public boolean declareUnCodeDeRepos() {
        return codeReposHebdomadaire != null || codeReposHebdomadaireDimanche != null;
    }

    public String getCodeReposHebdomadaire() {
        return codeReposHebdomadaire;
    }

    public String getCodeReposHebdomadaireDimanche() {
        return codeReposHebdomadaireDimanche;
    }

    /**
     * Comptabilité associée à un code, ou {@code null} si le code est inconnu.
     *
     * <p>Un code absent et un code {@code null} donnent le même résultat : dans les deux cas le
     * référentiel ne sait rien de l'activité, et l'appelant doit s'abstenir de juger.</p>
     *
     * <p>Le garde explicite n'est pas décoratif. Le comportement de {@code Map.get(null)} dépend
     * de l'implémentation : {@link java.util.HashMap} — celle des mappers — renvoie {@code null},
     * là où {@code Map.of(...)} lève une {@code NullPointerException}. Sans ce garde, la même
     * contrainte se tait en production et fait échouer le calcul de score en test.</p>
     */
    public ComptabiliteActivite getByCode(String codeActivite) {
        if (codeActivite == null) {
            return null;
        }
        return comptabilitesParCode.get(codeActivite);
    }

    public boolean contient(String codeActivite) {
        return codeActivite != null && comptabilitesParCode.containsKey(codeActivite);
    }

    public static ReferentielComptabiliteActivite neutre() {
        return new ReferentielComptabiliteActivite(Collections.emptyMap());
    }
}
