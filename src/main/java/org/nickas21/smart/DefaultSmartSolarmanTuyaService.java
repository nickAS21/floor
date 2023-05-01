package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.service.SolarmanStationsService;
import org.nickas21.smart.tuya.service.TuyaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.nickas21.smart.util.HttpUtil.formatter;

@Slf4j
@Service
public class DefaultSmartSolarmanTuyaService implements SmartSolarmanTuyaService {

    @Autowired
    SolarmanStationsService solarmanStationsService;

    @Autowired
    TuyaDeviceService tuyaDeviceService;

    @Override
    public void solarmanRealTimeDataStart() {
        do {
            double bmsSoc = solarmanStationsService.getRealTimeDataStart();
            if (bmsSoc < solarmanStationsService.getSolarmanDataSource().getBmsSocMin()) {
                // Reducing electricity consumption
                log.info("Reducing electricity consumption bmsSoc: [{}]", bmsSoc);
            } else if (bmsSoc > solarmanStationsService.getSolarmanDataSource().getBmsSocMax()) {
                // Increasing electricity consumption
                log.info("Increasing electricity consumption bmsSoc: [{}]", bmsSoc);
            } else {
                log.info("bmsSoc: [{}]", bmsSoc);
            }
            try {
                log.info("Current real time data [{}] ", formatter.format(new Date()));
                TimeUnit.SECONDS.sleep(solarmanStationsService.getSolarmanDataSource().getTimeOutSec());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } while (true);
    }
}
