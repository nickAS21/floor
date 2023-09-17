package org.nickas21.smart.solarman;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("smart.solarman")
public class SolarmanStationProperties {
    private Long timeoutSec = 600L;
    private double bmsSocMin = 87.0;
    private double bmsSocMax = 95.0;
    private double bmsSocAlarmWarn = 80.0;
    private double bmsSocAlarmError = 59.0;
}
