package org.nickas21.smart.data.dataEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.BatteryStatus;

import java.util.Map;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatteryInfo {

    String port;
    String address;
    double soc;
    double v;
    double a;
    BatteryStatus status; // Тепер enum
    boolean hasError;
    boolean isActive;
    Map<Integer, BatteryCellInfo> cells;

}
