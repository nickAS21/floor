package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiC1Data;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;

import java.util.Map;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.keyIdx;
import static org.nickas21.smart.util.StringUtils.formatTimestamp;
import static org.nickas21.smart.util.StringUtils.intToHex;
import static org.nickas21.smart.util.StringUtils.isBlank;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatteryInfoDto {

    private final String datePattern = "yyyy-MM-dd HH:mm";

    String timestamp;
    int port;
    double voltageCurV;
    double currentCurA;
    double socPercent;
    Double bmsTempValue;
    String bmsStatusStr;
    String errorInfoDataHex;
    String errorOutput;
    String connectionStatus;
    // Metadata for front
    Double deltaMv; // in V critical if > 0,110 V
    Integer minCellIdx;
    Integer maxCellIdx;
    Map<Integer, Float> cellVoltagesV;

    // Dacha akkum
    public BatteryInfoDto(DefaultSmartSolarmanTuyaService solarmanTuyaService, UsrTcpWiFiService usrTcpWiFiService){
        if (solarmanTuyaService.getPowerValueRealTimeData() != null && solarmanTuyaService.getPowerValueRealTimeData().getCollectionTime() != null) {
            long timeStamp = solarmanTuyaService.getPowerValueRealTimeData().getCollectionTime() * 1000;
            this.timestamp = formatTimestamp(timeStamp, datePattern);
            this.currentCurA = solarmanTuyaService.getPowerValueRealTimeData().getBmsCurrentValue();
            this.bmsTempValue = solarmanTuyaService.getPowerValueRealTimeData().getBmsTempValue();
            this.voltageCurV = solarmanTuyaService.getPowerValueRealTimeData().getBmsVoltageValue();
            this.socPercent = solarmanTuyaService.getPowerValueRealTimeData().getBatterySocValue();
            this.bmsStatusStr = solarmanTuyaService.getPowerValueRealTimeData().getBatteryStatusValue();
            this.errorInfoDataHex = intToHex(0);
            this.connectionStatus = usrTcpWiFiService.calculateStatus(timeStamp, solarmanTuyaService.getTimeoutSecUpdate());
        }
    }

    public BatteryInfoDto(Map.Entry<Integer, UsrTcpWiFiBattery> usrTcpWiFiBatteryEntry, Long timeoutSecUpdate, UsrTcpWiFiService usrTcpWiFiService){
        this.port = usrTcpWiFiBatteryEntry.getKey();
        UsrTcpWiFiBattery batteryData = usrTcpWiFiBatteryEntry.getValue();

        UsrTcpWifiC0Data c0Data = batteryData.getC0Data();
        if (c0Data != null && c0Data.getTimestamp() != null) {
            this.timestamp = formatTimestamp(c0Data.getTimestamp().toEpochMilli(), datePattern);
            this.currentCurA = c0Data.getCurrentCurA();
            this.socPercent = c0Data.getSocPercent();
            this.bmsStatusStr = c0Data.getBmsStatusStr();
            this.errorInfoDataHex =  intToHex(c0Data.getErrorInfoData());
            this.errorOutput = c0Data.getErrorOutput();
            this.connectionStatus = usrTcpWiFiService.getStatusByPort(this.port);
        }

        UsrTcpWifiC1Data c1Data = batteryData.getC1Data();
        if (c1Data != null && c1Data.getTimestamp() != null) {
            if (isBlank(this.timestamp)) {
                this.timestamp = formatTimestamp(c1Data.getTimestamp().toEpochMilli(), datePattern);
            }
            if (this.socPercent == 0) {
                this.socPercent = c1Data.getSocPercent();
            }
            if (this.voltageCurV == 0 && c1Data.getCellVoltagesV() != null) {
                this.voltageCurV = c1Data.getCellVoltagesV().values().stream()
                        .mapToDouble(Float::doubleValue)
                        .sum();
            }
            if (isBlank(this.errorInfoDataHex)){
                this.errorInfoDataHex =  intToHex(c1Data.getErrorInfoData());
            }
            if (isBlank(this.errorOutput) ){
                this.errorOutput =  c1Data.getErrorOutput();
            }
            this.deltaMv = c1Data.getDeltaMv() / 1000.0;  // this.deltaMv in V Critical > 0.100 V
            this.minCellIdx =  c1Data.getMinCellV().get(keyIdx).asInt();
            this.maxCellIdx =  c1Data.getMaxCellV().get(keyIdx).asInt();
            this.connectionStatus = usrTcpWiFiService.getStatusByPort(this.port);
            this.cellVoltagesV = c1Data.getCellVoltagesV();
        }
    }
}
