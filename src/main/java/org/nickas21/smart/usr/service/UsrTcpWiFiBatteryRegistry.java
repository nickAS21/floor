package org.nickas21.smart.usr.service;

import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class UsrTcpWiFiBatteryRegistry {

    private final Map<Integer, UsrTcpWiFiBattery> batteries = new ConcurrentHashMap<>();

    public void initBattery(int port) {
        batteries.putIfAbsent(port, new UsrTcpWiFiBattery(port));
    }

    public UsrTcpWiFiBattery getBattery(int port) {
        return batteries.get(port);
    }

    public Map<Integer, UsrTcpWiFiBattery> getAll() {
        return Collections.unmodifiableMap(batteries);
    }
}
