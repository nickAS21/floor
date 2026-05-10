package org.nickas21.smart;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.BatteryStatus;
import org.nickas21.smart.solarman.Seasons;
import org.nickas21.smart.solarman.SolarmanDevice;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.solarman.api.RealTimeData;
import org.nickas21.smart.solarman.api.RealTimeDataValue;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.golego.UsrTcpWiFiBmsSummary;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.nickas21.smart.util.DynamicScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.nickas21.smart.util.HttpUtil.batteryCurrentKey;
import static org.nickas21.smart.util.HttpUtil.batteryPowerKey;
import static org.nickas21.smart.util.HttpUtil.batterySocKey;
import static org.nickas21.smart.util.HttpUtil.batteryStatusKey;
import static org.nickas21.smart.util.HttpUtil.batteryVoltageKey;
import static org.nickas21.smart.util.HttpUtil.bmsCurrentKey;
import static org.nickas21.smart.util.HttpUtil.bmsSocKey;
import static org.nickas21.smart.util.HttpUtil.bmsTempKey;
import static org.nickas21.smart.util.HttpUtil.bmsVoltageKey;
import static org.nickas21.smart.util.HttpUtil.dailyBatteryChargeKey;
import static org.nickas21.smart.util.HttpUtil.dailyBatteryDischargeKey;
import static org.nickas21.smart.util.HttpUtil.dailyEnergyBuyKey;
import static org.nickas21.smart.util.HttpUtil.dailyEnergySellKey;
import static org.nickas21.smart.util.HttpUtil.getSunRiseSunset;
import static org.nickas21.smart.util.HttpUtil.gridRelayStatusKey;
import static org.nickas21.smart.util.HttpUtil.gridStatusKey;
import static org.nickas21.smart.util.HttpUtil.gridVoltageL1Key;
import static org.nickas21.smart.util.HttpUtil.gridVoltageL2Key;
import static org.nickas21.smart.util.HttpUtil.gridVoltageL3Key;
import static org.nickas21.smart.util.HttpUtil.homeDailyConsumptionPowerKey;
import static org.nickas21.smart.util.HttpUtil.invHMIKey;
import static org.nickas21.smart.util.HttpUtil.invMAINKey;
import static org.nickas21.smart.util.HttpUtil.invProtocolVerKey;
import static org.nickas21.smart.util.HttpUtil.invTempKey;
import static org.nickas21.smart.util.HttpUtil.productionDailySolarPowerKey;
import static org.nickas21.smart.util.HttpUtil.toLocaleDateString;
import static org.nickas21.smart.util.HttpUtil.toLocaleTimeString;
import static org.nickas21.smart.util.HttpUtil.totalEnergyBuyKey;
import static org.nickas21.smart.util.HttpUtil.totalEnergySellKey;
import static org.nickas21.smart.util.HttpUtil.totalGridPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalHomeConsumptionPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalProductionSolarPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalSolarPowerKey;
import static org.nickas21.smart.util.StringUtils.printMsgProgressBar;
import static org.nickas21.smart.util.StringUtils.stopProgressBar;
import static org.nickas21.smart.util.StringUtils.stopThread;

@Slf4j
@Service
public class DefaultSmartSolarmanTuyaService implements SmartSolarmanTuyaService {
    private double batterySocCur;
    private double stationConsumptionPower;
    @Getter
    private PowerValueRealTimeData powerValueRealTimeData;
    private boolean isDay;
    private boolean isDayPrevious;
    private boolean isUpdateToMinAfterIsDayFalse;
    private Instant curDate;
    private Long sunRiseDate;
    private Long sunSetDate;
    private Long sunRiseMax;
    private Long sunSetMin;
    @Getter
    private Long timeoutSecUpdate = 280L;   // 4 min
    private double batSocMinInMilliSec; // %
    private int freePowerCorrectMinMax;
    private int freePowerCorrectCnt;

    private Thread progressBarThread;

    private DynamicScheduler scheduler;

    @Value("${app.version:unknown}")
    @Getter
    private String version;

    private UsrTcpWiFiProperties tcpProps;
    private final SolarmanStationsService solarmanStationsService;
    private final TuyaDeviceService tuyaDeviceService;

    @Autowired
    @Lazy
    UsrTcpWiFiParseData usrTcpWiFiParseData;

