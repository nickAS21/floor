package org.nickas21.smart.tuya.tuyaEntity;

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
        return valueNew != null && !valueNew.equals(valueOld);
    }
}
