package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.util.LocationType;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAnalyticDto {
    private long timestamp;
    private PowerType powerType;
    private LocationType location;
    private double powerDay;
    private double powerNight;
    private double powerTotal;

    public DataAnalyticDto (LocationType location, PowerType powerType) {
        this.timestamp = System.currentTimeMillis();
        this.location = location;
        this.powerType = powerType;
        // Ініціалізуємо нулями, щоб уникнути Null при математичних операціях
        this.powerDay = 0.0;
        this.powerNight = 0.0;
        this.powerTotal = 0.0;
    }
}

