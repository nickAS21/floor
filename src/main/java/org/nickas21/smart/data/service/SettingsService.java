package org.nickas21.smart.data.service;

import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntityDto.DataSettingsDto;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final TuyaDeviceService deviceService;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;
    private final SolarmanStationsService solarmanStationsService;

    public SettingsService(TuyaDeviceService deviceService, DefaultSmartSolarmanTuyaService solarmanTuyaService, SolarmanStationsService solarmanStationsService) {
        this.deviceService = deviceService;
        this.solarmanTuyaService = solarmanTuyaService;
        this.solarmanStationsService = solarmanStationsService;
    }

    public DataSettingsDto getSettingsGolego() {
        DataSettingsDto dataSettingsDto = new DataSettingsDto(
                solarmanTuyaService.getVersion(),
                deviceService.isDevicesChangeHandleControlGolego(),
                deviceService.getLogsAppLimit(),
                deviceService.getBatteryCriticalNightSocWinterGolego(),
                deviceService.isHeaterGridOnAutoAllDayGolego()
                );

        return dataSettingsDto;
    }

    public DataSettingsDto getSettingsDacha() {
        DataSettingsDto dataSettingsDto = new DataSettingsDto(
                solarmanTuyaService.getVersion(),
                deviceService.isDevicesChangeHandleControlDacha(),
                deviceService.getLogsAppLimit(),
                deviceService.getBatteryCriticalNightSocWinterDacha(),
                deviceService.isHeaterGridOnAutoAllDayDacha()
        );
        dataSettingsDto.setHeaterNightAutoOnDachaWinter(deviceService.isHeaterNightAutoOnDachaWinter());
        if (solarmanStationsService.getSolarmanStation() != null) {
            dataSettingsDto.setSeasonsId(solarmanStationsService.getSolarmanStation().getSeasonsId());
        }
        return dataSettingsDto;
    }

    public DataSettingsDto setSettingsGolego(DataSettingsDto settingsGolego) {
        if (settingsGolego.getDevicesChangeHandleControl() != null) {
            deviceService.setDevicesChangeHandleControlGolego(settingsGolego.getDevicesChangeHandleControl()); //
        }
        if (settingsGolego.getBatteryCriticalNightSocWinter() != null) {
            deviceService.setBatteryCriticalNightSocWinterGolego(settingsGolego.getBatteryCriticalNightSocWinter());
        }
        if (settingsGolego.getHeaterGridOnAutoAllDay() != null) {
            deviceService.setHeaterGridOnAutoAllDayGolego(settingsGolego.getHeaterGridOnAutoAllDay());
        }
        if (settingsGolego.getLogsAppLimit() != null) {
            deviceService.setLogsAppLimit(settingsGolego.getLogsAppLimit());
        }
        return getSettingsGolego();
    }

    public DataSettingsDto setSettingsDacha(DataSettingsDto settingsDacha) {
        if (settingsDacha.getDevicesChangeHandleControl() != null) {
            deviceService.setDevicesChangeHandleControlDacha(settingsDacha.getDevicesChangeHandleControl());
        }
        if (settingsDacha.getBatteryCriticalNightSocWinter() != null) {
            deviceService.setBatteryCriticalNightSocWinterDacha(settingsDacha.getBatteryCriticalNightSocWinter());
        }
        if (settingsDacha.getHeaterNightAutoOnDachaWinter() != null) {
            deviceService.setHeaterNightAutoOnDachaWinter(settingsDacha.getHeaterNightAutoOnDachaWinter());
        }
        if (settingsDacha.getHeaterGridOnAutoAllDay() != null) {
            deviceService.setHeaterGridOnAutoAllDayDacha(settingsDacha.getHeaterGridOnAutoAllDay());
        }
        if (settingsDacha.getLogsAppLimit() != null) {
            deviceService.setLogsAppLimit(settingsDacha.getLogsAppLimit());
        }
        if (settingsDacha.getSeasonsId() != null) {
            solarmanStationsService.getSolarmanStation().setSeasonsId(settingsDacha.getSeasonsId());
        }

        return getSettingsDacha(); // Повертаємо оновлений стан
    }
}