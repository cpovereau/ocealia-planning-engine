package fr.project.planning.fileadapter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Lit un fichier JSON depuis le répertoire processing/ et le retourne sous forme de JsonNode.
 *
 * La désérialisation vers le DTO métier est déléguée au FileScenarioDispatcher,
 * qui connaît le type cible selon le scenarioType.
 */
@Service
public class FileRequestReader {

    private final ObjectMapper objectMapper;

    public FileRequestReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Lit et parse le fichier JSON donné.
     *
     * @param file chemin vers le fichier JSON (dans processing/)
     * @return le contenu JSON sous forme de JsonNode
     * @throws IOException si le fichier est illisible ou n'est pas un JSON valide
     */
    public JsonNode read(Path file) throws IOException {
        return objectMapper.readTree(file.toFile());
    }
}
