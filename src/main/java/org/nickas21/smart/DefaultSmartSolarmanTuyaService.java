package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.service.SolarmanStationsService;
import org.nickas21.smart.tuya.service.TuyaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        bmsSocCur = solarmanStationsService.getRealTimeDataStart();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::setBmsSocCur, 0, solarmanStationsService.getSolarmanDataSource().getTimeOutSec(), TimeUnit.SECONDS);
     }

    private void setBmsSocCur() {
        double bmsSocNew = solarmanStationsService.getRealTimeDataStart();
        if (bmsSocNew != bmsSocCur) {
            if (bmsSocNew < solarmanStationsService.getSolarmanDataSource().getBmsSocMin()) {
                // Reducing electricity consumption
                log.info("Reducing electricity consumption bmsSoc: [{}]", bmsSocNew);
            } else if (bmsSocNew > solarmanStationsService.getSolarmanDataSource().getBmsSocMax()) {
                // Increasing electricity consumption
                log.info("Increasing electricity consumption bmsSoc: [{}]", bmsSocNew);
            }
            bmsSocCur = bmsSocNew;
        }
        log.info("bmsSoc: [{}] Current real time data [{}] ", bmsSocCur, formatter.format(new Date()));
    }


}
