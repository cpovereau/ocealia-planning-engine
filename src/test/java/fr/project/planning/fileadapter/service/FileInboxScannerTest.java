package fr.project.planning.fileadapter.service;

import fr.project.planning.fileadapter.config.FileAdapterProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileInboxScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void scanInbox_retourneUniquementLesFichiersJson() throws IOException {
        Files.createFile(tempDir.resolve("requete1.json"));
        Files.createFile(tempDir.resolve("requete2.json"));
        Files.createFile(tempDir.resolve("ignore.txt"));
        Files.createFile(tempDir.resolve("autre.xml"));

        FileInboxScanner scanner = new FileInboxScanner(buildProperties(tempDir.toString()));

        List<Path> result = scanner.scanInbox();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getFileName().toString().endsWith(".json"));
    }

    @Test
    void scanInbox_retourneListeVide_quandAucunFichierJson() throws IOException {
        Files.createFile(tempDir.resolve("data.xml"));

        FileInboxScanner scanner = new FileInboxScanner(buildProperties(tempDir.toString()));

        assertThat(scanner.scanInbox()).isEmpty();
    }

    @Test
    void scanInbox_retourneListeVide_quandInboxEstVide() {
        FileInboxScanner scanner = new FileInboxScanner(buildProperties(tempDir.toString()));

        assertThat(scanner.scanInbox()).isEmpty();
    }

    @Test
    void scanInbox_ignoreLesSousRepertoires() throws IOException {
        Files.createDirectory(tempDir.resolve("sous-dossier"));
        Files.createFile(tempDir.resolve("valide.json"));

        FileInboxScanner scanner = new FileInboxScanner(buildProperties(tempDir.toString()));

        List<Path> result = scanner.scanInbox();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName().toString()).isEqualTo("valide.json");
    }

    // --- helper ---

    private FileAdapterProperties buildProperties(String inboxPath) {
        FileAdapterProperties props = new FileAdapterProperties();
        props.getPaths().setInbox(inboxPath);
        return props;
    }
}
