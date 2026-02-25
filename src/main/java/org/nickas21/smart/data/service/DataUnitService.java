package org.nickas21.smart.data.service;


import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntityDto.BatteryInfoDto;
import org.nickas21.smart.data.dataEntityDto.DataUnitDto;
import org.nickas21.smart.data.dataEntityDto.DataDeviceDto;
import org.nickas21.smart.data.dataEntityDto.DataInverterDto;
import org.nickas21.smart.data.dataEntityDto.InverterInfo;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.service.UsrTcpWiFiBatteryRegistry;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;
import org.nickas21.smart.util.LocationType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.datePatternGridStatus;
import static org.nickas21.smart.util.LocationType.DACHA;
import static org.nickas21.smart.util.LocationType.GOLEGO;
import static org.nickas21.smart.util.StringUtils.formatTimestamp;

@Service
public class DataUnitService {

    private final UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;
    private final UsrTcpWiFiService usrTcpWiFiService;


    public DataUnitService(UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry, DefaultSmartSolarmanTuyaService solarmanTuyaService, UsrTcpWiFiService usrTcpWiFiService) {
        this.usrTcpWiFiBatteryRegistry = usrTcpWiFiBatteryRegistry;
        this.solarmanTuyaService = solarmanTuyaService;
        this.usrTcpWiFiService = usrTcpWiFiService;
    }

    public DataUnitDto getUnitGolego() {
        List<BatteryInfoDto> batteries = this.getBatteries (GOLEGO);
        List<DataDeviceDto> devices = new ArrayList<>();
        Integer portInverterGolego = usrTcpWiFiService.getTcpProps().getPortInverterGolego();
        Long lastTimestamp = usrTcpWiFiService.getLastTimeActiveByPort(portInverterGolego).orElse(0L);
        String timestamp =  formatTimestamp(lastTimestamp, datePatternGridStatus);
        String connectionStatus = usrTcpWiFiService.getStatusByPort(portInverterGolego);
        DataInverterDto dataInverterDto = new DataInverterDto(timestamp, portInverterGolego, connectionStatus, InverterInfo.GOLEGO);
        return new DataUnitDto(batteries, dataInverterDto, devices);
    }

    public DataUnitDto getUnitDacha() {
        List<BatteryInfoDto> batteries = this.getBatteries (DACHA);
        List<DataDeviceDto> devices = new ArrayList<>();
        Integer portInverterDacha = usrTcpWiFiService.getTcpProps().getPortInverterDacha();
        Long lastTimestamp = usrTcpWiFiService.getLastTimeActiveByPort(portInverterDacha).orElse(0L);
        String timestamp =  formatTimestamp(lastTimestamp, datePatternGridStatus);
        String connectionStatus = usrTcpWiFiService.getStatusByPort(portInverterDacha);
        DataInverterDto dataInverterDto = new DataInverterDto(timestamp, portInverterDacha, connectionStatus, InverterInfo.DACHA);
        return new DataUnitDto(batteries, dataInverterDto, devices);
    }

    public List<BatteryInfoDto> getBatteries (LocationType location) {
        List<BatteryInfoDto> batteries = new ArrayList<>();
        if (GOLEGO.equals(location)) {
            Map<Integer, UsrTcpWiFiBattery> batteriesAll = this.usrTcpWiFiBatteryRegistry.getBatteriesAll();
            for (Map.Entry<Integer, UsrTcpWiFiBattery> entry : batteriesAll.entrySet()) {
                if (!entry.getKey().equals(this.usrTcpWiFiService.getTcpProps().getPortInverterGolego()) &&
                        !entry.getKey().equals(this.usrTcpWiFiService.getTcpProps().getPortInverterDacha())) {
                    BatteryInfoDto batteryInfoDto = new BatteryInfoDto(entry, this.usrTcpWiFiService);
                    batteries.add(batteryInfoDto);
                }
            }
            return batteries;
        } else if (DACHA.equals(location)) {
            BatteryInfoDto batteryInfoDto = new BatteryInfoDto(this.solarmanTuyaService, this.usrTcpWiFiService);
            batteries.add(batteryInfoDto);
        }
        return  batteries;
    }

}

