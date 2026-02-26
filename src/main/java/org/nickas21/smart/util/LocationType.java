package org.nickas21.smart.util;

import lombok.Getter;

@Getter
public enum LocationType {
    DACHA("Dacha", "Country house in Kyiv region"),
    GOLEGO("Golego", "The flat in Kyiv");

    private final String nameForFile;
    private final String description;

    LocationType(String nameForFile, String description) {
        this.nameForFile = nameForFile;
        this.description = description;
    }

    public static LocationType getByName(String name) {
        if (name == null) return null;
        for (LocationType type : LocationType.values()) {
            if (type.nameForFile.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}