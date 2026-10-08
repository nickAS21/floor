package org.nickas21.smart.usr.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.usr.config.PortStatus;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Attributes;
import org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry;
import org.nickas21.smart.usr.entity.golego.BatteryDataUsrTcpWiFi;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry.CMD_ADR_01;
import static org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry.CMD_CID1;
import static org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry.CMD_CID2_42;
import static org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry.CMD_CID2_44;
import static org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry.CMD_INFO_HEX;
import static org.nickas21.smart.usr.entity.UsrTcpWifiRS485_Data_GOOTO_Telemetry.CMD_VER;
import static org.nickas21.smart.util.StringUtils.bytesToHexDump;

@Slf4j
@Service
public class UsrTcpWiFiService {

//    @Value("${app.test_front:false}")
//    boolean testFront;

  // 4 min
  private java.util.concurrent.ScheduledExecutorService schedulerRs485;
    private final List<ServerSocket> serverSockets = new CopyOnWriteArrayList<>();
    private final java.util.concurrent.ConcurrentHashMap<Integer, Long> lastSeenMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Integer, Socket> activeConnections = new java.util.concurrent.ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PortStatus> portStatusMap = new ConcurrentHashMap<>();
    @Getter
    private String logsDir;
    @Getter
    private final UsrTcpWiFiProperties tcpProps;
    public final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry;
    private final DefaultSmartSolarmanTuyaService defaultSmartSolarmanTuyaService;

    @Autowired
    public UsrTcpWiFiService(UsrTcpWiFiParseData usrTcpWiFiParseData, UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry, UsrTcpWiFiProperties tcpProps, DefaultSmartSolarmanTuyaService defaultSmartSolarmanTuyaService
    ) {
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.usrTcpWiFiBatteryRegistry = usrTcpWiFiBatteryRegistry;
        this.defaultSmartSolarmanTuyaService = defaultSmartSolarmanTuyaService;
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
        int portsCnt = tcpProps.getPortsCnt();
        Integer[] ports = new Integer[portsCnt];
        for (int i = 0; i < portsCnt; i++) {
            ports[i] = portStart + i;
            if (ports[i].equals(usrTcpWiFiParseData.usrTcpWiFiProperties.getPortInverterGolego()) ||
                    usrTcpWiFiParseData.usrTcpWiFiProperties.getAllPortsInverterDacha().contains(ports[i])){
                usrTcpWiFiBatteryRegistry.initInverter(ports[i]);
                portStatusMap.put(ports[i], PortStatus.STANDBY);
            } else if (ports[i].equals(usrTcpWiFiParseData.usrTcpWiFiProperties.getPortBatMasterGolego()) ||
                    usrTcpWiFiParseData.usrTcpWiFiProperties.getAllPortsBatDacha().contains(ports[i])) {
                usrTcpWiFiBatteryRegistry.initBattery(ports[i]);
                portStatusMap.put(ports[i], PortStatus.STANDBY);
            }
        }
        log.info("USR TCP WiFi ports initialized: start={}, ports={}", portStart, Arrays.toString(ports));
        try {
            if (logsDir == null || logsDir.isBlank()) {
                logsDir = "/tmp/usr-bms";   // fallback for Kubernetes
            }
            Files.createDirectories(Paths.get(logsDir));
            log.info("LogsDir: [{}], Starting USR TCP WiFi listeners...", logsDir);
            usrTcpWiFiLogWriter.init(this.logsDir, usrTcpLogsWiFiProperties);
            for (int port : ports) {
                Thread t = new Thread(() -> listenOnPort(port), "usr-tcp-listener-" + port);
                t.setDaemon(true);
                t.start();
            }
//            sendInitialStartupRS485Commands();
            initUpdateTimeoutSchedulerRs485();
        } catch (Exception ex) {
            throw new IllegalStateException("USR TCP WiFi Service - Critical error, service failed to start", ex);
        }
    }

