package org.nickas21.smart.solarman.mq;

import lombok.Data;

@Data
public class RealTimeDataValue {
    String key;
    String value;
    String unit;
    String name;
}