    public DefaultSmartSolarmanTuyaService(SolarmanStationsService solarmanStationsService, TuyaDeviceService tuyaDeviceService) {
        this.solarmanStationsService = solarmanStationsService;
        this.tuyaDeviceService = tuyaDeviceService;
    }

    @PostConstruct
    public void init() {
        this.tcpProps = usrTcpWiFiParseData.getUsrTcpWiFiProperties();
    }

    @Override
    public void solarmanRealTimeDataStart() {
        this.batterySocCur = 0;
        this.stationConsumptionPower = solarmanStationsService.getSolarmanStation().getStationConsumptionPower();
        this.powerValueRealTimeData = PowerValueRealTimeData.builder().build();
        initUpdateTimeoutSheduler();
    }

    public void setBmsSocCur() {
        try {
            updatePowerValue();
            UsrTcpWiFiBmsSummary usrBmsSummary = usrTcpWiFiParseData.getBmsSummary(tcpProps.getPortMaster());
            double batCurNew = powerValueRealTimeData.getBatteryCurrentValue();
            double batVolNew = powerValueRealTimeData.getBatteryVoltageValue();
            double bmsVolNew = powerValueRealTimeData.getBmsVoltageValue();
            double bmsCurNew = powerValueRealTimeData.getBmsCurrentValue();
            double bmsTempNew = powerValueRealTimeData.getBmsTempValue();
            double batterySocNew = powerValueRealTimeData.getBatterySocValue();
            double batterySocUsr = usrBmsSummary == null ? -1: usrBmsSummary.socPercent();
            double batterySocMin = getBatSocMin();
            double batteryPowerNew = powerValueRealTimeData.getBatteryPowerValue();
            String batteryStatusNew = powerValueRealTimeData.getBatteryStatusValue();

            updateSunRiseSunSetDate();
            setIsDay();

            Instant curInst = Instant.now();
            String curInstStr = toLocaleTimeString(curInst);
            if (this.batterySocCur == 0) {
                log.info("""
                            Inverter version info: Protocol Version: [{}], -MAIN: [{}], -HMI: [{}].""",
                        powerValueRealTimeData.getInverterProtocolVersionValue(),
                        powerValueRealTimeData.getInverterMAINValue(),
                        powerValueRealTimeData.getInverterHMIValue());
                String msgProgressBar = "Start: " + curInstStr + ". Init parameters to TempSetMin: " + toLocaleTimeString(Instant.ofEpochMilli(curInst.toEpochMilli() + this.timeoutSecUpdate * 1000)) + ",  after [" + this.timeoutSecUpdate / 60 + "] min: ";
                this.setProgressBarThread(msgProgressBar);
                tuyaDeviceService.updateMessageAlarmToTelegram(null);
            } else {
                initUpdateTimeoutSheduler();
                this.tuyaDeviceService.updateGridStateOnLineToTelegram();
                this.tuyaDeviceService.updateOnOfSwitchRelay(batterySocNew, batterySocUsr, powerValueRealTimeData.getTotalGridPower());
            }

            log.info("""
                            \nCurrent data:
                            Current Dacha real time data: [{}], -Update Dacha real time data: [{}],
                            -batSocLast: [{} %], -batSocNew: [{} %], -deltaBmsSoc: [{} %], -batterySocMin: [{} %],
                            -batteryStatus: [{}], -batteryPower: [{} W], -batVolNew: [{} V], -batCurrentNew: [{} A],  -bmsVolNew: [{} V], -bmsCurrentNew: [{} A], -BMS Temperature: [{}  grad C]
                            -solarPower: [{} W], consumptionPower: [{} W], stationPower: [{} W],
                            -batteryDailyCharge: [{} kWh], -batteryDailyDischarge: [{} kWh],
                            -relayStatus: [{}], -gridStatusSolarman: [{}], -gridDachaStatusRealTime: [{}], -dailyBuy:[{} kWh], -dailySell: [{} kWh],
                            -AC (inverter) Temperature:  [{} grad C].
                            - usrBmsSummary:
                            -- Update Golego real time data: [{}],
                            -- batSocLast: [{} %],
                            -- gridGolegoStatusRealTime: [{}],
                             {}
                       """,
                    curInstStr,
                    toLocaleTimeString(powerValueRealTimeData.getCollectionTime() * 1000),

                    this.batterySocCur, batterySocNew,
                    String.format("%.2f", (batterySocNew - this.batterySocCur)),
                    String.format("%.2f", batterySocMin),

                    batteryStatusNew, batteryPowerNew,
                    batVolNew, batCurNew,
                    bmsVolNew, bmsCurNew,
                    bmsTempNew,

                    powerValueRealTimeData.getTotalProductionSolarPower(),
                    powerValueRealTimeData.getDailyHomeConsumptionPower(),
                    stationConsumptionPower,

                    powerValueRealTimeData.getDailyBatteryCharge(),
                    powerValueRealTimeData.getDailyBatteryDischarge(),

                    powerValueRealTimeData.getGridStatusRelay(),
                    powerValueRealTimeData.getGridStatusSolarman(),
                    tuyaDeviceService.getGridRelayCodeDachaStateOnLine(),
                    powerValueRealTimeData.getDailyEnergyBuy(),
                    powerValueRealTimeData.getDailyEnergySell(),
                    powerValueRealTimeData.getInverterTempValue(),
                    usrBmsSummary == null ? "null" : toLocaleTimeString(usrBmsSummary.timestamp().toEpochMilli()),
                    usrBmsSummary == null ? 0 : usrBmsSummary.socPercent(),
                    tuyaDeviceService.getGridRelayCodeGolegoStateOnLine(),
                    usrBmsSummary == null ? "null" : usrBmsSummary.bmsSummary());
            tuyaDeviceService.sendDachaGolegoBatteryChargeRemaining(batVolNew, batCurNew, bmsVolNew, bmsCurNew, bmsTempNew,
                    batterySocNew, batteryPowerNew, batteryStatusNew, usrBmsSummary);
            if (isDay) {
                isUpdateToMinAfterIsDayFalse = false;
                if (this.batterySocCur > 0) {
                    try {
                        if (tuyaDeviceService.getDevices() != null && tuyaDeviceService.getDevices().getDevIds() != null) {
                            boolean isCharge = getIsCharge(batterySocNew);
                            int freePowerCorrect = getFreePowerCorrect(batterySocNew, isCharge);
                            String infoActionDop;
                            String infoAction;
                            if (!isDayPrevious) {
                                tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                                infoAction = "After the night...";
                                infoActionDop = "TempSetMin";
                                isDayPrevious = isDay;
                            } else if (this.solarmanStationsService.getSolarmanStation().getSeasonsId() == Seasons.WINTER.getSeasonsId() || this.tuyaDeviceService.getGridRelayCodeDachaStateSwitch()) {
                                infoAction = "Is run GridRelayDachaStateSwitch: ";
                                infoActionDop = "Update TempSet from UpdateOnOfSwitchRelay";
                            } else {
                                if (batterySocNew < batterySocMin) {
                                    // Reducing electricity consumption
                                    this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                                    infoAction = "Reducing";
                                    infoActionDop = "TempSetMin";
                                } else {
                                    // Battery charge/discharge analysis program
                                    this.batteryChargeDischarge(isCharge, freePowerCorrect);
                                    infoAction = "Change";
                                    infoActionDop = "TempSet_Min/Max";
                                }
                            }
                            log.info("{} electricity consumption, battery batterySocNew [{}], battery StatusCorrect [{}], battery StatusFact [{}], freePower [{}],  action [{}].",
                                    infoAction,
                                    batterySocNew,
                                    isCharge,
                                    powerValueRealTimeData.getBatteryStatusValue(),
                                    freePowerCorrect,
                                    infoActionDop);
                        }
                    } catch (Exception e) {
                        log.error("isDay: [{}]", isDay, e);
                    }
                }
            } else {
                isDayPrevious = false;
                if (!isUpdateToMinAfterIsDayFalse) {
                    log.info("Update parameters isDay [{}]: Reducing electricity consumption, TempSetMin, Less than one hour until sunset,  SunSet start: [{}].", this.isDay, toLocaleTimeString(this.sunSetDate));
                    log.info("Night   at: [{}]", toLocaleTimeString(this.sunSetMin));
                    try {
                        this.isUpdateToMinAfterIsDayFalse = this.tuyaDeviceService.updateAllThermostatToMin("is Day == false");
                    } catch (Exception e) {
                        log.error("Update parameters Is Day [{}] UpdateAllThermostat to min. Error: [{}}]", this.isDay, e.getMessage());
                    }
                }
            }

            if (this.batterySocCur > 0) {
                String msgProgressBar = toLocaleTimeString(Instant.ofEpochMilli(curInst.toEpochMilli())) + ". Next update: " + toLocaleTimeString(Instant.ofEpochMilli(curInst.toEpochMilli() + this.timeoutSecUpdate * 1000)) + ",  after [" + this.timeoutSecUpdate / 60 + "] min: ";
                this.setProgressBarThread(msgProgressBar);
            }
            batterySocCur = batterySocNew;
        } catch (Exception e) {
            log.error("Update parameters: Is Day [{}]", this.isDay, e);
        }
    }

