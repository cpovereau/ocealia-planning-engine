package fr.project.planning.fileadapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.project.planning.fileadapter.config.FileAdapterProperties;
import fr.project.planning.fileadapter.model.FileProcessingResult;
import fr.project.planning.fileadapter.model.FileProcessingStatus;
import fr.project.planning.fileadapter.scenario.FileScenarioDispatcher;
import fr.project.planning.fileadapter.scenario.Sc01FileScenarioExecutionFacade;
import fr.project.planning.fileadapter.service.FileAdapterService;
import fr.project.planning.fileadapter.service.FileArchiveService;
import fr.project.planning.fileadapter.service.FileClaimService;
import fr.project.planning.fileadapter.service.FileErrorWriter;
import fr.project.planning.fileadapter.service.FileInputValidator;
import fr.project.planning.fileadapter.service.FileInboxScanner;
import fr.project.planning.fileadapter.service.FileProcessingLogger;
import fr.project.planning.fileadapter.service.FileRequestReader;
import fr.project.planning.fileadapter.service.FileResponseWriter;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc01ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration end-to-end du pipeline FileAdapter.
 *
 * Stratégie : tous les composants de transport (scanner, claim, validation,
 * reader, writer, error, archive) sont réels. Seul ScenarioSc01ExecutionService
 * est mocké car il nécessite un contexte OptaPlanner complet.
 *
 * On appelle uniquement fileAdapterService.processBatch() et on vérifie
 * l'état du système de fichiers après traitement.
 */
class FileAdapterEndToEndTest {

    @TempDir
    Path tempDir;

    private Path inbox;
    private Path processing;
    private Path outbox;
    private Path error;
    private Path archive;

    private FileAdapterService fileAdapterService;

    // ObjectMapper aligné sur le comportement Spring Boot :
    // - support des types date/heure Java (LocalDate, LocalTime, Instant)
    // - FAIL_ON_UNKNOWN_PROPERTIES=false (Spring Boot désactive cette option par défaut,
    //   ce qui permet aux façades de recevoir le payload complet sans connaître les champs
    //   d'enveloppe requestId/metadata)
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @BeforeEach
    void setUp() throws IOException {
        inbox      = Files.createDirectory(tempDir.resolve("inbox"));
        processing = Files.createDirectory(tempDir.resolve("processing"));
        outbox     = Files.createDirectory(tempDir.resolve("outbox"));
        error      = Files.createDirectory(tempDir.resolve("error"));
        archive    = Files.createDirectory(tempDir.resolve("archive"));

        FileAdapterProperties props = new FileAdapterProperties();
        props.getPaths().setInbox(inbox.toString());
        props.getPaths().setProcessing(processing.toString());
        props.getPaths().setOutbox(outbox.toString());
        props.getPaths().setError(error.toString());
        props.getPaths().setArchive(archive.toString());

        // Seul le service solveur est mocké — il nécessite un contexte OptaPlanner complet.
        // Tout le reste du pipeline (validation, lecture, écriture, archivage) est réel.
        ScenarioSc01ExecutionService sc01Service = mock(ScenarioSc01ExecutionService.class);
        when(sc01Service.solve(any())).thenReturn(new ScenarioResponseDTO());

        Sc01FileScenarioExecutionFacade sc01Facade =
                new Sc01FileScenarioExecutionFacade(sc01Service, objectMapper);

        fileAdapterService = new FileAdapterService(
                props,
                new FileInboxScanner(props),
                new FileClaimService(props),
                new FileInputValidator(props),
                new FileRequestReader(objectMapper),
                new FileScenarioDispatcher(List.of(sc01Facade)),
                new FileResponseWriter(objectMapper),
                new FileErrorWriter(objectMapper),
                new FileArchiveService(),
                new FileProcessingLogger()
        );
    }

    // -------------------------------------------------------------------------
    // Cas nominal — fichier valide SC-01
    // -------------------------------------------------------------------------

    @Test
    void processBatch_fichierValide_produitSuccessEtArchive() throws IOException {
        Files.writeString(inbox.resolve("requete-001.json"), payloadSc01Valide("REQ-001"));

        List<FileProcessingResult> results = fileAdapterService.processBatch();

        // résultat
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FileProcessingStatus.SUCCESS);
        assertThat(results.get(0).getJobId()).isNotBlank();
        assertThat(results.get(0).getSourceFileName()).isEqualTo("requete-001.json");

        // inbox et processing vidés
        assertThat(Files.list(inbox).toList()).isEmpty();
        assertThat(Files.list(processing).toList()).isEmpty();

        // outbox contient le fichier résultat nommé <input>_RESULT.json
        List<Path> outboxFiles = Files.list(outbox).toList();
        assertThat(outboxFiles).hasSize(1);
        assertThat(outboxFiles.get(0).getFileName()).hasToString("requete-001_RESULT.json");

