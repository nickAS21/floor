package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataSettingsDto {

    // Спільне поле
    private String versionBackend;
    private Boolean devicesChangeHandleControl;
    private Integer logsAppLimit;
    // Специфічне поле (використовуємо Double замість double, щоб воно могло бути null)
    private Double batteryCriticalNightSocWinter;
    private Boolean heaterNightAutoOnDachaWinter;
    private Integer seasonsId;
}