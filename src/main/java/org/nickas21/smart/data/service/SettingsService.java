package org.nickas21.smart.data.service;

import org.nickas21.smart.data.dataEntityDto.DataSettingsDto;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final TuyaDeviceService deviceService;

    public SettingsService(TuyaDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public DataSettingsDto getSettingsGolego() {
        DataSettingsDto dataSettingsDto = new DataSettingsDto();
        dataSettingsDto.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlGolego());
        return dataSettingsDto;
    }

    public DataSettingsDto getSettingsDacha() {
        DataSettingsDto dataSettingsDto = new DataSettingsDto();
        dataSettingsDto.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlDacha());
        dataSettingsDto.setBatteryCriticalNightSocWinter(deviceService.getBatteryCriticalNightSocWinter());
        dataSettingsDto.setLogsDachaLimit(deviceService.getLogsDachaLimit());
        return dataSettingsDto;
    }

    public DataSettingsDto setSettingsGolego(DataSettingsDto settingsGolego) {
        if (settingsGolego.getDevicesChangeHandleControl() != null) {
            deviceService.setDevicesChangeHandleControlGolego(settingsGolego.getDevicesChangeHandleControl());
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
        if (settingsDacha.getBatteryCriticalNightSocWinter() != null) {
            deviceService.setLogsDachaLimit(settingsDacha.getLogsDachaLimit());
        }

        return getSettingsDacha(); // Повертаємо оновлений стан
    }
}