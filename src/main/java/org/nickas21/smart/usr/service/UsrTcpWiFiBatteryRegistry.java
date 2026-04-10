package org.nickas21.smart.usr.service;

import lombok.Getter;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.BatteryDataBase;
import org.nickas21.smart.usr.entity.InverterDataBase;
import org.nickas21.smart.usr.entity.dacha.InverterDataDacha;
import org.nickas21.smart.usr.entity.golego.BatteryDataUsrTcpWiFi;
import org.nickas21.smart.usr.entity.golego.InverterDataGolego;
import org.nickas21.smart.usr.entity.golego.InverterGolegoData32;
import org.nickas21.smart.usr.entity.golego.InverterGolegoData90;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Component
public class UsrTcpWiFiBatteryRegistry {

    @Getter
    public final UsrTcpWiFiProperties usrTcpWiFiProperties;

    private final Map<Integer, BatteryDataBase> batteries = new ConcurrentHashMap<>();
    private final Map<Integer, InverterDataBase> inverters = new ConcurrentHashMap<>();

    public UsrTcpWiFiBatteryRegistry(UsrTcpWiFiProperties usrTcpWiFiProperties) {
        this.usrTcpWiFiProperties = usrTcpWiFiProperties;
    }

    public void initBattery(Integer port) {
        batteries.putIfAbsent(port, new BatteryDataUsrTcpWiFi(port));
    }
    public void initInverter(Integer port) {
        if (port.equals(this.usrTcpWiFiProperties.getPortInverterGolego())) {
            inverters.putIfAbsent(port, new InverterDataGolego(port, new InverterGolegoData32(), new InverterGolegoData90()));
        } else if (port.equals(this.usrTcpWiFiProperties.getPortInverterDacha())) {
            inverters.putIfAbsent(port, new InverterDataDacha(port));
        }
    }

    public <T extends BatteryDataBase> T getBattery(Integer port, Class<T> clazz) {
        BatteryDataBase battery = batteries.get(port);
        if (clazz.isInstance(battery)) {
            return clazz.cast(battery);
        }
        return null;
    }

    public <T extends BatteryDataBase> Map<Integer, T> getBatteriesAll(Class<T> clazz) {
        return batteries.entrySet().stream()
                .filter(entry -> clazz.isInstance(entry.getValue()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> clazz.cast(entry.getValue())
                        ),
                        Collections::unmodifiableMap
                ));
    }
    public <T extends InverterDataBase> T getInverter(Integer port, Class<T> clazz) {
        // computeIfAbsent автоматично виконає ініціалізацію, якщо ключа немає
        InverterDataBase inverter = inverters.computeIfAbsent(port, k -> {
            initInverter(k);
            return inverters.get(k);
        });

        if (clazz.isInstance(inverter)) {
            return clazz.cast(inverter);
        }
        return null;
    }

    public Map<Integer, InverterDataBase> getInvertersAll() {
        return Collections.unmodifiableMap(inverters);
    }
}
