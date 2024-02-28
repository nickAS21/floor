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
    private Long timeoutSec = 600L;
    @Builder.Default
    private double batSocMin = 60.0;
    @Builder.Default
    private double batSocMax = 95.0;
    @Builder.Default
    private double batSocAlarmWarn = 70.0;
    @Builder.Default
    private double batSocAlarmError = 40.0;
    @Builder.Default
    private double stationConsumptionPower = 75.0;
    private double locationLat;   //": 50.31023634165624,
    private double locationLng;
}
