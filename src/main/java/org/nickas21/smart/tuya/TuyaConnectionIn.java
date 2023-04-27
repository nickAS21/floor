package org.nickas21.smart.tuya;

import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;

public interface TuyaConnectionIn {

//    void update(String accessId, String accessKey) throws Exception;

    void destroy();

    void process(TuyaConnectionMsg msg);
}
