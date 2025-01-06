package org.nickas21.smart.solarman;

import lombok.Getter;

public enum BatteryStatus {
    CHARGING("Charging", 85.00),
    STATIC("Static", 100.00),
    DISCHARGING("Discharging", 50.00);

    @Getter
    private final String type;
    @Getter
    private final double soc;

    BatteryStatus(String type, double soc) {
        this.type = type;
        this.soc = soc;
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
