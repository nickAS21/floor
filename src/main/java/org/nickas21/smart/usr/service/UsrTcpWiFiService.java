package org.nickas21.smart.usr.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.config.PortStatus;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class UsrTcpWiFiService {

//    @Value("${app.test_front:false}")
//    boolean testFront;

    private final List<ServerSocket> serverSockets = new CopyOnWriteArrayList<>();
    private final java.util.concurrent.ConcurrentHashMap<Integer, Long> lastSeenMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Integer, Socket> activeConnections = new java.util.concurrent.ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PortStatus> portStatusMap = new ConcurrentHashMap<>();
    private String logsDir;


    private final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry;
    @Getter
    private final UsrTcpWiFiProperties tcpProps;

    @Autowired
    public UsrTcpWiFiService(UsrTcpWiFiParseData usrTcpWiFiParseData, UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry, UsrTcpWiFiProperties tcpProps
    ) {
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.usrTcpWiFiBatteryRegistry = usrTcpWiFiBatteryRegistry;
        this.tcpProps = tcpProps;
    }

    // --------------------------
    // INIT CONFIG
    // --------------------------
    public void init() {
        UsrTcpWiFiLogWriter usrTcpWiFiLogWriter = usrTcpWiFiParseData.getLogWriter();
        UsrTcpLogsWiFiProperties usrTcpLogsWiFiProperties = usrTcpWiFiParseData.getUsrTcpLogsWiFiProperties();
        this.logsDir = usrTcpLogsWiFiProperties.getDir();
        UsrTcpWiFiProperties tcpProps = usrTcpWiFiParseData.getUsrTcpWiFiProperties();
        Integer portStart = tcpProps.getPortStart();
        int batteriesCnt = tcpProps.getBatteriesCnt();
        Integer[] ports = new Integer[batteriesCnt];
        for (int i = 0; i < batteriesCnt; i++) {
            ports[i] = portStart + i;
            if (ports[i].equals(usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterGolego()) ||
                    ports[i].equals(usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterDacha())){
                usrTcpWiFiBatteryRegistry.initInverter(ports[i]);
            } else {
                usrTcpWiFiBatteryRegistry.initBattery(ports[i]);
            }
            lastSeenMap.put(ports[i], System.currentTimeMillis()); // Ініціалізація часу
        }
        log.info("USR TCP WiFi ports initialized: start={}, ports={}", portStart, Arrays.toString(ports));
        try {
            if (logsDir == null || logsDir.isBlank()) {
                logsDir = "/tmp/usr-bms";   // fallback for Kubernetes
            }
            Files.createDirectories(Paths.get(logsDir));
            log.info("LogsDir: [{}], Starting USR TCP WiFi listeners...", logsDir);
            usrTcpWiFiLogWriter.init(this.logsDir, tcpProps, usrTcpLogsWiFiProperties, ports);
            for (int port : ports) {
                Thread t = new Thread(() -> listenOnPort(port), "usr-tcp-listener-" + port);
                t.setDaemon(true);
                t.start();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("USR TCP WiFi Service - Critical error, service failed to start", ex);
        }
    }

    @Scheduled(fixedRateString = "${usr.tcp.check-rate:60000}") // Перевірка кожні 1 хвилин
    public void monitorInactivity() {
        long now = System.currentTimeMillis();

        // Значення з вашого UsrTcpWiFiProperties
        long timeoutStandby = tcpProps.getMonitorInactivityTimeOut(); // 1200000 (20 хв)
        long timeoutOffline = tcpProps.getMarginMs();                 // 3660000 (61 хв)

        lastSeenMap.forEach((port, lastSeen) -> {
            long diff = now - lastSeen;
            PortStatus currentStatus = portStatusMap.getOrDefault(port, PortStatus.OFFLINE);

            if (diff < timeoutStandby) {
                // ПОРТ АКТИВНИЙ
                if (currentStatus != PortStatus.ACTIVE) {
                    log.info("Порт {}: Стан змінено на ACTIVE", port);
                    portStatusMap.put(port, PortStatus.ACTIVE);
                }
            }
            else if (diff >= timeoutStandby && diff < timeoutOffline) {
                // ПОРТ УМОВНО АКТИВНИЙ (STANDBY)
                if (currentStatus != PortStatus.STANDBY) {
                    log.warn("Порт {}: Немає даних {} хв. Стан STANDBY. Скидаємо сокет...", port, diff/60000);
                    portStatusMap.put(port, PortStatus.STANDBY);
                    forceCloseSocket(port); // Закриваємо для реконнекту
                }
            }
            else {
                // ПОРТ АБСОЛЮТНО НЕ АКТИВНИЙ (OFFLINE)
                if (currentStatus != PortStatus.OFFLINE) {
                    log.error("Порт {}: OFFLINE (> 60 хв). Керування приладами ЗАБОРОНЕНО!", port);
                    portStatusMap.put(port, PortStatus.OFFLINE);
                    // Тут викликати метод вимкнення критичних реле
                }
            }
        });
    }

    private void forceCloseSocket(int port) {
        Socket conn = activeConnections.get(port);
        if (conn != null && !conn.isClosed()) {
            try {
                conn.close();
                log.info("Порт {}: Сокет примусово закрито для оновлення сесії.", port);
            } catch (IOException e) {
                log.error("Помилка закриття сокета на порту {}", port);
            }
        }
    }

    // ------------------ listener per port ------------------
    private void listenOnPort(int port) {
        // Prepare initial "waiting" system message
        Path logDir = Paths.get(logsDir);
        String dirMsg = Files.exists(logDir) ? "" : "Created log directory: " + logDir + "\n";
        if (!dirMsg.isEmpty()) {
            String waitMsg = String.format("*** Waiting for connection on %s:%d ***", "0.0.0.0", port);
            String listenOnPortMessages = dirMsg + waitMsg + "\n";
            log.info(listenOnPortMessages);
        }
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            serverSockets.add(server);
            while (true) {
                try (Socket conn = server.accept()) {
                    handleConnection(conn, port);
                } catch (SocketException se) {
                    // For close socket with cleanup()
                    if (server.isClosed()) {
                        log.info("ServerSocket on port {} closed externally.", port);
                        break;
                    }
                    log.error("Connection error on port {}", port, se);
                } catch (Exception e) {
                    log.error("Connection error on port {}", port, e);
                }            }
        } catch (Exception e) {
            log.error("ServerSocket error on port {}", port, e);
        }
    }

    // --------------------------
    // HANDLE ONE CONNECTION
    // --------------------------
    private void handleConnection(Socket conn, int port) {
        // 1. Реєструємо активне з'єднання та початковий час для Watchdog
        activeConnections.put(port, conn);
        lastSeenMap.put(port, System.currentTimeMillis());
        // Встановлюємо початковий статус
        portStatusMap.put(port, PortStatus.ACTIVE);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (InputStream in = conn.getInputStream()) {
            byte[] readBuf = new byte[4096];
            int read;

            while ((read = in.read(readBuf)) != -1) {
                if (read > 0) {
                    // 2. ОНОВЛЕННЯ ЧАСУ: Дані прийшли, скидаємо таймер неактивності
                    lastSeenMap.put(port, System.currentTimeMillis());

                    // Якщо ми були в STANDBY/OFFLINE, повертаємо в ACTIVE
                    if (portStatusMap.get(port) != PortStatus.ACTIVE) {
                        portStatusMap.put(port, PortStatus.ACTIVE);
                    }

                    buffer.write(readBuf, 0, read);

                    // ВАША ЛОГІКА ПАРСИНГУ
                    byte[] remaining = usrTcpWiFiParseData.parseAndProcessData(buffer.toByteArray(), port);

                    buffer.reset();
                    if (remaining != null && remaining.length > 0) {
                        buffer.write(remaining);
                    }
                }
            }

            // нормальне закриття з'єднання
            flushRemaining(buffer, port);

        } catch (SocketException e) {
            flushRemaining(buffer, port);
            log.debug("Client disconnected (reset) on port {}", port);

        } catch (IOException e) {
            flushRemaining(buffer, port);
            log.warn("I/O error on port {}", port, e);
        } finally {
            // 3. ОЧИЩЕННЯ: Видаляємо сокет з активних при розриві
            activeConnections.remove(port);
            // Ми НЕ видаляємо port з lastSeenMap, щоб моніторинг бачив час останнього коннекту
        }
    }
    // --------------------------
    // CLEANUP / SHUTDOWN
    // --------------------------
    public void cleanup() {
        log.info("Start destroy UsrTcpWiFiService: closing server sockets...");

        // 1. ЗУПИНКА TCP СЛУХАЧІВ
        for (ServerSocket server : serverSockets) {
            if (server != null && !server.isClosed()) {
                try {
                    // Закриття сокета примусить server.accept() видати SocketException і зупинити потік.
                    server.close();
                    log.info("Closed ServerSocket on port {}", server.getLocalPort());
                } catch (IOException e) {
                    log.error("Error closing ServerSocket on port {}", server.getLocalPort(), e);
                }
            }
        }
        log.info("UsrTcpWiFiService cleanup completed.");
    }

    private void flushRemaining(ByteArrayOutputStream buffer, int port) {
        if (buffer.size() > 0) {
            try {
                usrTcpWiFiParseData.parseAndProcessData(buffer.toByteArray(), port);
            } catch (Exception e) {
                log.debug("Failed to process remaining buffer on port {}", port);
            }
            buffer.reset();
        }
    }

    // Метод 1: Для основної системи (по порту)
    public String getStatusByPort(Integer port) {
        return portStatusMap.getOrDefault(port, PortStatus.OFFLINE).name();
    }

    // Метод 2: Для дачі (по часу та SOC)
    public String calculateStatus(Long lastUpdateTimestamp, Long soc) {
        if (lastUpdateTimestamp == null || soc == null || soc <= 0) {
            return PortStatus.OFFLINE.name();
        }

        long diff = System.currentTimeMillis() - lastUpdateTimestamp;

        // Використовуємо ваші константи: 20 хв та 61 хв
        if (diff < tcpProps.getMonitorInactivityTimeOut()) {
            return PortStatus.ACTIVE.name();
        } else if (diff < tcpProps.getMarginMs()) {
            return PortStatus.STANDBY.name();
        } else {
            return PortStatus.OFFLINE.name();
        }
    }
}
