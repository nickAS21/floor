package org.nickas21.smart.tuya;

import org.nickas21.smart.tuya.mq.MqEnv;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.util.TuyaRegion;

public interface TuyaConnectionIn {

    void init(TuyaConnectionConfiguration conf) throws Exception;

    void update(String accessId, String accessKey) throws Exception;

    void destroy();

    void process(TuyaConnectionMsg msg);
}
