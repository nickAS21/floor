package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.usr.entity.golego.BatteryDataUsrTcpWiFi;
import org.nickas21.smart.usr.entity.golego.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.golego.UsrTcpWifiC1Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;

import java.util.Map;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.datePatternGridStatus;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.keyIdx;
import static org.nickas21.smart.util.StringUtils.formatTimestamp;
import static org.nickas21.smart.util.StringUtils.intToHex;
import static org.nickas21.smart.util.StringUtils.isBlank;

@Slf4j
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatteryInfoDto {
    protected String timestamp;
    protected int port;
    protected double voltageCurV;
    protected double currentCurA;
    protected double socPercent;
    Double bmsTempValue;
    String bmsStatusStr;
    String errorInfoDataHex;
    protected String errorOutput;
    protected String connectionStatus;
    Integer cyclesCount;
    protected Double deltaMv; // in V critical if > 0,110 V
    Integer minCellIdx;
    Integer maxCellIdx;
    Map<Integer, Float> cellVoltagesV;
    double sohPercent = 100.0;

    // Dacha akum
    public BatteryInfoDto(DefaultSmartSolarmanTuyaService solarmanTuyaService, UsrTcpWiFiService usrTcpWiFiService){
        if (solarmanTuyaService.getPowerValueRealTimeData() != null && solarmanTuyaService.getPowerValueRealTimeData().getCollectionTime() != null) {
            long timeStamp = solarmanTuyaService.getPowerValueRealTimeData().getCollectionTime() * 1000;
            this.timestamp = formatTimestamp(timeStamp, datePatternGridStatus);
            this.currentCurA = solarmanTuyaService.getPowerValueRealTimeData().getBmsCurrentValue();
            this.bmsTempValue = solarmanTuyaService.getPowerValueRealTimeData().getBmsTempValue();
            this.voltageCurV = solarmanTuyaService.getPowerValueRealTimeData().getBmsVoltageValue();
            this.socPercent = solarmanTuyaService.getPowerValueRealTimeData().getBatterySocValue();
            this.bmsStatusStr = solarmanTuyaService.getPowerValueRealTimeData().getBatteryStatusValue();
            this.errorInfoDataHex = intToHex(0);
            this.connectionStatus = usrTcpWiFiService.calculateStatus(timeStamp, solarmanTuyaService.getTimeoutSecUpdate());
        }
    }

    // Usr Golego/ GOOTO akum
    public BatteryInfoDto(Map.Entry<Integer, BatteryDataUsrTcpWiFi> usrTcpWiFiBatteryEntry, UsrTcpWiFiService usrTcpWiFiService){
        this.port = usrTcpWiFiBatteryEntry.getKey();
        BatteryDataUsrTcpWiFi batteryData = usrTcpWiFiBatteryEntry.getValue();
        // Usr Golego
        UsrTcpWifiC0Data c0Data = batteryData.getC0Data();
        if (c0Data != null && c0Data.getTimestamp() != null) {
            this.timestamp = formatTimestamp(c0Data.getTimestamp().toEpochMilli(), datePatternGridStatus);
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
                this.timestamp = formatTimestamp(c1Data.getTimestamp().toEpochMilli(), datePatternGridStatus);
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
            this.minCellIdx =  c1Data.getMinCellV() == null ? -1 :  c1Data.getMinCellV().get(keyIdx).asInt();
            this.maxCellIdx =  c1Data.getMaxCellV() == null ? -1 : c1Data.getMaxCellV().get(keyIdx).asInt();
            this.connectionStatus = usrTcpWiFiService.getStatusByPort(this.port);
            this.cellVoltagesV = c1Data.getCellVoltagesV();
        }
        // Bat GOOTO
        UsrTcpWifiRS485_Data_GOOTO_Telemetry dataGOOTO_Telemetry = batteryData.getRs485_Data_GOOTO_Telemetry();
        if (dataGOOTO_Telemetry != null && dataGOOTO_Telemetry.getTimestamp() != null) {
            this.timestamp = formatTimestamp(dataGOOTO_Telemetry.getTimestamp().toEpochMilli(), datePatternGridStatus);
            this.currentCurA = dataGOOTO_Telemetry.getCurrentCurA();
            this.voltageCurV = dataGOOTO_Telemetry.getVoltageCurV();
            this.socPercent = dataGOOTO_Telemetry.getSocPercent();
            this.bmsStatusStr = dataGOOTO_Telemetry.getBmsStatusStr();
            this.connectionStatus = usrTcpWiFiService.getStatusByPort(this.port);
            this.cyclesCount = dataGOOTO_Telemetry.getCyclesCount();
            this.sohPercent = dataGOOTO_Telemetry.getSohPercent();
            if ((this.bmsTempValue == null || this.bmsTempValue == 0) && dataGOOTO_Telemetry.getBmsTempValue() != null) {
                this.bmsTempValue = dataGOOTO_Telemetry.getBmsTempValue();
            }

            if (this.voltageCurV == 0 && dataGOOTO_Telemetry.getCellVoltagesV() != null) {
                this.voltageCurV = dataGOOTO_Telemetry.getCellVoltagesV().values().stream()
                        .mapToDouble(Float::doubleValue)
                        .sum();
            }

            this.minCellIdx =  dataGOOTO_Telemetry.getMinCellV() == null ? -1 :  dataGOOTO_Telemetry.getMinCellV().get(keyIdx).asInt();
            this.maxCellIdx =  dataGOOTO_Telemetry.getMaxCellV() == null ? -1 : dataGOOTO_Telemetry.getMaxCellV().get(keyIdx).asInt();


            if (isBlank(this.errorInfoDataHex)){
                this.errorInfoDataHex =  dataGOOTO_Telemetry.getErrorInfoDataHex();
            }
            if (isBlank(this.errorOutput) ){
                this.errorOutput =  dataGOOTO_Telemetry.getErrorOutput();
            }

            this.deltaMv = dataGOOTO_Telemetry.getDeltaMv() / 1000.0;  // this.deltaMv in V Critical > 0.100 V
            this.cellVoltagesV = dataGOOTO_Telemetry.getCellVoltagesV();
        }
    }
}
