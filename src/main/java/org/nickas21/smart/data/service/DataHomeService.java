package org.nickas21.smart.data.service;

import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntityDto.DataHomeDto;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;
import org.springframework.stereotype.Service;

@Service
public class DataHomeService {

    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;
    private final TuyaDeviceService deviceService;
    private final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final UsrTcpWiFiService usrTcpWiFiService;

    public DataHomeService(DefaultSmartSolarmanTuyaService solarmanTuyaService, TuyaDeviceService deviceService, UsrTcpWiFiParseData usrTcpWiFiParseData, UsrTcpWiFiService usrTcpWiFiService) {
        this.solarmanTuyaService = solarmanTuyaService;
        this.deviceService = deviceService;
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.usrTcpWiFiService = usrTcpWiFiService;
    }

    public DataHomeDto getDataGolego() {
        return new DataHomeDto(this.deviceService, this.usrTcpWiFiParseData, this.usrTcpWiFiService);
    }

    public DataHomeDto getDataDacha() {
        return new DataHomeDto(this.solarmanTuyaService, this.deviceService, this.usrTcpWiFiService);
    }
}
