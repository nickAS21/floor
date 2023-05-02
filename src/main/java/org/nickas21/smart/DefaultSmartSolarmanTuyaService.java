package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.mq.RealTimeData;
import org.nickas21.smart.solarman.mq.RealTimeDataValue;
import org.nickas21.smart.solarman.service.SolarmanStationsService;
import org.nickas21.smart.tuya.service.TuyaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.nickas21.smart.util.HttpUtil.bmsSoc;
import static org.nickas21.smart.util.HttpUtil.formatter;

@Slf4j
@Service
public class DefaultSmartSolarmanTuyaService implements SmartSolarmanTuyaService {
    private double bmsSocCur;

    @Autowired
    SolarmanStationsService solarmanStationsService;

    @Autowired
    TuyaDeviceService tuyaDeviceService;

    @Override
    public void solarmanRealTimeDataStart() {
        bmsSocCur = getBmsSocValue();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::setBmsSocCur, 0, solarmanStationsService.getSolarmanDataSource().getTimeOutSec(), TimeUnit.SECONDS);
     }

    private void setBmsSocCur() {
        double bmsSocNew = getBmsSocValue();
        if (bmsSocNew != bmsSocCur) {
            if (bmsSocNew < solarmanStationsService.getSolarmanDataSource().getBmsSocMin()) {
                // Reducing electricity consumption
                this.setReducingElectricityCons();
                log.info("Reducing electricity consumption bmsSoc.");
            } else if (bmsSocNew > solarmanStationsService.getSolarmanDataSource().getBmsSocMax()) {
                // Increasing electricity consumption
                this.setIncreasingElectricityCons();
                log.info("Increasing electricity consumption bmsSoc.");
            }
            bmsSocCur = bmsSocNew;
        }
    }


    //        String deviceIdTest = "bf11fce4b500291373jnn2";
//        sendPostRequestCommand(deviceIdTest, "temp_set", 5); // temp_current
    private void setReducingElectricityCons() {
        this.tuyaDeviceService.updateAllTermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMin());
    }

    private void setIncreasingElectricityCons() {
        this.tuyaDeviceService.updateAllTermostat(this.tuyaDeviceService.getConnectionConfiguration().getTempSetMax());
    }

    private double getBmsSocValue() {
        RealTimeData solarmanRealTimeData = solarmanStationsService.getRealTimeDataStart();
        Optional<RealTimeDataValue> realTimeDataValueOpt = solarmanRealTimeData.getDataList().stream().filter(value -> value.getKey().equals(bmsSoc)).findFirst();
        double bmsSocValue = realTimeDataValueOpt.isPresent() ? Double.valueOf(realTimeDataValueOpt.get().getValue()) : 0;
        String update = realTimeDataValueOpt.isPresent() ? formatter.format(new Date(solarmanRealTimeData.getCollectionTime())) : "error";
        log.info("bmsSocValue: [{}] Update real time data [{}]  Current real time data [{}] ", bmsSocValue, update, formatter.format(new Date()));
        return bmsSocValue;
    }



}
