package org.nickas21.smart.data.dataEntityDto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DataAnalyticResponse {
    private String zoneId;
    private List<DataAnalyticDto> dataAnalyticDtos;
}
