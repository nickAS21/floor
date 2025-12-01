package org.nickas21.smart.usr;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.unit.UsrTcpWifiUnit;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.nickas21.smart.usr.unit.UsrTcpWiFiUtils.ID_LENGTH;
import static org.nickas21.smart.usr.unit.UsrTcpWiFiUtils.MIN_PACKET_LENGTH;
import static org.nickas21.smart.usr.unit.UsrTcpWiFiUtils.START_SIGN;
import static org.nickas21.smart.usr.unit.UsrTcpWiFiUtils.bytesToHex;

@Slf4j
@Service
public class UsrTcpWiFiService {

    private final Integer[] ports;
    Map<String, UsrTcpWifiUnit> units = new ConcurrentHashMap<>();

    // last payloads map: key = "<port>_<typeFrameHex>"
    private final ConcurrentMap<String, String> lastPayloads = new ConcurrentHashMap<>();
    //  private int version;                    // [2] ver (Наприклад, 0C0C)
    private int majorVersion;
    private int minorVersion;

    // system messages prefix per port (matches Python's SYSTEM_MESSAGES_PREFIX per PORT)
    private final Map<Integer, StringBuilder> systemMessagesPrefixPerPort = new ConcurrentHashMap<>();

    public UsrTcpWiFiService(UsrTcpWiFiProperties usrTcpWiFiProperties) {
        int portStart = usrTcpWiFiProperties.getPortStart();
        int batteriesCnt = usrTcpWiFiProperties.getBatteriesCnt();
        this.ports = new Integer[batteriesCnt];
        for (int i = 0; i < batteriesCnt; i++) this.ports[i] = portStart + i;
        log.info("USR TCP WiFi ports initialized: start={}, ports={}", portStart, Arrays.toString(ports));
    }

    public void init() {
        log.info("Starting USR TCP WiFi listeners...");
        for (int port : ports) {
            Thread t = new Thread(() -> listenOnPort(port), "usr-tcp-listener-" + port);
            t.setDaemon(true);
            t.start();
        }
    }

