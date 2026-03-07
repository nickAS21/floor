package org.nickas21.smart.util;

import lombok.Getter;

import java.time.ZoneId;

@Getter
public enum LocationType {
    DACHA("Dacha", "Country house in Kyiv region", ZoneId.of("Europe/Kyiv")),
    GOLEGO("Golego", "The flat in Kyiv", ZoneId.of("Europe/Kyiv"));

    private final String nameForFile;
    private final String description;
    private final ZoneId zoneId;

    LocationType(String nameForFile, String description, ZoneId zoneId) {
        this.nameForFile = nameForFile;
        this.description = description;
        this.zoneId = zoneId;
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