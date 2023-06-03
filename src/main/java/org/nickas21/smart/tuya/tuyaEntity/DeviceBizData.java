package org.nickas21.smart.tuya.tuyaEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceBizData {
    String devId;
    String uid;
    String name;
}
