package org.nickas21.smart.solarman.mq;

import lombok.Data;

import java.util.List;

@Data
public class Communication {
    String deviceSn;
    Long deviceId;
    String parentSn;
    String deviceType;  // COLLECTOR "INVERTER"
    Long deviceState; //":1,
    Long updateTime; //":1682931742,
    String timeZone; //":"EET",
    List <Communication> childList;
}
