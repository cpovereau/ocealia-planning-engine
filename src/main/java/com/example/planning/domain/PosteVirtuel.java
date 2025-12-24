package com.example.planning.domain;

public final class PosteVirtuel implements Ressource {

    private final String id;

    public PosteVirtuel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