    @Scheduled(fixedRateString = "${usr.tcp.check-rate:60000}")
    public void monitorInactivity() {
        long now = Instant.now().toEpochMilli();

        long timeoutStandby = tcpProps.getMonitorInactivityTimeOut(); // 20 хв
        long timeoutOffline = tcpProps.getMarginMs();                  // 61 хв

        for (Integer port : portStatusMap.keySet()) {
            Long lastSeen = lastSeenMap.get(port);
            PortStatus currentStatus = portStatusMap.getOrDefault(port, PortStatus.OFFLINE);

            if (lastSeen == null || lastSeen == 0L) {
                continue;
            }

            long diff = now - lastSeen;

            if (diff < timeoutStandby) {
                if (currentStatus != PortStatus.ACTIVE) {
                    log.info("Порт {}: Стан змінено на ACTIVE", port);
                    portStatusMap.put(port, PortStatus.ACTIVE);
                }
            }
            else if (diff < timeoutOffline) {
                if (currentStatus != PortStatus.STANDBY) {
                    log.warn("Порт {}: Немає даних {} хв. Стан STANDBY.", port, diff / 60000);
                    portStatusMap.put(port, PortStatus.STANDBY);
                    // ❌ forceCloseSocket(port) ТУТ ВІДСУТНІЙ! Сокет живе.
                }
            }
            else {
                if (currentStatus != PortStatus.OFFLINE) {
                    log.error("Порт {}: OFFLINE (> 60 хв). Керування приладами ЗАБОРОНЕНО!", port);
                    portStatusMap.put(port, PortStatus.OFFLINE);
                    forceCloseSocket(port); // ✅ Примусовий реконнект ТІЛЬКИ у глибокому OFFLINE
                }
            }
        }
    }

    /**
     * Retries to retrieve an active socket from activeConnections map until (baseTimeoutMs * attemptCount) is reached.
     *
     * @param port target TCP port
     * @param cnt attempt multiplier (e.g. 1 = 3000ms, 2 = 6000ms)
     * @return active Socket or null if timeout reached
     */
    private Socket getActiveSocket(int port, int cnt) {
        long startTime = System.currentTimeMillis();
        long baseTimeoutMs = 3000L; // 3000 мс (3 секунди)
        long totalTimeoutMs = baseTimeoutMs * cnt;

        while ((System.currentTimeMillis() - startTime) < totalTimeoutMs) {
            Socket socket = activeConnections.get(port);
            if (socket != null && !socket.isClosed() && socket.isConnected()) {
                return socket;
            }
        }

        log.warn("Port [{}]: Active socket did not appear within {} ms (attempt multiplier: {})", port, totalTimeoutMs, cnt);
        return null;
    }

    public void sendInitialStartupRS485Commands() {
        log.info("Розпочинаємо виконання стартових RS485 команд...");
        Integer masterBatPortGolego = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterGolego();
        Socket socketRs485Golego = getActiveSocket(masterBatPortGolego, 1);
        if (socketRs485Golego != null) {
            log.info("Golego -> Port [{}]: Socket acquired on 1st attempt. Executing startup commands (44, 4F, 51, 60, 93)...", masterBatPortGolego);
            executeStartupSequence(masterBatPortGolego);
        } else if (getActiveSocket (masterBatPortGolego, 2) != null)  {
            log.info("Golego -> Port [{}]: Socket acquired on 2nd attempt (6s timeout). Executing startup commands (44, 51, 93)...", masterBatPortGolego);
            executeStartupSequence(masterBatPortGolego);
        } else {
            log.warn("Port [{}]: Failed to acquire socket for Golego master. Skipping.", masterBatPortGolego);
        }
        Integer masterBatPortDacha = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterDacha();
        Socket socketRs485Dacha = getActiveSocket(masterBatPortDacha, 1);
        if (socketRs485Dacha != null) {
            log.info("Dacha -> Port [{}]: Socket acquired on 1st attempt. Executing startup commands (44, 4F, 51, 60, 93)...", masterBatPortDacha);
            executeStartupSequence(masterBatPortDacha);
        } else if (getActiveSocket (masterBatPortDacha, 2) != null)  {
            log.info("Dacha -> Port [{}]: Socket acquired on 2nd attempt (6s timeout). Executing startup commands (44, 51, 93)...", masterBatPortDacha);
            executeStartupSequence(masterBatPortDacha);
        } else {
            log.warn("Port [{}]: Failed to acquire socket for Golego master. Skipping.", masterBatPortDacha);
        }
    }

