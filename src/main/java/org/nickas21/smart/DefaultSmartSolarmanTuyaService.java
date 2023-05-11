package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.solarman.mq.RealTimeData;
import org.nickas21.smart.tuya.service.TuyaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.nickas21.smart.util.HttpUtil.bmsSocKey;
import static org.nickas21.smart.util.HttpUtil.formatter;
import static org.nickas21.smart.util.HttpUtil.formatter_D_M_Y;
import static org.nickas21.smart.util.HttpUtil.getSunRiseSunset;
import static org.nickas21.smart.util.HttpUtil.gridRelayStatusKey;
import static org.nickas21.smart.util.HttpUtil.gridStatusKey;
import static org.nickas21.smart.util.HttpUtil.totalConsumptionPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalEnergySellKey;
import static org.nickas21.smart.util.HttpUtil.totalGridPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalSolarPowerKey;

@Slf4j
@Service
public class DefaultSmartSolarmanTuyaService implements SmartSolarmanTuyaService {
    private double bmsSocCur;
    private PowerValueRealTimeData powerValueRealTimeData;
    private boolean isDay;
    private Date curDate;
    private Date sunRiseDate;
    private Date sunSetDate;

    @Autowired
    SolarmanStationsService solarmanStationsService;

    @Autowired
    TuyaDeviceService tuyaDeviceService;

