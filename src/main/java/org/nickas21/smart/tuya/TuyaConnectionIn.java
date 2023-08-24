package org.nickas21.smart.tuya;

import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;

public interface TuyaConnectionIn {

    void preDestroy() throws Exception;

    void process(TuyaConnectionMsg msg);
}
