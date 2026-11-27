package dev.rebase.viewgrid.module;

public enum Category {
    VISUALS("Visuals"),
    WORLD("World"),
    CAMERA("Camera");

    private final String title;

    Category(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
