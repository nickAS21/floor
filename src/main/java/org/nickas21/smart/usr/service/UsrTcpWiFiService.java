package org.nickas21.smart.usr.service;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

@Slf4j
@Service
public class UsrTcpWiFiService {

    @Value("${app.test_front:false}")
    boolean testFront;

    private final Integer[] ports;
    private String logsDir;


    @Autowired
    @Lazy
    UsrTcpWiFiParseData usrTcpWiFiParseData;

    private final UsrTcpWiFiProperties usrTcpWiFiProperties;
    private final UsrTcpWiFiLogWriter usrTcpWiFiLogWriter;

    public UsrTcpWiFiService(UsrTcpWiFiProperties usrTcpWiFiProperties,
                             UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry,
                             UsrTcpWiFiLogWriter usrTcpWiFiLogWriter) {
        this.usrTcpWiFiProperties = usrTcpWiFiProperties;
        this.usrTcpWiFiLogWriter = usrTcpWiFiLogWriter;
        int portStart = usrTcpWiFiProperties.getPortStart();
        int batteriesCnt = usrTcpWiFiProperties.getBatteriesCnt();
        this.logsDir = usrTcpWiFiProperties.getLogsDir();
        this.ports = new Integer[batteriesCnt];
        for (int i = 0; i < batteriesCnt; i++) {
            this.ports[i] = portStart + i;
            usrTcpWiFiBatteryRegistry.initBattery(this.ports[i]);
        }
        log.info("USR TCP WiFi ports initialized: start={}, ports={}", portStart, Arrays.toString(ports));
    }

    // --------------------------
    // INIT CONFIG
    // --------------------------
    public void init() {
        try {
            if (logsDir == null || logsDir.isBlank()) {
                logsDir = "/tmp/usr-bms";   // fallback for Kubernetes
            }
            if (!testFront) {
                Path dir = Paths.get(logsDir);

                if (Files.exists(dir)) {
                    Files.walk(dir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            }
            Files.createDirectories(Paths.get(logsDir));
            log.info("LogsDir: [{}], Starting USR TCP WiFi listeners...", logsDir);
            usrTcpWiFiLogWriter.init(this.logsDir, this.usrTcpWiFiProperties, ports);
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
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                try (Socket conn = server.accept()) {
                    handleConnection(conn, port);
                } catch (Exception e) {
                    log.error("Connection error on port {}", port, e);
                }
            }
        } catch (Exception e) {
            log.error("ServerSocket error on port {}", port, e);
        }
    }

    // --------------------------
    // HANDLE ONE CONNECTION
    // --------------------------
    private void handleConnection(Socket conn, int port) {
        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] readBuf = new byte[4096];
            int read;

            while ((read = in.read(readBuf)) != -1) {
                if (read == 0) continue;
                buffer.write(readBuf, 0, read);

                byte[] after = usrTcpWiFiParseData.parseAndProcessData(buffer.toByteArray(), port);
                buffer.reset();
                buffer.write(after);
            }
        } catch (Exception e) {
            log.error("Error while processing connection on port {}", port, e);
        }
    }
}
