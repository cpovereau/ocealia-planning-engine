package fr.project.planning.fileadapter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FileArchiveServiceTest {

    @TempDir
    Path tempDir;

    private final FileArchiveService archiveService = new FileArchiveService();

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Path archiveLeaf(String clientId) {
        LocalDate today = LocalDate.now();
        return tempDir.resolve("archive")
                .resolve(String.format("%04d", today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(clientId);
    }

    // -------------------------------------------------------------------------
    // cas nominal SUCCESS : input déplacé + output copié
    // -------------------------------------------------------------------------

    @Test
    void archiveJob_success_inputDeplacéEtOutputCopié() throws IOException {
        Path archiveBaseDir = Files.createDirectory(tempDir.resolve("archive"));
        Path processing = Files.createDirectory(tempDir.resolve("processing"));
        Path outbox = Files.createDirectory(tempDir.resolve("outbox"));

        Path inputFile  = processing.resolve("req.json");
        Path outputFile = outbox.resolve("req_RESULT.json");
        Files.writeString(inputFile, "{\"requestId\":\"REQ-001\"}");
        Files.writeString(outputFile, "{\"status\":\"SUCCESS\"}");

        archiveService.archiveJob(inputFile, outputFile, archiveBaseDir, "CLIENT01");

        Path leaf = archiveLeaf("CLIENT01");

        // input déplacé : plus dans processing/
        assertThat(inputFile).doesNotExist();
        assertThat(leaf.resolve("req.json")).exists();

        // output copié : toujours dans outbox/ ET dans archive
        assertThat(outputFile).exists();
        assertThat(leaf.resolve("req_RESULT.json")).exists();
    }

    // -------------------------------------------------------------------------
    // cas ERROR : input déplacé + fichier error copié
    // -------------------------------------------------------------------------

    @Test
    void archiveJob_error_inputDeplacéEtErrorCopié() throws IOException {
        Path archiveBaseDir = Files.createDirectory(tempDir.resolve("archive"));
        Path processing = Files.createDirectory(tempDir.resolve("processing"));
        Path errorDir   = Files.createDirectory(tempDir.resolve("error"));

        Path inputFile = processing.resolve("bad.json");
        Path errorFile = errorDir.resolve("bad_RESULT.json");
        Files.writeString(inputFile, "{}");
        Files.writeString(errorFile, "{\"status\":\"ERROR\"}");

        archiveService.archiveJob(inputFile, errorFile, archiveBaseDir, "CLIENT02");

        Path leaf = archiveLeaf("CLIENT02");

        assertThat(inputFile).doesNotExist();
        assertThat(leaf.resolve("bad.json")).exists();
        assertThat(errorFile).exists();
        assertThat(leaf.resolve("bad_RESULT.json")).exists();
    }

    // -------------------------------------------------------------------------
    // clientId absent → répertoire UNKNOWN
    // -------------------------------------------------------------------------

    @Test
    void archiveJob_clientIdNull_utiliseFallbackUNKNOWN() throws IOException {
        Path archiveBaseDir = Files.createDirectory(tempDir.resolve("archive"));
        Path processing = Files.createDirectory(tempDir.resolve("processing"));
        Path outbox = Files.createDirectory(tempDir.resolve("outbox"));

        Path inputFile  = processing.resolve("req.json");
        Path outputFile = outbox.resolve("req_RESULT.json");
        Files.writeString(inputFile, "{}");
        Files.writeString(outputFile, "{}");

        archiveService.archiveJob(inputFile, outputFile, archiveBaseDir, null);

        Path leaf = archiveLeaf("UNKNOWN");
        assertThat(leaf).isDirectory();
        assertThat(leaf.resolve("req.json")).exists();
        assertThat(leaf.resolve("req_RESULT.json")).exists();
    }

    @Test
    void archiveJob_clientIdVide_utiliseFallbackUNKNOWN() throws IOException {
        Path archiveBaseDir = Files.createDirectory(tempDir.resolve("archive"));
        Path processing = Files.createDirectory(tempDir.resolve("processing"));
        Path outbox = Files.createDirectory(tempDir.resolve("outbox"));

        Path inputFile  = processing.resolve("req.json");
        Path outputFile = outbox.resolve("req_RESULT.json");
        Files.writeString(inputFile, "{}");
        Files.writeString(outputFile, "{}");

        archiveService.archiveJob(inputFile, outputFile, archiveBaseDir, "   ");

        Path leaf = archiveLeaf("UNKNOWN");
        assertThat(leaf).isDirectory();
        assertThat(leaf.resolve("req.json")).exists();
    }

    // -------------------------------------------------------------------------
    // output absent : pas d'erreur, input archivé quand même
    // -------------------------------------------------------------------------

    @Test
    void archiveJob_outputAbsent_inputArchivéSansErreur() throws IOException {
        Path archiveBaseDir = Files.createDirectory(tempDir.resolve("archive"));
        Path processing = Files.createDirectory(tempDir.resolve("processing"));

        Path inputFile  = processing.resolve("req.json");
        Path outputFile = tempDir.resolve("outbox").resolve("req_RESULT.json"); // n'existe pas
        Files.writeString(inputFile, "{}");

        // ne doit pas lever d'exception
        archiveService.archiveJob(inputFile, outputFile, archiveBaseDir, "CLIENT01");

        Path leaf = archiveLeaf("CLIENT01");
        assertThat(leaf.resolve("req.json")).exists();
    }

    // -------------------------------------------------------------------------
    // multi-clients : pas de collision entre deux clientIds
    // -------------------------------------------------------------------------

    @Test
    void archiveJob_deuxClients_archivésSéparément() throws IOException {
        Path archiveBaseDir = Files.createDirectory(tempDir.resolve("archive"));
        Path processing = Files.createDirectory(tempDir.resolve("processing"));
        Path outbox = Files.createDirectory(tempDir.resolve("outbox"));

        Path inputA  = processing.resolve("reqA.json");
        Path outputA = outbox.resolve("reqA_RESULT.json");
        Path inputB  = processing.resolve("reqB.json");
        Path outputB = outbox.resolve("reqB_RESULT.json");

        Files.writeString(inputA, "{\"client\":\"A\"}");
        Files.writeString(outputA, "{\"status\":\"SUCCESS\",\"client\":\"A\"}");
        Files.writeString(inputB, "{\"client\":\"B\"}");
        Files.writeString(outputB, "{\"status\":\"SUCCESS\",\"client\":\"B\"}");

        archiveService.archiveJob(inputA, outputA, archiveBaseDir, "CLIENT_A");
        archiveService.archiveJob(inputB, outputB, archiveBaseDir, "CLIENT_B");

        Path leafA = archiveLeaf("CLIENT_A");
        Path leafB = archiveLeaf("CLIENT_B");

        assertThat(leafA.resolve("reqA.json")).exists();
        assertThat(leafA.resolve("reqA_RESULT.json")).exists();
        assertThat(leafB.resolve("reqB.json")).exists();
        assertThat(leafB.resolve("reqB_RESULT.json")).exists();

        // pas de fuite entre clients
        assertThat(leafA.resolve("reqB.json")).doesNotExist();
        assertThat(leafB.resolve("reqA.json")).doesNotExist();
    }
}
