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
}
