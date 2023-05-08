package org.nickas21.smart.solarman.service;

import org.nickas21.smart.solarman.mq.RealTimeData;
import org.nickas21.smart.solarman.source.SolarmanDataSource;

import java.util.concurrent.CountDownLatch;

public interface SolarmanStationsService {

    void setSolarmanMqttDataSource(SolarmanDataSource solarmanMqttDataConnection);

    void init(CountDownLatch c);

    RealTimeData getRealTimeData();

    SolarmanDataSource getSolarmanDataSource();

}
