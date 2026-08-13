package fr.project.planning.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SchemaPublie — confronte ce que le moteur produit à {@code 50_ScenarioResponse.schema.json}.
 *
 * <p>Ce schéma est publié aux intégrateurs et porte {@code additionalProperties: false} : un champ
 * ajouté au code et pas au schéma fait rejeter la réponse chez un client qui la valide. Le défaut
 * s'était déjà produit — le bloc {@code IgnoredCreneaux} déclarait une propriété jamais sérialisée
 * et en exigeait une qui n'existait nulle part (lot S8.4). Rien ne l'avait vu, parce que rien ne
 * comparait les deux.</p>
 *
 * <p>Ce n'est pas un validateur JSON Schema — le projet n'en embarque pas — mais il attrape la
 * classe d'erreur qui s'est produite : <strong>un nom de champ présent d'un côté et pas de
 * l'autre</strong>, dans les deux sens, sur tout un sous-arbre.</p>
 *
 * <p><strong>Limite connue</strong> : le schéma vit sous {@code docs/} et n'est pas déclaré comme
 * entrée de la tâche de test. Une modification portant sur le seul schéma peut laisser Gradle
 * considérer les tests à jour ; {@code gradle cleanTest test} le force.</p>
 */
public final class SchemaPublie {

    private static final Path FICHIER = Path.of("docs/50_ScenarioResponse.schema.json");

    private final JsonNode defs;

    private SchemaPublie(JsonNode defs) {
        this.defs = defs;
    }

    /** Charge les définitions du schéma publié. */
    public static SchemaPublie charger() throws Exception {
        return new SchemaPublie(new ObjectMapper().readTree(Files.readString(FICHIER)).get("$defs"));
    }

    /**
     * Compare un sous-arbre produit à la définition qui le décrit.
     *
     * @param produit    le nœud tel que le moteur l'a sérialisé
     * @param definition nom de la définition dans {@code $defs} — par exemple {@code Diagnostics}
     * @param chemin     préfixe de lecture pour les messages d'écart
     * @return les écarts constatés, vide si les deux concordent
     */
    public List<String> confronter(JsonNode produit, String definition, String chemin) {
        List<String> ecarts = new ArrayList<>();
        descendre(produit, defs.get(definition), chemin, ecarts);
        return ecarts;
    }

    /**
     * Compare les noms de propriétés d'un nœud produit à ceux que le schéma déclare, puis redescend
     * dans les objets et les tableaux d'objets.
     */
    private void descendre(JsonNode produit, JsonNode definition, String chemin,
                           List<String> ecarts) {

        if (produit == null || definition == null) {
            return;
        }
        JsonNode proprietes = definition.get("properties");
        if (proprietes == null) {
            return;
        }

        Set<String> declarees = new LinkedHashSet<>();
        proprietes.fieldNames().forEachRemaining(declarees::add);

        Set<String> produites = new LinkedHashSet<>();
        produit.fieldNames().forEachRemaining(produites::add);

        for (String champ : produites) {
            if (!declarees.contains(champ)) {
                ecarts.add(chemin + "." + champ + " est restitué mais absent du schéma");
            }
        }

        JsonNode requis = definition.get("required");
        if (requis != null) {
            for (JsonNode champ : requis) {
                if (!produites.contains(champ.asText())) {
                    ecarts.add(chemin + "." + champ.asText()
                            + " est exigé par le schéma mais absent de la réponse");
                }
            }
        }

        for (String champ : produites) {
            JsonNode sousDefinition = resoudre(proprietes.get(champ));
            if (sousDefinition == null) {
                continue;
            }
            JsonNode valeur = produit.get(champ);

            if (valeur.isObject()) {
                descendre(valeur, sousDefinition, chemin + "." + champ, ecarts);
            } else if (valeur.isArray() && sousDefinition.get("items") != null) {
                JsonNode elementDefinition = resoudre(sousDefinition.get("items"));
                int index = 0;
                for (Iterator<JsonNode> it = valeur.elements(); it.hasNext(); index++) {
                    JsonNode element = it.next();
                    if (element.isObject()) {
                        descendre(element, elementDefinition,
                                chemin + "." + champ + "[" + index + "]", ecarts);
                    }
                }
            }
        }
    }

    /** Suit un {@code $ref} local, ou rend la définition telle quelle. */
    private JsonNode resoudre(JsonNode noeud) {
        if (noeud == null) {
            return null;
        }
        JsonNode ref = noeud.get("$ref");
        if (ref == null) {
            return noeud;
        }
        return defs.get(ref.asText().substring(ref.asText().lastIndexOf('/') + 1));
    }
}
