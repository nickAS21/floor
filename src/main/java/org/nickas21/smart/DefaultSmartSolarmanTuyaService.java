package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.BatteryStatus;
import org.nickas21.smart.solarman.SolarmanSocPercentage;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.solarman.api.RealTimeData;
import org.nickas21.smart.solarman.api.RealTimeDataValue;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.nickas21.smart.util.HttpUtil.batteryCurrentKey;
import static org.nickas21.smart.util.HttpUtil.batteryDailyChargeKey;
import static org.nickas21.smart.util.HttpUtil.batteryDailyDischargeKey;
import static org.nickas21.smart.util.HttpUtil.batteryPowerKey;
import static org.nickas21.smart.util.HttpUtil.batterySocKey;
import static org.nickas21.smart.util.HttpUtil.batteryStatusKey;
import static org.nickas21.smart.util.HttpUtil.batteryVoltageKey;
import static org.nickas21.smart.util.HttpUtil.bmsSocKey;
import static org.nickas21.smart.util.HttpUtil.consumptionTotalPowerKey;
import static org.nickas21.smart.util.HttpUtil.dailyEnergyBuyKey;
import static org.nickas21.smart.util.HttpUtil.dailyEnergySellKey;
import static org.nickas21.smart.util.HttpUtil.getSunRiseSunset;
import static org.nickas21.smart.util.HttpUtil.gridRelayStatusKey;
import static org.nickas21.smart.util.HttpUtil.gridStatusKey;
import static org.nickas21.smart.util.HttpUtil.productionTotalSolarPowerKey;
import static org.nickas21.smart.util.HttpUtil.toLocaleDateString;
import static org.nickas21.smart.util.HttpUtil.toLocaleTimeString;
import static org.nickas21.smart.util.HttpUtil.totalConsumptionPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalEnergyBuyKey;
import static org.nickas21.smart.util.HttpUtil.totalEnergySellKey;
import static org.nickas21.smart.util.HttpUtil.totalGridPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalSolarPowerKey;
import static org.nickas21.smart.util.StringUtils.printMsgWithProgressBar;

@Slf4j
@Service
public class DefaultSmartSolarmanTuyaService implements SmartSolarmanTuyaService {
    private double batterySocCur;
    private double stationConsumptionPower;
    private PowerValueRealTimeData powerValueRealTimeData;
    private boolean isDay;
    private Instant curDate;
    private Long sunRiseDate;
    private Long sunSetDate;
    private Long sunRiseMax;
    private Long sunSetMin;
    private double batSocMinInMilliSec; // %

    @Autowired
    SolarmanStationsService solarmanStationsService;

    @Autowired
    TuyaDeviceService tuyaDeviceService;

