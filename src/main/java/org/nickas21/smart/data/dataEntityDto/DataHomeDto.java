package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.PowerValueRealTimeData;
import org.nickas21.smart.solarman.BatteryStatus;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.config.PortStatus;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.dacha.InverterDataDacha;
import org.nickas21.smart.usr.entity.dacha.InverterDataDachaAcBatteryBlock106;
import org.nickas21.smart.usr.entity.dacha.InverterDataDachaBmsBlock16;
import org.nickas21.smart.usr.entity.dacha.InverterDataDachaDailyTotalBlock118;
import org.nickas21.smart.usr.entity.dacha.InverterDataDachaOutToHomeBlock8;
import org.nickas21.smart.usr.entity.dacha.InverterDataLoadDcBlock80;
import org.nickas21.smart.usr.entity.golego.BatteryDataUsrTcpWiFi;
import org.nickas21.smart.usr.entity.golego.InverterDataGolego;
import org.nickas21.smart.usr.entity.golego.InverterGolegoData90;
import org.nickas21.smart.usr.entity.golego.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.service.UsrTcpWiFiBatteryRegistry;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;
import org.nickas21.smart.util.LocationType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.ZoneOffset.UTC;
import static org.nickas21.smart.util.StringUtils.formatTimestamp;

@Slf4j
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataHomeDto {

    private final double golegoPowerDefault = 42.0; // only  2 - WiFi routers
    public static final double golegoInverterPowerDefault = 10.0;
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

    double temperatureOut;
    double humidityOut;
    double luminanceOut;
    double temperatureIn;
    double humidityIn;
    double luminanceIn;


    // Dacha
    public DataHomeDto(DefaultSmartSolarmanTuyaService solarmanTuyaService, UsrTcpWiFiParseData usrTcpWiFiParseData, TuyaDeviceService tuyaDeviceService, UsrTcpWiFiService usrTcpWiFiService) {
        InverterDataDacha inverterData = usrTcpWiFiParseData.usrTcpWiFiBatteryRegistry.getInverter(usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterDacha(), InverterDataDacha.class);
        PowerValueRealTimeData powerValueRealTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        if (inverterData != null && inverterData.getLastTime().toEpochMilli() != inverterData.getStartTime().toEpochMilli()){
            this.timestamp = inverterData.getLastTime().toEpochMilli();
            InverterDataDachaAcBatteryBlock106 batteryBlock106 = inverterData.getInverterDataDachaAcBatteryBlock106();
            this.batterySoc = batteryBlock106.getSoc();
            this.batteryVol = batteryBlock106.getBatteryVoltage();
            InverterDataDachaBmsBlock16 bmsBlock16 = inverterData.getInverterDataDachaBmsBlock16();
            this.batteryCurrent = calibrateCurrent(batteryBlock106.getBatteryCurrent(), bmsBlock16.getCurrent());
            this.batteryStatus = resolveBatteryStatus(this.batteryCurrent);

            InverterDataLoadDcBlock80 dcBlock80 = inverterData.getInverterDataLoadDcBlock80();
            this.solarPower = dcBlock80.getTotalDcPowerSumPv();
            InverterDataDachaOutToHomeBlock8 outToHomeBlock8 = inverterData.getInverterDataDachaOutToHomeBlock8();
            this.homePower = outToHomeBlock8.getPowerOutTotal();

            InverterDataDachaDailyTotalBlock118 dailyTotalBlock118 = inverterData.getInverterDataDachaDailyTotalBlock118();
            this.dailyProductionSolarPower = dailyTotalBlock118.getDailyProductionSolarPower();
            this.dailyBatteryCharge = dailyTotalBlock118.getDailyBatteryCharge();
            this.dailyBatteryDischarge = dailyTotalBlock118.getDailyBatteryDischarge();
        }
        else if (powerValueRealTimeData != null && powerValueRealTimeData.getCollectionTime() != null) {
            long ts = powerValueRealTimeData.getCollectionTime() * 1000L;
            long offsetMs = updateTimeStampToUtc(ts, LocationType.DACHA.getZoneId());
            this.timestamp = ts + offsetMs;
            this.batterySoc = powerValueRealTimeData.getBatterySocValue();
            this.batteryVol = powerValueRealTimeData.getBatteryVoltageValue();
            this.batteryCurrent = calibrateCurrent(powerValueRealTimeData.getBatteryCurrentValue(), powerValueRealTimeData.getBmsCurrentValue());
            this.batteryStatus = powerValueRealTimeData.getBatteryStatusValue();
            this.solarPower = powerValueRealTimeData.getTotalProductionSolarPower();
            this.homePower = powerValueRealTimeData.getTotalHomePower();

            this.dailyProductionSolarPower = powerValueRealTimeData.getDailyProductionSolarPower();
            this.dailyBatteryCharge = powerValueRealTimeData.getDailyBatteryCharge();
            this.dailyBatteryDischarge = powerValueRealTimeData.getDailyBatteryDischarge();
        }

        if (powerValueRealTimeData != null && powerValueRealTimeData.getCollectionTime() != null) {
            this.gridPower = powerValueRealTimeData.getTotalGridPower();
            this.dailyGridPower = powerValueRealTimeData.getDailyEnergyBuy();
            this.dailyConsumptionPower = powerValueRealTimeData.getDailyHomeConsumptionPower();
            this.gridVoltageLs.put(1, powerValueRealTimeData.getGridVoltageL1());
            this.gridVoltageLs.put(2, powerValueRealTimeData.getGridVoltageL2());
            this.gridVoltageLs.put(3, powerValueRealTimeData.getGridVoltageL3());
        }

        DataTemperatureDto temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureOutDacha);
        if (temperatureDto != null) {
           this.temperatureOut = temperatureDto.getTemperature();
           this.humidityOut = temperatureDto.getHumidity();
           this.luminanceOut = temperatureDto.getLuminance();
        }
        temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureInDacha);
        if (temperatureDto != null) {
           this.temperatureIn = temperatureDto.getTemperature();
           this.humidityIn = temperatureDto.getHumidity();
           this.luminanceIn = temperatureDto.getLuminance();
        }
        Boolean gridRelayCodeDachaStateOnLine = tuyaDeviceService.getGridRelayCodeDachaStateOnLine();
        if (gridRelayCodeDachaStateOnLine != null) this.gridStatusRealTimeOnLine = gridRelayCodeDachaStateOnLine;
        Boolean gridRelayCodeDachaStateSwitch =  tuyaDeviceService.getGridRelayCodeDachaStateSwitch();
        if (gridRelayCodeDachaStateSwitch!= null) this.gridStatusRealTimeSwitch = gridRelayCodeDachaStateSwitch;
        Map.Entry<Long, Boolean>  lastUpdateTimeGridStatusEntryDacha =  tuyaDeviceService.getLastUpdateTimeGridStatusInfoDacha();
        this.timestampLastUpdateGridStatus = lastUpdateTimeGridStatusEntryDacha != null ? formatTimestamp(lastUpdateTimeGridStatusEntryDacha.getKey(), datePatternGridStatus) : "null";
        if (solarmanTuyaService.getPowerValueRealTimeData() != null) {
            String connectionBatteryStatus = usrTcpWiFiService.calculateStatus(solarmanTuyaService.getPowerValueRealTimeData().getCollectionTime() * 1000, solarmanTuyaService.getTimeoutSecUpdate());
            log.warn("Dacha inverter and battery: is -> [{}]", connectionBatteryStatus);
        }
        log.warn("DataHomeDacha  time long: [{}], time_UTC: [{}] \n - from dto: [{}]", this.timestamp, formatTimestamp(this.timestamp, datePatternGridStatus, UTC), this);
    }

    // Golego
    public DataHomeDto(TuyaDeviceService deviceService, UsrTcpWiFiParseData usrTcpWiFiParseData, UsrTcpWiFiService usrTcpWiFiService) {
        int portDacha = usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterDacha();
        log.warn("Dacha inverter port [{}]: is -> [{}]", portDacha, usrTcpWiFiService.getStatusByPort(portDacha));
        UsrTcpWiFiProperties tcpProps = usrTcpWiFiParseData.getUsrTcpWiFiProperties();
        BatteryDataUsrTcpWiFi batteryDataUsrTcpWiFi = usrTcpWiFiParseData.getBattery(tcpProps.getPortMaster());
        Boolean gridRelayCodeGolegoStateOnLine = deviceService.getGridRelayCodeGolegoStateOnLine();
        if (gridRelayCodeGolegoStateOnLine != null) this.gridStatusRealTimeOnLine = gridRelayCodeGolegoStateOnLine;
        Boolean gridRelayCodeGolegoStateSwitch =  deviceService.getGridRelayCodeGolegoStateSwitch();
        if (gridRelayCodeGolegoStateSwitch != null) this.gridStatusRealTimeSwitch = gridRelayCodeGolegoStateSwitch;
        if (batteryDataUsrTcpWiFi != null) {
            UsrTcpWifiC0Data c0Data = batteryDataUsrTcpWiFi.getC0Data();
            int portStart = tcpProps.getPortStart();
            int batteriesCnt = tcpProps.getBatteriesCnt();
            double batteryCurrentAll = 0;
            double batterySocSum = 0;
            int batteriesActiveCnt = 0;
            List<Integer> batteriesNoActive = new ArrayList<>();
            for (int i = 0; i < batteriesCnt; i++) {
                int port = portStart + i;
                if (port == usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterGolego() ) {
                    log.warn("Golego inverter port [{}]: is -> [{}]", port, usrTcpWiFiService.getStatusByPort(port));
                } else if (port > usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterDacha()) {
                    log.warn("Free Ports [{}]: is -> [{}]", port, usrTcpWiFiService.getStatusByPort(port));
                } else  {
                    BatteryDataUsrTcpWiFi batteryDataUsrTcpWiFiA = usrTcpWiFiParseData.getBattery(port);
                    if (batteryDataUsrTcpWiFiA != null && batteryDataUsrTcpWiFiA.getC0Data() != null) {
                        batteryCurrentAll += batteryDataUsrTcpWiFiA.getC0Data().getCurrentCurA();
                        if (batteryDataUsrTcpWiFiA.getC0Data().getSocPercent() != 0 &&  PortStatus.ACTIVE.name().equals(usrTcpWiFiService.getStatusByPort(port))) {
                            batterySocSum += batteryDataUsrTcpWiFiA.getC0Data().getSocPercent();
                            batteriesActiveCnt++;
                        } else {
                            batteriesNoActive.add(port);
                        }
                    }
                }

            }
            log.warn("Golego battery: BatteriesActivCnt [{}] BatteriesNoActive {}", batteriesActiveCnt, !batteriesNoActive.isEmpty() ? batteriesNoActive : 0);

            if (c0Data.getTimestamp() != null) {
                long offsetMs = updateTimeStampToUtc(c0Data.getTimestamp().toEpochMilli()/1000L, LocationType.GOLEGO.getZoneId());
                this.timestamp = c0Data.getTimestamp().toEpochMilli() + offsetMs;
            }
            this.batterySoc = batteriesActiveCnt == 0 ? 0 : batterySocSum/batteriesActiveCnt;

            // from inverter
            UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry = usrTcpWiFiParseData.getUsrTcpWiFiBatteryRegistry();
            Integer portInverterGolego = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortInverterGolego();
            InverterDataGolego inverterDataGolego = usrTcpWiFiBatteryRegistry.getInverter(portInverterGolego, InverterDataGolego.class);
            if (inverterDataGolego != null && inverterDataGolego.getInverterGolegoData90() != null && inverterDataGolego.getInverterGolegoData90().getHexMap().length > 0) {
                InverterGolegoData90 inverterGolegoData90 = inverterDataGolego.getInverterGolegoData90();
                this.batteryStatus = inverterGolegoData90.getStatus();
                this.batteryVol = inverterGolegoData90.getBatteryVoltage();
                this.batteryCurrent = inverterGolegoData90.getBatteryCurrent();
                this.homePower = inverterGolegoData90.getLoadOutputActivePower();
                this.gridVoltageLs.put(1, inverterGolegoData90.getAcInputVoltage());
            } else {
                this.batteryStatus = c0Data.getBmsStatusStr();
                this.batteryVol = c0Data.getVoltageCurV();
                this.batteryCurrent = Math.round(batteryCurrentAll * 100.0) / 100.0;
                 if (this.batteryCurrent == 0 && this.gridPower == 0) {
                    this.homePower = 0;
                } else if (this.batteryCurrent < 0) {
                    this.homePower = (this.batteryVol * Math.abs(this.batteryCurrent)) - golegoInverterPowerDefault;
                } else {
                    this.homePower = this.golegoPowerDefault;
                }
            }
            if (this.gridStatusRealTimeOnLine && this.gridStatusRealTimeSwitch) {
                this.gridPower = this.batteryVol * this.batteryCurrent + this.homePower + golegoInverterPowerDefault;
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
        log.warn("DataHomeGolego  time long: [{}], time_UTC: [{}] \n - from dto: [{}]", this.timestamp, formatTimestamp(this.timestamp, datePatternGridStatus, UTC), this);
    }


    public static synchronized List<DataAnalyticDto> updateTimeStampToUtc(List<DataAnalyticDto> incomingLocalPoints) {
        if (incomingLocalPoints == null) return new ArrayList<>();
        for (DataAnalyticDto p : incomingLocalPoints) {
            if (p.getTimestamp() != 0) {
                long offsetMs = updateTimeStampToUtc(p.getTimestamp(), p.getLocation().getZoneId());
                p.setTimestamp(p.getTimestamp() + offsetMs);
            }
        }
        return incomingLocalPoints;
    }

    public static synchronized long updateTimeStampToUtc(long timestamp, ZoneId zoneId) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zdt = instant.atZone(zoneId);
        return zdt.getOffset().getTotalSeconds() * 1000L;
    }

    public static double calibrateCurrent(double sensorValue, double directionSource) {
        double absoluteValue = Math.abs(sensorValue);
        // 1. Фільтр "шуму" (мертва зона)
        // Якщо струм менше 0.1A, вважаємо, що це похибка датчика
        if (absoluteValue < 0.1) {
            return 0.0;
        }
        // 2. Визначення знака за джерелом напрямку (BMS)
        // Якщо BMS чітко показує від'ємне значення — повертаємо розряд
        if (directionSource < -0.01) {
            return -absoluteValue;
        }

        // В усіх інших випадках — заряд (позитивне значення)
        return absoluteValue;
    }

    /**
     * Визначає текстовий статус батареї на основі відкаліброваного струму.
     * Використовує існуючий enum BatteryStatus для отримання назв типів.
     */
    private String resolveBatteryStatus(double current) {
        if (current > 0.05) {
            return BatteryStatus.CHARGING_60.getType(); // "Charging"
        } else if (current < -0.05) {
            return BatteryStatus.DISCHARGING.getType(); // "Discharging"
        } else {
            return BatteryStatus.STATIC.getType();      // "Static"
        }
    }
}
