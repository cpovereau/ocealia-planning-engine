package fr.project.planning.fileadapter.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.project.planning.fileadapter.config.FileAdapterProperties;
import fr.project.planning.fileadapter.model.FileJobContext;
import fr.project.planning.fileadapter.model.FileJobPaths;
import fr.project.planning.fileadapter.model.FileProcessingResult;
import fr.project.planning.fileadapter.model.FileProcessingStatus;
import fr.project.planning.fileadapter.model.FileSuccessResponse;
import fr.project.planning.fileadapter.scenario.FileScenarioDispatcher;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Orchestre le pipeline complet de traitement d'un batch de fichiers.
 *
 * Pipeline par fichier :
 * inbox scan → claim → validate meta → read → validate payload → dispatch → write success/error → archive
 *
 * Ce service ne contient aucune logique métier de solveur.
 * Il délègue intégralement au FileScenarioDispatcher, qui lui-même
 * délègue aux services métier existants (SC-01, SC-03).
 */
@Service
public class FileAdapterService {

    private static final Logger log = LoggerFactory.getLogger(FileAdapterService.class);

    private final FileAdapterProperties properties;
    private final FileInboxScanner inboxScanner;
    private final FileClaimService claimService;
    private final FileInputValidator inputValidator;
    private final FileRequestReader requestReader;
    private final FileScenarioDispatcher scenarioDispatcher;
    private final FileResponseWriter responseWriter;
    private final FileErrorWriter errorWriter;
    private final FileArchiveService archiveService;
    private final FileProcessingLogger processingLogger;

    public FileAdapterService(FileAdapterProperties properties,
                               FileInboxScanner inboxScanner,
                               FileClaimService claimService,
                               FileInputValidator inputValidator,
                               FileRequestReader requestReader,
                               FileScenarioDispatcher scenarioDispatcher,
                               FileResponseWriter responseWriter,
                               FileErrorWriter errorWriter,
                               FileArchiveService archiveService,
                               FileProcessingLogger processingLogger) {
        this.properties = properties;
        this.inboxScanner = inboxScanner;
        this.claimService = claimService;
        this.inputValidator = inputValidator;
        this.requestReader = requestReader;
        this.scenarioDispatcher = scenarioDispatcher;
        this.responseWriter = responseWriter;
        this.errorWriter = errorWriter;
        this.archiveService = archiveService;
        this.processingLogger = processingLogger;
    }

    /**
     * Exécute un batch : scan de l'inbox puis traitement de chaque fichier disponible.
     *
     * @return liste des résultats, un par fichier tenté
     */
    public List<FileProcessingResult> processBatch() {
        List<Path> inboxFiles = inboxScanner.scanInbox();
        if (inboxFiles.isEmpty()) {
            log.debug("[FileAdapter] Inbox vide, rien à traiter.");
            return List.of();
        }

        log.info("[FileAdapter] {} fichier(s) détecté(s) dans l'inbox.", inboxFiles.size());
        return inboxFiles.stream()
                .map(this::processOneFile)
                .toList();
    }

    // --- private ---

    private FileProcessingResult processOneFile(Path inboxFile) {
        return claimService.claim(inboxFile)
                .map(this::executeJob)
                .orElseGet(() -> FileProcessingResult.builder()
                        .status(FileProcessingStatus.ERROR)
                        .jobId("unknown")
                        .sourceFileName(inboxFile.getFileName().toString())
                        .processingTime(Duration.ZERO)
                        .errorMessage("Fichier non revendiqué (conflit concurrent ou fichier disparu)")
                        .build());
    }

    private FileProcessingResult executeJob(FileJobContext context) {
        processingLogger.logStart(context.getJobId(), context.getSourceFileName());
        FileJobPaths jobPaths = resolveJobPaths(context);
        Path archiveBaseDir = Path.of(properties.getPaths().getArchive());

        // requestId et clientId extraits dès que le JSON est lisible, pour traçabilité §14.9
        String requestId = null;
        String clientId = null;

        try {
            inputValidator.validateFileMeta(context.getProcessingFile());
            JsonNode payload = requestReader.read(context.getProcessingFile());
            requestId = extractRequestId(payload);
            clientId = extractClientId(payload);
            inputValidator.validatePayload(payload);
            if (log.isInfoEnabled()) {
                log.info("[FileAdapter] requestId={} scenarioType={} clientId={} fichier={}",
                        requestId != null ? requestId : "-",
                        extractField(payload, "scenarioType"),
                        clientId != null ? clientId : "-",
                        context.getSourceFileName());
            }

            ScenarioResponseDTO response = scenarioDispatcher.dispatch(payload);
            responseWriter.write(new FileSuccessResponse(requestId, response), jobPaths.getOutboxFile());
            archiveService.archiveJob(context.getProcessingFile(), jobPaths.getOutboxFile(),
                    archiveBaseDir, clientId);

            FileProcessingResult result = FileProcessingResult.builder()
                    .status(FileProcessingStatus.SUCCESS)
                    .jobId(context.getJobId())
                    .sourceFileName(context.getSourceFileName())
                    .processingTime(elapsed(context.getStartTime()))
                    .build();

            processingLogger.logSuccess(result);
            return result;

        } catch (Exception e) {
            errorWriter.write(requestId, context.getSourceFileName(), e, jobPaths.getErrorFile());
            archiveService.archiveJob(context.getProcessingFile(), jobPaths.getErrorFile(),
                    archiveBaseDir, clientId);

            FileProcessingResult result = FileProcessingResult.builder()
                    .status(FileProcessingStatus.ERROR)
                    .jobId(context.getJobId())
                    .sourceFileName(context.getSourceFileName())
                    .processingTime(elapsed(context.getStartTime()))
                    .errorMessage(e.getMessage())
                    .build();

            processingLogger.logError(result);
            return result;
        }
    }

    /**
     * Reconstruit les chemins de sortie selon le contrat §13.5 :
     * outbox et error → <nom_input>_RESULT.json
     */
    private FileJobPaths resolveJobPaths(FileJobContext context) {
        String baseName = context.getSourceFileName();
        String resultName = baseName.endsWith(".json")
                ? baseName.substring(0, baseName.length() - 5) + "_RESULT.json"
                : baseName + "_RESULT.json";

        return new FileJobPaths(
                Path.of(properties.getPaths().getOutbox()).resolve(resultName),
                Path.of(properties.getPaths().getError()).resolve(resultName)
        );
    }

    /** Extrait le requestId de la racine du payload JSON, null si absent. */
    private String extractRequestId(JsonNode payload) {
        return extractField(payload, "requestId");
    }

    /** Extrait le clientId depuis metadata.clientId, null si absent. */
    private String extractClientId(JsonNode payload) {
        if (payload == null) return null;
        JsonNode metadata = payload.get("metadata");
        if (metadata == null || metadata.isNull()) return null;
        JsonNode clientIdNode = metadata.get("clientId");
        return (clientIdNode != null && !clientIdNode.isNull()) ? clientIdNode.asText() : null;
    }

    private String extractField(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    private Duration elapsed(Instant start) {
        return Duration.between(start, Instant.now());
    }
}
