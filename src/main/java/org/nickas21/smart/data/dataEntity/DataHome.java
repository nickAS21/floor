package org.nickas21.smart.data.dataEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.PowerValueRealTimeData;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataHome {

    // Dacha -Update real time data: powerValueRealTimeData.getCollectionTime() * 1000
    // Golego
    long timestamp;
    // batSocNew - dacha
    double batterySoc;
    String batteryStatus;
    double batteryVol;
    double batteryCurrent;
    boolean gridStatusRealTime;
    double solarPower;

    public DataHome(DefaultSmartSolarmanTuyaService solarmanTuyaService, TuyaDeviceService deviceService) {
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
            this.solarPower = powerValueRealTimeData.getProductionTotalSolarPowerValue();
        }
        this.gridStatusRealTime = deviceService.getGridRelayCodeDachaStateOnLine() != null && deviceService.getGridRelayCodeDachaStateOnLine();
        log.warn("DataHomeDacha [{}]", this);
    }

    public DataHome(TuyaDeviceService deviceService, UsrTcpWiFiParseData usrTcpWiFiParseData, int port) {
        UsrTcpWiFiBattery usrTcpWiFiBattery = usrTcpWiFiParseData.getBattery(port);
        log.info("usrTcpWiFiBattery");
        if ( usrTcpWiFiBattery != null) {
            log.warn("usrTcpWiFiBatteryRegistry is [not null]");
            log.warn("usrTcpWiFiBattery [{}]", usrTcpWiFiBattery);
            UsrTcpWifiC0Data c0Data = usrTcpWiFiBattery.getC0Data();
            this.timestamp = c0Data.getTimestamp().toEpochMilli();
            this.batterySoc = c0Data.getSocPercent();
            this.batteryStatus = c0Data.getBmsStatusStr();
            this.batteryVol = c0Data.getVoltageCurV();
            this.batteryCurrent = c0Data.getCurrentCurA() * 8;
            this.solarPower = 0;
        } else {
            log.warn("usrTcpWiFiBatteryRegistry is [null]");
        }
        log.warn("TuyaDeviceService [{}]", deviceService);
        this.gridStatusRealTime = deviceService.getGridRelayCodeGolegoStateOnLine() != null && deviceService.getGridRelayCodeGolegoStateOnLine();
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
     *      -batteryDailyCharge: [1.1 kWh], -batteryDailyDischarge: [1.9 kWh],
     *      -relayStatus: [Break], -gridStatusSolarman: [Static], -gridStatusRealTime: [true], -dailyBuy:[0.0 kWh], -dailySell: [0.0 kWh],
     *      -AC (inverter) Temperature:  [38.5 grad C].
     *      - usrBmsSummary: batSocLast: [96 %]
     *       - BMS status Static
     * - Voltage: 53.00 V
     * - Current: -4.80 A
     * - Cells delta: 0.003 V
     */

