package org.nickas21.smart.data.service;


import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntityDto.BatteryInfoDto;
import org.nickas21.smart.data.dataEntityDto.DataDeviceDto;
import org.nickas21.smart.data.dataEntityDto.DataInverterDto;
import org.nickas21.smart.data.dataEntityDto.DataUnitDto;
import org.nickas21.smart.data.dataEntityDto.InverterInfo;
import org.nickas21.smart.usr.entity.golego.BatteryDataUsrTcpWiFi;
import org.nickas21.smart.usr.service.UsrTcpWiFiBatteryRegistry;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;
import org.nickas21.smart.util.LocationType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        var props = usrTcpWiFiService.getTcpProps();
        Integer portInverterDacha = props.getPortInverterDacha();
        Long lastTimestamp = usrTcpWiFiService.getLastTimeActiveByPort(portInverterDacha).orElse(0L);
        String timestamp =  formatTimestamp(lastTimestamp, datePatternGridStatus);

        String connectionStatus = usrTcpWiFiService.getStatusByPort(portInverterDacha);
        String connectionStatusAll = props.getAllPortsInverterDacha().stream()
                .map(port -> {
                    String role = (port.equals(portInverterDacha)) ? "master" : "slave" + (port - portInverterDacha);
                    String status = usrTcpWiFiService.getStatusByPort(port);
                    return "%s: port - %d status - %s;".formatted(role, port, status);
                })
                .collect(Collectors.joining(" "));
        InverterInfo inverterInfo = InverterInfo.DACHA;
        inverterInfo.setModelName("SUN-12K-SG05LP3-EU-SM2 -> " + connectionStatusAll);

        DataInverterDto dataInverterDto = new DataInverterDto(timestamp, portInverterDacha, connectionStatus, inverterInfo);
        return new DataUnitDto(batteries, dataInverterDto, devices);
    }

    public List<BatteryInfoDto> getBatteries (LocationType location) {
        List<BatteryInfoDto> batteries = new ArrayList<>();
        if (GOLEGO.equals(location)) {
            Map<Integer, BatteryDataUsrTcpWiFi> batteriesAll = this.usrTcpWiFiBatteryRegistry.getBatteriesAll(BatteryDataUsrTcpWiFi.class);
            for (Map.Entry<Integer, BatteryDataUsrTcpWiFi> entry : batteriesAll.entrySet()) {
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

