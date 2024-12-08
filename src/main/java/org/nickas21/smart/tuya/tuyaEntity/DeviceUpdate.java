package org.nickas21.smart.tuya.tuyaEntity;

import com.fasterxml.jackson.databind.node.BooleanNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
public class DeviceUpdate {
    String fieldNameValueUpdate;
    Object valueNew;
    Object valueOld;

    public boolean isUpdate() {
        if (valueNew == null) {
            return false;
        } else if (valueOld == null) {
            return true;
        } else {
            Object valNew = valueNew instanceof BooleanNode ? ((BooleanNode) valueNew).asBoolean() : valueNew;
            Object valOld = valueOld instanceof BooleanNode ? ((BooleanNode) valueOld).asBoolean() : valueOld;
            return !valNew.equals(valOld);
        }
    }
}
