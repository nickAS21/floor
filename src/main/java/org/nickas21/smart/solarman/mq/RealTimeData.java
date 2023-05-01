package org.nickas21.smart.solarman.mq;

import lombok.Data;

import java.util.List;

@Data
public class RealTimeData {
    Integer code;
    String msg;
    boolean success;
    String requestId;
    String deviceSn;
    Long deviceId;
    String deviceType;      //": "INVERTER",
    Integer deviceState;    //: 1,
    Long collectionTime;
    List<RealTimeDataValue> dataList;
}

