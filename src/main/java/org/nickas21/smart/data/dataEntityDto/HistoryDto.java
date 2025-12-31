package org.nickas21.smart.data.dataEntityDto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoryDto {

    String timestamp;
    String gridStatus;      // Online / Offline
    String gridDuration;   // Тривалість (наприклад, "3h 20m")
    List<BatteryInfoDto> batteries = new ArrayList<>(); // Твоя існуюча модель батарей

}
