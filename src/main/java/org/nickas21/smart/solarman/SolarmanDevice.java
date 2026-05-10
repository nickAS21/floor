package org.nickas21.smart.solarman;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolarmanDevice {
    private String loggerSn;
    private String inverterSn;
    private Long inverterId;
    private Long loggerId;
}
