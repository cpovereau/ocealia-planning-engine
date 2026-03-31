package fr.project.planning.fileadapter.service;

import fr.project.planning.fileadapter.config.FileAdapterProperties;
import fr.project.planning.fileadapter.model.FileJobContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FileClaimServiceTest {

    @TempDir
    Path inboxDir;

    @TempDir
    Path processingDir;

    @Test
    void claim_deplaceLeFileVersProcessing() throws IOException {
        Path inboxFile = inboxDir.resolve("job.json");
        Files.writeString(inboxFile, "{\"scenarioType\":\"SC-01\"}");

        FileClaimService service = new FileClaimService(buildProperties(processingDir.toString()));
        Optional<FileJobContext> result = service.claim(inboxFile);

        assertThat(result).isPresent();
        assertThat(result.get().getSourceFileName()).isEqualTo("job.json");
        assertThat(result.get().getProcessingFile()).exists();
        assertThat(result.get().getJobId()).isNotBlank();
        assertThat(result.get().getStartTime()).isNotNull();
        assertThat(inboxFile).doesNotExist();
    }

    @Test
    void claim_retourneEmpty_quandLeFichierNExistePas() {
        Path fichierInexistant = inboxDir.resolve("absent.json");

        FileClaimService service = new FileClaimService(buildProperties(processingDir.toString()));
        Optional<FileJobContext> result = service.claim(fichierInexistant);

        assertThat(result).isEmpty();
    }

    @Test
    void claim_genereUnJobIdUnique_pourChaqueAppel() throws IOException {
        Path file1 = inboxDir.resolve("a.json");
        Path file2 = inboxDir.resolve("b.json");
        Files.writeString(file1, "{}");
        Files.writeString(file2, "{}");

        FileClaimService service = new FileClaimService(buildProperties(processingDir.toString()));
        String jobId1 = service.claim(file1).map(FileJobContext::getJobId).orElseThrow();
        String jobId2 = service.claim(file2).map(FileJobContext::getJobId).orElseThrow();

        assertThat(jobId1).isNotEqualTo(jobId2);
    }

    @Test
    void claim_retourneEmpty_pourNomFichierAvecPointPoint() throws IOException {
        // Crée un fichier dont le nom résolu contiendrait un traversal
        Path fakeFile = inboxDir.resolve("normal.json");
        Files.writeString(fakeFile, "{}");

        // Simule un Path dont getFileName() retourne un nom dangereux
        Path dangerousName = Path.of("../secret.json");

        FileClaimService service = new FileClaimService(buildProperties(processingDir.toString()));
        Optional<FileJobContext> result = service.claim(dangerousName);

        assertThat(result).isEmpty();
    }

    @Test
    void claim_retourneEmpty_pourNomFichierAvecSlash() {
        Path dangerousName = Path.of("subdir/file.json");

        FileClaimService service = new FileClaimService(buildProperties(processingDir.toString()));
        Optional<FileJobContext> result = service.claim(dangerousName);

        assertThat(result).isEmpty();
    }

    // --- helper ---

    private FileAdapterProperties buildProperties(String processingPath) {
        FileAdapterProperties props = new FileAdapterProperties();
        props.getPaths().setProcessing(processingPath);
        return props;
    }
}
