package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceDto {

    String name;
    String type;
    boolean isOnline;
    boolean isOn;
    Double currentValue;
    Double settingValue;

}
