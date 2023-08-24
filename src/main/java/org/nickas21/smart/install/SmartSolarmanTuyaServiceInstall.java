package org.nickas21.smart.install;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.solarman.source.ApiSolarmanDataSource;
import org.nickas21.smart.solarman.source.SolarmanDataSource;
import org.nickas21.smart.tuya.TuyaConnection;
import org.nickas21.smart.tuya.source.ApiTuyaDataSource;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.nickas21.smart.util.SmartSolarmanTuyaThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@ComponentScan({"org.nickas21.smart"})
public class SmartSolarmanTuyaServiceInstall {

//    final ExecutorService submitExecutor = Executors.newFixedThreadPool(3, SmartSolarmanTuyaThreadFactory.forName(getClass().getSimpleName() + "-smart_T_S"));
    final ExecutorService submitExecutor = Executors.newFixedThreadPool(3, SmartSolarmanTuyaThreadFactory.forName("smart_S_T"));

    @Autowired
    private TuyaConnection tuyaConnection;

    @Autowired
    private SolarmanStationsService solarmanStationsService;

    @Autowired
    DefaultSmartSolarmanTuyaService smartSolarmanTuyaService;

    @Autowired
    private ApiTuyaDataSource tuyaDataSource;

    @Autowired
    private ApiSolarmanDataSource solarmanDataSource;

    public TuyaMessageDataSource performInstall() {
        try {
            TuyaMessageDataSource tuyaConnectionConfiguration = tuyaDataSource.getTuyaConnectionConfiguration();

            if (tuyaConnectionConfiguration != null) {
                tuyaConnection.init(tuyaConnectionConfiguration, submitExecutor);
                SolarmanDataSource solarmanDataConnection = solarmanDataSource.getSolarmanDataSource();
                CountDownLatch cdl = new CountDownLatch(1);
                solarmanStationsService.init(cdl, solarmanDataConnection, submitExecutor);
                cdl.await();
                smartSolarmanTuyaService.solarmanRealTimeDataStart();
                return tuyaConnectionConfiguration;
            } else {
                log.error("Input parameters error: - TuyaConnectionConfiguration: [null].");
                return null;
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
        List<Runnable> runnableList = submitExecutor.shutdownNow();
        log.debug("Stopped executor service, list of returned runnables: {}", runnableList);
    }
}
