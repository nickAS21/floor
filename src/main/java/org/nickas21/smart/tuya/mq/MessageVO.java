
package org.nickas21.smart.tuya.mq;

import lombok.Data;
import org.nickas21.smart.util.JacksonUtil;

import java.io.Serializable;

@Data
public class MessageVO implements Serializable {

    private String data;
    private Integer protocol;
    private String pv;
    private String sign;
    private Long t;

    @Override
    public String toString() {
        return JacksonUtil.toString(this);
    }
}
