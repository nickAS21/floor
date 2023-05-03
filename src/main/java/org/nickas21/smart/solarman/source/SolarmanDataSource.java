package org.nickas21.smart.solarman.source;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.nickas21.smart.solarman.constant.SolarmanRegion;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SolarmanDataSource {
    String name;
    SolarmanRegion region;
    String appId;
    String secret;
    String userName;
    String passHash;
    Long stationId;
    String inverterSn;
    Long inverterId;
    String loggerSn;
    Long loggerId;
    String passWord;
    Long timeOutSec;
    double bmsSocMin;
    double bmsSocMax;
    double bmsSocAlarmWarn;
    double bmsSocAlarmError;
    double bmsSocStepValueChange;
}

