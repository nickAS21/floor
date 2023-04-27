package org.nickas21.smart.solarman.service;

import org.nickas21.smart.solarman.source.SolarmanMqttDataSource;

import java.util.concurrent.ExecutorService;

public interface SolarmanInverterService {

    void setExecutorService(ExecutorService executor);

    void setSolarmanMqttDataSource(SolarmanMqttDataSource solarmanMqttDataConnection);

    void init();
}
