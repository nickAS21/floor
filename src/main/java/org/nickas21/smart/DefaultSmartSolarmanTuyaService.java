package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.BatteryStatus;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.solarman.api.RealTimeData;
import org.nickas21.smart.solarman.api.RealTimeDataValue;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.util.DynamicScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static org.nickas21.smart.util.HttpUtil.batteryCurrentKey;
import static org.nickas21.smart.util.HttpUtil.batteryDailyChargeKey;
import static org.nickas21.smart.util.HttpUtil.batteryDailyDischargeKey;
import static org.nickas21.smart.util.HttpUtil.batteryPowerKey;
import static org.nickas21.smart.util.HttpUtil.batterySocKey;
import static org.nickas21.smart.util.HttpUtil.batteryStatusKey;
import static org.nickas21.smart.util.HttpUtil.batteryVoltageKey;
import static org.nickas21.smart.util.HttpUtil.bmsCurrentKey;
import static org.nickas21.smart.util.HttpUtil.bmsSocKey;
import static org.nickas21.smart.util.HttpUtil.bmsTempKey;
import static org.nickas21.smart.util.HttpUtil.bmsVoltageKey;
import static org.nickas21.smart.util.HttpUtil.consumptionTotalPowerKey;
import static org.nickas21.smart.util.HttpUtil.dailyEnergyBuyKey;
import static org.nickas21.smart.util.HttpUtil.dailyEnergySellKey;
import static org.nickas21.smart.util.HttpUtil.getSunRiseSunset;
import static org.nickas21.smart.util.HttpUtil.gridRelayStatusKey;
import static org.nickas21.smart.util.HttpUtil.gridStatusKey;
import static org.nickas21.smart.util.HttpUtil.invHMIKey;
import static org.nickas21.smart.util.HttpUtil.invMAINKey;
import static org.nickas21.smart.util.HttpUtil.invProtocolVerKey;
import static org.nickas21.smart.util.HttpUtil.invTempKey;
import static org.nickas21.smart.util.HttpUtil.productionTotalSolarPowerKey;
import static org.nickas21.smart.util.HttpUtil.timeLocalMinutesNightTariffStart_1;
import static org.nickas21.smart.util.HttpUtil.timeLocalMinutesNightTariffStart_2;
import static org.nickas21.smart.util.HttpUtil.timeLocalNightTariffFinish;
import static org.nickas21.smart.util.HttpUtil.toLocaleDateString;
import static org.nickas21.smart.util.HttpUtil.toLocaleDateTimeHour;
import static org.nickas21.smart.util.HttpUtil.toLocaleDateTimeMinutes;
import static org.nickas21.smart.util.HttpUtil.toLocaleTimeString;
import static org.nickas21.smart.util.HttpUtil.totalConsumptionPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalEnergyBuyKey;
import static org.nickas21.smart.util.HttpUtil.totalEnergySellKey;
import static org.nickas21.smart.util.HttpUtil.totalGridPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalSolarPowerKey;
import static org.nickas21.smart.util.StringUtils.printMsgProgressBar;
import static org.nickas21.smart.util.StringUtils.stopProgressBar;
import static org.nickas21.smart.util.StringUtils.stopThread;

@Slf4j
@Service
public class DefaultSmartSolarmanTuyaService implements SmartSolarmanTuyaService {
    private double batterySocCur;
    private boolean batterySocCriticalNightCharging60 = false; // false == 50; true == 60
    private double stationConsumptionPower;
    private PowerValueRealTimeData powerValueRealTimeData;
    private boolean isDay;
    private boolean isDayPrevious;
    private boolean isUpdateToMinAfterIsDayFalse;
    private Instant curDate;
    private Long sunRiseDate;
    private Long sunSetDate;
    private Long sunRiseMax;
    private Long sunSetMin;
    private Long timeoutSecUpdate;
    private double batSocMinInMilliSec; // %
    private int freePowerCorrectMinMax;
    private int freePowerCorrectCnt;

