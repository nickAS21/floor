package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.util.LocationType;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAnalyticDto extends DataAnalytic {

    public DataAnalyticDto(){}

    public DataAnalyticDto(long timestamp, LocationType location,
                           double powerDay, double powerNight, double powerTotal) {
        this.timestamp = timestamp;
        this.location = location;
        this.gridPower = 0.0;
        this.gridDailyDayPower = powerDay;
        this.gridDailyNightPower = powerNight;
        this.gridDailyTotalPower= powerTotal;
        this.solarPower = 0.0;
        this.solarDailyPower = 0.0;
        this.homePower = 0.0;
        this.homeDailyPower = 0.0;
        this.bmsSoc = 0.0;
        this.bmsDailyDischarge = 0.0;
        this.bmsDailyCharge = 0.0;
    }

    public DataAnalyticDto (LocationType location) {
        this.timestamp = System.currentTimeMillis();
        this.location = location;
        this.bmsSoc = 0.0;
        this.gridPower = 0.0;
        this.gridDailyDayPower = 0.0;
        this.gridDailyNightPower = 0.0;
        this.gridDailyTotalPower = 0.0;
        this.solarPower = 0.0;
        this.solarDailyPower = 0.0;
        this.homePower = 0.0;
        this.homeDailyPower = 0.0;
        this.bmsSoc = 0.0;
        this.bmsDailyDischarge = 0.0;
        this.bmsDailyCharge = 0.0;
    }
}

