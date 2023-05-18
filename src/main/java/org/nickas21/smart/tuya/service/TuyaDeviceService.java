package org.nickas21.smart.tuya.service;

import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import java.util.concurrent.ExecutorService;

public interface TuyaDeviceService {

   void init(ExecutorService executor) throws Exception;

   void devicesFromUpDateStatusValue(TuyaConnectionMsg msg) throws Exception;

   void sendPostRequestCommand(String deviceId, String code, Object value, String... deviceName) throws Exception;

   void sendGetRequestLogs(String deviceId, Long start_time, Long end_time, Integer size);

   void updateAllThermostat(Integer temp_set, String... filters) throws Exception;

   void updateThermostatBatteryCharge(int deltaPower, String... filters) throws Exception;

   void updateThermostatBatteryDischarge(int deltaPower, String... filters) throws Exception;

   TuyaMessageDataSource getConnectionConfiguration();

   void setConnectionConfiguration(TuyaMessageDataSource connectionConfiguration);

   void destroy() throws Exception;

}
