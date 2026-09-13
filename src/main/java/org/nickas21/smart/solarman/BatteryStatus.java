package org.nickas21.smart.solarman;

import lombok.Getter;

public enum BatteryStatus {
    STATIC("Static", 98.00),
    NOT_CHARGING_DAY_MORE_70("Not Charging Golego after ALARM", 70.00),            // if more is not charging at day after Alarm Golego
    CHARGING_60("Charging", 60.00),            // if more is not charging at night
    MIN_DISCHARGING_DAY_45("Min Discharging normal", 45.00),            // if more is not charging at night
    DISCHARGING("Discharging", 40.00),      //  if less is charging at night and winter
    ALARM("Alarm", 25.00);                  //  if less is charging all

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
