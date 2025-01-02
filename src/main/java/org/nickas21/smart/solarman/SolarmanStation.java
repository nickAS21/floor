package org.nickas21.smart.solarman;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolarmanStation {
    private String name;
    private Long stationId;
    private String inverterSn;
    private Long inverterId;
    private Long loggerId;
    @Builder.Default
    private Long timeoutSec = 120L;
    @Builder.Default
    private double batSocMinMin = 50.0;
    @Builder.Default
    private double batSocMinMax = 80.0;
    @Builder.Default
    private double batSocMax = 97.0;
    @Builder.Default
    private double batSocAlarmWarn = 70.0;
    @Builder.Default
    private double batSocAlarmError = 40.0;
    @Builder.Default
    private double stationConsumptionPower = 50.0;
    @Builder.Default
    private int dopPowerToMax = 2000;
    @Builder.Default
    private int dopPowerToMin = 800;
    @Builder.Default
    private int seasonsId = 3;  // Summer - 3, Winter - 1
    private double locationLat;   //": 50.31023634165624,
    private double locationLng;
}
