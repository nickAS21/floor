package org.nickas21.smart;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.mq.RealTimeData;
import org.nickas21.smart.solarman.service.SolarmanStationsService;
import org.nickas21.smart.tuya.service.TuyaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.nickas21.smart.util.HttpUtil.bmsSocKey;
import static org.nickas21.smart.util.HttpUtil.formatter;
import static org.nickas21.smart.util.HttpUtil.formatter_D_M_Y;
import static org.nickas21.smart.util.HttpUtil.getSunRiseSunset;
import static org.nickas21.smart.util.HttpUtil.totalConsumptionPowerKey;
import static org.nickas21.smart.util.HttpUtil.totalEnergySellKey;
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
        updatePowerValue();
        double bmsSocNew = powerValueRealTimeData.getBmsSocValue();
        updateSunRiseSunSetDate();
        String updateTimeData = formatter.format(new Date(powerValueRealTimeData.getCollectionTime() * 1000));
        String deltaPower = (powerValueRealTimeData.getTotalSolarPower() - powerValueRealTimeData.getTotalConsumptionPower()) + " w";
        log.info("\n-Current real time data [{}] \n-Update real time data [{}] \n-BmsSocNew = [{}] \n-deltaPower [{}]",
                formatter.format(new Date()), updateTimeData, bmsSocNew, deltaPower);
        if (this.bmsSocCur > 0 &&
                this.curDate.getTime() > this.sunRiseDate.getTime() &&
                this.curDate.getTime() <= (this.sunSetDate.getTime() - 3600000)) {
            isDay = true;
            try {
               if (bmsSocNew >= solarmanStationsService.getSolarmanDataSource().getBmsSocMax()) {
                    // Increasing electricity consumption
                    this.setIncreasingElectricityConsumption(bmsSocNew);
                } else if (bmsSocNew < solarmanStationsService.getSolarmanDataSource().getBmsSocMin()) {
                    // Reducing electricity consumption
                    this.setReducingElectricityConsumption(bmsSocNew);
                }  else {
                    // Battery charge/discharge analysis program
                    this.batteryChargeDischarge(bmsSocNew, (powerValueRealTimeData.getTotalSolarPower() - powerValueRealTimeData.getTotalConsumptionPower()));
                }
            } catch (Exception e) {
                if ("token invalid".equals(e.getMessage())) {
                    this.updateTuyaToken();
                } else {
                    log.error("", e);
                }
            }
        } else if (isDay && this.curDate.getTime() > (this.sunSetDate.getTime() - 3600000)) {
            log.info("Reducing electricity consumption, TempSetMin,  SunSet start: [{}].", this.sunSetDate);
            isDay = false;
            try {
                this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMin());
            } catch (Exception e) {
                log.error("SunSet, updateAllThermostat to min.", e);
            }
        }
        bmsSocCur = bmsSocNew;
    }

    private void setIncreasingElectricityConsumption(double bmsSocNew) throws Exception {
        log.info("Increasing electricity consumption, TempSetMax, bmsSocNew [{}].", bmsSocNew);
        this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMax());
    }

    private void setReducingElectricityConsumption(double bmsSocNew) throws Exception {
        log.info("Reducing electricity consumption, TempSetMin,  bmsSocNew [{}].", bmsSocNew);
        this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMin());
    }

    private void batteryChargeDischarge(double bmsSocNew, int deltaPower) throws Exception {
        String stateBmsSoc = (bmsSocNew - bmsSocCur) > 0 ? "charge" : "discharge";
        log.info("Battery analysis. bmsSocCur = [{}], bmsSocNew = [{}], [{}] bmsSoc = [{}], deltaPower [{}]",
                bmsSocCur, bmsSocNew, stateBmsSoc, (bmsSocNew - bmsSocCur), deltaPower);
        if (deltaPower > 0) {             // Battery charge
            this.tuyaDeviceService.updateThermostatBatteryCharge(deltaPower);
        } else if (deltaPower < 0) {      // Battery discharge
            this.tuyaDeviceService.updateThermostatBatteryDischarge(deltaPower);
        }
    }

    @SneakyThrows
    private void updatePowerValue() {
        RealTimeData solarmanRealTimeData = solarmanStationsService.getRealTimeData();

        double bmsSocValue = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsSocKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        int totalSolarPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalSolarPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        int totalConsumptionPower = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalConsumptionPowerKey)).findFirst()
                .map(realTimeDataValue -> Integer.parseInt(realTimeDataValue.getValue())).orElse(0);

        double totalEnergySell = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(totalEnergySellKey)).findFirst()
                .map(realTimeDataValue -> Double.parseDouble(realTimeDataValue.getValue())).orElse(0.0);

        powerValueRealTimeData.setCollectionTime(solarmanRealTimeData.getCollectionTime());
        powerValueRealTimeData.setBmsSocValue(bmsSocValue);
        powerValueRealTimeData.setTotalSolarPower(totalSolarPower);
        powerValueRealTimeData.setTotalConsumptionPower(totalConsumptionPower);
        powerValueRealTimeData.setTotalEnergySell(totalEnergySell);
    }

    private void updateSunRiseSunSetDate() {
        Date curTimeDate = new Date();
        String curTimeDateDMY = formatter_D_M_Y.format(curTimeDate);
        String curDateDMY = this.curDate == null ? null : formatter_D_M_Y.format(this.curDate);
        if (curDateDMY == null || !curDateDMY.equals(curTimeDateDMY)) {
            Calendar[] sunRiseSunSetDate = getSunRiseSunset(solarmanStationsService.getSolarmanDataSource().getLocationLat(),
                    solarmanStationsService.getSolarmanDataSource().getLocationLng());
            this.sunRiseDate = sunRiseSunSetDate[0].getTime();
            this.sunSetDate = sunRiseSunSetDate[1].getTime();
            log.info("Sunrise at: [{}]", this.sunRiseDate);
            log.info("Sunset at: [{}]", this.sunSetDate);
        }
        this.curDate = curTimeDate;
    }

    // cod: [1010], msg: [token invalid]
    private void updateTuyaToken() {
        log.error("Tuya token invalid");
        this.tuyaDeviceService.refreshTuyaToken();
        try {
            log.info("Tuya token update. Rerun setBmsSocCur.");
            setBmsSocCur();
        } catch (Exception e) {
            log.error("", e);
        }
    }
}

