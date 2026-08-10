package fr.project.planning.domain.metier;

import java.util.Map;
import java.util.Collections;

public class ReferentielComptabiliteActivite {

    private final Map<String, ComptabiliteActivite> comptabilitesParCode;

    public ReferentielComptabiliteActivite(
            Map<String, ComptabiliteActivite> comptabilitesParCode
    ) {
        this.comptabilitesParCode = comptabilitesParCode;
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