    // ------------------ listener per port ------------------
    private void listenOnPort(int port) {
        log.info("Listener started on port {}", port);

        Path logDir = Paths.get("./logs_" + port);
        Path allLogFile = logDir.resolve("all.log");

        // init system messages prefix for this port
        systemMessagesPrefixPerPort.putIfAbsent(port, new StringBuilder());

        try {
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                log.info("Created log dir for port {} : {}", port, logDir);
            }

            // Prepare initial "waiting" system message - mirror Python prepare_system_messages
            String dirMsg = Files.exists(logDir) ? "" : "Created log directory: " + logDir + "\n";
            String waitMsg = String.format("*** Waiting for connection on %s:%d ***", "0.0.0.0", port);
            StringBuilder sbPrefix = systemMessagesPrefixPerPort.get(port);
            if (!dirMsg.isEmpty()) sbPrefix.append(dirMsg);
            sbPrefix.append(waitMsg).append("\n");

            try (ServerSocket server = new ServerSocket(port)) {
                log.info("Listening on port {}", port);

                while (true) {
                    try (Socket conn = server.accept()) {
                        String connectMsg = "Connected to " + conn.getRemoteSocketAddress();
                        log.info(connectMsg);

                        // append connect message to system prefix (same as Python)
                        sbPrefix.append(connectMsg).append("\n");

                        // write all accumulated system messages into all.log (once, as Python does)
                        writeToAllLog(allLogFile, sbPrefix.toString().trim() + System.lineSeparator());

                        // buffer for incoming data
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        InputStream in = conn.getInputStream();
                        byte[] readBuf = new byte[4096];
                        int read;

                        while ((read = in.read(readBuf)) != -1) {
                            if (read == 0) continue;
                            buffer.write(readBuf, 0, read);

                            byte[] after = parseAndProcessData(buffer.toByteArray(), port, logDir, allLogFile, sbPrefix);
                            buffer.reset();
                            buffer.write(after);
                        }

                        log.info("Connection closed by client: {}", conn.getRemoteSocketAddress());
                    } catch (Exception e) {
                        log.error("Error in connection loop on port " + port, e);
                        // continue accepting next connection
                    }
                }

            }
        } catch (Exception e) {
            log.error("Critical TCP error on port " + port, e);
        }
    }

    // ------------------ utilities: time & write ------------------
    private String getCurrentTimeMs() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return "[" + now.format(f) + "]";
    }

    private void writeToAllLog(Path allLogFile, String entry) {
        try {
            Files.writeString(allLogFile, entry + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write to all.log ({})", allLogFile, e);
        }
    }

    /**
     * Writes type_<TYPE>.log with the same rules as Python:
     * - skip A2 and D0
     * - skip empty payload
     * - skip if same payload as last
     * - when first write for C1/C0 include the system prefix (connect messages)
     */
    private void writeToTypeLog(int port, Path logDir, String typeFrameHex, String payloadHex, byte[] payloadBytes,
                                String crcStatusMessage, StringBuilder systemMessagesPrefix) {
        if (payloadBytes == null || payloadBytes.length == 0) {
            return;
        }

        String decoded = null;
        switch (typeFrameHex) {
            case "A2":
            case "D0":
                // Якщо тип "A2" або "D0", повертаємося (як у початковому if-else)
                return;
            case "C0":
                decoded = UsrTcpWiFiDecoders.decodeC0Payload(payloadBytes);
                break; // Обов'язково break, щоб не перейти до наступного case
            case "C1":
                decoded = UsrTcpWiFiDecoders.decodeC1Payload(payloadBytes);
         }

        Path filePath = logDir.resolve("type_" + typeFrameHex + ".log");
        String mapKey = port + "_" + typeFrameHex;
        String last = lastPayloads.get(mapKey);
        if (payloadHex.equals(last)) return;

        StringBuilder logLine = new StringBuilder();
        logLine.append(getCurrentTimeMs()).append(" ").append(payloadHex);

        if (decoded != null && !decoded.isEmpty()) logLine.append(System.lineSeparator()).append(decoded);

        if (crcStatusMessage != null) {
            logLine.append(System.lineSeparator()).append(crcStatusMessage).append(System.lineSeparator());
        }

        StringBuilder contentToWrite = new StringBuilder();

        // If it's first write for this type on this port, add system prefix (like Python)
        if (("C1".equals(typeFrameHex) || "C0".equals(typeFrameHex)) && last == null) {
            if (systemMessagesPrefix != null && systemMessagesPrefix.length() > 0) {
                contentToWrite.append(systemMessagesPrefix.toString().trim()).append(System.lineSeparator()).append(System.lineSeparator());
            }
        }
        contentToWrite.append(logLine.toString()).append(System.lineSeparator());

        try {
            Files.writeString(filePath, contentToWrite.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            lastPayloads.put(mapKey, payloadHex);
        } catch (IOException e) {
            log.error("Failed to write type log for " + typeFrameHex + " on port " + port, e);
        }
    }

    // ------------------ parse & process (core) ------------------
    private byte[] parseAndProcessData(byte[] buffer, int port, Path logDir, Path allLogFile, StringBuilder systemMessagesPrefix) {
        if (buffer == null || buffer.length == 0) return buffer;
        int currentIndex = 0;
        int endIndex = -1;
        List<byte[]> packets = new ArrayList<>();

        while (true) {
            int startIndex = indexOf(buffer, START_SIGN, currentIndex);
            if (startIndex == -1) break;
            int foundEnd = indexOf(buffer, START_SIGN, startIndex + START_SIGN.length);
            if (foundEnd == -1) {
                if (buffer.length - startIndex >= MIN_PACKET_LENGTH) {
                    packets.add(Arrays.copyOfRange(buffer, startIndex, buffer.length));
                    currentIndex = buffer.length;
                }
                break;
            } else {
                byte[] pkt = Arrays.copyOfRange(buffer, startIndex, foundEnd);
                if (pkt.length >= MIN_PACKET_LENGTH) packets.add(pkt);
                currentIndex = foundEnd;
                endIndex = foundEnd;
            }
        }

        for (byte[] packet : packets) {
            try {
                if (packet.length < 2 || packet[0] != START_SIGN[0] || packet[1] != START_SIGN[1]) {
                    log.warn("ERROR: Invalid start signature!");
                    continue;
                }

                String typeFrameHex = String.format("%02X", packet[2] & 0xFF);

                int idStart = 3;
                int idEnd = idStart + ID_LENGTH;
                String idDisplay = "";
                if (packet.length >= idEnd) {
                    byte[] idBytes = Arrays.copyOfRange(packet, idStart, idEnd);
                    idDisplay = new String(idBytes, java.nio.charset.StandardCharsets.US_ASCII);
                }

                if (packet.length <= idEnd) {
                    log.warn("WARNING: Packet of type {} is too short for CRC.", typeFrameHex);
                    continue;
                }

                byte[] payloadWithCrc = Arrays.copyOfRange(packet, idEnd, packet.length);
                if (payloadWithCrc.length < 2) {
                    log.warn("WARNING: Packet of type {} is too short for CRC.", typeFrameHex);
                    continue;
                }

                byte[] payloadBytes = Arrays.copyOfRange(payloadWithCrc, 0, payloadWithCrc.length - 2);
                byte[] crcBytes = Arrays.copyOfRange(payloadWithCrc, payloadWithCrc.length - 2, payloadWithCrc.length);
                String payloadHex = bytesToHex(payloadBytes);
                String crcHex = bytesToHex(crcBytes);

                String crcMessage = UsrTcpWifiCrcUtilities.checkPacketCrc(packet, typeFrameHex);

                String timestamp = getCurrentTimeMs();
                String fullPacketHex = bytesToHex(packet);
                String output = timestamp + " " + fullPacketHex;

                String payloadDisplayDetails;
                if (payloadBytes.length == 0) {
                    payloadDisplayDetails = "null - CRC: " + crcHex;
                } else if ("A2".equals(typeFrameHex)) {
                    String versionInfo = UsrTcpWiFiDecoders.decodeA2Payload(payloadBytes);
                    payloadDisplayDetails = payloadHex + " " + versionInfo.strip().replace("\n", " ");
                } else {
                    payloadDisplayDetails = payloadHex;
                }

                String details = String.format("  TYPE: %s ID: %s Payload: %s", typeFrameHex, idDisplay, payloadDisplayDetails);
                String crcLine = "  " + crcMessage;

                log.info("""
                    {}
                      TYPE: {}  ID: {}  Payload: {}
                      {}
                    """,
                            output,                    // "[timestamp] <hex packet>"
                            typeFrameHex,              // C0/C1/A2/etc
                            idDisplay,                 // ASCII 10-byte ID
                            payloadDisplayDetails,     // payload HEX or A2 string
                            crcLine                    // "CRC Status: OK..."
                        );

                // Extra BMS info for C0
                if ("C0".equals(typeFrameHex)) {
                    String infoBmsMsg = UsrTcpWiFiDecoders.decodeC0BmsInfoPayload(payloadBytes);
                    if (infoBmsMsg != null && !infoBmsMsg.isBlank()) {
                        log.info("""
                            {}
                            """, infoBmsMsg.trim());
                                }
                }

                String allLogEntry = output + System.lineSeparator() + details + System.lineSeparator() + crcLine;
                writeToAllLog(allLogFile, allLogEntry);

                writeToTypeLog(port, logDir, typeFrameHex, payloadHex, payloadBytes, crcLine, systemMessagesPrefix);

            } catch (Exception e) {
                log.error("ERROR processing packet: " + e.getMessage(), e);
                log.error("Packet (HEX): " + bytesToHex(packet));
            }
        }

        if (packets.isEmpty()) return buffer;
        else {
            if (endIndex == -1) {
                return buffer;
            } else {
                if (currentIndex >= buffer.length) return new byte[0];
                return Arrays.copyOfRange(buffer, currentIndex, buffer.length);
            }
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle, int fromIndex) {
        outer:
        for (int i = Math.max(0, fromIndex); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
