package fr.project.planning.scenarios.dto.request;

public class ResourceRefDTO {

    private ResourceKind kind;
    private String id;

    public ResourceKind getKind() {
        return kind;
    }

    public void setKind(ResourceKind kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}