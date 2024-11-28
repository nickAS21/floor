package org.nickas21.smart.util;

import lombok.Getter;

import java.util.TreeMap;

public class SolarmanSocUtil {

    public static double restFloatVoltage = 54.6;
    public static double percentage_99 = 53.34;
    public static double percentage_97 = 53.32;
    public static double percentage_95 = 53.30;
    public static double percentage_90 = 53.26;
    public static double percentage_50 = 51.2;
    public static double percentage_20 = 49.6;
    public static double percentage_15 = 48.8;
    public static double percentage_10 = 48.0;

    private static final TreeMap<Integer, Double> percentageToVoltageMap = new TreeMap<>();
    static {
        percentageToVoltageMap.put(100, 54.6);
        percentageToVoltageMap.put(99, 53.34);
        percentageToVoltageMap.put(97, 53.32);
        percentageToVoltageMap.put(95, 53.30);
        percentageToVoltageMap.put(90, 53.28);
        percentageToVoltageMap.put(50, 51.2);
        percentageToVoltageMap.put(20, 49.6);
        percentageToVoltageMap.put(15, 48.8);
        percentageToVoltageMap.put(10, 48.0);
        percentageToVoltageMap.put(5, 47.0);
        percentageToVoltageMap.put(0, 46.5);
    }

    public static double getPercentageVoltage (double vCur) {
        // less 48
        double batSocNew = 9.00;
        // (100% → 99%)
        if (vCur >= percentage_99) {
            batSocNew =  99 + ((vCur - percentage_99) / (restFloatVoltage - percentage_99)) * (100 - 99);
        }

        // (99% → 97%)
        else if (vCur >= percentage_97) {
            batSocNew =  97 + ((vCur - percentage_97) / (percentage_99 - percentage_97)) * (99 - 97);
        }

        // (97% → 95%)
        else if (vCur >= percentage_95) {
            batSocNew =  95 + ((vCur - percentage_95) / (percentage_97 - percentage_95)) * (97 - 95);
        }
        // (95% → 90%)
        else if (vCur >= percentage_90) {
            batSocNew =  90 + ((vCur - percentage_90) / (percentage_95 - percentage_90)) * (95 - 90);
        }
        // (90% → 50%)
        else if (vCur >= percentage_50) {
            batSocNew =  50 + ((vCur - percentage_50) / (percentage_90 - percentage_50)) * (90 - 50);
        }

        // (50% → 20%)
        else if (vCur >= percentage_20) {
            batSocNew =  20 + ((vCur - percentage_20) / (percentage_50 - percentage_20)) * (50 - 20);
        }

        // (20% → 15%)
        else if (vCur >= percentage_15) {
            batSocNew =  15 + ((vCur - percentage_15) / (percentage_20 - percentage_15)) * (20 - 15);
        }

        // (15% → 10%)
        else if (vCur >= percentage_10) {
            batSocNew =  10 + ((vCur - percentage_10) / (percentage_15 - percentage_10)) * (15 - 10);
        }

        return Math.round(batSocNew * 100.00) / 100.00;
    }

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
        REST_FLOAT(100.00, 54.6, "Rest_Float"),
        PERCENTAGE_99(99.00, 53.33,"Percentage_99"),
        PERCENTAGE_98(97.00, 53.32,"Percentage_97"),
        PERCENTAGE_95(95.00, 53.31,"Percentage_95"),

        PERCENTAGE_90(90.00, 53.28,"Percentage_90"),
        PERCENTAGE_85(85.00, 53.02,"Percentage_85"),
        PERCENTAGE_80(80.00, 52.76,"Percentage_80"),
        PERCENTAGE_75(75.00, 52.50,"Percentage_75"),
        PERCENTAGE_70(70.00, 52.24,"Percentage_70"),
        PERCENTAGE_65(65.00, 51.98,"Percentage_65"),
        PERCENTAGE_60(60.00, 51.72,"Percentage_60"),
        PERCENTAGE_55(55.00, 51.46,"Percentage_55"),
        PERCENTAGE_50(50.00, 51.20,"Percentage_50"),

        PERCENTAGE_45(45.00, 50.93,"Percentage_45"),
        PERCENTAGE_40(40.00, 50.67,"Percentage_40"),
        PERCENTAGE_35(35.00, 50.40,"Percentage_35"),
        PERCENTAGE_30(30.00, 50.13,"Percentage_30"),
        PERCENTAGE_25(25.00, 49.87,"Percentage_25"),
        PERCENTAGE_20(20.00, 49.60,"Percentage_20"),

        PERCENTAGE_15(15.00, 48.80,"Percentage_15"),
        PERCENTAGE_12(12.00, 48.32,"Percentage_12"),

        PERCENTAGE_10(10.00, 48.00,"Percentage_10"),
        PERCENTAGE_05(05.00, 47.00,"Percentage_05"),
        PERCENTAGE_00(00.00, 46.50,"Percentage_00");

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

        public static SolarmanSocPercentage fromPercentage(double voltage) {
            for (SolarmanSocPercentage to : SolarmanSocPercentage.values()) {
                if (to.voltage <= voltage) {
                    return to;
                }
            }
            return null;
        }
    }

    public static void calculatePercentage () {
        for (int p = 100; p >= 0; p -= 5) {
            System.out.printf("Percentage: %d%% -> Voltage: %.2f V%n", p, getVoltageForPercentage(p));
        }
    }

    public static double getVoltageForPercentage(int percentage) {
        if (percentageToVoltageMap.containsKey(percentage)) {
            return percentageToVoltageMap.get(percentage);
        }
        Integer lowerKey = percentageToVoltageMap.floorKey(percentage);
        Integer upperKey = percentageToVoltageMap.ceilingKey(percentage);

        if (lowerKey == null || upperKey == null) {
            throw new IllegalArgumentException("Percentage out of range");
        }

        double V1 = percentageToVoltageMap.get(lowerKey);
        double V2 = percentageToVoltageMap.get(upperKey);
        return V1 + (V2 - V1) * (percentage - lowerKey) / (upperKey - lowerKey);
    }
}
