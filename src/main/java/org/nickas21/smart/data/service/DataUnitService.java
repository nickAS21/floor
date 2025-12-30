package org.nickas21.smart.data.service;


import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntityDto.BatteryInfoDto;
import org.nickas21.smart.data.dataEntityDto.DataUnitDto;
import org.nickas21.smart.data.dataEntityDto.DeviceDto;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.service.UsrTcpWiFiBatteryRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataUnitService {

    private final UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;

    public DataUnitService(UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry, DefaultSmartSolarmanTuyaService solarmanTuyaService) {
        this.usrTcpWiFiBatteryRegistry = usrTcpWiFiBatteryRegistry;
        this.solarmanTuyaService = solarmanTuyaService;
    }

    public DataUnitDto getUnitGolego() {
        Map<Integer, UsrTcpWiFiBattery> batteriesAll = this.usrTcpWiFiBatteryRegistry.getAll();
        List<BatteryInfoDto> batteries = new ArrayList<>();
        List<DeviceDto> devices = new ArrayList<>();
        for(Map.Entry<Integer, UsrTcpWiFiBattery> entry : batteriesAll.entrySet()) {
            BatteryInfoDto batteryInfoDto = new BatteryInfoDto(entry, this.solarmanTuyaService.getTimeoutSecUpdate());
            batteries.add(batteryInfoDto);
        }
        return new DataUnitDto(batteries, devices);
    }

//    public DataHomeDto getUnitDacha(devices) {
//
//    }
}

