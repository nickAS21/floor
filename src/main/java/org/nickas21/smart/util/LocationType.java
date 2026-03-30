package org.nickas21.smart.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

    // Додаємо анотацію, щоб Jackson знав, як перетворювати String -> Enum
    @JsonCreator
    public static LocationType getByName(String name) {
        if (name == null) return null;
        for (LocationType type : LocationType.values()) {
            // Це дозволить розуміти і "DACHA", і "dacha", і навіть "Dacha"
            if (type.nameForFile.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    // (Опціонально) Якщо хочеш, щоб у JSON завжди писалося гарне ім'я "Dacha" замість "DACHA"
    @JsonValue
    public String getNameForFile() {
        return nameForFile;
    }
}