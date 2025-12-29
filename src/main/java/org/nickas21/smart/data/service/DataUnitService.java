package org.nickas21.smart.data.service;

import org.jvnet.hk2.annotations.Service;
import org.nickas21.smart.data.dataEntity.DataSettings;
import org.nickas21.smart.tuya.TuyaDeviceService;

@Service
public class DataUnitService {

    private final TuyaDeviceService deviceService;

    public DataUnitService(TuyaDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public DataSettings getUintGolego() {
        DataSettings dataSettings = new DataSettings();
        dataSettings.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlGolego());
        return dataSettings;
    }

    public DataSettings getUnitDacha() {
        DataSettings dataSettings = new DataSettings();
        dataSettings.setDevicesChangeHandleControl(deviceService.isDevicesChangeHandleControlDacha());
        dataSettings.setBatteryCriticalNightSocWinter(deviceService.getBatteryCriticalNightSocWinter());
        return dataSettings;
    }
}
