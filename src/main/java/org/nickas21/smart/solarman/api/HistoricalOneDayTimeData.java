package org.nickas21.smart.solarman.api;

import lombok.Data;

import java.util.List;

@Data
public class HistoricalOneDayTimeData {
    String code;
    Long deviceId;
    String deviceSn;
    String deviceType;      //": "INVERTER",
    List<ParamDataList> paramDataList;
    String msg;
    boolean success;
    String requestId;
    Integer timeType;
}

