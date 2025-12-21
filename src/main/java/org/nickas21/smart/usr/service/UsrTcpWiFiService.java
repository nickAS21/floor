package org.nickas21.smart.usr.service;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class UsrTcpWiFiService {

//    @Value("${app.test_front:false}")
//    boolean testFront;

    private final List<ServerSocket> serverSockets = new CopyOnWriteArrayList<>();
    private Integer[] ports;
    private String logsDir;


    private final UsrTcpWiFiParseData usrTcpWiFiParseData;

    private final UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry;

    @Autowired
    public UsrTcpWiFiService(UsrTcpWiFiParseData usrTcpWiFiParseData, UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry
    ) {
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.usrTcpWiFiBatteryRegistry = usrTcpWiFiBatteryRegistry;
    }

    // --------------------------
    // INIT CONFIG
    // --------------------------
    public void init() {
        UsrTcpWiFiLogWriter usrTcpWiFiLogWriter = usrTcpWiFiParseData.getLogWriter();
        UsrTcpLogsWiFiProperties usrTcpLogsWiFiProperties = usrTcpWiFiParseData.getUsrTcpLogsWiFiProperties();
        this.logsDir = usrTcpLogsWiFiProperties.getDir();
        UsrTcpWiFiProperties tcpProps = usrTcpWiFiParseData.getUsrTcpWiFiProperties();
        int portStart = tcpProps.getPortStart();
        int batteriesCnt = tcpProps.getBatteriesCnt();
        this.ports = new Integer[batteriesCnt];
        for (int i = 0; i < batteriesCnt; i++) {
            this.ports[i] = portStart + i;
            usrTcpWiFiBatteryRegistry.initBattery(this.ports[i]);
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
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (InputStream in = conn.getInputStream()) {
            byte[] readBuf = new byte[4096];
            int read;

            while ((read = in.read(readBuf)) != -1) {
                if (read > 0) {
                    buffer.write(readBuf, 0, read);

                    byte[] remaining =
                            usrTcpWiFiParseData.parseAndProcessData(buffer.toByteArray(), port);

                    buffer.reset();
                    if (remaining != null && remaining.length > 0) {
                        buffer.write(remaining);
                    }
                }
            }

            // нормальне закриття з'єднання
            flushRemaining(buffer, port);

        } catch (SocketException e) {
            // connection reset — НОРМА
            flushRemaining(buffer, port);
            log.debug("Client disconnected (reset) on port {}", port);

        } catch (IOException e) {
            flushRemaining(buffer, port);
            log.warn("I/O error on port {}", port, e);
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
}
