package org.nickas21.smart.solarman;

import lombok.Getter;

public enum BatteryStatus {
    CHARGING("Charging", 60.00),        // if more is not charging at night
    STATIC("Static", 98.00),
    DISCHARGING("Discharging", 50.00),      //  if less is charging at night and winter
    ALARM("Alarm", 30.00);                  //  if less is charging all

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
