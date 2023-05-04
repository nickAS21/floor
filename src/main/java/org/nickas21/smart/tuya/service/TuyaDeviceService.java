package org.nickas21.smart.tuya.service;

import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;

import java.util.concurrent.ExecutorService;

public interface TuyaDeviceService {

   void init();

   TuyaToken refreshTuyaToken();

   void devicesFromUpDateStatusValue(TuyaConnectionMsg msg);

   void sendPostRequestCommand(String deviceId, String code, Object value, String... deviceName) throws Exception;

   void sendGetRequestLogs(String deviceId, Long start_time, Long end_time, Integer size);

   void updateAllThermostat(Integer temp_set) throws Exception;

   void updateThermostatBatteryCharge(int deltaPower) throws Exception;

   void updateThermostatBatteryDischarge(int deltaPower) throws Exception;

   TuyaMessageDataSource getConnectionConfiguration();

   void setConnectionConfiguration(TuyaMessageDataSource connectionConfiguration);

   void setExecutorService(ExecutorService executor);
}
