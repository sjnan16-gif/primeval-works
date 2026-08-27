package com.primevalworks.world.work;

public enum DinosaurCommandMode {
    HOME("Home", "Returns to the Command Table and resumes its saved base assignment."),
    STAY("Stay", "Pauses work and holds the area where you left it."),
    FOLLOW("Follow", "Travels with you, defends you, and can receive Dino Whistle field orders.");

    private final String title;
    private final String description;

    DinosaurCommandMode(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public static DinosaurCommandMode byId(int id) {
        return values()[Math.max(0, Math.min(values().length - 1, id))];
    }
}
