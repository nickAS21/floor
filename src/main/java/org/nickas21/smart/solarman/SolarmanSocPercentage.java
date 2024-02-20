package org.nickas21.smart.solarman;

import lombok.Getter;

public enum SolarmanSocPercentage {
    /**
     * I use in volts:
     *     100% (55,300)
     *     90% (53,600) - connection of heaters
     *     80% (53,100) - disconnection of heaters
     *     60% (52,300) - disconnection of the Grid (in case of bad weather in the period from 23:00 to 07:00)
     *     30% (51,500) - disconnection of the Grid (summer or sunny weather)
     * External network connection:
     *     25% (51,300) regardless of time
     *     if current Time == 01:00 Discharge depth: 50% (52,200);
     */

    PERCENTAGE_00(00.00, 40.0,"Percentage_00"),
    PERCENTAGE_05(05.00, 44.0,"Percentage_05"),
    PERCENTAGE_10(10.00, 48.0,"Percentage_10"),
    PERCENTAGE_15(15.00, 49.6,"Percentage_15"),
    PERCENTAGE_20(20.00, 51.2,"Percentage_20"),
    PERCENTAGE_25(25.00, 51.35,"Percentage_25"),
    PERCENTAGE_30(30.00, 51.5,"Percentage_30"),
    PERCENTAGE_35(35.00, 51.75,"Percentage_35"),
    PERCENTAGE_40(40.00, 52.0,"Percentage_40"),
    PERCENTAGE_45(45.00, 52.1,"Percentage_45"),
    PERCENTAGE_50(50.00, 52.2,"Percentage_50"),
    PERCENTAGE_55(55.00, 52.25,"Percentage_55"),
    PERCENTAGE_60(60.00, 52.3,"Percentage_60"),
    PERCENTAGE_65(65.00, 52.55,"Percentage_65"),
    PERCENTAGE_70(70.00, 52.8,"Percentage_70"),
    PERCENTAGE_75(75.00, 52.95,"Percentage_75"),
    PERCENTAGE_80(80.00, 53.1,"Percentage_80"),
    PERCENTAGE_85(85.00, 53.35,"Percentage_85"),
    PERCENTAGE_90(90.00, 53.6,"Percentage_90"),
    PERCENTAGE_95(95.00, 54.0,"Percentage_95"),
    REST_FLOAT(100.00, 54.4, "Rest_Float"),
    CHARGE_MAX(100.00, 55.6, "Charge_Max");

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

    public static SolarmanSocPercentage fromPercentage(float voltage) {
        for (SolarmanSocPercentage to : SolarmanSocPercentage.values()) {
            if (to.voltage == voltage) {
                return to;
            }
        }
        return null;
    }
    public static SolarmanSocPercentage fromVoltage(float percentage) {
        for (SolarmanSocPercentage to : SolarmanSocPercentage.values()) {
            if (to.percentage == percentage) {
                return to;
            }
        }
        return null;
    }
}
