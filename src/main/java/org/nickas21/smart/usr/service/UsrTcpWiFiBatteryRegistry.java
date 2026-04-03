package org.nickas21.smart.usr.service;

import lombok.Getter;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.InverterDataGolego;
import org.nickas21.smart.usr.entity.InvertorGolegoData32;
import org.nickas21.smart.usr.entity.InvertorGolegoData90;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class UsrTcpWiFiBatteryRegistry {

    @Getter
    public final UsrTcpWiFiProperties usrTcpWiFiProperties;

    private final Map<Integer, UsrTcpWiFiBattery> batteries = new ConcurrentHashMap<>();
    private final Map<Integer, InverterDataGolego> inverters = new ConcurrentHashMap<>();

    public UsrTcpWiFiBatteryRegistry(UsrTcpWiFiProperties usrTcpWiFiProperties) {
        this.usrTcpWiFiProperties = usrTcpWiFiProperties;
    }

    public void initBattery(Integer port) {
        batteries.putIfAbsent(port, new UsrTcpWiFiBattery(port));
    }
    public void initInverter(Integer port) {
        if (port.equals(this.usrTcpWiFiProperties.getPortInverterGolego())) {
            inverters.putIfAbsent(port, new InverterDataGolego(port, new InvertorGolegoData32(), new InvertorGolegoData90()));
        }
    }

    public UsrTcpWiFiBattery getBattery(Integer port) {
        return batteries.get(port);
    }

    public InverterDataGolego getInverter(Integer port) {
        return inverters.get(port);
    }

    public Map<Integer, UsrTcpWiFiBattery> getBatteriesAll() {
        return Collections.unmodifiableMap(batteries);
    }

    public Map<Integer, InverterDataGolego> getInvertersAll() {
        return Collections.unmodifiableMap(inverters);
    }
}
