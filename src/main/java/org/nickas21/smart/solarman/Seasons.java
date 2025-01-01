package org.nickas21.smart.solarman;

import lombok.Getter;

public enum Seasons {
    WINTER("Winter", 1),
    SPRING("Spring", 2),
    SUMMER("Summer", 3),
    AUTUMN("Autumn", 4);

    @Getter
    private final String type;
    @Getter
    private final int seasonsId;

    Seasons(String type, int seasonId) {
        this.type = type;
        this.seasonsId = seasonId;
    }

    public static Seasons fromType(String type) {
        for (Seasons to : Seasons.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}
