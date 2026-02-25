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

    // Спільні поля
    private String versionBackend;
    private Boolean devicesChangeHandleControl;
    private Integer logsAppLimit;
    // Специфічне поле (використовуємо Double замість double, щоб воно могло бути null)
    private Double batteryCriticalNightSocWinter; // BatteryStatus -> max = STATIC("Static", 98.00) min =  DISCHARGING("Discharging", 40.00),
    private Boolean heaterGridOnAutoAllDay;
    // Dacha
    private Boolean heaterNightAutoOnDachaWinter;
    private Integer seasonsId;

    public DataSettingsDto(String versionBackend, Boolean devicesChangeHandleControl, Integer logsAppLimit, Double batteryCriticalNightSocWinter, Boolean heaterGridOnAutoAllDay) {
        this.versionBackend = versionBackend;
        this.devicesChangeHandleControl = devicesChangeHandleControl;
        this.logsAppLimit = logsAppLimit;
        this.batteryCriticalNightSocWinter = batteryCriticalNightSocWinter;
        this.heaterGridOnAutoAllDay = heaterGridOnAutoAllDay;
    }
}