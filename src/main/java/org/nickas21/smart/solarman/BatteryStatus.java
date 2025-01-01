package org.nickas21.smart.solarman;

import lombok.Getter;

public enum BatteryStatus {
    CHARGING("Charging", 95.00),
    STATIC("Static", 100.00),
    DISCHARGING("Discharging", 20.00);

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
