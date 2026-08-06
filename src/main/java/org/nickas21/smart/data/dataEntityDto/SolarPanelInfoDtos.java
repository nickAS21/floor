package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.PowerValueRealTimeData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.datePatternGridStatus;
import static org.nickas21.smart.util.StringUtils.formatTimestamp;

@Slf4j
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolarPanelInfoDtos {
    String timestamp;
    Map<String, SolarPanelInfoDto> panels = new ConcurrentHashMap<>();
    public SolarPanelInfoDtos(PowerValueRealTimeData powerValueRealTimeData) {
        long timeStamp = powerValueRealTimeData.getCollectionTime() * 1000;
        this.timestamp = formatTimestamp(timeStamp, datePatternGridStatus);
        SolarPanelInfoDto pv1_m1_SolarPanelInfoDto = new SolarPanelInfoDto(
                timeStamp, 1,
                powerValueRealTimeData.getInverterM1ParallelInformationValue(),
                powerValueRealTimeData.getInverterM1DcVoltagePV1Value(),
                powerValueRealTimeData.getInverterM1DcCurrentPV1Value(),
                powerValueRealTimeData.getInverterM1DcPowerPV1Value()
                );
        SolarPanelInfoDto pv2_m1_SolarPanelInfoDto = new SolarPanelInfoDto(
                timeStamp, 2,
                powerValueRealTimeData.getInverterM1ParallelInformationValue(),
                powerValueRealTimeData.getInverterM1DcVoltagePV2Value(),
                powerValueRealTimeData.getInverterM1DcCurrentPV2Value(),
                powerValueRealTimeData.getInverterM1DcPowerPV2Value()
        );
        SolarPanelInfoDto pv1_s1_SolarPanelInfoDto = new SolarPanelInfoDto(
                timeStamp, 1,
                powerValueRealTimeData.getInverterS2ParallelInformationValue(),
                powerValueRealTimeData.getInverterS2DcVoltagePV1Value(),
                powerValueRealTimeData.getInverterS2DcCurrentPV1Value(),
                powerValueRealTimeData.getInverterS2DcPowerPV1Value()
        );
        SolarPanelInfoDto pv2_s1_SolarPanelInfoDto =  new SolarPanelInfoDto(
                timeStamp, 2,
                powerValueRealTimeData.getInverterS2ParallelInformationValue(),
                powerValueRealTimeData.getInverterS2DcVoltagePV2Value(),
                powerValueRealTimeData.getInverterS2DcCurrentPV2Value(),
                powerValueRealTimeData.getInverterS2DcPowerPV2Value()
        );

        panels.put(pv1_m1_SolarPanelInfoDto.getPvKey(), pv1_m1_SolarPanelInfoDto);
        panels.put(pv2_m1_SolarPanelInfoDto.getPvKey(), pv2_m1_SolarPanelInfoDto);
        panels.put(pv1_s1_SolarPanelInfoDto.getPvKey(), pv1_s1_SolarPanelInfoDto);
        panels.put(pv2_s1_SolarPanelInfoDto.getPvKey(), pv2_s1_SolarPanelInfoDto);
    }
}
