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

    @Autowired
    SolarmanStationsService solarmanStationsService;

    @Autowired
    TuyaDeviceService tuyaDeviceService;

    @Override
    public void solarmanRealTimeDataStart() { this.isDay = true;
        this.bmsSocCur = 0;
        this.powerValueRealTimeData = PowerValueRealTimeData.builder().build();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::setBmsSocCur, 0, solarmanStationsService.getSolarmanDataSource().getTimeOutSec(), TimeUnit.SECONDS);
    }

    private void setBmsSocCur() {
        Calendar[] sunRiseSunSet = getSunRiseSunset(solarmanStationsService.getSolarmanDataSource().getLocationLat(),
                solarmanStationsService.getSolarmanDataSource().getLocationLng());
        Date currentTime = new Date();
        log.info("Sunrise at: [{}] [{}]", sunRiseSunSet[0].getTime(), sunRiseSunSet[0].getTime().getTime());
        log.info("Sunset at: [{}] [{}]", sunRiseSunSet[1].getTime(), sunRiseSunSet[1].getTime().getTime());
        log.info("CurrentTime at: [{}] [{}]", currentTime, currentTime.getTime());
        if (currentTime.getTime() > sunRiseSunSet[0].getTime().getTime() && currentTime.getTime() <= (sunRiseSunSet[1].getTime().getTime() - 3600000)) {
            isDay = true;
            updatePowerValue();
            double bmsSocNew = powerValueRealTimeData.getBmsSocValue();
            String updateTimeData = formatter.format(new Date(powerValueRealTimeData.getCollectionTime() * 1000));
            log.info("BmsSocNew = [{}] Update real time data [{}]  Current real time data [{}] ", bmsSocNew, updateTimeData, formatter.format(new Date()));
            try {
                if (bmsSocNew < solarmanStationsService.getSolarmanDataSource().getBmsSocMin()) {
                    // Reducing electricity consumption
                    this.setReducingElectricityConsumption(bmsSocNew);
                } else if (bmsSocNew > solarmanStationsService.getSolarmanDataSource().getBmsSocMax() || this.bmsSocCur == 0) {
                    // Increasing electricity consumption
                    this.setIncreasingElectricityConsumption(bmsSocNew);
                } else {
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
            bmsSocCur = bmsSocNew;
        } else if (isDay && currentTime.getTime() > (sunRiseSunSet[1].getTime().getTime() - 3600000)) {
            log.info("Reducing electricity consumption, TempSetMin,  SunSet start: [{}].", sunRiseSunSet[1].getTime());
            isDay = false;
            try {
                this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMin());
            } catch (Exception e) {
                log.error("SunSet, updateAllThermostat to min.", e);
            }
        }
    }

    private void setReducingElectricityConsumption(double bmsSocNew) throws Exception {
        log.info("Reducing electricity consumption, TempSetMin,  bmsSocNew [{}].", bmsSocNew);
        this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMin());
    }

    private void setIncreasingElectricityConsumption(double bmsSocNew) throws Exception {
        log.info("Increasing electricity consumption, TempSetMax, bmsSocNew [{}].", bmsSocNew);
        this.tuyaDeviceService.updateAllThermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMax());
    }

    private void batteryChargeDischarge(double bmsSocNew, int deltaPower) throws Exception {
        String stateBmsSoc = (bmsSocNew - bmsSocCur) > 0 ? "charge" : "discharge";
        log.info("Battery analysis. bmsSocCur = [{}], bmsSocNew = [{}], [{}] bmsSoc = [{}], deltaPower [{}]",
                bmsSocCur, bmsSocNew, stateBmsSoc, (bmsSocNew - bmsSocCur), deltaPower);
        if ((bmsSocNew - bmsSocCur) >= solarmanStationsService.getSolarmanDataSource().getBmsSocStepValueChange()
                && deltaPower > 0) {             // Battery charge
            this.tuyaDeviceService.updateThermostatBatteryCharge(deltaPower);
        } else if (((bmsSocNew - bmsSocCur) <= (-solarmanStationsService.getSolarmanDataSource().getBmsSocStepValueChange())
                || bmsSocNew == bmsSocCur) && deltaPower < 0) {      // Battery discharge
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

