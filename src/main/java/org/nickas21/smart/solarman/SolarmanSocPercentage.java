package org.nickas21.smart.solarman;

import lombok.Getter;

public enum SolarmanSocPercentage {
    /**
     * I use in volts:
     *     100% (55,600)
     *     90% (53,600) - connection of heaters (All)
     *     60% (52,400) - disconnection of heaters (All)
     *     60% (52,400) - disconnection of the Grid (in case of bad weather in the period from 23:00 to 07:00)
     *     30% (51,600) - disconnection of the Grid (summer or sunny weather)
     * External network connection:
     *     25% (51,400) regardless of time
     *     if current Time == 01:00 Discharge depth: 50% (52,200);
     */
    CHARGE_MAX(100.00, 55.6, "Charge_Max"),
    REST_FLOAT(100.00, 54.4, "Rest_Float"),
    PERCENTAGE_99(99.00, 53.50,"Percentage_99"),
    PERCENTAGE_95(95.00, 53.15,"Percentage_95"),
    PERCENTAGE_90(90.00, 53.00,"Percentage_90"),
    PERCENTAGE_85(85.00, 52.45,"Percentage_85"),
    PERCENTAGE_80(80.00, 52.40,"Percentage_80"),
    PERCENTAGE_75(75.00, 52.35,"Percentage_75"),
    PERCENTAGE_70(70.00, 52.30,"Percentage_70"),
    PERCENTAGE_65(65.00, 52.25,"Percentage_65"),
    PERCENTAGE_60(60.00, 52.20,"Percentage_60"),
    PERCENTAGE_50(55.00, 52.15,"Percentage_55"),
    PERCENTAGE_55(50.00, 52.10,"Percentage_50"),
    PERCENTAGE_45(45.00, 52.95,"Percentage_45"),
    PERCENTAGE_40(40.00, 52.85,"Percentage_40"),
    PERCENTAGE_35(35.00, 51.70,"Percentage_35"),
    PERCENTAGE_30(30.00, 51.55,"Percentage_30"),
    PERCENTAGE_25(25.00, 51.40,"Percentage_25"),
    PERCENTAGE_20(20.00, 51.20,"Percentage_20"),
    PERCENTAGE_15(15.00, 50.40,"Percentage_15"),
    PERCENTAGE_12(12.00, 49.00,"Percentage_12"),
    PERCENTAGE_10(10.00, 48.00,"Percentage_10"),
    PERCENTAGE_05(05.00, 44.00,"Percentage_05"),
    PERCENTAGE_00(00.00, 40.00,"Percentage_00");

    @Getter
    private final double percentage;
    @Getter
    private final double voltage;
    @Getter
    private final String type;


    SolarmanSocPercentage(double percentage, double voltage, String type) {
        this.percentage = percentage;
        this.voltage = voltage;
        this.type = type;
    }

    public static SolarmanSocPercentage fromType(String type) {
        for (SolarmanSocPercentage to : SolarmanSocPercentage.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }


    public static SolarmanSocPercentage fromPercentage(double voltage) {
        for (SolarmanSocPercentage to : SolarmanSocPercentage.values()) {
            if (to.voltage <= voltage) {
                return to;
            }
        }
        return null;
    }
    public static SolarmanSocPercentage fromVoltage(double percentage) {
        for (SolarmanSocPercentage to : SolarmanSocPercentage.values()) {
            if (to.percentage <= percentage) {
                return to;
            }
        }
        return null;
    }
}
