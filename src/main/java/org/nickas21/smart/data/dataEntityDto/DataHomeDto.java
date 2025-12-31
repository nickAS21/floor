package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.PowerValueRealTimeData;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.nickas21.smart.util.StringUtils.formatTimestamp;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataHomeDto {

    private final double golegoPowerDefault = 42.0; // only  2 - WiFi routers
    private final double golegoInverterPowerDefault = 10.0;
    public static final String datePatternGridStatus = "yyyy-MM-dd HH:mm";

    // Dacha -Update real time data: powerValueRealTimeData.getCollectionTime() * 1000
    // Golego
    long timestamp;
    // batSocNew - dacha
    double batterySoc;
    String batteryStatus;
    double batteryVol;
    double batteryCurrent;
    String batteriesFault;

    double solarPower;
    double homePower;
    double gridPower;
    boolean gridStatusRealTimeOnLine;
    boolean gridStatusRealTimeSwitch;

    double dailyConsumptionPower;
    double dailyGridPower;
    double dailyBatteryCharge;
    double dailyBatteryDischarge;
    double dailyProductionSolarPower;

    String timestampLastUpdateGridStatus;


    public DataHomeDto(DefaultSmartSolarmanTuyaService solarmanTuyaService, TuyaDeviceService deviceService) {
        PowerValueRealTimeData powerValueRealTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        if (powerValueRealTimeData != null) {
            this.timestamp = powerValueRealTimeData.getCollectionTime() * 1000;
            this.batterySoc = powerValueRealTimeData.getBatterySocValue();
            this.batteryStatus = powerValueRealTimeData.getBatteryStatusValue();
            this.batteryVol = powerValueRealTimeData.getBatteryVoltageValue();
            this.batteryCurrent = Math.copySign(
                    Math.abs(powerValueRealTimeData.getBatteryCurrentValue()), // Only value
                    powerValueRealTimeData.getBmsCurrentValue()                // Only range
            );
            this.solarPower = powerValueRealTimeData.getTotalProductionSolarPower();
            this.homePower = powerValueRealTimeData.getTotalHomePower();
            this.gridPower = powerValueRealTimeData.getTotalGridPower();

            this.dailyConsumptionPower = powerValueRealTimeData.getDailyHomeConsumptionPower();
            this.dailyGridPower = powerValueRealTimeData.getDailyEnergyBuy();
            this.dailyBatteryCharge = powerValueRealTimeData.getDailyBatteryCharge();
            this.dailyBatteryDischarge = powerValueRealTimeData.getDailyBatteryDischarge();
            this.dailyProductionSolarPower = powerValueRealTimeData.getDailyProductionSolarPower();
        }
        this.gridStatusRealTimeOnLine = deviceService.getGridRelayCodeDachaStateOnLine() != null && deviceService.getGridRelayCodeDachaStateOnLine();
        this.gridStatusRealTimeSwitch =  deviceService.getGridRelayCodeDachaStateSwitch() != null && deviceService.getGridRelayCodeDachaStateSwitch();
        Map.Entry<Long, Boolean>  lastUpdateTimeGridStatusEntryDacha =  deviceService.getLastUpdateTimeGridStatusInfoDacha();
        this.timestampLastUpdateGridStatus = lastUpdateTimeGridStatusEntryDacha != null ? formatTimestamp(lastUpdateTimeGridStatusEntryDacha.getKey(), datePatternGridStatus) : "null";
        log.warn("DataHomeDacha [{}]", this);
    }

    public DataHomeDto(TuyaDeviceService deviceService, UsrTcpWiFiParseData usrTcpWiFiParseData) {
        UsrTcpWiFiProperties tcpProps = usrTcpWiFiParseData.getUsrTcpWiFiProperties();
        UsrTcpWiFiBattery usrTcpWiFiBattery = usrTcpWiFiParseData.getBattery(tcpProps.getPortMaster());
        this.gridStatusRealTimeOnLine = deviceService.getGridRelayCodeGolegoStateOnLine() != null && deviceService.getGridRelayCodeGolegoStateOnLine();
        this.gridStatusRealTimeSwitch =  deviceService.getGridRelayCodeGolegoStateSwitch() != null && deviceService.getGridRelayCodeGolegoStateSwitch();
        if (usrTcpWiFiBattery != null) {
            UsrTcpWifiC0Data c0Data = usrTcpWiFiBattery.getC0Data();
            int portStart = tcpProps.getPortStart();
            int batteriesCnt = tcpProps.getBatteriesCnt();
            double batteryCurrentAll = 0;
            double batterySocMin = c0Data.getSocPercent();
            int batteriesActiveCnt = 0;
            List<Integer> batteriesNoActive = new ArrayList<>();
            for (int i = 0; i < batteriesCnt; i++) {
                int port = portStart + i;
                UsrTcpWiFiBattery usrTcpWiFiBatteryA = usrTcpWiFiParseData.getBattery(port);
                if (usrTcpWiFiBatteryA != null && usrTcpWiFiBatteryA.getC0Data() != null) {
//                    log.warn("port [{}] batteryCurrent [{}] soc [{}]", port, usrTcpWiFiBatteryA.getC0Data().getCurrentCurA(), usrTcpWiFiBatteryA.getC0Data().getSocPercent());
                    batteryCurrentAll += usrTcpWiFiBatteryA.getC0Data().getCurrentCurA();
                    if (usrTcpWiFiBatteryA.getC0Data().getSocPercent() != 0) {
                        batteriesActiveCnt++;
                    } else {
                        batteriesNoActive.add(port);
                    }
                    batterySocMin = usrTcpWiFiBatteryA.getC0Data().getSocPercent() != 0 ? Math.min(batterySocMin, usrTcpWiFiBatteryA.getC0Data().getSocPercent()) : batterySocMin;
                }
            }
//            log.warn("port All batteryCurrent [{}]", batteryCurrentAll);
            this.timestamp = c0Data.getTimestamp() != null ? c0Data.getTimestamp().toEpochMilli() : 0;
            this.batterySoc = batterySocMin;
            this.batteryStatus = c0Data.getBmsStatusStr();
            this.batteryVol = c0Data.getVoltageCurV();
            this.batteryCurrent = batteryCurrentAll;
//            log.warn("batterySoc [{}] batteryVol [{}] batteryCurrent [{}] BatteriesActiv [{}]",this.batterySoc, this.batteryVol, this.batteryCurrent, batteriesActiveCnt);
            log.warn("Golego: BatteriesActivCnt [{}] BatteriesNoActive {}",batteriesActiveCnt, batteriesNoActive.toString());
            if (this.gridStatusRealTimeOnLine && this.gridStatusRealTimeSwitch) {
                this.gridPower = this.batteryVol * this.batteryCurrent + golegoPowerDefault + golegoInverterPowerDefault;
            } else {
                this.gridPower = 0;
            }
            if (this.batteryCurrent == 0 && this.gridPower == 0) {
                this.homePower = 0;
            } else if (this.batteryCurrent < 0 ) {
                this.homePower = (this.batteryVol * Math.abs(this.batteryCurrent)) - this.golegoInverterPowerDefault;
            } else {
                this.homePower = this.golegoPowerDefault;
            }
            this.solarPower = 0;
            this.dailyConsumptionPower = 0;
            this.dailyGridPower = 0;
            this.dailyBatteryCharge = 0;
            this.dailyBatteryDischarge = 0;
            this.dailyProductionSolarPower = 0;
            Map.Entry<Long, Boolean>  lastUpdateTimeGridStatusEntryHome =  deviceService.getLastUpdateTimeGridStatusInfoHome();
            this.timestampLastUpdateGridStatus = lastUpdateTimeGridStatusEntryHome != null ? formatTimestamp(lastUpdateTimeGridStatusEntryHome.getKey(), datePatternGridStatus) : "null";
        }
        log.warn("DataHomeGolego [{}]", this);
    }
}
    /**
     * time: -> -Update real time data: [1:46:42 PM],
     * - yyyy-mm-dd-hh:mm:sec
     * Battery:
     * - Soc % -> -batSocNew: [71.0 %]
     * - Status - Caharge/Dischare/Static -> -batteryStatus: [Charging]
     * - V ->  -bmsVolNew: [53.67 V]
     * - A ->  -bmsCurrentNew: [2.0 A],
     * Grid:
     * - off/on -> --gridDachaStatusRealTime: [true] /gridGo;egoStatusRealTime
     * Sollar:
     * - Wat -> -solarPower: [342.0 W]
     * Home:
     * - only picture
     *
     */

    /**
     *  Current data:
     *      Current real time data: [1:48:18 PM], -Update real time data: [1:46:42 PM],
     *      -batSocLast: [71.0 %], -batSocNew: [71.0 %], -deltaBmsSoc: [0.00 %], -batterySocMin: [58.01 %],
     *      -batteryStatus: [Charging], -batteryPower: [-181.0 W], -batVolNew: [53.87 V], -batCurrentNew: [-3.36 A],  -bmsVolNew: [53.69 V], -bmsCurrentNew: [3.0 A], -BMS Temperature: [12.1  grad C]
     *      -solarPower: [399.0 W], consumptionPower: [121.0 W], stationPower: [50.0 W],
     *      -dailyBatteryCharge: [1.1 kWh], -dailyBatteryDischarge: [1.9 kWh],
     *      -relayStatus: [Break], -gridStatusSolarman: [Static], -gridStatusRealTimeOnLine: [true], -dailyBuy:[0.0 kWh], -dailySell: [0.0 kWh],
     *      -AC (inverter) Temperature:  [38.5 grad C].
     *      - usrBmsSummary: batSocLast: [96 %]
     *       - BMS status Static
     * - Voltage: 53.00 V
     * - Current: -4.80 A
     * - Cells delta: 0.003 V
     */