    /**
     * Виконує стартовий залп статичних команд:
     * - 0x44: System Alarms & FET Statuses (UsrTcpWifiRS485_Data_GOOTO_Attributes)
     * - 0x51: Manufacturer Info (UsrTcpWifiRS485_51Data)
     * - 0x93: Serial Number (UsrTcpWifiRS485_93Data)
     */
    private void executeStartupSequence(int port) {
        // Створюємо об'єкт метаданих, який буде наповнюватися трьома командами
        UsrTcpWifiRS485_Data_GOOTO_Attributes metaDataRs485 = new UsrTcpWifiRS485_Data_GOOTO_Attributes();

        // Послідовно викликаємо CID2: 0x44, 0x51, 0x93
        executeRs485AttributesCommand(port, UsrTcpWifiRS485_Data_GOOTO_Attributes.CMD_CID2_4F, metaDataRs485); // Alarms, FETs & Timestamp
        executeRs485AttributesCommand(port, UsrTcpWifiRS485_Data_GOOTO_Attributes.CMD_CID2_51, metaDataRs485); // Manufacturer Info
        executeRs485AttributesCommand(port, UsrTcpWifiRS485_Data_GOOTO_Attributes.CMD_CID2_93, metaDataRs485); // Serial Number

        log.info("Port [{}]: Startup metadata sequence completed successfully.", port);
    }

    /**
     * Executes metadata command and routes raw response to UsrTcpWifiRS485_Data_GOOTO_Attributes parser.
     */
    public void executeRs485AttributesCommand(int port, int cid2, UsrTcpWifiRS485_Data_GOOTO_Attributes metaData) {
        String responseAscii = sendAndReceiveRaw(port, cid2);

        if (responseAscii == null) {
            return;
        }

        switch (cid2) {
//            case UsrTcpWifiRS485_Data_GOOTO_Attributes.CMD_CID2_44:
//                metaData.parseAndUpdate44(responseAscii);
//                break;
            case UsrTcpWifiRS485_Data_GOOTO_Attributes.CMD_CID2_4F:
                metaData.parseAndUpdate4F(responseAscii, Instant.now());
                break;
            case UsrTcpWifiRS485_Data_GOOTO_Attributes.CMD_CID2_51:
                metaData.parseAndUpdate51(responseAscii);
                break;
            case UsrTcpWifiRS485_Data_GOOTO_Attributes.CMD_CID2_93:
                metaData.parseAndUpdate93(responseAscii);
                break;
            default:
                log.warn("Port [{}]: Unhandled CID2 0x{} for MetaData", port, Integer.toHexString(cid2));
                break;
        }
    }



    private String sendAndReceiveRaw(int port, int cid2) {
        Socket socket = activeConnections.get(port);

        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            log.warn("Port [{}]: Cannot execute CID2 0x{} - active socket missing or closed",
                    port, Integer.toHexString(cid2).toUpperCase());
            return null;
        }

        String command = buildRs485AsciiCommand(cid2);
        // TODO only debug
        log.debug("Port [{}]: SEND CID2 [0x{}] command [{}] HEX_SEND CID2 [{}]",
                port,
                Integer.toHexString(cid2).toUpperCase(),
                command,
                bytesToHexDump(command.getBytes(StandardCharsets.US_ASCII)));

