package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.PowerValueRealTimeData;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.InverterData;
import org.nickas21.smart.usr.entity.InvertorGolegoData90;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.service.UsrTcpWiFiBatteryRegistry;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.util.StringUtils.formatTimestamp;

@Slf4j
@Data
@NoArgsConstructor
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
    Map<Integer, Double> gridVoltageLs = new ConcurrentHashMap<>();

    double dailyConsumptionPower;
    double dailyGridPower;
    double dailyBatteryCharge;
    double dailyBatteryDischarge;
    double dailyProductionSolarPower;

    String timestampLastUpdateGridStatus;


    // Dacha
    public DataHomeDto(DefaultSmartSolarmanTuyaService solarmanTuyaService, TuyaDeviceService deviceService, UsrTcpWiFiService usrTcpWiFiService) {
        PowerValueRealTimeData powerValueRealTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        if (powerValueRealTimeData != null && powerValueRealTimeData.getCollectionTime() != null) {
            this.timestamp = powerValueRealTimeData.getCollectionTime() * 1000;
            this.batterySoc = powerValueRealTimeData.getBatterySocValue();
            this.batteryStatus = powerValueRealTimeData.getBatteryStatusValue();
            this.batteryVol = powerValueRealTimeData.getBatteryVoltageValue();
            this.batteryCurrent = Math.copySign(
                    Math.abs(powerValueRealTimeData.getBatteryCurrentValue()), // Only value
                    powerValueRealTimeData.getBmsCurrentValue()                // Only range
            );
//            this.batteryCurrent = this.batteryStatus.equals("Discharging") ? -this.batteryCurrent : this.batteryCurrent;
            this.solarPower = powerValueRealTimeData.getTotalProductionSolarPower();
            this.homePower = powerValueRealTimeData.getTotalHomePower();
            this.gridPower = powerValueRealTimeData.getTotalGridPower();

            this.dailyConsumptionPower = powerValueRealTimeData.getDailyHomeConsumptionPower();
            this.dailyGridPower = powerValueRealTimeData.getDailyEnergyBuy();
            this.dailyBatteryCharge = powerValueRealTimeData.getDailyBatteryCharge();
            this.dailyBatteryDischarge = powerValueRealTimeData.getDailyBatteryDischarge();
            this.dailyProductionSolarPower = powerValueRealTimeData.getDailyProductionSolarPower();
            this.gridVoltageLs.put(1, powerValueRealTimeData.getGridVoltageL1());
            this.gridVoltageLs.put(2, powerValueRealTimeData.getGridVoltageL2());
            this.gridVoltageLs.put(3, powerValueRealTimeData.getGridVoltageL3());
        }
        Boolean gridRelayCodeDachaStateOnLine = deviceService.getGridRelayCodeDachaStateOnLine();
        if (gridRelayCodeDachaStateOnLine != null) this.gridStatusRealTimeOnLine = gridRelayCodeDachaStateOnLine;
        Boolean gridRelayCodeDachaStateSwitch =  deviceService.getGridRelayCodeDachaStateSwitch();
        if (gridRelayCodeDachaStateSwitch!= null) this.gridStatusRealTimeSwitch = gridRelayCodeDachaStateSwitch;
        Map.Entry<Long, Boolean>  lastUpdateTimeGridStatusEntryDacha =  deviceService.getLastUpdateTimeGridStatusInfoDacha();
        this.timestampLastUpdateGridStatus = lastUpdateTimeGridStatusEntryDacha != null ? formatTimestamp(lastUpdateTimeGridStatusEntryDacha.getKey(), datePatternGridStatus) : "null";
        if (solarmanTuyaService.getPowerValueRealTimeData() != null) {
            String connectionBatteryStatus = usrTcpWiFiService.calculateStatus(solarmanTuyaService.getPowerValueRealTimeData().getCollectionTime() * 1000, solarmanTuyaService.getTimeoutSecUpdate());
            log.warn("Dacha inverter and battery: is -> [{}]", connectionBatteryStatus);
        }
        log.warn("DataHomeDacha [{}]", this);
    }

    // Golego
    public DataHomeDto(TuyaDeviceService deviceService, UsrTcpWiFiParseData usrTcpWiFiParseData, UsrTcpWiFiService usrTcpWiFiService) {
        int portDacha = usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterDacha();
        log.warn("Dacha inverter port [{}]: is -> [{}]", portDacha, usrTcpWiFiService.getStatusByPort(portDacha));
        UsrTcpWiFiProperties tcpProps = usrTcpWiFiParseData.getUsrTcpWiFiProperties();
        UsrTcpWiFiBattery usrTcpWiFiBattery = usrTcpWiFiParseData.getBattery(tcpProps.getPortMaster());
        Boolean gridRelayCodeGolegoStateOnLine = deviceService.getGridRelayCodeGolegoStateOnLine();
        if (gridRelayCodeGolegoStateOnLine != null) this.gridStatusRealTimeOnLine = gridRelayCodeGolegoStateOnLine;
        Boolean gridRelayCodeGolegoStateSwitch =  deviceService.getGridRelayCodeGolegoStateSwitch();
        if (gridRelayCodeGolegoStateSwitch != null) this.gridStatusRealTimeSwitch = gridRelayCodeGolegoStateSwitch;
        if (usrTcpWiFiBattery != null) {
            UsrTcpWifiC0Data c0Data = usrTcpWiFiBattery.getC0Data();
            int portStart = tcpProps.getPortStart();
            int batteriesCnt = tcpProps.getBatteriesCnt();
            double batteryCurrentAll = 0;
            double batterySocMax = c0Data.getSocPercent();
            int batteriesActiveCnt = 0;
            List<Integer> batteriesNoActive = new ArrayList<>();
            for (int i = 0; i < batteriesCnt; i++) {
                int port = portStart + i;
                if (port >= usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterGolego() ) {
                    log.warn("Golego inverter port [{}]: is -> [{}]", port, usrTcpWiFiService.getStatusByPort(port));
                } else  {
                    UsrTcpWiFiBattery usrTcpWiFiBatteryA = usrTcpWiFiParseData.getBattery(port);
                    if (usrTcpWiFiBatteryA != null && usrTcpWiFiBatteryA.getC0Data() != null) {
                        batteryCurrentAll += usrTcpWiFiBatteryA.getC0Data().getCurrentCurA();
                        if (usrTcpWiFiBatteryA.getC0Data().getSocPercent() != 0) {
                            batteriesActiveCnt++;
                        } else {
                            batteriesNoActive.add(port);
                        }
                        // TODO - 8894 - 20% this is bad then only master
                        batterySocMax = usrTcpWiFiBatteryA.getC0Data().getSocPercent() != 0 ? Math.max(batterySocMax, usrTcpWiFiBatteryA.getC0Data().getSocPercent()) : batterySocMax;
                    }
                }

            }
            log.warn("Golego battery: BatteriesActivCnt [{}] BatteriesNoActive {}", batteriesActiveCnt, !batteriesNoActive.isEmpty() ? batteriesNoActive : 0);

            this.timestamp = c0Data.getTimestamp() != null ? c0Data.getTimestamp().toEpochMilli() : 0;
            this.batterySoc = batterySocMax;

            // from inverter
            UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry = usrTcpWiFiParseData.getUsrTcpWiFiBatteryRegistry();
            Integer portInverterGolego = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortInverterGolego();
            InverterData inverterDataGolego = usrTcpWiFiBatteryRegistry.getInverter(portInverterGolego);
            if (inverterDataGolego != null && inverterDataGolego.getInvertorGolegoData90() != null && inverterDataGolego.getInvertorGolegoData90().getHexMap().length > 0) {
                InvertorGolegoData90 invertorGolegoData90 = inverterDataGolego.getInvertorGolegoData90();
                this.batteryStatus = invertorGolegoData90.getStatus();
                this.batteryVol = invertorGolegoData90.getBatteryVoltage();
                this.batteryCurrent = invertorGolegoData90.getBatteryCurrent();
                this.homePower = invertorGolegoData90.getLoadOutputActivePower();
                this.gridVoltageLs.put(1, invertorGolegoData90.getAcInputVoltage());
            } else {
                this.batteryStatus = c0Data.getBmsStatusStr();
                this.batteryVol = c0Data.getVoltageCurV();
                this.batteryCurrent = Math.round(batteryCurrentAll * 100.0) / 100.0;;
                 if (this.batteryCurrent == 0 && this.gridPower == 0) {
                    this.homePower = 0;
                } else if (this.batteryCurrent < 0) {
                    this.homePower = (this.batteryVol * Math.abs(this.batteryCurrent)) - this.golegoInverterPowerDefault;
                } else {
                    this.homePower = this.golegoPowerDefault;
                }
            }
            if (this.gridStatusRealTimeOnLine && this.gridStatusRealTimeSwitch) {
                this.gridPower = this.batteryVol * this.batteryCurrent + golegoPowerDefault + golegoInverterPowerDefault;
            } else {
                this.gridPower = 0;
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
