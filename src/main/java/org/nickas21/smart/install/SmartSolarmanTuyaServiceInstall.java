package org.nickas21.smart.install;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.tuya.TuyaConnection;
import org.nickas21.smart.tuya.source.ApiTuyaDataSource;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
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

    @Autowired
    private ApiTuyaDataSource tuyaDataSource;

    @PostConstruct
    public void performInstall() {
        try {
            TuyaMessageDataSource tuyaConnectionConfiguration = tuyaDataSource.getTuyaConnectionConfiguration();

            if (tuyaConnectionConfiguration != null) {
                tuyaConnection.init(tuyaConnectionConfiguration);
                solarmanStationsService.init();
                smartSolarmanTuyaService.solarmanRealTimeDataStart();
            } else {
                log.error("Input parameters error: - TuyaConnectionConfiguration: [null].");
                throw new RuntimeException("Input parameters error: - TuyaConnectionConfiguration: [null].");
            }
        } catch (Exception e) {
            log.error("Unexpected error during SmartSolarmanTuyaService installation!", e);
            throw new SmartSolarmanTuyaException("Unexpected error during SmartSolarmanTuyaService installation!", e);
        }
    }

    @PreDestroy
    public void preDestroy() throws Exception {
        if (this.tuyaConnection != null) {
            this.tuyaConnection.preDestroy();
        }
        log.debug("Stopped executor service, list of returned runnables");
    }
}
