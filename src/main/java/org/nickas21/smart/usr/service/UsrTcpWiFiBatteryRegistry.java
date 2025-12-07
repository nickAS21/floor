package org.nickas21.smart.usr.service;

import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWiFiErrorRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component
public class UsrTcpWiFiBatteryRegistry {

    private final Map<Integer, UsrTcpWiFiBattery> batteries = new ConcurrentHashMap<>();
    // errors per port - persisted as JSON lines
    private final ConcurrentMap<Integer, List<UsrTcpWiFiErrorRecord>> errorsMap = new ConcurrentHashMap<>();

    public void initBattery(int port) {
        batteries.putIfAbsent(port, new UsrTcpWiFiBattery(port));
    }

    public UsrTcpWiFiBattery getBattery(int port) {
        return batteries.get(port);
    }

    public Map<Integer, UsrTcpWiFiBattery> getAll() {
        return Collections.unmodifiableMap(batteries);
    }


    /**
     * Append error record in-memory and persist as JSON-line file (append).
     * This will survive restart as it's written to disk.
     */
    public void appendErrorRecord(int port, UsrTcpWiFiErrorRecord err) {
        errorsMap.computeIfAbsent(port, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(err);
    }
}
