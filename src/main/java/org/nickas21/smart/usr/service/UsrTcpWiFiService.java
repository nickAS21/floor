package org.nickas21.smart.usr.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.usr.config.PortStatus;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.entity.golego.BatteryDataUsrTcpWiFi;
import org.nickas21.smart.usr.entity.golego.UsrTcpWifiRS485_42Data;
import org.nickas21.smart.usr.entity.golego.UsrTcpWifiRS485_MetaData;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.nickas21.smart.util.StringUtils.intToHex;
import static org.nickas21.smart.util.StringUtils.stringToHexDump;

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
            } else if (ports[i] <= usrTcpWiFiParseData.usrTcpWiFiProperties.getPortBatMasterGolego()) {
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
            usrTcpWiFiLogWriter.init(this.logsDir, usrTcpLogsWiFiProperties);
            for (int port : ports) {
                Thread t = new Thread(() -> listenOnPort(port), "usr-tcp-listener-" + port);
                t.setDaemon(true);
                t.start();
            }
            sendInitialStartupRS485Commands();
            initUpdateTimeoutScheduler();
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
     * - 0x44: System Alarms & FET Statuses (UsrTcpWifiRS485_MetaData)
     * - 0x51: Manufacturer Info (UsrTcpWifiRS485_51Data)
     * - 0x93: Serial Number (UsrTcpWifiRS485_93Data)
     */
    private void executeStartupSequence(int port) {
        // Створюємо об'єкт метаданих, який буде наповнюватися трьома командами
        UsrTcpWifiRS485_MetaData metaDataRs485 = new UsrTcpWifiRS485_MetaData();

        // Послідовно викликаємо CID2: 0x44, 0x51, 0x93
        executeRs485Command(port, UsrTcpWifiRS485_MetaData.CMD_CID2_44, metaDataRs485); // Alarms, FETs & Timestamp
//        executeRs485Command(port, UsrTcpWifiRS485_MetaData.CMD_CID2_4F, metaDataRs485); // Alarms, FETs & Timestamp
//        executeRs485Command(port, UsrTcpWifiRS485_MetaData.CMD_CID2_51, metaDataRs485); // Manufacturer Info
//        executeRs485Command(port, UsrTcpWifiRS485_MetaData.CMD_CID2_60, metaDataRs485); // Manufacturer Info
//        executeRs485Command(port, UsrTcpWifiRS485_MetaData.CMD_CID2_93, metaDataRs485); // Serial Number

        log.info("Port [{}]: Startup metadata sequence completed successfully.", port);
    }

    /**
     * Executes metadata command and routes raw response to UsrTcpWifiRS485_MetaData parser.
     */
    public void executeRs485Command(int port, int cid2, UsrTcpWifiRS485_MetaData metaData) {
        String responseAscii = sendAndReceiveRaw(port, cid2);

        if (responseAscii == null) {
            return;
        }

        switch (cid2) {
            case UsrTcpWifiRS485_MetaData.CMD_CID2_44:
                metaData.parseAndUpdate44(responseAscii, Instant.now());
                break;
            case UsrTcpWifiRS485_MetaData.CMD_CID2_4F:
                metaData.parseAndUpdate4F(responseAscii);
                break;
            case UsrTcpWifiRS485_MetaData.CMD_CID2_51:
                metaData.parseAndUpdate51(responseAscii);
                break;
            case UsrTcpWifiRS485_MetaData.CMD_CID2_60:
                metaData.parseAndUpdate60(responseAscii);
                break;
            case UsrTcpWifiRS485_MetaData.CMD_CID2_93:
                metaData.parseAndUpdate93(responseAscii);
                break;
            default:
                log.warn("Port [{}]: Unhandled CID2 0x{} for MetaData", port, Integer.toHexString(cid2));
                break;
        }
    }

    /**
     * Low-level I/O method that handles raw RS485 Request/Response exchange over TCP socket.
     * Checks for Pylontech response status RTN code before returning data.
     *           int adr = 1;
     *         String command = buildRs485AsciiCommand(adr, cid2);
     *                 log.info("Command [{}] stringToHexDump Command [{}] execute CID2 [0x{}] adr [{}] - active socket", command, stringToHexDump(command), Integer.toHexString(cid2).toUpperCase(), adr);
     *         String cid2Str = intToHex(cid2).substring(2);
     *         command = buildRs485AsciiCommand(cid2Str);
     */
//    private String sendAndReceiveRaw(int port, int cid2) {
//        Socket socket = activeConnections.get(port);
//        if (socket == null || socket.isClosed() || !socket.isConnected()) {
//            log.warn("Port [{}]: Cannot execute CID2 0x{} - active socket missing or closed",
//                    port, Integer.toHexString(cid2).toUpperCase());
//            return null;
//        }
//
//        // Fixed ADR = 0x01 for Master battery (DIP switches all OFF)
//        int adr = 1;
//        String command = buildRs485AsciiCommand(adr, cid2);
//                log.info("Command [{}] stringToHexDump Command [{}] execute CID2 [0x{}] adr [{}] - active socket", command, stringToHexDump(command), Integer.toHexString(cid2).toUpperCase(), adr);
//        String cid2Str = intToHex(cid2).substring(2);
//        command = buildRs485AsciiCommand(cid2Str);
//        synchronized (socket) {
//            try {
//                InputStream in = socket.getInputStream();
//                OutputStream out = socket.getOutputStream();
//
//                // Clear stale input buffer
//                if (in.available() > 0) {
//                    in.skip(in.available());
//                }
//
//                // Send ASCII command
//                out.write(command.getBytes(StandardCharsets.US_ASCII));
//                out.flush();
//
//                socket.setSoTimeout(2000);
////                byte[] buffer = new byte[512];
////                int bytesRead = in.read(buffer);
////
////                if (bytesRead > 0) {
////                    String responseAscii = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII).trim();
//                StringBuilder sb = new StringBuilder();
//                long startTime = System.currentTimeMillis();
//                while ((System.currentTimeMillis() - startTime) < 2000) {
//                    int b = in.read();
//                    if (b == -1) break;
//                    char c = (char) b;
//                    sb.append(c);
//                    if (c == '\r' || c == '\n') break;
//                }
//                String responseAscii = sb.toString().trim();
//
//                if (!responseAscii.isEmpty()) {
//
//                    log.info("BMS returned strDump [{}]  for CID2 [0x{}]  responseAscii  [{}]", stringToHexDump(responseAscii), Integer.toHexString(cid2).toUpperCase(), responseAscii, Integer.toHexString(cid2).toUpperCase());
//                    // Base Pylontech ASCII validation (~ header and minimal frame length)
////                    if (responseAscii.startsWith("~") && responseAscii.length() >= 10) {
////                        // Check RTN Code at index 7..9
////                        String rtnHex = responseAscii.substring(7, 9);
//                    // Якщо прийшло декілька склеєних кадрів (є більше однієї '~'), беремо останній повноцінний кадр
//                    if (responseAscii.lastIndexOf('~') > 0) {
//                        responseAscii = responseAscii.substring(responseAscii.lastIndexOf('~'));
//                    }
//
//// Validation (~ header and minimal frame length)
//                    if (responseAscii.startsWith("~") && responseAscii.length() >= 10) {
//                        // Check RTN Code at index 7..9
//                        String rtnHex = responseAscii.substring(7, 9);
//                        int rtnCode = Integer.parseInt(rtnHex, 16);
//                        log.info("Port [{}]: BMS returned error for CID2 0x{} -> Code [0x{}]: {}",
//                                port, Integer.toHexString(cid2).toUpperCase(), rtnHex, getBmsErrorDescription(rtnCode));
//                        if (rtnCode != 0x00) {
//                            return null;
//                        }
//
//                        log.debug("Port [{}]: RS485 Response [CID2 0x{}]: SUCCESS", port, Integer.toHexString(cid2).toUpperCase());
//                        return responseAscii;
//                    } else {
//                        log.warn("Port [{}]: Invalid RS485 ASCII frame received for CID2 0x{}: {}",
//                                port, Integer.toHexString(cid2).toUpperCase(), responseAscii);
//                    }
//                }
//            } catch (SocketTimeoutException e) {
//                log.warn("Port [{}]: Timeout waiting for RS485 response (CID2 0x{})",
//                        port, Integer.toHexString(cid2).toUpperCase());
//            } catch (IOException e) {
//                log.error("Port [{}]: IO Error on RS485 exchange (CID2 0x{}): {}",
//                        port, Integer.toHexString(cid2).toUpperCase(), e.getMessage());
//            } finally {
//                try {
//                    socket.setSoTimeout(0);
//                } catch (IOException ignored) {}
//            }
//        }
//        return null;
//    }

    private String sendAndReceiveRaw(int port, int cid2) {
        Socket socket = activeConnections.get(port);

        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            log.warn("Port [{}]: Cannot execute CID2 0x{} - active socket missing or closed",
                    port, Integer.toHexString(cid2).toUpperCase());
            return null;
        }

        int adr = 1;
        String command = buildRs485AsciiCommand(adr, cid2);
        String cid2Str = intToHex(cid2).substring(2);
        command = buildRs485AsciiCommand(cid2Str);

        log.info("Port [{}]: SEND CID2 [0x{}] command [{}] HEX [{}]",
                port,
                Integer.toHexString(cid2).toUpperCase(),
                command,
                stringToHexDump(command));

        synchronized (socket) {
            try {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                // =================================================================
                // purge_buffer(sock)
                // =================================================================

                socket.setSoTimeout(50);

                try {
                    byte[] purgeBuffer = new byte[1024];

                    while (true) {
                        int bytesRead = in.read(purgeBuffer);

                        if (bytesRead <= 0) {
                            break;
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                    // Expected: no more stale data in the socket.
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

                            for (int i = 0; i < bytesRead; i++) {
                                if (chunk[i] == '\r' || chunk[i] == '\n') {
                                    hasEnd = true;
                                    break;
                                }
                            }

                            if (hasStart && hasEnd) {
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

                String responseAscii = buffer.toString(StandardCharsets.US_ASCII);

                log.info("Port [{}]: RAW BMS response CID2 [0x{}], bytes [{}], HEX [{}], ASCII [{}]",
                        port,
                        Integer.toHexString(cid2).toUpperCase(),
                        buffer.size(),
                        stringToHexDump(responseAscii),
                        responseAscii);

                return responseAscii;

            } catch (IOException e) {
                log.error("Port [{}]: IO Error on RS485 exchange (CID2 0x{}): {}",
                        port,
                        Integer.toHexString(cid2).toUpperCase(),
                        e.getMessage(),
                        e);

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

    public void initUpdateTimeoutScheduler() {
        this.schedulerRs485 = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        this.schedulerRs485.scheduleAtFixedRate(
                this::onSchedulerTick42,
                0,
                this.defaultSmartSolarmanTuyaService.getTimeoutSecUpdate() == null ? 140L : this.defaultSmartSolarmanTuyaService.getTimeoutSecUpdate(),
                java.util.concurrent.TimeUnit.SECONDS
        );
    }

    private void onSchedulerTick42() {
//        pollRs485Devices(usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterGolego());
        pollRs485Devices(usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterDacha());
    }

    public void forceCloseSocket(int port) {
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
        activeConnections.put(port, conn);
        lastSeenMap.put(port, System.currentTimeMillis());
        portStatusMap.put(port, PortStatus.ACTIVE);

        // Battery ports: не читаємо сокет, тільки тримаємо з'єднання
        if (port == usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterGolego()
                || port == usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortBatMasterDacha()) {

            try {
                while (!conn.isClosed()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                activeConnections.remove(port);
            }
            return;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (InputStream in = conn.getInputStream()) {
            byte[] readBuf = new byte[4096];
            int read;

            while ((read = in.read(readBuf)) != -1) {
                if (read > 0) {
                    // ПРАВИЛО 1: Оновлюємо час АКТИВНОСТІ завжди, коли прийшли байти
                    lastSeenMap.put(port, System.currentTimeMillis());
                    portStatusMap.put(port, PortStatus.ACTIVE);

                    buffer.write(readBuf, 0, read);

                    try {
                        byte[] remaining = usrTcpWiFiParseData.parseAndProcessData(buffer.toByteArray(), port);
                        buffer.reset();
                        if (remaining != null && remaining.length > 0) {
                            buffer.write(remaining);
                        }
                    } catch (Exception e) {
                        // ПРАВИЛО 2: Якщо дані не розпарсились - логуємо помилку декодера,
                        // але НЕ скидаємо ACTIVE статус, бо фізично пристрій щось шле.
                        log.error("Port {}: Decoder error - invalid data format: {}", port, e.getMessage());
                        buffer.reset(); // Очищуємо "сміття", щоб не забити пам'ять
                    }
                }
            }
            flushRemaining(buffer, port);
        } catch (IOException e) {
            log.warn("Connection lost on port {}", port);
        } finally {
            activeConnections.remove(port);
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

    public Optional<Long> getLastTimeActiveByPort(Integer port) {
        return Optional.ofNullable(lastSeenMap.get(port));
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

    /**
     * Опитування RS485 пристроїв, виключно для порту Master батареї Golego.
     * @param masterBatPort
     */
    /**
     * Scheduled polling method for 0x42 telemetry frame.
     */
    public void pollRs485Devices(Integer masterBatPort) {
        String responseAscii = sendAndReceiveRaw(masterBatPort, UsrTcpWifiRS485_42Data.CMD_CID2_42);
        if (responseAscii != null) {
            UsrTcpWifiRS485_42Data data42 = UsrTcpWifiRS485_42Data.parse(responseAscii);
            if (data42 != null) {
                usrTcpWiFiBatteryRegistry.updateBatteryData42(masterBatPort, data42);
                usrTcpWiFiBatteryRegistry
                        .getBattery(masterBatPort, BatteryDataUsrTcpWiFi.class)
                        .setLastTime(data42.getTimestamp());
                log.debug("Port [{}]: Telemetry 0x42 updated and lastTime set to {}", masterBatPort, data42.getTimestamp());
            }
        }
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
    public String buildRs485AsciiCommand(int adr, int cid2) {
        if (cid2 == 0x60) {
            log.info("HARDCODED COMMAND FOR CID2 [0x60] EXECUTED");
            return "~201246600000FDAB\r";
        }
        int ver = 0x20;  // Версія протоколу 2.0
        int cid1 = 0x46; // Pylontech Li-ion BMS

        // Базовий заголовок (UPPERCASE)
        String header = String.format("%02X%02X%02X%02X", ver, adr, cid1, cid2);

        // INFO / LENGTH частина (ТІЛЬКИ UPPERCASE!)
        String lengthAndInfo;


        switch (cid2) {
            case 0x42: // Analog Telemetry
            case 0x44: // Alarms / MOSFET Flags
            case 0x4F: // Protocol Version
            case 0x51: // Manufacturer / Model
            case 0x93: // Serial Number
            case 0x96: // Firmware Version
                lengthAndInfo = "e00201"; // Маленькі букви e00201!
                break;
            default:
                lengthAndInfo = "0000";
                break;
        }

        // Тіло кадру для розрахунку суми
        String rawFrame = header + lengthAndInfo;

        // Розрахунок підсумкової контрольної суми CHKCHR (LRC)
         int sum = 0;
        for (char c : rawFrame.toCharArray()) {
            int v = (int) c;
            sum += v;
            log.info("'{}' -> {} (0x{})", c, v, Integer.toHexString(v).toUpperCase());
        }
        log.info("ASCII SUM = {} (0x{})", sum, Integer.toHexString(sum).toUpperCase());

        int chkchr = (~sum + 1) & 0xFFFF;

        // Повертаємо кадр (з великими буквами контрольної суми)
        String result = "~" + rawFrame + String.format("%04X", chkchr) + "\r";
        log.info("From Command [{}] stringToHexDump rawFrame [{}] execute CID2 [0x{}] adr [{}] chkchrHex [{}] chkchrInt [{}] - active socket", rawFrame, stringToHexDump(rawFrame), Integer.toHexString(cid2).toUpperCase(), adr, String.format("%04X", chkchr), chkchr);
        return result;
    }

    public static String buildRs485AsciiCommand(String cid2) {
        String key = cid2.toUpperCase().trim();
        String adr = "01";
        switch (key) {
            case "51":
            case "51H":
                return "~20014651e00201FD15\r";
            case "93":
            case "93H":
                return "~20014693e00201FD0F\r";
            case "96":
            case "96H":
                return "~20014696e00201FD0C\r";
            case "4F":
            case "4FH":
                return "~2001464Fe00201FD01\r";
//            case "42":
//            case "42H":
//                return "~20014642e00201FD9F\r";
//            case "44":
//            case "44H":
//                return "~20014644e00201FD9D\r";
            case "60":
            case "60H":
                String hex60 = "7E323031323436363030303030464441420D";
                return "~2001464Fe00201FD01\r";
            default:
                // Дефолтний хардкод, якщо передано щось інше
                return "~20" + adr + "46" + cid2 + "e00201FD15\r";
        }
    }
}
