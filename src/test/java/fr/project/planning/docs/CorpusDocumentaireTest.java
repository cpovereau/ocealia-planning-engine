package fr.project.planning.docs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CorpusDocumentaireTest — lot S8.5
 *
 * <p>La documentation du moteur mélangeait trois conventions de nommage et trois de ses fichiers
 * portaient des accents. Le dépôt est configuré {@code core.ignorecase=true} : la casse d'un nom
 * peut alors diverger entre le disque et l'index git <strong>sans que rien ne le signale</strong>,
 * et un lien juste sur un poste devient mort sur un autre. C'est arrivé à
 * {@code 90_SUIVI_DEVELOPPEMENT_MOTEUR.md}, suivi en minuscules par git pendant que le disque le
 * portait en majuscules — treize citations en majuscules auraient cassé au premier clone
 * sensible à la casse.</p>
 *
 * <p>Renommer une fois ne suffit pas : sans garde-fou, la dérive recommence au prochain document
 * créé. Ces trois tests verrouillent ce qui a été réparé — la convention, l'absence de lien mort,
 * et la complétude de l'index.</p>
 *
 * <p>Ils portent sur la <strong>documentation du moteur</strong>, soit les {@code .md} à la racine
 * de {@code docs/}. Deux exclusions assumées : les artefacts machine ({@code *.schema.json},
 * {@code *.yaml}), dont le nom est une référence externe qu'on ne renomme pas unilatéralement, et
 * le sous-répertoire {@code Windev_part/}, partagé avec l'équipe WinDev.</p>
 *
 * <p><strong>Convention d'écriture qui en découle</strong> : un document <em>disparu</em> se cite
 * sans son extension. Ces tests ne peuvent pas distinguer une citation d'un exemple, et donner son
 * extension à un fichier inexistant — pour expliquer justement qu'il n'existe pas — les ferait
 * échouer.</p>
 */
class CorpusDocumentaireTest {

    private static final Path DOCS = Path.of("docs");
    private static final Path INDEX = DOCS.resolve("00_INDEX_DOCUMENTATION.md");

    /** La racine du dépôt, pour ses seuls {@code .md} : {@code README.md} et ce qui l'accompagne. */
    private static final Path RACINE = Path.of(".");

    /** Un nom de document cité dans un texte : `92_CADRAGE_SCENARIO_SC-06.md`, chemin compris. */
    private static final Pattern CITATION = Pattern.compile("[\\w./\\-]*[\\w\\-]+\\.md");

    /** Où un document peut être cité : le corpus lui-même, et le code qui s'y réfère. */
    private static final List<Path> RACINES_CITANTES = List.of(DOCS, Path.of("src"));

    /**
     * Les {@code .md} de la racine du dépôt sont <strong>cités et citants</strong>, sans être
     * soumis à la convention de nommage ni à l'index — {@code README.md} n'est pas un document du
     * moteur, c'est sa porte d'entrée.
     *
     * <p>Ajouté le 2026-08-18, après que ce garde-fou a laissé le README dériver pendant des mois :
     * il annonçait cinq scénarios de test disparus, une arborescence en {@code com.example}, et
     * renvoyait vers un document inexistant. La garde ne veillait que sur {@code docs/} et
     * {@code src/} — la racine était un angle mort, et c'est précisément le fichier qu'un nouvel
     * arrivant ouvre en premier.</p>
     */
    private static List<Path> documentsRacine() {
        try (Stream<Path> s = Files.list(RACINE)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------
    // 1. La convention de nommage
    // ---------------------------------------------------------

    @Test
    @DisplayName("Tout document du moteur est nommé en majuscules, sans accent")
    void documentsDuMoteur_respectentLaConvention() {
        List<String> ecarts = new ArrayList<>();

        for (String nom : documentsDuMoteur()) {
            String base = nom.substring(0, nom.length() - ".md".length());
            if (!base.equals(base.toUpperCase(Locale.ROOT))) {
                ecarts.add(nom + " — le nom n'est pas en majuscules");
            }
            if (!base.equals(sansAccents(base))) {
                ecarts.add(nom + " — le nom porte des accents (attendu : " + sansAccents(base) + ".md)");
            }
        }

        assertTrue(ecarts.isEmpty(),
                "Convention : NN_SUJET_DU_DOCUMENT suivi de .md, en majuscules et sans accent.\n"
                        + "Sur un dépôt core.ignorecase=true, une casse divergente entre le disque et "
                        + "l'index git ne se voit pas — et casse les liens ailleurs.\n  - "
                        + String.join("\n  - ", ecarts));
    }

    // ---------------------------------------------------------
    // 2. Aucun lien mort
    // ---------------------------------------------------------

    @Test
    @DisplayName("Tout document cité, dans le corpus comme dans le code, existe bel et bien")
    void citations_designentToutesUnDocumentExistant() {
        Set<String> existants = new LinkedHashSet<>();
        for (Path racine : RACINES_CITANTES) {
            parcourir(racine).forEach(p -> existants.add(p.getFileName().toString()));
        }
        documentsRacine().forEach(p -> existants.add(p.getFileName().toString()));

        List<String> morts = new ArrayList<>();
        for (Path fichier : citants()) {
            String contenu = lire(fichier);
            Matcher m = CITATION.matcher(contenu);
            while (m.find()) {
                String cite = m.group();
                String nom = cite.substring(cite.replace('\\', '/').lastIndexOf('/') + 1);
                if (!existants.contains(nom)) {
                    morts.add(nom + "  (cité par " + fichier + ")");
                }
            }
        }

        assertTrue(morts.isEmpty(),
                "Ces documents sont cités mais n'existent pas :\n  - "
                        + String.join("\n  - ", morts.stream().distinct().toList()));
    }

    // ---------------------------------------------------------
    // 3. L'index est complet
    // ---------------------------------------------------------

    @Test
    @DisplayName("Tout document du moteur figure à l'index, et l'index ne cite que du réel")
    void index_estCompletEtExact() {
        String index = lire(INDEX);

        Set<String> cites = new LinkedHashSet<>();
        Matcher m = CITATION.matcher(index);
        while (m.find()) {
            String cite = m.group();
            cites.add(cite.substring(cite.lastIndexOf('/') + 1));
        }

        List<String> absents = documentsDuMoteur().stream()
                .filter(nom -> !nom.equals(INDEX.getFileName().toString()))
                .filter(nom -> !cites.contains(nom))
                .toList();

        assertTrue(absents.isEmpty(),
                "Ces documents existent mais ne figurent pas à l'index "
                        + "(00_INDEX_DOCUMENTATION.md) — un document introuvable n'est pas lu :\n  - "
                        + String.join("\n  - ", absents));
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    /** Les .md à la racine de docs/ — la documentation du moteur, sans les artefacts partagés. */
    private static List<String> documentsDuMoteur() {
        try (Stream<Path> s = Files.list(DOCS)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".md"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tous les fichiers texte susceptibles de citer un document. */
    private static List<Path> citants() {
        List<Path> fichiers = new ArrayList<>(documentsRacine());
        for (Path racine : RACINES_CITANTES) {
            parcourir(racine)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".md") || n.endsWith(".java") || n.endsWith(".yaml");
                    })
                    .forEach(fichiers::add);
        }
        return fichiers;
    }

    private static Stream<Path> parcourir(Path racine) {
        try {
            return Files.walk(racine).filter(Files::isRegularFile).toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String lire(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sansAccents(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
