package org.nickas21.smart.data.dataEntityDto;

import lombok.Getter;

@Getter
public enum PowerType {
    GRID("grid"),
    SOLAR("solar"),
    BATTERY("battery");

    private final String description;

    PowerType(String description) { this.description = description; }

    @com.fasterxml.jackson.annotation.JsonValue
    public String getDescription() {
        return description;
    }

    public static PowerType getByName(String name) {
        if (name == null) return null;
        for (PowerType type : PowerType.values()) {
            if (type.description.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
