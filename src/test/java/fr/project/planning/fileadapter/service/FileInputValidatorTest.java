package fr.project.planning.fileadapter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.fileadapter.config.FileAdapterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileInputValidatorTest {

    @TempDir
    Path tempDir;

    private FileInputValidator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        FileAdapterProperties props = new FileAdapterProperties();
        props.setMaxInputFileSizeBytes(512); // 512 octets pour les tests
        validator = new FileInputValidator(props);
    }

    // -------------------------------------------------------------------------
    // validateFileMeta — extension
    // -------------------------------------------------------------------------

    @Test
    void validateFileMeta_rejette_extensionNonJson() throws IOException {
        Path fichier = tempDir.resolve("requete.xml");
        Files.writeString(fichier, "<data/>");

        assertThatThrownBy(() -> validator.validateFileMeta(fichier))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining(".xml")
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("INVALID_EXTENSION");
    }

    @Test
    void validateFileMeta_rejette_fichierSansExtension() throws IOException {
        Path fichier = tempDir.resolve("requete");
        Files.writeString(fichier, "{}");

        assertThatThrownBy(() -> validator.validateFileMeta(fichier))
                .isInstanceOf(FileInputValidationException.class)
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("INVALID_EXTENSION");
    }

    // -------------------------------------------------------------------------
    // validateFileMeta — taille
    // -------------------------------------------------------------------------

    @Test
    void validateFileMeta_rejette_fichierTropVolumineux() throws IOException {
        Path fichier = tempDir.resolve("gros.json");
        // 513 octets > limite de 512
        Files.writeString(fichier, "{" + "\"x\":\"" + "a".repeat(507) + "\"}");

        assertThatThrownBy(() -> validator.validateFileMeta(fichier))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining("maximum autorisé")
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void validateFileMeta_accepte_fichierDansLimite() throws IOException {
        Path fichier = tempDir.resolve("ok.json");
        Files.writeString(fichier, "{}"); // 2 octets < 512

        // Ne doit pas lever d'exception
        validator.validateFileMeta(fichier);
    }

    // -------------------------------------------------------------------------
    // validatePayload — structure JSON
    // -------------------------------------------------------------------------

    @Test
    void validatePayload_rejette_jsonTableau() throws IOException {
        JsonNode payload = objectMapper.readTree("[{\"x\":1}]");

        assertThatThrownBy(() -> validator.validatePayload(payload))
                .isInstanceOf(FileInputValidationException.class)
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("INVALID_JSON");
    }

    // -------------------------------------------------------------------------
    // validatePayload — champs obligatoires
    // -------------------------------------------------------------------------

    @Test
    void validatePayload_rejette_requestIdAbsent() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "scenarioType": "SC-01",
                  "metadata": { "clientId": "CLIENT01", "timestamp": "2026-03-30T10:00:00Z" }
                }
                """);

        assertThatThrownBy(() -> validator.validatePayload(payload))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining("requestId")
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("MISSING_FIELD");
    }

    @Test
    void validatePayload_rejette_scenarioTypeAbsent() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": "REQ001",
                  "metadata": { "clientId": "CLIENT01", "timestamp": "2026-03-30T10:00:00Z" }
                }
                """);

        assertThatThrownBy(() -> validator.validatePayload(payload))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining("scenarioType")
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("MISSING_FIELD");
    }

    @Test
    void validatePayload_rejette_metadataClientIdAbsent() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": "REQ001",
                  "scenarioType": "SC-01",
                  "metadata": { "timestamp": "2026-03-30T10:00:00Z" }
                }
                """);

        assertThatThrownBy(() -> validator.validatePayload(payload))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining("metadata.clientId")
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("MISSING_FIELD");
    }

    @Test
    void validatePayload_rejette_metadataTimestampAbsent() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": "REQ001",
                  "scenarioType": "SC-01",
                  "metadata": { "clientId": "CLIENT01" }
                }
                """);

        assertThatThrownBy(() -> validator.validatePayload(payload))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining("metadata.timestamp")
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("MISSING_FIELD");
    }

    @Test
    void validatePayload_rejette_metadataAbsente() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": "REQ001",
                  "scenarioType": "SC-01"
                }
                """);

        assertThatThrownBy(() -> validator.validatePayload(payload))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining("metadata.")
                .extracting(e -> ((FileInputValidationException) e).getCode())
                .isEqualTo("MISSING_FIELD");
    }

    @Test
    void validatePayload_rejette_champVide() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": "",
                  "scenarioType": "SC-01",
                  "metadata": { "clientId": "CLIENT01", "timestamp": "2026-03-30T10:00:00Z" }
                }
                """);

        assertThatThrownBy(() -> validator.validatePayload(payload))
                .isInstanceOf(FileInputValidationException.class)
                .hasMessageContaining("requestId");
    }

    // -------------------------------------------------------------------------
    // cas nominal
    // -------------------------------------------------------------------------

    @Test
    void validatePayload_accepte_payloadValide() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": "REQ001",
                  "scenarioType": "SC-01",
                  "metadata": { "clientId": "CLIENT01", "timestamp": "2026-03-30T10:00:00Z" }
                }
                """);

        // Ne doit pas lever d'exception
        validator.validatePayload(payload);
    }
}
