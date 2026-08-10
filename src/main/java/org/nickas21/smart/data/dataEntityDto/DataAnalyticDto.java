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

    long timestamp;
    LocationType location;
    double gridPower;
    double gridDailyDayPower;
    double gridDailyNightPower;
    double gridDailyTotalPower;
    double solarPower;
    double solarDailyPower;
    double homePower;
    double homeDailyPower;
    double bmsSoc;
    double bmsDailyDischarge;
    double bmsDailyCharge;
    double temperatureOut;
    double humidityOut;
    double luminanceOut;
    double temperatureIn;
    double humidityIn;
    double luminanceIn;
   SolarPanelInfoDtos panelInfoDtos;

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
        this.temperatureOut = 0.0;
        this.humidityOut = 0.0;
        this.luminanceOut = 0.0;
        this.temperatureIn = 0.0;
        this.humidityIn = 0.0;
        this.luminanceIn = 0.0;
        this.panelInfoDtos = null;
    }
}