    @Override
    public void solarmanRealTimeDataStart() {
        this.isDay = true;
        this.batterySocCur = 0;
        this.stationConsumptionPower = solarmanStationsService.getSolarmanStation().getStationConsumptionPower();
        this.powerValueRealTimeData = PowerValueRealTimeData.builder().build();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::setBmsSocCur, 0, solarmanStationsService.getSolarmanStation().getTimeoutSec(), TimeUnit.SECONDS);
    }

    private void setBmsSocCur() {
        log.info("Is Day [{}]", isDay);
        try {
            updatePowerValue();
            double batVolNew = powerValueRealTimeData.getBatteryVoltageValue();
            SolarmanSocPercentage percentage = SolarmanSocPercentage.fromPercentage(batVolNew);
            double batterySocNew = percentage != null ? percentage.getPercentage() : 0;
            double batterySocMin = getBatSocMin();
            int batteryPowerNew = powerValueRealTimeData.getBatteryPowerValue();
            String batteryStatusNew = powerValueRealTimeData.getBatteryStatusValue();

            updateSunRiseSunSetDate();

            String batteryPowerNewStr = -batteryPowerNew + " W";
            Instant curInst = Instant.now();
            String curInstStr = toLocaleTimeString(curInst);
            printMsgWithProgressBar(curInstStr + ". Next update: " + toLocaleTimeString(Instant.ofEpochMilli(curInst.toEpochMilli() + solarmanStationsService.getSolarmanStation().getTimeoutSec()*1000)) + ",  after [" + solarmanStationsService.getSolarmanStation().getTimeoutSec()/60 + "] min: ",
                    solarmanStationsService.getSolarmanStation().getTimeoutSec()*1000);
            log.info("""
                            Current data:\s
                            Current real time data: [{}], -Update real time data: [{}],\s
                            -batSocLast: [{} %], -batSocNew: [{} %], -deltaBmsSoc: [{} %], -batterySocMin: [{} %],\s
                            -batteryStatus: [{}], -batVolNew: [{} V], -batCurrentNew: [{} A],\s
                            -batteryPower: [{}], -solarPower: [{} W], consumptionPower: [{} W], stationPower: [{} W],\s
                            -batteryDailyCharge: [{} kWh], -batteryDailyDischarge: [{} kWh],\s
                            -relayStatus: [{}], -gridStatus: [{}], -dailyBuy:[{} kWh], -dailySell: [{} kWh].""",
                    curInstStr,
                    toLocaleTimeString(powerValueRealTimeData.getCollectionTime() * 1000),

                    this.batterySocCur,
                    batterySocNew,
                    (batterySocNew - this.batterySocCur),
                    String.format( "%.2f", batterySocMin),

                    batteryStatusNew,
                    batVolNew,
                    powerValueRealTimeData.getBatteryCurrentValue(),
                    batteryPowerNewStr,
                    powerValueRealTimeData.getProductionTotalSolarPowerValue(),
                    powerValueRealTimeData.getConsumptionTotalPowerValue(),
                    stationConsumptionPower,

                    powerValueRealTimeData.getBatteryDailyCharge(),
                    powerValueRealTimeData.getBatteryDailyDischarge(),

                    powerValueRealTimeData.getGridRelayStatus(),
                    powerValueRealTimeData.getGridStatus(),
                    powerValueRealTimeData.getDailyEnergyBuy(),
                    powerValueRealTimeData.getDailyEnergySell());

            if (this.sunRiseDate != null && this.sunSetDate != null) {
                if (this.batterySocCur > 0 &&
                        this.curDate.toEpochMilli() > this.sunRiseDate &&
                        this.curDate.toEpochMilli() <= this.sunSetMin) {
                    isDay = true;
                    try {
                        if (tuyaDeviceService.devices != null && tuyaDeviceService.devices.getDevIds() != null) {
                            int freePower = getFreePower();
                            if (batterySocNew < batterySocMin) {
                                // Reducing electricity consumption
                                log.info("Reducing electricity consumption, battery Status [{}], freePower [{}],  action [{}].",
                                        batteryStatusNew,
                                        freePower,
                                        "TempSetMin");
                                tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                            } else {
                                // Battery charge/discharge analysis program
                                log.info("Change in electricity consumption, battery Status [{}], freePower [{}],  action [{}].",
                                        batteryStatusNew,
                                        freePower,
                                        "TempSet_Min/Max");
                                this.batteryChargeDischarge(batteryStatusNew, freePower);
                            }
                        }
                    } catch (Exception e) {
                        log.error("", e);
                    }
                } else if (isDay && this.curDate.toEpochMilli() > this.sunSetMin) {
                    log.info("Reducing electricity consumption, TempSetMin,  SunSet start: [{}].", this.sunSetDate);
                    isDay = false;
                    try {
                        if (this.batterySocCur > 0) {
                            this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getDeviceProperties().getTempSetMin());
                        }
                    } catch (Exception e) {
                        log.error("SunSet, updateAllThermostat to min.", e);
                    }
                }
            } else if (this.batterySocCur > 0) {
                log.info("Time out, update SunRiseDate and SunSetDate...");
            }
            batterySocCur = batterySocNew;
        } catch (Exception e) {
            log.error("Failed updatePower or SunRiseSunSetDate or updateThermostat, [{}]", e.getMessage());
        }
    }

    private void batteryChargeDischarge(String batteryStatusNew, int freeBatteryPower) throws Exception {
        boolean isCharge = freeBatteryPower >= 0 && (BatteryStatus.CHARGING.getType().equals(batteryStatusNew) || BatteryStatus.STATIC.getType().equals(batteryStatusNew));
        log.info("Battery: status -> [{}], freePower [{}], state: [{}]", batteryStatusNew, freeBatteryPower, isCharge);
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

        double bmsSocValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsSocKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        // battery
        double batterySocValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batterySocKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        String batteryStatusValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryStatusKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse(null);
        int batteryPowerValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);
        double batteryCurrentValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryCurrentKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batteryVoltageValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryVoltageKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batteryDailyChargeValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryDailyChargeKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double batteryDailyDischargeValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(batteryDailyDischargeKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double productionTotalSolarPowerValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(productionTotalSolarPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);
        double consumptionTotalPowerValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(consumptionTotalPowerKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);


        int totalSolarPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalSolarPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        int totalConsumptionPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalConsumptionPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        double totalEnergySell = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalEnergySellKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        double totalEnergyBuy = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalEnergyBuyKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        double dailyEnergySell = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(dailyEnergySellKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        double dailyEnergyBuy = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(dailyEnergyBuyKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        int totalGridPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalGridPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        String gridRelayStatus = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(gridRelayStatusKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse(null);
        String gridStatus = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(gridStatusKey)).findFirst()
                .map(RealTimeDataValue::getValue).orElse(null);

        powerValueRealTimeData.setCollectionTime(solarmanRealTimeData.getCollectionTime());
        powerValueRealTimeData.setBmsSocValue(bmsSocValue);

        powerValueRealTimeData.setBatterySocValue(batterySocValue);
        powerValueRealTimeData.setBatteryStatusValue(batteryStatusValue);
        powerValueRealTimeData.setBatteryPowerValue(batteryPowerValue);
        powerValueRealTimeData.setBatteryCurrentValue(batteryCurrentValue);
        powerValueRealTimeData.setBatteryVoltageValue(batteryVoltageValue);
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
        powerValueRealTimeData.setGridRelayStatus(gridRelayStatus);
        powerValueRealTimeData.setGridStatus(gridStatus);
        powerValueRealTimeData.setTotalGridPower(totalGridPower);
    }

    private void updateSunRiseSunSetDate() {
        Instant curTimeDate = Instant.now();
        String curTimeDateDMY = toLocaleDateString(curTimeDate);
        String curSunSetDateDMY = this.curDate == null ? null : toLocaleDateString(this.sunSetDate);
        if (!curTimeDateDMY.equals(curSunSetDateDMY)) {
            Long[] sunRiseSunSetDate;
            sunRiseSunSetDate = getSunRiseSunset(solarmanStationsService.getSolarmanStation().getLocationLat(),
                    solarmanStationsService.getSolarmanStation().getLocationLng());
            curSunSetDateDMY = toLocaleDateString(sunRiseSunSetDate[0]);
            if (curTimeDateDMY.equals(curSunSetDateDMY)) {
                this.sunRiseDate = sunRiseSunSetDate[0];
                this.sunSetDate = sunRiseSunSetDate[1];
                this.sunRiseMax = this.sunRiseDate + ((this.sunSetDate - this.sunRiseDate)/2);
                this.sunSetMin = this.sunSetDate - 3600000;   // - 1 hour
                this.batSocMinInMilliSec = getBatSocMinInMilliSec();    // %/milliSec

            } else {
                this.sunRiseDate = null;
                this.sunSetDate = null;
                this.sunRiseMax = null;
                this.sunSetMin = null;
                this.batSocMinInMilliSec = solarmanStationsService.getSolarmanStation().getBatSocMinMin();
            }
        }
        this.curDate = curTimeDate;
    }

    private Double getBatSocMin(){
        if (this.curDate != null) {
            if (this.sunRiseMax != null && this.sunSetMin != null
                    && this.curDate.toEpochMilli() > this.sunRiseMax && this.curDate.toEpochMilli() <= this.sunSetMin){
                long deltaSunRise = this.curDate.toEpochMilli() - this.sunRiseMax;
                double deltaBatSocMinMin = deltaSunRise * this.batSocMinInMilliSec;
                double batSocMin = solarmanStationsService.getSolarmanStation().getBatSocMinMin() + deltaBatSocMinMin;
                return batSocMin < solarmanStationsService.getSolarmanStation().getBatSocMinMax() ?  batSocMin :
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

    private int getFreePower(){
        int freePower = (int)(powerValueRealTimeData.getProductionTotalSolarPowerValue() -
                powerValueRealTimeData.getConsumptionTotalPowerValue() -
                stationConsumptionPower);
        if (this.sunRiseMax != null && this.sunRiseMax > System.currentTimeMillis()) {
            freePower += solarmanStationsService.getSolarmanStation().getDopPowerToMax();
        }
        return freePower;
    }
}


