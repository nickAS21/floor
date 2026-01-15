package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoryFullInfoDto {
    private double solarPower;
    private double homePower;
    private double gridPower;
    private String gridDuration;
    private List<BatteryInfoDto> batteries = new ArrayList<>();
}