package dev.engsplay.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUALS("Visuals"),
    PLAYER("Player"),
    WORLD("World"),
    HUD("HUD"),
    SKIN("Skin");

    private final String title;

    Category(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