        // archive : structure YYYY/MM/clientId/ avec input + output
        Path archiveLeaf = archiveLeaf("CLIENT-A");
        assertThat(Files.list(archiveLeaf).map(Path::getFileName).map(Path::toString).toList())
                .containsExactlyInAnyOrder("requete-001.json", "requete-001_RESULT.json");

        // error reste vide
        assertThat(Files.list(error).toList()).isEmpty();

        // contenu JSON de l'outbox : enveloppe FileSuccessResponse
        JsonNode outboxJson = objectMapper.readTree(outboxFiles.get(0).toFile());
        assertThat(outboxJson.get("requestId").asText()).isEqualTo("REQ-001");
        assertThat(outboxJson.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(outboxJson.has("response")).isTrue();
    }

    // -------------------------------------------------------------------------
    // Cas d'erreur — requestId manquant
    // -------------------------------------------------------------------------

    @Test
    void processBatch_requestIdManquant_produitErrorEtArchive() throws IOException {
        Files.writeString(inbox.resolve("invalide-001.json"), payloadSansRequestId());

        List<FileProcessingResult> results = fileAdapterService.processBatch();

        // résultat
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FileProcessingStatus.ERROR);
        assertThat(results.get(0).getErrorMessage()).containsIgnoringCase("requestId");

        // inbox et processing vidés
        assertThat(Files.list(inbox).toList()).isEmpty();
        assertThat(Files.list(processing).toList()).isEmpty();

        // error contient le fichier résultat
        List<Path> errorFiles = Files.list(error).toList();
        assertThat(errorFiles).hasSize(1);
        assertThat(errorFiles.get(0).getFileName()).hasToString("invalide-001_RESULT.json");

        // archive : structure YYYY/MM/clientId/ avec input + error
        Path archiveLeaf = archiveLeaf("CLIENT-A");
        assertThat(Files.list(archiveLeaf).map(Path::getFileName).map(Path::toString).toList())
                .containsExactlyInAnyOrder("invalide-001.json", "invalide-001_RESULT.json");

        // outbox reste vide
        assertThat(Files.list(outbox).toList()).isEmpty();

        // contenu JSON du fichier error : format §13.5
        JsonNode errorJson = objectMapper.readTree(errorFiles.get(0).toFile());
        assertThat(errorJson.get("status").asText()).isEqualTo("ERROR");
        assertThat(errorJson.get("error").get("code").asText()).isEqualTo("MISSING_FIELD");
        assertThat(errorJson.get("error").get("message").asText()).containsIgnoringCase("requestId");
    }

    // -------------------------------------------------------------------------
    // Cas d'erreur — inbox vide
    // -------------------------------------------------------------------------

    @Test
    void processBatch_inboxVide_retourneListeVide() {
        List<FileProcessingResult> results = fileAdapterService.processBatch();

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /** Retourne le répertoire feuille archive/YYYY/MM/clientId/ pour la date courante. */
    private Path archiveLeaf(String clientId) {
        LocalDate today = LocalDate.now();
        return archive
                .resolve(String.format("%04d", today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(clientId);
    }

    private String payloadSc01Valide(String requestId) {
        return """
                {
                  "requestId": "%s",
                  "scenarioType": "SC-01",
                  "metadata": {
                    "clientId": "CLIENT-A",
                    "timestamp": "2026-03-30T10:00:00Z"
                  },
                  "planningContext": {
                    "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
                    "strategieScoring": "EXPLOITATION"
                  },
                  "scenarioParameters": {
                    "resourceRef": { "kind": "SALARIE", "id": "1041" },
                    "dailyAmplitudeHours": 8.0,
                    "shiftStart": "08:00",
                    "shiftEndAlert": "17:00",
                    "lunchBreak": { "start": "12:00", "end": "13:00" },
                    "workedDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
                    "holidayDates": []
                  },
                  "dataSet": {
                    "ressources": {
                      "salaries": [
                        {
                          "id": "1041",
                          "type": "SALARIE",
                          "capaciteCible": 2400,
                          "activitesAutorisees": ["TRAVAIL"],
                          "lieuxAutorises": ["CASTRES"],
                          "postesComptablesCompatibles": ["PC-100"]
                        }
                      ],
                      "postesVirtuels": []
                    },
                    "creneaux": [],
                    "referentiels": {
                      "activites": [
                        {
                          "codeActiviteId": "travail",
                          "compteDansCharge": true,
                          "genereDetteRepos": false,
                          "estServiceCritique": false
                        }
                      ]
                    }
                  }
                }
                """.formatted(requestId);
    }

    private String payloadSansRequestId() {
        return """
                {
                  "scenarioType": "SC-01",
                  "metadata": {
                    "clientId": "CLIENT-A",
                    "timestamp": "2026-03-30T10:00:00Z"
                  }
                }
                """;
    }
}
