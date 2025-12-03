package org.nickas21.smart.usr.service;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.data.UsrTcpWiFiDecoders;
import org.nickas21.smart.usr.data.UsrTcpWiFiMessageType;
import org.nickas21.smart.usr.data.UsrTcpWifiCrcUtilities;
import org.nickas21.smart.usr.entity.UsrTcpWiFiErrorRecord;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiC1Data;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecord;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.START_SIGN;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.ID_END;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.ID_START;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.MIN_PACKET_LENGTH;

import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C0;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C1;
import static org.nickas21.smart.util.StringUtils.bytesToHex;

@Slf4j
@Service
public class UsrTcpWiFiParseData {

    private final UsrTcpWiFiLogWriter logWriter;
    private final UsrTcpWiFiBatteryRegistry batteryRegistry;

    public UsrTcpWiFiParseData(UsrTcpWiFiLogWriter logWriter, UsrTcpWiFiBatteryRegistry batteryRegistry) {
        this.logWriter = logWriter;
        this.batteryRegistry = batteryRegistry;
    }

    // ------------------ parse & process (core) ------------------
    protected byte[] parseAndProcessData(byte[] buffer, int port, String hostAddress) {
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
                if (!validationPacket(packet)) {
                    continue;
                }
                UsrTcpWiFiMessageType msgType = UsrTcpWiFiMessageType.fromByte(packet[2]);
                if (C0.equals(msgType) || C1.equals(msgType)) {
                    String typeFrameName = msgType.name();

                    byte[] idBytes = Arrays.copyOfRange(packet, ID_START, ID_END);
                    String idValue = new String(idBytes, java.nio.charset.StandardCharsets.US_ASCII);

                    byte[] payloadWithCrc = Arrays.copyOfRange(packet, ID_END, packet.length);
                    byte[] payloadBytes = Arrays.copyOfRange(payloadWithCrc, 0, payloadWithCrc.length - 2);
                    String crcMessage = UsrTcpWifiCrcUtilities.checkPacketCrc(packet, typeFrameName);

                    Instant nowInstant = Instant.now();
                    long timestamp = nowInstant.toEpochMilli();
                    String timestampStr = getCurrentTimeString(nowInstant);
                    String fullPacketHex = bytesToHex(packet);
                    String output = String.format(
                        """
                         [%s] %s %s => %s
                        """, hostAddress, timestampStr, fullPacketHex, crcMessage
                    );

                    // Extra BMS info for C0 and update C0/C1
                    UsrTcpWiFiErrorRecord errorRecord = this.batteryRegistry.getBattery(port).getErrRecord();
                    if (C0.equals(msgType)) {
                        String infoBmsMsg = UsrTcpWiFiDecoders.decodeC0BmsInfoPayload(payloadBytes, output);
                        if (!infoBmsMsg.isBlank()) {
                            log.info("""
                                    {}
                                    """, infoBmsMsg.trim());
                        }
                        UsrTcpWifiC0Data c0Data = this.batteryRegistry.getBattery(port).getC0Data();
                        UsrTcpWiFiDecoders.decodeC0Payload(payloadBytes, c0Data, errorRecord, nowInstant);
                        this.batteryRegistry.getBattery(port).setLastTime(nowInstant);
                    } else {
                        UsrTcpWifiC1Data c1Data = this.batteryRegistry.getBattery(port).getC1Data();
                        UsrTcpWiFiDecoders.decodeC1Payload(payloadBytes, c1Data, errorRecord, nowInstant);
                        this.batteryRegistry.getBattery(port).setLastTime(nowInstant);
                    }
                    // write to file last
                    this.logWriter.append(new UsrTcpWiFiPacketRecord(
                            timestamp,
                            port,
                            typeFrameName,
                            payloadBytes.length,
                            payloadBytes));
                }
            } catch (Exception e) {
                String msgError = String.format("Error processing packet at %s:%d. Packet (HEX): [%s]", hostAddress, port, bytesToHex(packet));
                log.error(msgError + e.getMessage(), e);
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

    private String getCurrentTimeString(Instant now) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return "[" + f.format(now.atZone(ZoneId.systemDefault())) + "]";
    }

    private boolean validationPacket(byte[] packet) {
        if (packet.length < MIN_PACKET_LENGTH) {
            log.warn("WARNING: Packet of type is too short for Ident + CRC length.");
            return  false;
        }
        if (packet[0] != START_SIGN[0] || packet[1] != START_SIGN[1] ) {
            log.warn("ERROR: Invalid start signature!");
            return false;
        }
        return true;
    }
}
