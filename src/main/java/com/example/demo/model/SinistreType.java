package com.example.demo.model;

public enum SinistreType {
    ACCIDENT_AUTOMOBILE("Accident automobile"),
    DEGATS_EAU("Dégâts des eaux"),
    INCENDIE("Incendie"),
    VOL("Vol/Cambriolage"),
    CATASTROPHE_NATURELLE("Catastrophe naturelle"),
    BRIS_DE_GLACE("Bris de glace"),
    RESPONSABILITE_CIVILE("Responsabilité civile"),
    AUTRES("Autres");

    private final String description;

    SinistreType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
