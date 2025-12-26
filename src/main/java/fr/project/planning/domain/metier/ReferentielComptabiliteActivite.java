package fr.project.planning.domain.metier;

import java.util.Map;

public class ReferentielComptabiliteActivite {

    private final Map<String, ComptabiliteActivite> comptabilitesParCode;

    public ReferentielComptabiliteActivite(
            Map<String, ComptabiliteActivite> comptabilitesParCode
    ) {
        this.comptabilitesParCode = comptabilitesParCode;
    }

    public ComptabiliteActivite getByCode(String codeActivite) {
        return comptabilitesParCode.get(codeActivite);
    }

    public boolean contient(String codeActivite) {
        return comptabilitesParCode.containsKey(codeActivite);
    }
}
