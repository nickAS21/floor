package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiC1Data;

import java.util.Map;

import static org.nickas21.smart.util.StringUtils.formatTimestamp;
import static org.nickas21.smart.util.StringUtils.intToHex;
import static org.nickas21.smart.util.StringUtils.isBlank;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatteryInfoDto {

    private final String datePattern = "yyyy-MM-dd HH:mm";
    private final long marginMs = 60 * 1000L; // +1 minutes

    String timestamp;
    int port;
    double voltageCurV;
    double currentCurA;
    double socPercent;
    String bmsStatusStr;
    String errorInfoDataHex;
    String errorOutput;
    Boolean isActive = false;
    // Metadata for front
    Double deltaMv; // in V critical if > 0,110 V
    Integer minCellIdx;
    Integer maxCellIdx;
    Map<Integer, Float> cellVoltagesV;

    public BatteryInfoDto(Map.Entry<Integer, UsrTcpWiFiBattery> usrTcpWiFiBatteryEntry, long timeoutSecUpdate){
        this.port = usrTcpWiFiBatteryEntry.getKey();
        UsrTcpWiFiBattery batteryData = usrTcpWiFiBatteryEntry.getValue();

        UsrTcpWifiC0Data c0Data = batteryData.getC0Data();
        if (c0Data != null) {
            this.timestamp = formatTimestamp(c0Data.getTimestamp().toEpochMilli(), datePattern);
            this.currentCurA = c0Data.getCurrentCurA();
            this.socPercent = c0Data.getSocPercent();
            this.bmsStatusStr = c0Data.getBmsStatusStr();
            this.errorInfoDataHex =  intToHex(c0Data.getErrorInfoData());
            this.errorOutput = c0Data.getErrorOutput();
            this.isActive = getIsActive(c0Data.getTimestamp().toEpochMilli(), timeoutSecUpdate);
        }

        UsrTcpWifiC1Data c1Data = batteryData.getC1Data();
        if (c1Data != null) {
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
            this.minCellIdx =  c1Data.getMinCellV().get("keyIdx").asInt();
            this.maxCellIdx =  c1Data.getMaxCellV().get("keyIdx").asInt();
            if (!this.isActive) {
                this.isActive = getIsActive(c1Data.getTimestamp().toEpochMilli(), timeoutSecUpdate);
            }
            this.cellVoltagesV = c1Data.getCellVoltagesV();

        }
    }

    private boolean getIsActive(long timestamp, long timeoutSecUpdate) {
        long timeoutMs = timeoutSecUpdate * 1000L;
        long currentMillis = System.currentTimeMillis();
        return this.socPercent > 0 && (currentMillis - timestamp) < (timeoutMs + marginMs);
    }
}
