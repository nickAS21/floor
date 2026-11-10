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
import org.nickas21.smart.usr.entity.dacha.InverterDataDachaDailyTotalBlock118;
import org.nickas21.smart.usr.entity.dacha.InverterDataDachaOutToHomeBlock8;
import org.nickas21.smart.usr.entity.golego.BatteryDataUsrTcpWiFi;
import org.nickas21.smart.usr.entity.golego.InverterDataGolego;
import org.nickas21.smart.usr.entity.golego.InverterGolegoData90;
import org.nickas21.smart.usr.entity.golego.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry;
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

    private final double golegoHomePowerDefault = 42.0; // only  2 - WiFi routers
    public static final double golegoInverterPowerDefault = 10.0;
    public static final String datePatternGridStatus = "yyyy-MM-dd HH:mm";
    private static final long APP_START_TIME = System.currentTimeMillis();

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
    SolarPanelInfoDtos panelInfoDtos = null;
    double homePower;
    double gridPower;
    boolean gridStatusRealTimeOnLine;
    boolean gridStatusRealTimeSwitch;
    Map<Integer, Double> gridVoltageLs = new ConcurrentHashMap<>();

    double dailyConsumptionPower;
    double dailyGridPower;
    double dailyGridDayPower;
    double dailyGridNightPower;
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
        PowerValueRealTimeData powerValueRealTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        if (powerValueRealTimeData != null && powerValueRealTimeData.getCollectionTime() != null) {
            long ts = powerValueRealTimeData.getCollectionTime() * 1000L;
            long offsetMs = updateTimeStampToUtc(ts, LocationType.DACHA.getZoneId());
            this.timestamp = ts + offsetMs;
            this.batterySoc = powerValueRealTimeData.getBatterySocValue();
            this.batteryVol = powerValueRealTimeData.getBatteryVoltageValue();
            this.batteryCurrent = calibrateCurrent(powerValueRealTimeData.getBatteryCurrentValue(), powerValueRealTimeData.getBmsCurrentValue());
            this.batteryStatus = powerValueRealTimeData.getBatteryStatusValue();
            this.solarPower = powerValueRealTimeData.getTotalProductionSolarPower();
            this.panelInfoDtos =  new SolarPanelInfoDtos(powerValueRealTimeData);
            this.homePower = powerValueRealTimeData.getTotalHomePower();

            this.dailyProductionSolarPower = powerValueRealTimeData.getDailyProductionSolarPower();
            this.dailyBatteryCharge = powerValueRealTimeData.getDailyBatteryCharge();
            this.dailyBatteryDischarge = powerValueRealTimeData.getDailyBatteryDischarge();

            this.gridPower = powerValueRealTimeData.getTotalGridPower();
            this.dailyGridPower = powerValueRealTimeData.getDailyEnergyBuy();
            this.dailyConsumptionPower = this.dailyProductionSolarPower + this.dailyGridPower - this.dailyBatteryCharge + this.dailyBatteryDischarge;
            this.gridVoltageLs.put(1, powerValueRealTimeData.getGridVoltageL1());
            this.gridVoltageLs.put(2, powerValueRealTimeData.getGridVoltageL2());
            this.gridVoltageLs.put(3, powerValueRealTimeData.getGridVoltageL3());
        } else {
            UsrTcpWiFiProperties props = usrTcpWiFiParseData.usrTcpWiFiProperties;
            List<Integer> portsDacha = props.getAllPortsInverterDacha();
            boolean allPortsActive = portsDacha.stream()
                    .map(usrTcpWiFiService::getStatusByPort)
                    .allMatch(status -> PortStatus.ACTIVE.name().equalsIgnoreCase(status));
            InverterDataDacha inverterMasterData = null;
            if (allPortsActive)
                inverterMasterData = usrTcpWiFiParseData.usrTcpWiFiBatteryRegistry.getInverter(props.getPortInverterDacha(), InverterDataDacha.class);
            if (inverterMasterData != null && inverterMasterData.getLastTime().toEpochMilli() != inverterMasterData.getStartTime().toEpochMilli()) {
                long rawTs = inverterMasterData.getLastTime().toEpochMilli();
                long offsetMs = updateTimeStampToUtc(rawTs, LocationType.DACHA.getZoneId());
                this.timestamp = rawTs + offsetMs;
                // 1. Блок Акумулятора (BMS) — строго з Мастера (8900)
                InverterDataDachaAcBatteryBlock106 batteryBlock106 = inverterMasterData.getInverterDataDachaAcBatteryBlock106();
                if (batteryBlock106 != null) {
                    this.batterySoc = batteryBlock106.getSoc();
                    this.batteryVol = batteryBlock106.getBatteryVoltage();
//            InverterDataDachaBmsBlock16 bmsBlock16 = inverterMasterData.getInverterDataDachaBmsBlock16();
//            this.batteryCurrent = calibrateCurrent(batteryBlock106.getBatteryCurrent(), bmsBlock16.getCurrent());
                    this.batteryCurrent = batteryBlock106.getBatteryCurrent();
                    this.batteryStatus = resolveBatteryStatus(this.batteryCurrent);
                }
                // 2. Блок Дому (Load) — строго з Мастера
                InverterDataDachaOutToHomeBlock8 outToHomeBlock8 = inverterMasterData.getInverterDataDachaOutToHomeBlock8();
                this.homePower = outToHomeBlock8.getPowerOutTotal();
                // 3. Подобові лічильники Акумулятора — строго з Мастера
                InverterDataDachaDailyTotalBlock118 dailyTotalBlock118 = inverterMasterData.getInverterDataDachaDailyTotalBlock118();
                this.dailyBatteryCharge = dailyTotalBlock118.getDailyBatteryCharge();
                this.dailyBatteryDischarge = dailyTotalBlock118.getDailyBatteryDischarge();
                // 4. --- СУМУВАННЯ СОНЦЯ З УСІХ ЛОКАЛЬНИХ ЛОГЕРІВ ---
                int totalSolarPowerSum = 0;
                double dailyProductionSolarSum = 0.0;
                for (Integer port : portsDacha) {
                    InverterDataDacha inv = usrTcpWiFiParseData.usrTcpWiFiBatteryRegistry.getInverter(port, InverterDataDacha.class);
                    if (inv != null) {
                        if (inv.getInverterDataLoadDcBlock80() != null) {
                            totalSolarPowerSum += inv.getInverterDataLoadDcBlock80().getTotalDcPowerSumPv();
//                            inv.getInverterDataLoadDcBlock80().getDcPowerPv1();
//                            inv.getInverterDataLoadDcBlock80().getDcPowerPv2();
                        }
                        if (inv.getInverterDataDachaDailyTotalBlock118() != null) {
                            dailyProductionSolarSum += inv.getInverterDataDachaDailyTotalBlock118().getDailyProductionSolarPower();
                        }
                    }
                }
                // Записуємо фінальні сумовані значення по сонцю
                this.solarPower = totalSolarPowerSum;
                this.dailyProductionSolarPower = dailyProductionSolarSum;
            }
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

        Boolean gridRelayCodeDachaStateSwitch = tuyaDeviceService.getGridRelayCodeDachaStateSwitch();
        if (gridRelayCodeDachaStateSwitch != null) this.gridStatusRealTimeSwitch = gridRelayCodeDachaStateSwitch;

        Map.Entry<Long, Boolean> lastUpdateTimeGridStatusEntryDacha = tuyaDeviceService.getLastUpdateTimeGridStatusInfoDacha();
        if (lastUpdateTimeGridStatusEntryDacha != null && lastUpdateTimeGridStatusEntryDacha.getKey() != null) {
            this.timestampLastUpdateGridStatus = formatTimestamp(lastUpdateTimeGridStatusEntryDacha.getKey(), datePatternGridStatus);
            this.gridStatusRealTimeSwitch = Boolean.TRUE.equals(lastUpdateTimeGridStatusEntryDacha.getValue());
        } else {
            this.timestampLastUpdateGridStatus = formatTimestamp(System.currentTimeMillis(), datePatternGridStatus);
        }
        PowerValueRealTimeData realTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        if (realTimeData != null && realTimeData.getCollectionTime() != null) {
            String connectionBatteryStatus = usrTcpWiFiService.calculateStatus(realTimeData.getCollectionTime() * 1000, solarmanTuyaService.getTimeoutSecUpdate());
            log.warn("Dacha inverter and battery: is -> [{}]", connectionBatteryStatus);
        }
        log.warn("DataHomeDacha  time long: [{}], time_UTC: [{}] \n - from dto: [{}]", this.timestamp, formatTimestamp(this.timestamp, datePatternGridStatus, UTC), this);
    }

    // Golego Constructor
    public DataHomeDto(TuyaDeviceService deviceService, UsrTcpWiFiParseData usrTcpWiFiParseData, TuyaDeviceService tuyaDeviceService, UsrTcpWiFiService usrTcpWiFiService) {
        UsrTcpWiFiProperties tcpProps = usrTcpWiFiParseData.getUsrTcpWiFiProperties();
        Integer portInverterGolego = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortInverterGolego();
        Integer portBatMasterGolego = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterGolego();
        UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry = usrTcpWiFiParseData.getUsrTcpWiFiBatteryRegistry();

        // розрахунок SOC / Current / Voltage from battery
        List<Integer> batteriesActive = new ArrayList<>();
        double batteryCurrentAll = 0;
        double batterySocAll = 0;
        for (Integer port : tcpProps.getAllPortsBatGolego()) {
            BatteryDataUsrTcpWiFi batteryData = usrTcpWiFiParseData.getBattery(port);
            if (batteryData == null) continue;
            UsrTcpWifiC0Data c0Data = batteryData.getC0Data();
            UsrTcpWifiRS485_Data_GOOTO_Telemetry dataGOOTOTelemetry = batteryData.getRs485_Data_GOOTO_Telemetry();
            // Збір telemetry && Active
            if (PortStatus.ACTIVE.name().equals(usrTcpWiFiService.getStatusByPort(port))) {
                if (c0Data != null && c0Data.getTimestamp() != null && c0Data.getSocPercent() > 0) {
                    batterySocAll += c0Data.getSocPercent();
                    batteryCurrentAll += c0Data.getCurrentCurA();
                    if (portBatMasterGolego.equals(port)) {
                        this.batteryVol = c0Data.getVoltageCurV();
                    } else if (this.batteryVol == 0) {
                        this.batteryVol = c0Data.getVoltageCurV();
                    }
                    long offsetMs = updateTimeStampToUtc(c0Data.getTimestamp().toEpochMilli(), LocationType.GOLEGO.getZoneId());
                    this.timestamp = c0Data.getTimestamp().toEpochMilli() + offsetMs;
                    this.batteryStatus = c0Data.getBmsStatusStr();
                    batteriesActive.add(port);
                } else if (dataGOOTOTelemetry != null && dataGOOTOTelemetry.getTimestamp() != null && dataGOOTOTelemetry.getSocPercent() > 0) {
                    batterySocAll += dataGOOTOTelemetry.getSocPercent();
                    batteryCurrentAll += dataGOOTOTelemetry.getCurrentCurA();
                    if (portBatMasterGolego.equals(port)) {
                        this.batteryVol = dataGOOTOTelemetry.getVoltageCurV();
                    } else if (this.batteryVol == 0) {
                        this.batteryVol = dataGOOTOTelemetry.getVoltageCurV();
                    }
                    long offsetDataGOOTOTelemetry = updateTimeStampToUtc(dataGOOTOTelemetry.getTimestamp().toEpochMilli(), LocationType.GOLEGO.getZoneId());
                    this.timestamp = dataGOOTOTelemetry.getTimestamp().toEpochMilli() + offsetDataGOOTOTelemetry;
                    this.batteryStatus = dataGOOTOTelemetry.getBmsStatusStr();
                    batteriesActive.add(port);
                }
            }
        }
        int batteriesActiveCnt = batteriesActive.size();
        this.batterySoc = batteriesActiveCnt == 0 ? 0 : batterySocAll / batteriesActiveCnt;
        this.batteryCurrent = Math.round(batteryCurrentAll * 100.0) / 100.0;
        this.batteryStatus = resolveBatteryStatus(this.batteryCurrent);

        // Логуємо стан портів батарей Golego
        List<Integer> batteriesNoActive = tcpProps.getAllPortsBatGolego().stream()
                .filter(port -> !batteriesActive.contains(port))
                .toList();
        log.warn("Golego battery: BatteriesActivCnt [{}] BatteriesNoActive {}", batteriesActiveCnt, !batteriesNoActive.isEmpty() ? batteriesNoActive : 0);

        // 1. Спочатку зчитуємо стан реле мережі Golego
        Boolean gridRelayCodeGolegoStateOnLine = deviceService.getGridRelayCodeGolegoStateOnLine();
        if (gridRelayCodeGolegoStateOnLine != null) this.gridStatusRealTimeOnLine = gridRelayCodeGolegoStateOnLine;

        Boolean gridRelayCodeGolegoStateSwitch = deviceService.getGridRelayCodeGolegoStateSwitch();
        if (gridRelayCodeGolegoStateSwitch != null) this.gridStatusRealTimeSwitch = gridRelayCodeGolegoStateSwitch;

        Map.Entry<Long, Boolean> lastUpdateTimeGridStatusEntryHome = deviceService.getLastUpdateTimeGridStatusInfoGolego();

        long statusTs;
        if (lastUpdateTimeGridStatusEntryHome != null && lastUpdateTimeGridStatusEntryHome.getKey() != null) {
            statusTs = lastUpdateTimeGridStatusEntryHome.getKey();
            this.gridStatusRealTimeSwitch = Boolean.TRUE.equals(lastUpdateTimeGridStatusEntryHome.getValue());
        } else {
            statusTs = System.currentTimeMillis();
            // Якщо подій ще не було — вважаємо реле увімкненим за поточним станом switch
            if (gridRelayCodeGolegoStateSwitch != null) {
                this.gridStatusRealTimeSwitch = gridRelayCodeGolegoStateSwitch;
            }
        }

        // Формуємо статус: якщо тумблер увімкнений — строго "On", інакше "Off"
        String currentStatusStr = this.gridStatusRealTimeSwitch ? "On" : "Off";

        // Записуємо дату та статус через \n, щоб UI відмалював слово "On" під часом
        this.timestampLastUpdateGridStatus = formatTimestamp(statusTs, datePatternGridStatus) + "\n" + currentStatusStr;

        boolean isGridRelayActive = this.gridStatusRealTimeOnLine && this.gridStatusRealTimeSwitch;

        // 2. from inverter
        boolean hasBatteryData = batteriesActiveCnt > 0 && this.batteryVol > 0;

        InverterDataGolego inverterDataGolego = usrTcpWiFiBatteryRegistry.getInverter(portInverterGolego, InverterDataGolego.class);
        if (inverterDataGolego != null) {
            InverterGolegoData90 inverterGolegoData90 = inverterDataGolego.getInverterGolegoData90();
            log.debug("Golego Inverter: entity90 {}; : entity32 {}; ", inverterGolegoData90, inverterDataGolego.getInverterGolegoData32());

            if (inverterGolegoData90 != null) {
                // Потужність навантаження з інвертора або фолбек
                if (inverterGolegoData90.getLoadOutputActivePower() != null) {
                    this.homePower = inverterGolegoData90.getLoadOutputActivePower();
                } else {
                    applyHomePowerGolego();
                }

                // Вхідна напруга та потужність мережі залежно від реле
                Double acVoltage = inverterGolegoData90.getAcInputVoltage();
                if (isGridRelayActive && acVoltage != null && acVoltage > 0) {
                    this.gridVoltageLs.put(1, acVoltage);

                    double chargePower = (inverterGolegoData90.getBatteryChargingCurrent() != null && inverterGolegoData90.getBatteryVoltage() != null)
                            ? inverterGolegoData90.getBatteryChargingCurrent() * inverterGolegoData90.getBatteryVoltage()
                            : 0.0;

                    this.gridPower = this.homePower + chargePower + golegoInverterPowerDefault;
                } else {
                    this.gridVoltageLs.put(1, 0.0);
                    this.gridPower = 0.0;
                }

                // Якщо BMS НЕ ДАЛА даних — беремо все з інвертора як фолбек
                if (!hasBatteryData) {
                    this.batteryStatus = inverterGolegoData90.getStatus();
                    this.batteryVol = inverterGolegoData90.getBatteryVoltage();
                    this.batteryCurrent = inverterGolegoData90.getBatteryCurrent();
                    if (this.batterySoc == 0 && inverterGolegoData90.getSoc() > 0) {
                        this.batterySoc = inverterGolegoData90.getSoc();
                    }
                }
            } else {
                applyHomePowerGolego();
                this.gridVoltageLs.put(1, 0.0);
                this.gridPower = 0.0;
            }
        } else {
            applyHomePowerGolego();
            this.gridVoltageLs.put(1, 0.0);
            this.gridPower = 0.0;
        }

        if (this.batteryStatus != null && this.batterySoc == 0 && this.batteryVol > 0) {
            this.batterySoc = calculateSocByVoltage(this.batteryVol);
        }

        DataTemperatureDto temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureOutGolego);
        if (temperatureDto != null) {
            this.temperatureOut = temperatureDto.getTemperature();
            this.humidityOut = temperatureDto.getHumidity();
            this.luminanceOut = temperatureDto.getLuminance();
        }
        temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureInGolego);
        if (temperatureDto != null) {
            this.temperatureIn = temperatureDto.getTemperature();
            this.humidityIn = temperatureDto.getHumidity();
            this.luminanceIn = temperatureDto.getLuminance();
        }

        this.solarPower = 0;
        this.dailyConsumptionPower = 0;
        this.dailyGridPower = 0;
        this.dailyBatteryCharge = 0;
        this.dailyBatteryDischarge = 0;
        this.dailyProductionSolarPower = 0;

        if (this.timestamp == 0) {
            this.timestamp = System.currentTimeMillis();
            long offsetMs = updateTimeStampToUtc(this.timestamp, LocationType.GOLEGO.getZoneId());
            this.timestamp += offsetMs;
        }
        log.warn("DataHomeGolego  time long: [{}], time_UTC: [{}] \n - from dto: [{}]", this.timestamp, formatTimestamp(this.timestamp, datePatternGridStatus, UTC), this);
    }

    /**
     * Розрахунок SOC (%) на основі напруги (V) для 48V збірки (16S).
     * @param batteryVol вхідна напруга в вольтах
     * @return State of Charge (SOC) від 0.0 до 100.0
     */
    public static double calculateSocByVoltage(Double batteryVol) {
        if (batteryVol == null) return 0.0;
        if (batteryVol >= 58.4) return 100.0;
        if (batteryVol <= 40.0) return 0.0;

        // Працюємо виключно з об'єктом Integer
        Integer mv = (int) (batteryVol * 1000);

        return switch (mv) {
            // Чистий об'єктний тип Integer + Guard (when)
            case Integer v when v >= 54000 -> {
                double k = (100.0 - 99.0) / (58.4 - 54.0);
                yield 99.0 + k * (batteryVol - 54.0);
            }

            // Чистий об'єктний тип Integer + Guard (when)
            case Integer v when v >= 50400 -> {
                double k = (99.0 - 14.0) / (54.0 - 50.4);
                yield 14.0 + k * (batteryVol - 50.4);
            }

            // Обов'язковий дефолт для обробки решти значень Integer та null-безпеки
            default -> {
                double k = (14.0 - 0.0) / (50.4 - 40.0);
                yield 0.0 + k * (batteryVol - 40.0);
            }
        };
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

    /**
     * Встановлює споживання будинку для Golego.
     * Якщо є дані з інвертора — береться LoadOutputActivePower.
     * Якщо інвертор офлайн — береться розрахунок по розряду батареї або чергове навантаження.
     */
//    private void applyHomePowerGolego(InverterGolegoData90 inverterGolegoData90) {
//        if (inverterGolegoData90 != null && inverterGolegoData90.getLoadOutputActivePower() != null) {
//            this.homePower = inverterGolegoData90.getLoadOutputActivePower();
//        } else if (this.batteryCurrent < -0.1 && this.batteryVol > 0) {
//            // Батарея розряджається: потужність розряду мінус споживання інвертора
//            double dischargePower = (this.batteryVol * Math.abs(this.batteryCurrent)) - golegoInverterPowerDefault;
//            this.homePower = Math.max(0.0, dischargePower);
//        } else {
//            // Батарея заряджається або в спокої за відсутності інвертора: чергове живлення роутерів
//            this.homePower = this.golegoHomePowerDefault;
//        }
//    }

//    /**
//     * Ініціалізує статус та час реле мережі для Golego.
//     * Усуває розбіжність між відображенням на UI та розрахунком потужності.
//     */
//    private void resolveGridRelayStatusGolego(TuyaDeviceService deviceService) {
//        Boolean online = deviceService.getGridRelayCodeGolegoStateOnLine();
//        this.gridStatusRealTimeOnLine = Boolean.TRUE.equals(online);
//
//        Map.Entry<Long, Boolean> lastUpdateEntry = deviceService.getLastUpdateTimeGridStatusInfoGolego();
//        if (lastUpdateEntry != null) {
//            this.gridStatusRealTimeSwitch = Boolean.TRUE.equals(lastUpdateEntry.getValue());
//            this.timestampLastUpdateGridStatus = formatTimestamp(lastUpdateEntry.getKey(), datePatternGridStatus);
//        } else {
//            Boolean switchState = deviceService.getGridRelayCodeGolegoStateSwitch();
//            this.gridStatusRealTimeSwitch = Boolean.TRUE.equals(switchState);
//            this.timestampLastUpdateGridStatus = "null";
//        }
//    }
//
//    /**
//     * Розраховує напругу та потужність мережі на основі стану реле та даних інвертора.
//     */
//    private void resolveGridPowerGolego(InverterGolegoData90 inverterData90, boolean isGridRelayActive) {
//        if (inverterData90 == null) {
//            this.gridVoltageLs.put(1, 0.0);
//            this.gridPower = 0.0;
//            return;
//        }
//
//        Double acVoltage = inverterData90.getAcInputVoltage();
//        if (isGridRelayActive && acVoltage != null && acVoltage > 0) {
//            this.gridVoltageLs.put(1, acVoltage);
//
//            double chargePower = (inverterData90.getBatteryChargingCurrent() != null && inverterData90.getBatteryVoltage() != null)
//                    ? inverterData90.getBatteryChargingCurrent() * inverterData90.getBatteryVoltage()
//                    : 0.0;
//
//            // gridPower = навантаження будинку + потужність заряду акумулятора + власне споживання інвертора
//            this.gridPower = this.homePower + chargePower + golegoInverterPowerDefault;
//        } else {
//            this.gridVoltageLs.put(1, 0.0);
//            this.gridPower = 0.0;
//        }
//    }

    private void applyHomePowerGolego() {
        if (this.batteryCurrent < -0.1 && this.batteryVol > 0) {
            double dischargePower = (this.batteryVol * Math.abs(this.batteryCurrent)) - golegoInverterPowerDefault;
            this.homePower = Math.max(0.0, dischargePower);
        } else {
            this.homePower = this.golegoHomePowerDefault;
        }
    }
}
