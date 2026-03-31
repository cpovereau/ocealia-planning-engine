package fr.project.planning.fileadapter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.fileadapter.config.FileAdapterProperties;
import fr.project.planning.fileadapter.model.FileJobContext;
import fr.project.planning.fileadapter.model.FileProcessingResult;
import fr.project.planning.fileadapter.model.FileProcessingStatus;
import fr.project.planning.fileadapter.scenario.FileScenarioDispatcher;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileAdapterServiceTest {

    private final FileAdapterProperties properties = buildProperties();
    private final FileInboxScanner inboxScanner = mock(FileInboxScanner.class);
    private final FileClaimService claimService = mock(FileClaimService.class);
    private final FileInputValidator inputValidator = mock(FileInputValidator.class);
    private final FileRequestReader requestReader = mock(FileRequestReader.class);
    private final FileScenarioDispatcher dispatcher = mock(FileScenarioDispatcher.class);
    private final FileResponseWriter responseWriter = mock(FileResponseWriter.class);
    private final FileErrorWriter errorWriter = mock(FileErrorWriter.class);
    private final FileArchiveService archiveService = mock(FileArchiveService.class);
    private final FileProcessingLogger processingLogger = mock(FileProcessingLogger.class);

    private final FileAdapterService service = new FileAdapterService(
            properties, inboxScanner, claimService, inputValidator, requestReader,
            dispatcher, responseWriter, errorWriter, archiveService, processingLogger
    );

    @Test
    void processBatch_retourneListeVide_quandInboxEstVide() {
        when(inboxScanner.scanInbox()).thenReturn(List.of());

        List<FileProcessingResult> results = service.processBatch();

        assertThat(results).isEmpty();
        verifyNoInteractions(claimService);
    }

    @Test
    void processBatch_retourneSuccess_quandFichierTraiteCorrectement() throws Exception {
        Path fakeInboxFile = Path.of("inbox/requete.json");
        Path fakeProcessingFile = Path.of("processing/requete.json");
        FileJobContext context = new FileJobContext("job-1", "requete.json", fakeProcessingFile, Instant.now());

        when(inboxScanner.scanInbox()).thenReturn(List.of(fakeInboxFile));
        when(claimService.claim(fakeInboxFile)).thenReturn(Optional.of(context));
        JsonNode fakePayload = new ObjectMapper().readTree("{\"scenarioType\":\"SC-01\"}");
        when(requestReader.read(fakeProcessingFile)).thenReturn(fakePayload);
        when(dispatcher.dispatch(fakePayload)).thenReturn(new ScenarioResponseDTO());

        List<FileProcessingResult> results = service.processBatch();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FileProcessingStatus.SUCCESS);
        assertThat(results.get(0).getJobId()).isEqualTo("job-1");
        verify(responseWriter).write(any(), any());
        verify(archiveService).archiveJob(any(), any(), any(), any());
        verifyNoInteractions(errorWriter);
    }

    @Test
    void processBatch_retourneError_quandDispatchEchoue() throws Exception {
        Path fakeInboxFile = Path.of("inbox/invalide.json");
        Path fakeProcessingFile = Path.of("processing/invalide.json");
        FileJobContext context = new FileJobContext("job-2", "invalide.json", fakeProcessingFile, Instant.now());

        when(inboxScanner.scanInbox()).thenReturn(List.of(fakeInboxFile));
        when(claimService.claim(fakeInboxFile)).thenReturn(Optional.of(context));
        JsonNode fakePayload = new ObjectMapper().readTree("{\"scenarioType\":\"SC-99\"}");
        when(requestReader.read(fakeProcessingFile)).thenReturn(fakePayload);
        when(dispatcher.dispatch(fakePayload)).thenThrow(new IllegalArgumentException("Type inconnu SC-99"));

        List<FileProcessingResult> results = service.processBatch();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FileProcessingStatus.ERROR);
        assertThat(results.get(0).getErrorMessage()).contains("Type inconnu SC-99");
        verify(errorWriter).write(any(), any(), any(), any());
        verify(archiveService).archiveJob(any(), any(), any(), any());
        verifyNoInteractions(responseWriter);
    }

    @Test
    void processBatch_retourneError_quandClaimEchoue() {
        Path fakeInboxFile = Path.of("inbox/conflit.json");

        when(inboxScanner.scanInbox()).thenReturn(List.of(fakeInboxFile));
        when(claimService.claim(fakeInboxFile)).thenReturn(Optional.empty());

        List<FileProcessingResult> results = service.processBatch();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FileProcessingStatus.ERROR);
        assertThat(results.get(0).getSourceFileName()).isEqualTo("conflit.json");
        verifyNoInteractions(requestReader, dispatcher, responseWriter, errorWriter, archiveService);
    }

    @Test
    void processBatch_traitePlusieursFilesDUnCoup() throws Exception {
        Path file1 = Path.of("inbox/a.json");
        Path file2 = Path.of("inbox/b.json");
        Path proc1 = Path.of("processing/a.json");
        Path proc2 = Path.of("processing/b.json");

        when(inboxScanner.scanInbox()).thenReturn(List.of(file1, file2));
        when(claimService.claim(file1)).thenReturn(Optional.of(new FileJobContext("j1", "a.json", proc1, Instant.now())));
        when(claimService.claim(file2)).thenReturn(Optional.of(new FileJobContext("j2", "b.json", proc2, Instant.now())));

        JsonNode payload = new ObjectMapper().readTree("{\"scenarioType\":\"SC-01\"}");
        when(requestReader.read(any())).thenReturn(payload);
        when(dispatcher.dispatch(any())).thenReturn(new ScenarioResponseDTO());

        List<FileProcessingResult> results = service.processBatch();

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getStatus() == FileProcessingStatus.SUCCESS);
    }

    // --- helper ---

    private FileAdapterProperties buildProperties() {
        FileAdapterProperties props = new FileAdapterProperties();
        props.getPaths().setOutbox("data/file-adapter/outbox");
        props.getPaths().setError("data/file-adapter/error");
        props.getPaths().setArchive("data/file-adapter/archive");
        return props;
    }
}
