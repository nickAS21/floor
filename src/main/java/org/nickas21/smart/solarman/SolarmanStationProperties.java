package org.nickas21.smart.solarman;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("smart.solarman")
public class SolarmanStationProperties {
    private Long timeoutSec;
    private double batSocMinMin;
    private double batSocMinMax;
    private double batSocMax;
    private double batSocAlarmWarn;
    private double batSocAlarmError;
    private double stationConsumptionPower;
}