    private Thread progressBarThread;

    private DynamicScheduler scheduler;
    private final boolean debugging = false;

    @Value("${app.version:unknown}")
    String version;
    @Autowired
    SolarmanStationsService solarmanStationsService;

    @Autowired
    TuyaDeviceService tuyaDeviceService;

    @Override
    public void solarmanRealTimeDataStart() {
        this.batterySocCur = 0;
        this.stationConsumptionPower = solarmanStationsService.getSolarmanStation().getStationConsumptionPower();
        this.powerValueRealTimeData = PowerValueRealTimeData.builder().build();
        initUpdateTimeoutSheduler();
        tuyaDeviceService.setDebugging(this.debugging);
    }

    public void setBmsSocCur() {
        try {
            updatePowerValue();
            double batCurNew = powerValueRealTimeData.getBatteryCurrentValue();
            double batVolNew = powerValueRealTimeData.getBatteryVoltageValue();
            double bmsVolNew = powerValueRealTimeData.getBmsVoltageValue();
            double bmsCurNew = powerValueRealTimeData.getBmsCurrentValue();
            double bmsTempNew = powerValueRealTimeData.getBmsTempValue();
            // old if battery mode == USER
            // double batterySocNew = getPercentageVoltage(batVolNew);
            // double batterySocFromSolarman = powerValueRealTimeData.getBatterySocValue();
            double batterySocNew = powerValueRealTimeData.getBatterySocValue();
            double batterySocMin = getBatSocMin();
            double batteryPowerNew = powerValueRealTimeData.getBatteryPowerValue();
            String batteryStatusNew = powerValueRealTimeData.getBatteryStatusValue();


            updateSunRiseSunSetDate();
            setIsDay();

//            String batteryPowerNewStr = -batteryPowerNew + " W";
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
                tuyaDeviceService.updateGridStateOnLineToTelegram();
                // old if matter mode == USER
//                tuyaDeviceService.updateOnOfSwitchRelay(batterySocFromSolarman);
                tuyaDeviceService.updateOnOfSwitchRelay(batterySocNew, this.batterySocCriticalNightCharging60);
            }

            log.info("""
                            Current data:\s
                            Current real time data: [{}], -Update real time data: [{}],\s
                            -batSocLast: [{} %], -batSocNew: [{} %], -deltaBmsSoc: [{} %], -batterySocMin: [{} %],\s
                            -batteryStatus: [{}], -batteryPower: [{} W], -batVolNew: [{} V], -batCurrentNew: [{} A],  -bmsVolNew: [{} V], -bmsCurrentNew: [{} A], -BMS Temperature: [{}  grad C]\s
                            -solarPower: [{} W], consumptionPower: [{} W], stationPower: [{} W],\s
                            -batteryDailyCharge: [{} kWh], -batteryDailyDischarge: [{} kWh],\s
                            -relayStatus: [{}], -gridStatusSolarman: [{}], -gridStatusRealTime: [{}], -dailyBuy:[{} kWh], -dailySell: [{} kWh],
                            -AC (inverter) Temperature:  [{} grad C].
                        """,
                    curInstStr,
                    toLocaleTimeString(powerValueRealTimeData.getCollectionTime() * 1000),

                    this.batterySocCur, batterySocNew,
//                    batterySocFromSolarman,
                    String.format("%.2f", (batterySocNew - this.batterySocCur)),
                    String.format("%.2f", batterySocMin),

                    batteryStatusNew, batteryPowerNew,
                    batVolNew, batCurNew,
                    bmsVolNew, bmsCurNew,
                    bmsTempNew,

                    powerValueRealTimeData.getProductionTotalSolarPowerValue(),
                    powerValueRealTimeData.getConsumptionTotalPowerValue(),
                    stationConsumptionPower,

                    powerValueRealTimeData.getBatteryDailyCharge(),
                    powerValueRealTimeData.getBatteryDailyDischarge(),

                    powerValueRealTimeData.getGridStatusRelay(),
                    powerValueRealTimeData.getGridStatusSolarman(),
                    tuyaDeviceService.getGridRelayCodeDachaStateOnLine(),
                    powerValueRealTimeData.getDailyEnergyBuy(),
                    powerValueRealTimeData.getDailyEnergySell(),
                    powerValueRealTimeData.getInverterTempValue());
            tuyaDeviceService.sendBatteryChargeRemaining(batVolNew, batCurNew, bmsVolNew, bmsCurNew, bmsTempNew, batterySocNew, batteryPowerNew, batteryStatusNew);
            if (isDay) {
                isUpdateToMinAfterIsDayFalse = false;
                if (this.batterySocCur > 0) {
                    try {
                        if (tuyaDeviceService.devices != null && tuyaDeviceService.devices.getDevIds() != null) {
                            boolean isCharge = getIsCharge(batterySocNew);
                            int freePowerCorrect = getFreePowerCorrect(batterySocNew, isCharge);
                            String infoActionDop;
                            String infoAction;
                            if (!isDayPrevious) {
                                tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                                infoAction = "After the night...";
                                infoActionDop = "TempSetMin";
                                isDayPrevious = isDay;
                            } else if (batterySocNew < batterySocMin) {
                                // Reducing electricity consumption
                                tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                                infoAction = "Reducing";
                                infoActionDop = "TempSetMin";
                            } else {
                                // Battery charge/discharge analysis program
                                this.batteryChargeDischarge(isCharge, freePowerCorrect);
                                infoAction = "Change";
                                infoActionDop = "TempSet_Min/Max";
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
                // on/off heating: 23:10 >= time <= 6:50: NightTariff (1/2 => 23:15; last => 23:25)
                // Проблема: якщо реальний час: 18:40, Дача лічильник 17:27 => +1:13
                // Поправка                      00:13                     23:00
                int curHour = toLocaleDateTimeHour();
                int curMinutes = toLocaleDateTimeMinutes();

//                if (curHour >= timeLocalNightTariffStart || curHour < timeLocalNightTariffFinish) {
                if (curHour < timeLocalNightTariffFinish) {
                    if (powerValueRealTimeData.getGridStatusRelay().equals("Pull-in")) {
                        if (curMinutes >= (timeLocalMinutesNightTariffStart_1 + timeLocalMinutesNightTariffStart_2)) {
                            log.info("Update parameters isDay [{}]: Increased electricity consumption for everyone, TempSetNax, night tariff, exact time: [{}].", this.isDay, curHour);
                            tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMax());
                        } else if (curMinutes >= (timeLocalMinutesNightTariffStart_1)) {
                            log.info("Update parameters isDay [{}]: Increased electricity consumption for everyone, TempSetNax, NightTariffStart_1, exact time: [{}].", this.isDay, curHour);
                            tuyaDeviceService.updateAllThermostatNight_01(this.tuyaDeviceService.getDeviceProperties().getTempSetMax());
                        }
                    } else {
                        log.info("Update parameters isDay [{}]: Reducing electricity consumption, TempSetMin, No grid power supply, night tariff, exact time: [{}].", this.isDay, curHour);
                        tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                    }
                } else {
                    log.info("Update parameters isDay [{}]: Reducing electricity consumption, TempSetMin, night tariff has expired, exact time: [{}].", this.isDay, curHour);
                    tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                }
            }

            if (this.batterySocCur > 0) {
                // update auto switch dacha heating -> if (time = 7-8 && tempCurrentKuhny >= 5): if hour = 8-23 hand mode
                this.tuyaDeviceService.updateSwitchThermostatFirstFloor();
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

    private void updatePowerValue() {
        RealTimeData solarmanRealTimeData = solarmanStationsService.getRealTimeData();

        String inverterProtocolVersionValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(invProtocolVerKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse("");
        String inverterMAINValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(invMAINKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse("");
        String inverterHMIValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(invHMIKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse("");
        double inverterTempValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(invTempKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        // battery
        double bmsSocValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsSocKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double bmsTempValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsTempKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batterySocValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batterySocKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        String batteryStatusValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryStatusKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse(null);
        double batteryPowerValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse((double) 0);
        double bmsVoltageValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsVoltageKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
         double bmsCurrentValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsCurrentKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batteryVoltageValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryVoltageKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batteryCurrentValueFact = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryCurrentKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batteryCurrentValue =  batteryCurrentValueFact == 0 && batteryVoltageValue != 0 ? Math.round((batteryPowerValue/batteryVoltageValue) * 1000.0) / 1000.0 : batteryCurrentValueFact;
        double batteryDailyChargeValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryDailyChargeKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batteryDailyDischargeValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryDailyDischargeKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double productionTotalSolarPowerValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(productionTotalSolarPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double consumptionTotalPowerValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(consumptionTotalPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);


        double totalSolarPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalSolarPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse((double) 0);

        double totalConsumptionPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalConsumptionPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse((double) 0);

        double totalEnergySell = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalEnergySellKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        double totalEnergyBuy = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalEnergyBuyKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        double dailyEnergySell = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(dailyEnergySellKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        double dailyEnergyBuy = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(dailyEnergyBuyKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        double totalGridPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalGridPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse((double) 0);

        String gridRelayStatus = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(gridRelayStatusKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse(null);
        String gridStatus = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(gridStatusKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse(null);

        powerValueRealTimeData.setCollectionTime(solarmanRealTimeData.getCollectionTime());
        powerValueRealTimeData.setInverterProtocolVersionValue(inverterProtocolVersionValue);
        powerValueRealTimeData.setInverterMAINValue(inverterMAINValue);
        powerValueRealTimeData.setInverterHMIValue(inverterHMIValue);
        powerValueRealTimeData.setInverterTempValue(inverterTempValue);

        //battery
        powerValueRealTimeData.setBmsSocValue(bmsSocValue);
        powerValueRealTimeData.setBmsTempValue(bmsTempValue);
        powerValueRealTimeData.setBatterySocValue(batterySocValue);
        powerValueRealTimeData.setBatteryStatusValue(batteryStatusValue);
        powerValueRealTimeData.setBatteryPowerValue(batteryPowerValue);
        powerValueRealTimeData.setBatteryCurrentValue(batteryCurrentValue);
        powerValueRealTimeData.setBatteryVoltageValue(batteryVoltageValue);
        powerValueRealTimeData.setBmsVoltageValue(bmsVoltageValue);
        powerValueRealTimeData.setBmsCurrentValue(bmsCurrentValue);
        powerValueRealTimeData.setBatteryDailyCharge(batteryDailyChargeValue);
        powerValueRealTimeData.setBatteryDailyDischarge(batteryDailyDischargeValue);
        powerValueRealTimeData.setProductionTotalSolarPowerValue(productionTotalSolarPowerValue);
        powerValueRealTimeData.setConsumptionTotalPowerValue(consumptionTotalPowerValue);

        powerValueRealTimeData.setTotalSolarPower(totalSolarPower);
        powerValueRealTimeData.setTotalConsumptionPower(totalConsumptionPower);
        powerValueRealTimeData.setTotalEnergySell(totalEnergySell);
        powerValueRealTimeData.setTotalEnergyBuy(totalEnergyBuy);
        powerValueRealTimeData.setDailyEnergySell(dailyEnergySell);
        powerValueRealTimeData.setDailyEnergyBuy(dailyEnergyBuy);
        powerValueRealTimeData.setGridStatusRelay(gridRelayStatus);
        powerValueRealTimeData.setGridStatusSolarman(gridStatus);
        powerValueRealTimeData.setTotalGridPower(totalGridPower);
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
        int freePowerCorrect = (int) (powerValueRealTimeData.getProductionTotalSolarPowerValue() -
                powerValueRealTimeData.getConsumptionTotalPowerValue() - stationConsumptionPower);
        if (this.debugging) {
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


