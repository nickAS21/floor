package org.nickas21.smart.solarman.api;

import lombok.Data;

@Data
public class RealTimeDataValue {
    String key;
    String value;
    String unit;
    String name;
}