    private void batteryChargeDischarge(boolean isCharge, int freeBatteryPower) {
        if (isCharge) {     // Battery charge
            tuyaDeviceService.updateThermostatBatteryCharge(freeBatteryPower,
                    tuyaDeviceService.getDeviceProperties().getCategoryForControlPowers());
        } else {     // Battery discharge
            tuyaDeviceService.updateThermostatBatteryDischarge(freeBatteryPower,
                    tuyaDeviceService.getDeviceProperties().getCategoryForControlPowers());
        }
    }

    /**
     * For loop: Now we don't make a single query, but go through all the devices of the station.
     * 1) Summation: Parameters such as Solar Power, Load, Daily Energy are now added up.
     * 2) Average: For SOC (battery charge) and temperature, we divide the sum by the number of devices, because percentages cannot be summed.
     * 3) Shared data: We take the firmware versions and grid relay status from the first successfully polled inverter (usually the Master), since they are identical for the entire system.
     */
    private void updatePowerValue() {
        // Акумулятори для СУМ
        double totalSolarSum = 0, totalHomeSum = 0, totalGridSum = 0;
        double batteryPowerSum = 0, bmsCurrentSum = 0, batteryCurrentFactSum = 0;
        double dailyChargeSum = 0, dailyDischargeSum = 0;
        double totalProdSolarSum = 0, dailyProdSolarSum = 0, dailyHomeConsSum = 0;
        double totalSellSum = 0, totalBuySum = 0, dailySellSum = 0, dailyBuySum = 0;

        // Акумулятори для СЕРЕДНІХ значень
        double bmsSocSum = 0, batterySocSum = 0, bmsTempSum = 0, invTempSum = 0;
        double bmsVoltSum = 0, batteryVoltSum = 0;

        int activeDevices = 0;
        long latestTime = 0;

        for (SolarmanDevice device : this.solarmanStationsService.solarmanStation.getDevices().values()) {
            RealTimeData data = this.solarmanStationsService.getRealTimeData(device.getInverterSn(), device.getInverterId());
            if (data == null || data.getDataList() == null) continue;

            activeDevices++;
            latestTime = Math.max(latestTime, data.getCollectionTime());
            List<RealTimeDataValue> list = data.getDataList();

            // 1. Сумуємо показники
            totalSolarSum += getDouble(list, totalSolarPowerKey);
            totalHomeSum += getDouble(list, totalHomeConsumptionPowerKey);
            totalGridSum += getDouble(list, totalGridPowerKey);
            batteryPowerSum += getDouble(list, batteryPowerKey);
            bmsCurrentSum += getDouble(list, bmsCurrentKey);
            batteryCurrentFactSum += getDouble(list, batteryCurrentKey);
            dailyChargeSum += getDouble(list, dailyBatteryChargeKey);
            dailyDischargeSum += getDouble(list, dailyBatteryDischargeKey);
            totalProdSolarSum += getDouble(list, totalProductionSolarPowerKey);
            dailyProdSolarSum += getDouble(list, productionDailySolarPowerKey);
            dailyHomeConsSum += getDouble(list, homeDailyConsumptionPowerKey);
            totalSellSum += getDouble(list, totalEnergySellKey);
            totalBuySum += getDouble(list, totalEnergyBuyKey);
            dailySellSum += getDouble(list, dailyEnergySellKey);
            dailyBuySum += getDouble(list, dailyEnergyBuyKey);

            // 2. Додаємо для розрахунку середнього
            bmsSocSum += getDouble(list, bmsSocKey);
            batterySocSum += getDouble(list, batterySocKey);
            bmsTempSum += getDouble(list, bmsTempKey);
            invTempSum += getDouble(list, invTempKey);
            bmsVoltSum += getDouble(list, bmsVoltageKey);
            batteryVoltSum += getDouble(list, batteryVoltageKey);

            // 3. Текстові дані та напругу мережі беремо з першого (Master)
            if (activeDevices == 1) {
                powerValueRealTimeData.setInverterProtocolVersionValue(getString(list, invProtocolVerKey));
                powerValueRealTimeData.setInverterMAINValue(getString(list, invMAINKey));
                powerValueRealTimeData.setInverterHMIValue(getString(list, invHMIKey));
                powerValueRealTimeData.setBatteryStatusValue(getString(list, batteryStatusKey));
                powerValueRealTimeData.setGridStatusRelay(getString(list, gridRelayStatusKey));
                powerValueRealTimeData.setGridStatusSolarman(getString(list, gridStatusKey));
                powerValueRealTimeData.setGridVoltageL1(getDouble(list, gridVoltageL1Key));
                powerValueRealTimeData.setGridVoltageL2(getDouble(list, gridVoltageL2Key));
                powerValueRealTimeData.setGridVoltageL3(getDouble(list, gridVoltageL3Key));
            }
        }

        if (activeDevices == 0) return;

        // ЗАПИС АГРЕГОВАНИХ ДАНИХ
        powerValueRealTimeData.setCollectionTime(latestTime);

        // Суми
        powerValueRealTimeData.setTotalSolarPower(totalSolarSum);
        powerValueRealTimeData.setTotalHomePower(totalHomeSum);
        powerValueRealTimeData.setTotalGridPower(totalGridSum);
        powerValueRealTimeData.setBatteryPowerValue(batteryPowerSum);
        powerValueRealTimeData.setBmsCurrentValue(bmsCurrentSum);
        powerValueRealTimeData.setDailyBatteryCharge(dailyChargeSum);
        powerValueRealTimeData.setDailyBatteryDischarge(dailyDischargeSum);
        powerValueRealTimeData.setTotalProductionSolarPower(totalProdSolarSum);
        powerValueRealTimeData.setDailyProductionSolarPower(dailyProdSolarSum);
        powerValueRealTimeData.setDailyHomeConsumptionPower(dailyHomeConsSum);
        powerValueRealTimeData.setTotalEnergySell(totalSellSum);
        powerValueRealTimeData.setTotalEnergyBuy(totalBuySum);
        powerValueRealTimeData.setDailyEnergySell(dailySellSum);
        powerValueRealTimeData.setDailyEnergyBuy(dailyBuySum);

        // Середні (ділимо на кількість активних інверторів)
        double avgBatteryVolt = batteryVoltSum / activeDevices;
        powerValueRealTimeData.setBmsSocValue(bmsSocSum / activeDevices);
        powerValueRealTimeData.setBatterySocValue(batterySocSum / activeDevices);
        powerValueRealTimeData.setBmsTempValue(bmsTempSum / activeDevices);
        powerValueRealTimeData.setInverterTempValue(invTempSum / activeDevices);
        powerValueRealTimeData.setBmsVoltageValue(bmsVoltSum / activeDevices);
        powerValueRealTimeData.setBatteryVoltageValue(avgBatteryVolt);

        // Розрахунок струму (Твоя логіка з Math.round)
        double batteryCurrentValue = (batteryCurrentFactSum == 0 && avgBatteryVolt != 0)
                ? Math.round((batteryPowerSum / avgBatteryVolt) * 1000.0) / 1000.0
                : batteryCurrentFactSum;
        powerValueRealTimeData.setBatteryCurrentValue(batteryCurrentValue);
    }

