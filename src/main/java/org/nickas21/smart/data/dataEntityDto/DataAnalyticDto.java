package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.util.LocationType;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAnalyticDto extends DataAnalytic {

    public DataAnalyticDto() {
    }

    // 2. Повний конструктор для мапінгу в сервісі (замість @AllArgsConstructor)
    public DataAnalyticDto(long timestamp, LocationType location,
                           double powerDay, double powerNight, double powerTotal) {
        this.timestamp = timestamp;
        this.location = location;
        this.gridDayPower = powerDay;
        this.gridNightPower = powerNight;
        this.gridTotalPower = powerTotal;
    }

    public DataAnalyticDto (LocationType location) {
        this.timestamp = System.currentTimeMillis();
        this.location = location;
        // Ініціалізуємо нулями, щоб уникнути Null при математичних операціях
        this.gridDayPower = 0.0;
        this.gridNightPower = 0.0;
        this.gridTotalPower = 0.0;
        this.solarPower = 0.0;
        this.homePower = 0.0;
        this.bmsSoc = 0.0;
    }
}

