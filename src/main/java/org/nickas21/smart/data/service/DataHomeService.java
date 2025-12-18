package org.nickas21.smart.data.service;

import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntity.DataHome;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.PORT_MASTER;

@Service
public class DataHomeService {

    @Autowired
    @Lazy
    UsrTcpWiFiParseData usrTcpWiFiParseData;

    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;
    private final TuyaDeviceService deviceService;

    public DataHomeService(DefaultSmartSolarmanTuyaService solarmanTuyaService, TuyaDeviceService deviceService) {
        this.solarmanTuyaService = solarmanTuyaService;
        this.deviceService = deviceService;
    }

    public DataHome getDataDacha() {
        return new DataHome(this.solarmanTuyaService, this.deviceService);
    }

    public DataHome getDataGolego() {
        return new DataHome(this.deviceService, this.usrTcpWiFiParseData, PORT_MASTER);
    }

}