    // Допоміжні методи для чистоти коду
    private double getDouble(List<RealTimeDataValue> list, String key) {
        return list.stream().filter(v -> v.getKey().equals(key)).findFirst()
                .map(v -> { try { return Double.parseDouble(v.getValue()); } catch (Exception e) { return 0.0; } })
                .orElse(0.0);
    }

    private String getString(List<RealTimeDataValue> list, String key) {
        return list.stream().filter(v -> v.getKey().equals(key)).findFirst()
                .map(RealTimeDataValue::getValue).orElse("");
    }


    private void updateSunRiseSunSetDate() {
        Instant curTimeDate = Instant.now();
        String curTimeDateDMY = toLocaleDateString(curTimeDate);
        String curSunSetDateDMY = this.sunSetDate == null ? null : toLocaleDateString(this.sunSetDate);
        String curSunSetTime = this.sunSetDate == null ? null : toLocaleTimeString(this.sunSetDate);
        if (!curTimeDateDMY.equals(curSunSetDateDMY)) {
            log.info("curTimeDateDMY:  [{}] : [{}]", curTimeDateDMY, toLocaleTimeString(curTimeDate));
            log.info("curSunSetDateDMY [{}] : [{}]", curSunSetDateDMY, curSunSetTime);
            Long[] sunRiseSunSetDate;
            sunRiseSunSetDate = getSunRiseSunset(solarmanStationsService.getSolarmanStation().getLocationLat(),
                    solarmanStationsService.getSolarmanStation().getLocationLng());
            curSunSetDateDMY = toLocaleDateString(sunRiseSunSetDate[0]);
            if (curTimeDateDMY.equals(curSunSetDateDMY)) {
                this.sunRiseDate = sunRiseSunSetDate[0];
                this.sunSetDate = sunRiseSunSetDate[1];
                this.sunRiseMax = this.sunRiseDate + ((this.sunSetDate - this.sunRiseDate) / 3 * 2);
                this.sunSetMin = this.sunSetDate - 3600000;   // - 1 hour
                log.info("Night   at: [{}]", toLocaleTimeString(this.sunSetMin));
                this.batSocMinInMilliSec = getBatSocMinInMilliSec();    // %/milliSec

            } else {
                this.batSocMinInMilliSec = solarmanStationsService.getSolarmanStation().getBatSocMinMin();
            }
        }
        this.curDate = curTimeDate;
    }

