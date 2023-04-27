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
public class SolarmanMqttDataSource {
    String name;
    SolarmanRegion region;
    String appId;
    String secret;
    String userName;
    String passHash;
    String stationId;
    String inverterId;
    String loggerId;
    int mqttPort;
    String topic;
    String mqttUsername;
    String passWord;
}

