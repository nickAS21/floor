package org.nickas21.smart.tuya.tuyaEntity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Devices {
    private Map<String, Device> devIds = new HashMap<>();
    private Boolean has_more;
    private Integer total;

    public Device getDeviceByName(String devName) {
        return devIds.values().stream().filter(v-> v.getName().equals(devName)).findFirst().get();
    }
}