    private void initUpdateTimeoutSheduler() {
        Long timeoutSecUpdateOld = this.timeoutSecUpdate;
        if (this.timeoutSecUpdate == null) {
            this.timeoutSecUpdate = solarmanStationsService.getSolarmanStation().getTimeoutSec() / 2; // 1 min
        } else {
//            if (isDay ) {
            this.timeoutSecUpdate = solarmanStationsService.getSolarmanStation().getTimeoutSec() * 2; // 4 min
//            } else {
//                this.timeoutSecUpdate = solarmanStationsService.getSolarmanStation().getTimeoutSec() * 15; // 30 min
//            }
        }
        if (!this.timeoutSecUpdate.equals(timeoutSecUpdateOld)) {
            if (this.scheduler == null) {
                this.scheduler = new DynamicScheduler(this.timeoutSecUpdate, this);
            } else {
                this.scheduler.updateTimeoutSecUpdate(this.timeoutSecUpdate);
            }
            tuyaDeviceService.setTimeoutSecUpdateMillis(this.timeoutSecUpdate);
        }
    }

    private Double getBatSocMin() {
        if (this.curDate != null && this.sunRiseDate != null) {
            if (this.sunRiseMax != null && this.sunSetMin != null
                    && this.curDate.toEpochMilli() > this.sunRiseMax && this.curDate.toEpochMilli() <= this.sunSetMin) {
                long deltaSunRise = this.curDate.toEpochMilli() - this.sunRiseMax;
                double deltaBatSocMinMin = deltaSunRise * this.batSocMinInMilliSec;
                double batSocMin = solarmanStationsService.getSolarmanStation().getBatSocMinMin() + deltaBatSocMinMin;
                return batSocMin < solarmanStationsService.getSolarmanStation().getBatSocMinMax() ? batSocMin :
                        solarmanStationsService.getSolarmanStation().getBatSocMinMax();
            } else if (this.curDate.toEpochMilli() > this.sunRiseDate && this.sunRiseMax != null && this.curDate.toEpochMilli() <= this.sunRiseMax) {
                return solarmanStationsService.getSolarmanStation().getBatSocMinMin();
            }
        }
        return solarmanStationsService.getSolarmanStation().getBatSocMinMax();
    }

