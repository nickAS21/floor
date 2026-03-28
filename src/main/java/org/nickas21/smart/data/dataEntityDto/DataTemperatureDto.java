package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.tuyaEntity.DeviceStatus;
import java.util.Map;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTemperatureDto {

    double temperature; //grad C
    double humidity;    // %
    double luminance;   // %

    public DataTemperatureDto(Map<String, DeviceStatus> deviceStatus) {
        this.temperature = getDouble(deviceStatus, "va_temperature") / 10;
        this.humidity = getDouble(deviceStatus, "va_humidity") / 10;
        this.luminance = getDouble(deviceStatus, "bright_value");
    }

    private double getDouble(Map<String, DeviceStatus> map, String key) {
        try {
            if (map == null || !map.containsKey(key)) {
                return 0.0;
            }
            DeviceStatus status = map.get(key);
            if (status == null || status.getValue() == null) {
                return 0.0;
            }
            Object value = status.getValue();
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } catch (Exception e) {
            log.warn("Cannot parse key {}: {}", key, e.getMessage());
        }
        return 0.0;
    }
}