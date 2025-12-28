package org.nickas21.smart.data.dataEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
// Ця анотація приховає null-поля в JSON, щоб не надсилати зайвого
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataSettings {

    // Спільне поле
    private Boolean devicesChangeHandleControl;

    // Специфічне поле (використовуємо Double замість double, щоб воно могло бути null)
    private Double batteryCriticalNightSocWinter;

}