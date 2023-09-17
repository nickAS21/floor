package org.nickas21.smart.solarman.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Communication {
    String deviceSn;
    Long deviceId;
    String parentSn;
    String deviceType;  // COLLECTOR "INVERTER"
    Long deviceState; //":1,
    Long updateTime; //":1682931742,
    String timeZone; //":"EET",
    List<Communication> childList;
}
