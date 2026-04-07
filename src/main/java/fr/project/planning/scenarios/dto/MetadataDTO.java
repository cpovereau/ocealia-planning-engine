package fr.project.planning.scenarios.dto;

import jakarta.validation.constraints.NotBlank;

public class MetadataDTO {

    @NotBlank(message = "metadata.clientId est obligatoire")
    private String clientId;

    @NotBlank(message = "metadata.timestamp est obligatoire")
    private String timestamp;

    private String source;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