        synchronized (socket) {
            try {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                // =================================================================
                // purge_buffer(sock) — МИТТЄВЕ ОЧИЩЕННЯ СМІТТЯ БЕЗ БЛОКУВАННЯ
                // =================================================================
                try {
                    int availableBytes = in.available();
                    if (availableBytes > 0) {
                        long skipped = in.skip(availableBytes);
                        log.debug("Port [{}]: Purged {} stale bytes from socket buffer", port, skipped);
                    }
                } catch (IOException e) {
                    log.warn("Port [{}]: Failed to purge stale socket buffer: {}", port, e.getMessage());
                }

                // =================================================================
                // sendall(cmd_str.encode("ascii"))
                // =================================================================

                out.write(command.getBytes(StandardCharsets.US_ASCII));
                out.flush();

                // =================================================================
                // Python:
                //
                // start_time = time.time()
                // sock.settimeout(0.15)
                // while (time.time() - start_time) < timeout:
                //     chunk = sock.recv(256)
                // =================================================================

                socket.setSoTimeout(150);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                long startTime = System.nanoTime();
                long timeoutNanos = 1_000_000_000L; // 1 second

                byte[] chunk = new byte[256];

                while ((System.nanoTime() - startTime) < timeoutNanos) {
                    try {
                        int bytesRead = in.read(chunk);

                        if (bytesRead == -1) {
                            log.warn("Port [{}]: Connection closed while waiting for CID2 0x{}",
                                    port,
                                    Integer.toHexString(cid2).toUpperCase());
                            break;
                        }

                        if (bytesRead > 0) {
                            buffer.write(chunk, 0, bytesRead);

                            /*
                             * Python:
                             *
                             * if b"~" in buffer and (b"\r" in chunk or b"\n" in chunk):
                             *     break
                             */
                            boolean hasStart = false;
                            boolean hasEnd = false;

                            byte[] currentBuffer = buffer.toByteArray();

                            for (byte b : currentBuffer) {
                                if (b == '~') {
                                    hasStart = true;
                                    break;
                                }
                            }

                            byte lastByte = chunk[bytesRead - 1];
                            if (lastByte == '\r' || lastByte == '\n') {
                                hasEnd = true;
                            }

                            if (hasStart && hasEnd && in.available() == 0) {
                                break;
                            }
                        }

                    } catch (SocketTimeoutException ignored) {
                        // Python recv() timeout -> continue loop.
                    }
                }

                if (buffer.size() == 0) {
                    log.warn("Port [{}]: No response for CID2 0x{}",
                            port,
                            Integer.toHexString(cid2).toUpperCase());
                    return null;
                }

                byte[] rawBytes = buffer.toByteArray();
                String responseAscii =  new String(rawBytes, StandardCharsets.US_ASCII);
                // TODO only debug
                log.debug("Port [{}]: RAW BMS response CID2 [0x{}], bytes [{}], HEX_response_CID2 [{}], ASCII [{}]",
                        port,
                        Integer.toHexString(cid2).toUpperCase(),
                        rawBytes.length,
                        bytesToHexDump(rawBytes),
                        responseAscii);

                return responseAscii;

            } catch (IOException e) {
                log.error("Port [{}]: IO Error on RS485 exchange (CID2 0x{}): {}",
                        port,
                        Integer.toHexString(cid2).toUpperCase(),
                        e.getMessage(),
                        e);
                if (e instanceof SocketException
                        && "Broken pipe".equalsIgnoreCase(e.getMessage())) {
                    log.warn("Port [{}]: Broken pipe. Forcing socket reconnect.", port);
                    forceCloseSocket(port);
                }

                return null;

            } finally {
                try {
                    socket.setSoTimeout(0);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String getBmsErrorDescription(int rtnCode) {
        return switch (rtnCode) {
            case 0x01 -> "VER error";
            case 0x02 -> "CHKSUM error";
            case 0x03 -> "LCHKSUM error";
            case 0x04 -> "CID2 invalid";
            case 0x05 -> "Command format error";
            case 0x06 -> "Invalid data (INFO invalid)";
            case 0x90 -> "ADR error";
            case 0x91 -> "Internal communication error";
            default -> "Unknown BMS error";
        };
    }

    public void initUpdateTimeoutSchedulerRs485() {
        // 2. Отримуємо значення періоду з дефолтом 140 секунд
        Long timeoutSec = this.defaultSmartSolarmanTuyaService.getTimeoutSecUpdate();
        long period = (timeoutSec != null && timeoutSec > 0) ? timeoutSec : 140L;

        // 3. Створюємо SingleThreadScheduledExecutor
        this.schedulerRs485 = Executors.newSingleThreadScheduledExecutor();

        // 4. Запускаємо з initialDelay = 0 (перший виклик миттєво)
        this.schedulerRs485.scheduleAtFixedRate(
                () -> {
                    try {
                        onSchedulerTicGOOTO_Telemetry();
                    } catch (Throwable t) {
                        // ОБОВ'ЯЗКОВО логуємо помилку, щоб шедулер не вмирав
                        log.error("Error occurred in RS485 GOOTO Telemetry scheduler tic", t);
                    }
                },
                10L, // 10 секунд паузи при старті сервісу
                period, // далі через кожні 'period' секунд
                TimeUnit.SECONDS
        );
    }

    private void onSchedulerTicGOOTO_Telemetry() {
        pollRs485Devices(usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterGolego());
        pollRs485Devices(usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterDacha());
    }

    public void forceCloseSocket(int port) {
        Socket conn = activeConnections.remove(port);
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
                    lastSeenMap.put(port, System.currentTimeMillis());
                    portStatusMap.put(port, PortStatus.ACTIVE);
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
        activeConnections.put(port, conn);
        log.info("Порт {}: Новий сокет підключено.", port);

        try {
            // Базовий Keep-Alive для виявлення розривів на рівні ОС
            conn.setKeepAlive(true);
        } catch (SocketException se) {
            log.warn("Port [{}]: Failed to set KeepAlive: {}", port, se.getMessage());
        }

        // =========================================================================
        // ГІЛКА 1: БАТАРЕЙНІ ПОРТИ (RS485 ОПИТУВАНИЙ РЕЖИМ)
        // =========================================================================
        if (usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterGolego().equals(port)
                || usrTcpWiFiParseData.getUsrTcpWiFiProperties().getAllPortsBatDacha().contains(port)) {

            log.debug("Port [{}]: Battery socket registered, starting initial RS485 poll", port);

            // Стартове опитування при підключенні
            CompletableFuture.runAsync(() -> {
                try {
                    pollRs485Devices(port);
                } catch (Throwable t) {
                    log.error("Port [{}]: Initial RS485 poll FAILED", port, t);
                }
            });

            try {
                // Встановлюємо Read Timeout (15 сек) для виявлення розриву, якщо пристрій закриє сокет
                conn.setSoTimeout(15000);
                InputStream in = conn.getInputStream();

                // Замість вічного sleep() чекаємо на фактичний стан з'єднання
                while (!conn.isClosed()) {
                    try {
                        int b = in.read();
                        if (b == -1) {
                            log.warn("Port [{}]: Battery connection closed by remote side (-1)", port);
                            break;
                        }
                    } catch (SocketTimeoutException ignored) {
                        // Таймаут потрібен лише для періодичної перевірки conn.isClosed()
                    }
                }
            } catch (IOException e) {
                log.warn("Port [{}]: Battery socket IO exception: {}", port, e.getMessage());
            } finally {
                activeConnections.remove(port);
                log.info("Port [{}]: Battery socket removed from activeConnections", port);
            }
            return;
        }

        // =========================================================================
        // ГІЛКА 2: ІНВЕРТОРНІ ПОРТИ (ПОСТІЙНИЙ СТРІМ ДАНИХ ВІД ІНВЕРТОРА)
        // =========================================================================
        try {
            // Встановлюємо 15-секундний SoTimeout, щоб in.read() не блокував потік вічно
            conn.setSoTimeout(15000);
        } catch (SocketException se) {
            log.warn("Port [{}]: Failed to set SoTimeout for inverter: {}", port, se.getMessage());
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (InputStream in = conn.getInputStream()) {
            byte[] readBuf = new byte[4096];

            while (true) {
                int read;
                try {
                    read = in.read(readBuf);
                    if (read == -1) {
                        log.warn("Port [{}]: Connection closed by remote side (-1)", port);
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    // Таймаут читання: даних немає 15 сек.
                    // НЕ закриваємо сокет і НЕ оновлюємо lastSeenMap!
                    if (conn.isClosed()) {
                        break;
                    }
                    continue;
                }

                if (read > 0) {
                    log.debug("Port [{}]: RECEIVED {} bytes from inverter, HEX [{}]",
                            port,
                            read,
                            bytesToHexDump(readBuf, read));

                    buffer.write(readBuf, 0, read);

                    try {
                        byte[] remaining = usrTcpWiFiParseData.parseAndProcessData(buffer.toByteArray(), port);

                        // ✅ ТІЛЬКИ ПІСЛЯ УСПІШНОГО РОЗПАРСУВАННЯ ОНОВЛЮЄМО ЧАС ТА СТАТУС!
                        lastSeenMap.put(port, System.currentTimeMillis());
                        portStatusMap.put(port, PortStatus.ACTIVE);

                        buffer.reset();
                        if (remaining != null && remaining.length > 0) {
                            buffer.write(remaining);
                        }
                    } catch (Exception e) {
                        // ❌ Якщо дані "сміття" або декодер впав — НЕ оновлюємо lastSeenMap!
                        log.error("Port {}: Decoder error - invalid data format: {}", port, e.getMessage());
                        buffer.reset();
                    }
                }
            }
            flushRemaining(buffer, port);
        } catch (IOException e) {
            log.warn("Port [{}]: Connection lost / IO Exception: {}", port, e.getMessage());
        } finally {
            activeConnections.remove(port);
            log.info("Port [{}]: Inverter socket removed from activeConnections", port);
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

    // Метод 1: Для основної системи від інвертора (по порту)
    public String getStatusByPort(Integer port) {
        return portStatusMap.getOrDefault(port, PortStatus.OFFLINE).name();
    }

    public Optional<Long> getLastTimeActiveByPort(Integer port) {
        return Optional.ofNullable(lastSeenMap.get(port));
    }

    // Метод 2: Для дачі (по часу та SOC)
    public String calculateStatus(Long lastUpdateTimestamp, Long soc) {
        // Якщо немає часу оновлення або SOC порожній / дорівнює 0 — це однозначно OFFLINE
        if (lastUpdateTimestamp == null || soc == null || soc <= 0) {
            return PortStatus.OFFLINE.name();
        }

        long diff = System.currentTimeMillis() - lastUpdateTimestamp;
        long timeoutMs = tcpProps.getMonitorInactivityTimeOut(); // 20 хв (1200000 ms)

        if (diff < timeoutMs) {
            return PortStatus.ACTIVE_INVERTER.name(); // Отримуємо дані через інвертор
        } else if (diff < tcpProps.getMarginMs()) {
            return PortStatus.STANDBY.name();         // Затримка оновлення (20-61 хв)
        } else {
            return PortStatus.OFFLINE.name();         // Немає даних > 61 хв
        }
    }

    /**
     * Опитування RS485 пристроїв, виключно для порту Master батареї Golego.
     * @param masterBatPort
     */
    /**
     * Scheduled polling method for 0x42 telemetry frame.
     */
    public void pollRs485Devices(Integer batPort) {
        BatteryDataUsrTcpWiFi battery = usrTcpWiFiBatteryRegistry.getBattery(batPort, BatteryDataUsrTcpWiFi.class);
        if (battery == null) {
            log.warn("Port [{}]: Battery registry returned null", batPort);
            return;
        }

        UsrTcpWifiRS485_Data_GOOTO_Telemetry telemetry = battery.getRs485_Data_GOOTO_Telemetry();
        if (telemetry == null) {
            telemetry = new UsrTcpWifiRS485_Data_GOOTO_Telemetry();
        }

        // =========================================================================
        // КРОК 1: Запит кадру 0x42 (до 5 спроб у разі помилки)
        // =========================================================================
        boolean parse42Success = false;

        for (int attempt = 1; attempt <= 5; attempt++) {
            String responseAsciiCid42 = sendAndReceiveRaw(batPort, CMD_CID2_42);
            if (responseAsciiCid42 != null) {
                parse42Success = telemetry.parse42(responseAsciiCid42);
                if (parse42Success) {
                    log.debug("Port [{}]: CID2 0x42 parsed successfully on attempt {}", batPort, attempt);
                    break; // Успішно!
                }
            }
            log.debug("Port [{}]: Failed to read/parse CID2 0x42 (attempt {}/5)", batPort, attempt);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        // Якщо за 5 спроб 0x42 не розпарсився — зупиняємо оновлення телеметрії ТА часу!
        if (!parse42Success) {
            log.error("Port [{}]: CID2 0x42 failed after 5 attempts. Skipping time update.", batPort);
            // ❌ time NOT updated, lastSeenMap NOT updated!
            // Сокет залишається відкритим для відновлення, але monitorInactivity() через timeout відправить його в OFFLINE
            return;
        }

        // =========================================================================
        // КРОК 2: Запит кадру 0x44 (одна спроба)
        // =========================================================================
        String responseAsciiCid44 = sendAndReceiveRaw(batPort, CMD_CID2_44);
        if (responseAsciiCid44 != null) {
            telemetry.parse44(responseAsciiCid44);
        }

        // =========================================================================
        // КРОК 3: Фінальне збереження та оновлення часу (ТІЛЬКИ ПРИ УСПІХУ КРОКУ 1)
        // =========================================================================
        Instant currentInstant = Instant.now();
        lastSeenMap.put(batPort, currentInstant.toEpochMilli());
        portStatusMap.put(batPort, PortStatus.ACTIVE);

        telemetry.setTimestamp(currentInstant);
        battery.setRs485_Data_GOOTO_Telemetry(telemetry);
        battery.setLastTime(telemetry.getTimestamp());

        log.debug("Port [{}]: Telemetry updated successfully at {}", batPort, telemetry.getTimestamp());
    }

    public boolean sendRs485Command(int port, String asciiCommand) {
        Socket conn = activeConnections.get(port);
        if (conn == null || conn.isClosed() || !conn.isConnected()) {
            log.warn("Порт {}: Неможливо відправити команду RS485 - сокет закритий або відсутній", port);
            return false;
        }

        try {
            OutputStream out = conn.getOutputStream();
            String fullPayload = asciiCommand.endsWith("\r") ? asciiCommand : asciiCommand + "\r";
            byte[] cmdBytes = fullPayload.getBytes(StandardCharsets.US_ASCII);

            out.write(cmdBytes);
            out.flush();
            log.debug("Порт {}: Відправлено RS485 команду -> {}", port, asciiCommand.trim());
            return true;
        } catch (IOException e) {
            log.error("Порт {}: Помилка відправки RS485 команди: {}", port, e.getMessage());
            forceCloseSocket(port);
            return false;
        }
    }

    /**
     * Генерація правильної ASCII RS485 команди з підставленням INFO-блоку та розрахунком CHKCHR.
     * Повністю сумісно з Python-скриптом Gooyto/Pylontech.
     * Payload (e00201) передається в lowercase, як у специфікації Gooto/Python.
     * @param adr  Адреса BMS (наприклад, 1)
     * @param cid2 Код команди (0x42, 0x44, 0x4F, 0x51, 0x93, 0x96 тощо)
     * @return Повний ASCII-рядок запиту зі стартовим '~' та кінцевим '\r'
     */
    /**
     * Канонічний обчислювач LENGTH: LCHKSUM (4 біти) + LENID (12 біт).
     * @param infoHex Рядок даних INFO (у HEX)
     * @return 4-символьний HEX-рядок (наприклад, "0000" або "E002")
     */
    public static String calculateLengthHex(String infoHex) {
        if (infoHex == null || infoHex.isBlank()) {
            return "0000";
        }

        int infoCharLen = infoHex.trim().length();
        int lenId = infoCharLen & 0x0FFF;

        int d1 = (lenId >> 8) & 0x0F;
        int d2 = (lenId >> 4) & 0x0F;
        int d3 = lenId & 0x0F;

        int lchksum = (~(d1 + d2 + d3) + 1) & 0x0F;
        int lengthVal = (lchksum << 12) | lenId;

        return String.format("%04X", lengthVal);
    }

    /**
     * Обчислення контрольної суми CHKSUM за сумою ASCII-кодів символів тіла кадру.
     */
    public static String calculateChecksum(String cmdBody) {
        int asciiSum = 0;
        for (char c : cmdBody.toCharArray()) {
            asciiSum += c;
        }
        int chk = (~asciiSum + 1) & 0xFFFF;
        return String.format("%04X", chk);
    }

    /**
     * Генерація канонічної ASCII RS485 команди для Pylontech/GOOTO BMS.
     *
     *  adr     Адреса BMS (якщо <= 0, адреса вибирається автоматично: 12 для 0x60.., 01 для інших)
     * @param cid2    Код команди (0x42, 0x44, 0x51, 0x60, 0x93, 0x96 тощо)
     * infoHex Блок даних (за замовчуванням порожній "", або "01", "12" за потреби)
     * @return Готовий ASCII-рядок для сокета з '~' та '\r'
     */
    public String buildRs485AsciiCommand(int cid2) {
        // Авто-корекція адреси: 60-ті команди йдуть на 12 (0x0C), решта за замовчуванням на 01
        int finalAdr = CMD_ADR_01;
        String info = CMD_INFO_HEX;
        String lengthHex = calculateLengthHex(info);

        // Формуємо тіло кадру (VER + ADR + CID1 + CID2 + LENGTH + INFO)
        String body = String.format("%02X%02X%02X%02X%s%s", CMD_VER, finalAdr, CMD_CID1, cid2, lengthHex, info);

        // Обчислюємо CHKSUM
        String chksumHex = calculateChecksum(body);

        String fullCommand = "~" + body + chksumHex + "\r";
        // TODO only debug
        log.debug("Built RS485 Cmd: CID2 [0x{}] | ADR [{}] | Body [{}] | CHKSUM [{}] | Full [{}]",
                Integer.toHexString(cid2).toUpperCase(),
                String.format("%02X", finalAdr),
                body,
                chksumHex,
                fullCommand.trim());

        return fullCommand;
    }
}
