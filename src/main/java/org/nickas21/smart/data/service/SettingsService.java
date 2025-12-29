package org.nickas21.smart.data.service;

import org.nickas21.smart.data.dataEntity.DataSettings;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final TuyaDeviceService deviceService;

    public SettingsService(TuyaDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public DataSettings getSettingsGolego() {
        DataSettings dataSettings = new DataSettings();
        dataSettings.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlGolego());
        return dataSettings;
    }

    public DataSettings getSettingsDacha() {
        DataSettings dataSettings = new DataSettings();
        dataSettings.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlDacha());
        dataSettings.setBatteryCriticalNightSocWinter(deviceService.getBatteryCriticalNightSocWinter());
        dataSettings.setLogsDachaLimit(deviceService.getLogsDachaLimit());
        return dataSettings;
    }

    public DataSettings setSettingsGolego(DataSettings settingsGolego) {
        if (settingsGolego.getDevicesChangeHandleControl() != null) {
            deviceService.setDevicesChangeHandleControlGolego(settingsGolego.getDevicesChangeHandleControl());
        }
        return getSettingsGolego();
    }

    public DataSettings setSettingsDacha(DataSettings settingsDacha) {
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