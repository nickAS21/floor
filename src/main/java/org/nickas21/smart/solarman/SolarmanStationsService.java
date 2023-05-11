package org.nickas21.smart.solarman;

import org.nickas21.smart.solarman.mq.RealTimeData;
import org.nickas21.smart.solarman.source.SolarmanDataSource;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public interface SolarmanStationsService {

    void init(CountDownLatch c, SolarmanDataSource solarmanDataConnection, ExecutorService executor)  throws Exception;

    RealTimeData getRealTimeData() throws IOException;

    SolarmanDataSource getSolarmanDataSource();

}
