package org.nickas21.smart.solarman.service;

import org.nickas21.smart.solarman.source.SolarmanDataSource;

import java.util.concurrent.ExecutorService;

public interface SolarmanStationsService {

    void setExecutorService(ExecutorService executor);

    void setSolarmanMqttDataSource(SolarmanDataSource solarmanMqttDataConnection);

    void init();

    double getRealTimeDataStart();

    SolarmanDataSource getSolarmanDataSource();

}
