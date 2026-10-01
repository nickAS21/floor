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
        }
        if (valueOld == null) {
            return true;
        }
        Object valNew = unwrapValue(valueNew);
        Object valOld = unwrapValue(valueOld);
        return !valNew.equals(valOld);
    }

    public Boolean getValueNewAsBoolean() {
        Object unwrapped = unwrapValue(valueNew);
        return unwrapped instanceof Boolean ? (Boolean) unwrapped : null;
    }

    private Object unwrapValue(Object val) {
        if (val instanceof BooleanNode) {
            return ((BooleanNode) val).asBoolean();
        }
        return val;
    }
}