package org.nickas21.smart.tuya.service;

import org.nickas21.smart.tuya.TuyaMessageDataSource;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;

import java.util.concurrent.ExecutorService;

public interface TuDeviceService {

   void init();

   void devicesUpDateStatusValue (TuyaConnectionMsg msg);

   void setConnectionConfiguration (TuyaMessageDataSource connectionConfiguration);
}
