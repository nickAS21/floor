package org.nickas21.smart.usr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWiFiErrorRecord;
import org.springframework.beans.factory.annotation.Value;
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
    // ./logs/usrTcpWiFiCur.log ./logs/usrTcpWiFiLast.log ./logs/usrTcpWiFiError.log
//
//    @Value("${usr.tcp.logs-dir:}")
//    String logsDir;
//
//    @Value("${usr.tcp.file-error:}")
//    String fileError;

    @Value("${usr.tcp.offline:120}")
    long offlineThresholdSeconds;

    @Value("${usr.tcp.removal:604800}")
    long removalThresholdSeconds; // default 3 days

    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

//    @PostConstruct
//    public void init() {
//        if (logsDir == null || logsDir.isBlank()) {
//            throw new IllegalStateException("usr.tcp.logs-dir is not configured");
//        }
//        if (fileError == null || fileError.isBlank()) {
//            throw new IllegalStateException("usr.tcp.file-error is not configured");
//        }
//
//        Path dir = Paths.get(logsDir);
//        try {
//            Files.createDirectories(dir);
//        } catch (IOException e) {
//            throw new IllegalStateException("Cannot create logs directory: " + logsDir, e);
//        }
//    }

    public void initBattery(int port) {
        batteries.putIfAbsent(port, new UsrTcpWiFiBattery(port));
    }

    public UsrTcpWiFiBattery getBattery(int port) {
        return batteries.get(port);
    }

    public Map<Integer, UsrTcpWiFiBattery> getAll() {
        return Collections.unmodifiableMap(batteries);
    }


//    /**
//     * Atomically update C0 data for a Battery. The dto must contain timestamp (from transport layer).
//     * We use compute to guarantee atomicity and to avoid creating races.
//     */
//    public void updateC0(int port, UsrTcpWifiC0Data c0) {
//        batteries.compute(port, (p, old) -> {
//            UsrTcpWiFiBattery updateBattery = old == null ? new UsrTcpWiFiBattery(port) : old;
//            updateBattery.setC0Data(c0);
//            updateBattery.setLastTime(c0.getTimestamp());
//            return updateBattery;
//        });
//    }
//
//    /**
//     * Atomically update C1 data for a Battery.
//     */
//    public void updateC1(int port, UsrTcpWifiC1Data c1) {
//        batteries.compute(port, (p, old) -> {
//            UsrTcpWiFiBattery updateBattery = old == null ? new UsrTcpWiFiBattery(port) : old;
//            updateBattery.setC1Data(c1);
//            updateBattery.setLastTime(c1.getTimestamp());
//            return updateBattery;
//        });
//    }

    /**
     * Append error record in-memory and persist as JSON-line file (append).
     * This will survive restart as it's written to disk.
     */
    public void appendErrorRecord(int port, UsrTcpWiFiErrorRecord err) {
        errorsMap.computeIfAbsent(port, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(err);

        // write to file (json-lines)
//        Path file = Paths.get(logsDir + fileError + ".log");
//        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
//            bw.write(objectMapper.writeValueAsString(err));
//            bw.newLine();
//        } catch (IOException e) {
//            // log and continue (don't fail registry)
//            System.err.println("Failed to persist error record for port " + port + ": " + e.getMessage());
//        }
    }

    /**
     * Read errors for port from memory (if available) or fallback to reading file on disk.
     */
//    public List<UsrTcpWiFiErrorRecord> getErrorsForPort(int port) {
//        List<UsrTcpWiFiErrorRecord> mem = errorsMap.get(port);
//        if (mem != null) return Collections.unmodifiableList(mem);
//
//        // lazy load from file
//        Path file = Paths.get(logsDir + fileError + ".log");
//        if (!Files.exists(file)) return Collections.emptyList();
//
//        List<UsrTcpWiFiErrorRecord> list = new ArrayList<>();
//        try {
//            Files.lines(file).forEach(line -> {
//                try {
//                    UsrTcpWiFiErrorRecord r = objectMapper.readValue(line, UsrTcpWiFiErrorRecord.class);
//                    list.add(r);
//                } catch (Exception ignore) {}
//            });
//        } catch (IOException ignored) {}
//
//        errorsMap.putIfAbsent(port, Collections.synchronizedList(list));
//        return Collections.unmodifiableList(list);
//    }

//    /**
//     * Scheduled cleanup: mark offline & optionally remove stale Batterys.
//     * Runs every 10 minutes by default.
//     */
//    @Scheduled(fixedDelayString = "${usr.tcp.cleanup.millis:600000}")
//    public void cleanupStaleBatteries() {
//        Instant now = Instant.now();
//        for (Map.Entry<Integer, UsrTcpWiFiBattery> e : batteries.entrySet()) {
//            UsrTcpWiFiBattery  battery = e.getValue();
//            Instant lastSeen = battery.getLastTime();
//            if (lastSeen == null) continue;
//            long ageSec = Duration.between(lastSeen, now).getSeconds();
//
//            // offline detection (UI) - we don't remove here
//            boolean offline = ageSec > offlineThresholdSeconds;
//            battery.setOnline(!offline);
//
//            // removal check
//            if (ageSec > removalThresholdSeconds) {
//                batteries.remove(e.getKey());
//                // note: we DO NOT delete history files or error logs; they are persisted
//            }
//        }
//    }
}
