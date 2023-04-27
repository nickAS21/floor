package org.nickas21.smart.tuya.service;

import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;

import java.util.concurrent.ExecutorService;

public interface TuyaDeviceService {

   void init();

   void devicesFromUpDateStatusValue(TuyaConnectionMsg msg);

   void sendPostRequestCommand(String deviceId, String code, Object value);

   void sendGetRequestLogs(String deviceId, Long start_time, Long end_time, Integer size);

   void updateAllTermostat (Integer temp_set);

   void setConnectionConfiguration (TuyaMessageDataSource connectionConfiguration);

   void setExecutorService(ExecutorService executor);
}
