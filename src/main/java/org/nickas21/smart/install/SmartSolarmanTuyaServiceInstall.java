package org.nickas21.smart.install;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.service.TelegramService;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.tuya.TuyaConnection;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    TelegramService telegramService;

    @Autowired
    UsrTcpWiFiService usrTcpWiFiService;

    @Value("${app.test_front:false}")
    boolean testFront;

    @PostConstruct
    public void performInstall() {
        if (!testFront) {
            tuyaConnection.init();
            solarmanStationsService.init();
            smartSolarmanTuyaService.solarmanRealTimeDataStart();
            telegramService.init();
        }
        // TO DO move to UP after finish tests front with BMS Golego
        usrTcpWiFiService.init();
    }

    @PreDestroy
    public void cleanup() throws Exception {
        if (this.smartSolarmanTuyaService != null) {
            this.smartSolarmanTuyaService.cleanup();
        }
        if (this.tuyaConnection != null) {
            this.tuyaConnection.cleanup();
        }
        log.info("Stopped executor service, list of returned runnables");
    }
}