    @Override
    public void solarmanRealTimeDataStart() {
        this.isDay = true;
        this.bmsSocCur = 0;
        this.powerValueRealTimeData = PowerValueRealTimeData.builder().build();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::setBmsSocCur, 0, solarmanStationsService.getSolarmanDataSource().getTimeOutSec(), TimeUnit.SECONDS);
    }

    private void setBmsSocCur() {
        log.info("Is Day [{}]", isDay);
        try {
            updatePowerValue();
            double bmsSocNew = powerValueRealTimeData.getBmsSocValue();
            updateSunRiseSunSetDate();
            String updateTimeData = formatter.format(new Date(powerValueRealTimeData.getCollectionTime() * 1000));
            String deltaPower = (powerValueRealTimeData.getTotalSolarPower() - powerValueRealTimeData.getTotalConsumptionPower()) + " w";
            String gidEnergySellOrBuy = powerValueRealTimeData.getTotalGridPower() >= 0 ? "Buying energy " : "Sale energy ";
            gidEnergySellOrBuy += powerValueRealTimeData.getTotalGridPower() + " w";
            log.info("Current real time data: [{}], -Update real time data: [{}], \n-bmsSocLast: [{}], " +
                            "-bmsSocNew: [{}], -deltaBmsSoc: [{}], -deltaPower: [{}], -relayStatus: [{}], -gridStatus: [{}], [{}].",
                    formatter.format(new Date()), updateTimeData, this.bmsSocCur, bmsSocNew, (bmsSocNew - this.bmsSocCur), deltaPower,
                    powerValueRealTimeData.getGridRelayStatus(),
                    powerValueRealTimeData.getGridStatus(),
                    gidEnergySellOrBuy);

            if (this.sunRiseDate != null && this.sunSetDate != null) {
                if (this.bmsSocCur > 0 &&
                        this.curDate.getTime() > this.sunRiseDate.getTime() &&
                        this.curDate.getTime() <= (this.sunSetDate.getTime() - 3600000)) {
                    isDay = true;
                    try {
                        if (bmsSocNew >= solarmanStationsService.getSolarmanDataSource().getBmsSocMax()) {   // 97%
                            // Increasing electricity consumption
                            this.setIncreasingElectricityConsumption(bmsSocNew);
                        } else if (bmsSocNew < solarmanStationsService.getSolarmanDataSource().getBmsSocMin()) {    // 87%

                            // Reducing electricity consumption
                            this.setReducingElectricityConsumption(bmsSocNew);
                        } else {
                            // Battery charge/discharge analysis program
                            this.batteryChargeDischarge(bmsSocNew, (powerValueRealTimeData.getTotalSolarPower() - powerValueRealTimeData.getTotalConsumptionPower()));
                        }
                    } catch (Exception e) {
                        log.error("", e);
                    }
                } else if (isDay && this.curDate.getTime() > (this.sunSetDate.getTime() - 3600000)) {
                    log.info("Reducing electricity consumption, TempSetMin,  SunSet start: [{}].", this.sunSetDate);
                    isDay = false;
                    try {
                        this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMin(),
                                this.tuyaDeviceService.getConnectionConfiguration().getCategoryForControlPowers());
                    } catch (Exception e) {
                        log.error("SunSet, updateAllThermostat to min.", e);
                    }
                }
            } else if (this.bmsSocCur > 0) {
                log.info("Time out, update SunRiseDate and SunSetDate...");
            }
            bmsSocCur = bmsSocNew;
        } catch (Exception e) {
            log.error("Failed updatePower or SunRiseSunSetDate or updateThermostat, [{}]", e.getMessage());
        }
    }

    private void setIncreasingElectricityConsumption(double bmsSocNew) throws Exception {
        log.info("Increasing electricity consumption, TempSetMax, bmsSocNew [{}].", bmsSocNew);
        this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMax(),
                this.tuyaDeviceService.getConnectionConfiguration().getCategoryForControlPowers());
    }

    private void setReducingElectricityConsumption(double bmsSocNew) throws Exception {
        log.info("Reducing electricity consumption, TempSetMin,  bmsSocNew [{}].", bmsSocNew);
        this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMin(),
                this.tuyaDeviceService.getConnectionConfiguration().getCategoryForControlPowers());
    }

    private void batteryChargeDischarge(double bmsSocNew, int deltaPower) throws Exception {
        boolean isCharge = deltaPower >= 0 && ((bmsSocNew - bmsSocCur) >= 0);
        String stateBmsSoc = isCharge ? "charge" : "discharge";
        log.info("Battery analysis -> [{}].", stateBmsSoc);
        if (isCharge) {     // Battery charge
            this.tuyaDeviceService.updateThermostatBatteryCharge(deltaPower,
                    this.tuyaDeviceService.getConnectionConfiguration().getCategoryForControlPowers());
        } else {     // Battery discharge
            this.tuyaDeviceService.updateThermostatBatteryDischarge(deltaPower,
                    this.tuyaDeviceService.getConnectionConfiguration().getCategoryForControlPowers());
        }
    }

    private void updatePowerValue() throws IOException {
        RealTimeData solarmanRealTimeData = solarmanStationsService.getRealTimeData();

        double bmsSocValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsSocKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        int totalSolarPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalSolarPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        int totalConsumptionPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalConsumptionPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        double totalEnergySell = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalEnergySellKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        int  totalGridPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalGridPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        String gridRelayStatus = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(gridRelayStatusKey)).findFirst()
                .map(realTimeDataValue -> realTimeDataValue.getValue()).orElse(null);
        String gridStatus = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(gridStatusKey)).findFirst()
                .map(realTimeDataValue -> realTimeDataValue.getValue()).orElse(null);

        powerValueRealTimeData.setCollectionTime(solarmanRealTimeData.getCollectionTime());
        powerValueRealTimeData.setBmsSocValue(bmsSocValue);
        powerValueRealTimeData.setTotalSolarPower(totalSolarPower);
        powerValueRealTimeData.setTotalConsumptionPower(totalConsumptionPower);
        powerValueRealTimeData.setTotalEnergySell(totalEnergySell);
        powerValueRealTimeData.setGridRelayStatus(gridRelayStatus);
        powerValueRealTimeData.setGridStatus(gridStatus);
        powerValueRealTimeData.setTotalGridPower(totalGridPower);
    }

    private void updateSunRiseSunSetDate() {
        Date curTimeDate = new Date();
        String curTimeDateDMY = formatter_D_M_Y.format(curTimeDate);
        String curSunSetDateDMY = this.curDate == null ? null : formatter_D_M_Y.format(this.sunSetDate);
        if (curSunSetDateDMY == null || !curTimeDateDMY.equals(curSunSetDateDMY)) {
            Date[] sunRiseSunSetDate;
            sunRiseSunSetDate = getSunRiseSunset(solarmanStationsService.getSolarmanDataSource().getLocationLat(),
                    solarmanStationsService.getSolarmanDataSource().getLocationLng());
            curSunSetDateDMY = formatter_D_M_Y.format(sunRiseSunSetDate[0]);
            if (curTimeDateDMY.equals(curSunSetDateDMY)) {
                this.sunRiseDate = sunRiseSunSetDate[0];
                this.sunSetDate = sunRiseSunSetDate[1];
            } else {
                this.sunRiseDate = null;
                this.sunSetDate = null;
            }
        }
        this.curDate = curTimeDate;
    }
}


