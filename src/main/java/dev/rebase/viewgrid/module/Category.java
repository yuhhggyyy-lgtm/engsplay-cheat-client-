package dev.rebase.viewgrid.module;

public enum Category {
    VISUALS("Visuals"),
    WORLD("World"),
    CAMERA("Camera");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
