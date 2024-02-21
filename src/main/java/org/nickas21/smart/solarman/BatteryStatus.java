package org.nickas21.smart.solarman;

import lombok.Getter;

public enum BatteryStatus {
    CHARGING("Charging"),
    STATIC("Static"),
    DISCHARGING("Discharging");

    @Getter
    private final String type;

    BatteryStatus(String type) {
        this.type = type;
    }

    public static BatteryStatus fromType(String type) {
        for (BatteryStatus to : BatteryStatus.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}
