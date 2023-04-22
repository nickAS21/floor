package org.nickas21.smart.tuya.service;

import org.nickas21.smart.tuya.TuyaMessageDataSource;

import java.util.concurrent.ExecutorService;

public interface TuDeviceService {
   void setExecutorService (ExecutorService executor);

   void setConnectionConfiguration (TuyaMessageDataSource connectionConfiguration);
}