    private double getBatSocMinInMilliSec() {
        long sunRisePeriod = this.sunSetMin - this.sunRiseMax;
        double deltaBatSocMin = solarmanStationsService.getSolarmanStation().getBatSocMinMax() - solarmanStationsService.getSolarmanStation().getBatSocMinMin();
        return deltaBatSocMin / sunRisePeriod;
    }

    private int getFreePowerCorrect(double batterySocNew, boolean isCharge) {
        int freePowerCorrect = (int) (powerValueRealTimeData.getTotalProductionSolarPower() -
                powerValueRealTimeData.getDailyHomeConsumptionPower() - stationConsumptionPower);
        if (tuyaDeviceService.isTestBotDebugging()) {
            this.freePowerCorrectMinMax = solarmanStationsService.getSolarmanStation().getDopPowerToMax() * 2;
            freePowerCorrect = Math.max(freePowerCorrect, this.freePowerCorrectMinMax);
        } else {
            if (this.sunRiseMax != null && this.sunRiseMax > System.currentTimeMillis()) {
                if (freePowerCorrectCnt == 0) {
                    if (this.freePowerCorrectMinMax == 0) {
                        this.freePowerCorrectMinMax = solarmanStationsService.getSolarmanStation().getDopPowerToMax();
                    } else {
                        if (batterySocNew >= solarmanStationsService.getSolarmanStation().getBatSocMax()) {
                            this.freePowerCorrectMinMax = solarmanStationsService.getSolarmanStation().getDopPowerToMax() * 2;
                        } else if (this.freePowerCorrectMinMax == solarmanStationsService.getSolarmanStation().getDopPowerToMax()) {
                            this.freePowerCorrectMinMax = solarmanStationsService.getSolarmanStation().getDopPowerToMin();
                        } else {
                            this.freePowerCorrectMinMax = solarmanStationsService.getSolarmanStation().getDopPowerToMax();
                        }
                    }
                    log.info("freePowerCorrectMinMax after update: [{}]", this.freePowerCorrectMinMax);
                    if (batterySocNew >= solarmanStationsService.getSolarmanStation().getBatSocMax()) {
                        freePowerCorrect = Math.max(freePowerCorrect, this.freePowerCorrectMinMax);
                    } else if (isCharge) {
                        if (freePowerCorrect < this.freePowerCorrectMinMax) {
                            if ((freePowerCorrect + solarmanStationsService.getSolarmanStation().getDopPowerToMin()) > this.freePowerCorrectMinMax) {
                                freePowerCorrect = this.freePowerCorrectMinMax;
                            } else {
                                freePowerCorrect += solarmanStationsService.getSolarmanStation().getDopPowerToMin();
                            }
                        }
                    }
                    freePowerCorrectCnt++;
                } else if (freePowerCorrectCnt > 1) {
                    freePowerCorrectCnt = 0;
                } else {
                    freePowerCorrectCnt++;
                }
            } else {
                freePowerCorrectCnt = 0;
            }
        }
        log.info("""
                        FreePowerCorrectCnt: [{}], freePowerCorrect: [{}]""",
                freePowerCorrectCnt, freePowerCorrect);
        return freePowerCorrect;
    }

