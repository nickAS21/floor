package org.nickas21.smart.install;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.tuya.TuyaConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmartSolarmanTuyaServiceInstall {

    @Autowired
    private TuyaConnection tuyaConnection;

    @Autowired
    private SolarmanStationsService solarmanStationsService;

    @Autowired
    DefaultSmartSolarmanTuyaService smartSolarmanTuyaService;

    @PostConstruct
    public void performInstall() {
        tuyaConnection.init();
        solarmanStationsService.init();
        smartSolarmanTuyaService.solarmanRealTimeDataStart();
    }

    @PreDestroy
    public void preDestroy() throws Exception {
        if (this.tuyaConnection != null) {
            this.tuyaConnection.preDestroy();
        }
        log.debug("Stopped executor service, list of returned runnables");
    }
}
