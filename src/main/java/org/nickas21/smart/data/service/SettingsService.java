package org.nickas21.smart.data.service;

import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntityDto.DataSettingsDto;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final TuyaDeviceService deviceService;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;

    public SettingsService(TuyaDeviceService deviceService, DefaultSmartSolarmanTuyaService solarmanTuyaService) {
        this.deviceService = deviceService;
        this.solarmanTuyaService = solarmanTuyaService;
    }

    public DataSettingsDto getSettingsGolego() {
        DataSettingsDto dataSettingsDto = new DataSettingsDto();
        dataSettingsDto.setVersionBackend( solarmanTuyaService.getVersion());
        dataSettingsDto.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlGolego());
        dataSettingsDto.setLogsAppLimit(deviceService.getLogsAppLimit());
        return dataSettingsDto;
    }

    public DataSettingsDto getSettingsDacha() {
        DataSettingsDto dataSettingsDto = new DataSettingsDto();
        dataSettingsDto.setVersionBackend( solarmanTuyaService.getVersion());
        dataSettingsDto.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlDacha());
        dataSettingsDto.setLogsAppLimit(deviceService.getLogsAppLimit());
        dataSettingsDto.setBatteryCriticalNightSocWinter(deviceService.getBatteryCriticalNightSocWinter());
        return dataSettingsDto;
    }

    public DataSettingsDto setSettingsGolego(DataSettingsDto settingsGolego) {
        if (settingsGolego.getDevicesChangeHandleControl() != null) {
            deviceService.setDevicesChangeHandleControlGolego(settingsGolego.getDevicesChangeHandleControl());
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
            deviceService.setBatteryCriticalNightSocWinter(settingsDacha.getBatteryCriticalNightSocWinter());
        }
        if (settingsDacha.getLogsAppLimit() != null) {
            deviceService.setLogsAppLimit(settingsDacha.getLogsAppLimit());
        }

        return getSettingsDacha(); // Повертаємо оновлений стан
    }
}