    private boolean getIsCharge(double batterySocNew) {
        return BatteryStatus.CHARGING_60.getType().contains(powerValueRealTimeData.getBatteryStatusValue())
                || BatteryStatus.STATIC.getType().equals(powerValueRealTimeData.getBatteryStatusValue())
                || solarmanStationsService.getSolarmanStation().getBatSocMax() <= batterySocNew;

    }

    private void setIsDay() {
        if (this.curDate != null && this.sunSetMin != null && this.sunRiseDate != null) {
            this.isDay = this.curDate.toEpochMilli() >= this.sunRiseDate && this.curDate.toEpochMilli() < this.sunSetMin;
        } else {
            this.isDay = false;
        }
        log.info("Is Day [{}]; Is Day Previous [{}]", isDay, isDayPrevious);
    }

    private void setProgressBarThread(String msgProgressBar) {
        if (this.progressBarThread != null && this.progressBarThread.isAlive()) {
            stopThread(this.progressBarThread);
        }
        long timeAllProgressBar = this.timeoutSecUpdate * 1000 - (this.timeoutSecUpdate * 1000 / 20); // minus 5%
        this.progressBarThread = new Thread(() -> printMsgProgressBar(Thread.currentThread(), msgProgressBar, timeAllProgressBar, this.version));
        progressBarThread.start();
    }

    public void cleanup() {
        log.info("Start destroy DefaultSmartSolarmanTuyaService");
        if (this.progressBarThread != null) {
            stopProgressBar();
            log.info("Stopped executor ProgressBar");
        }
        if (this.scheduler != null) {
            this.scheduler.shutdown();
            log.info("Stopped executor service");
        }
    }
}


