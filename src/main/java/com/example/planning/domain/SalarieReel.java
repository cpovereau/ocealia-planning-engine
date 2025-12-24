package com.example.planning.domain;

public final class SalarieReel implements Ressource {

    private final String id;

    public SalarieReel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
