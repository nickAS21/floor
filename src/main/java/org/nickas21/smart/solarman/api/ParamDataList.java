package org.nickas21.smart.solarman.api;

import lombok.Data;

import java.util.List;

@Data
public class ParamDataList {
    String collectTime;
    List<RealTimeDataValue> dataList;

}

