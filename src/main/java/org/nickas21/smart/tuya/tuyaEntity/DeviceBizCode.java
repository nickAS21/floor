package org.nickas21.smart.tuya.tuyaEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceBizCode {
    String bizCode;
    private Object value;
    private Object valueOld;
    private Long ts;
    DeviceBizData bizData;
}
