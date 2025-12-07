package org.nickas21.smart.usr.service;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.data.ErrorLogType;
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

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.ID_END;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.ID_START;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.MIN_PACKET_LENGTH;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.START_SIGN;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C0;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C1;
import static org.nickas21.smart.usr.data.UsrTcpWifiBalanceThresholds.CRITICAL_LIMIT;
import static org.nickas21.smart.usr.data.UsrTcpWifiBalanceThresholds.EMERGENCY_MAX;
import static org.nickas21.smart.util.StringUtils.bytesToHex;
import static org.nickas21.smart.util.StringUtils.intToHex;

@Slf4j
@Service
public class UsrTcpWiFiParseData {

    private final UsrTcpWiFiLogWriter logWriter;
    private final UsrTcpWiFiBatteryRegistry batteryRegistry;


    // --- Throttling for writing every 4 min ---
    private final List<UsrTcpWiFiPacketRecord> pendingErrorRecords = new ArrayList<>();
    private static final long WRITE_INTERVAL = 240_000; // 4 min
    private long lastWriteTimeError = 0;
    private long lastWriteTimeLast = 0;

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
                        UsrTcpWifiC0Data c0Data = this.batteryRegistry.getBattery(port).getC0Data();
                        UsrTcpWiFiDecoders.decodeC0Payload(payloadBytes, c0Data, hostAddress, nowInstant);
                        if (c0Data.getErrorInfoData() > 0) {
                            this.batteryRegistry.getBattery(port).setErrRecord(new UsrTcpWiFiErrorRecord(c0Data.getTimestamp(), intToHex(c0Data.getErrorInfoData()), c0Data.getErrorOutput()));
                        }
                        this.batteryRegistry.getBattery(port).setLastTime(c0Data.getTimestamp());
                        String infoC0BmsMsg = c0Data.decodeC0BmsInfoPayload(output);
                        if (!infoC0BmsMsg.isBlank()) {
                            log.info("""
                                    {}
                                    """, infoC0BmsMsg.trim());
                        }
                    } else {
                        UsrTcpWifiC1Data c1Data = this.batteryRegistry.getBattery(port).getC1Data();
                        UsrTcpWiFiDecoders.decodeC1Payload(payloadBytes, c1Data, errorRecord, nowInstant);
                        if (c1Data.getErrorInfoData() > 0) {
                            this.batteryRegistry.getBattery(port).setErrRecord(new UsrTcpWiFiErrorRecord(
                                    c1Data.getTimestamp(), intToHex(c1Data.getErrorInfoData()), c1Data.getErrorOutput()));
                        }
                        this.batteryRegistry.getBattery(port).setLastTime(c1Data.getTimestamp());
                        String infoC1BmsMsg = c1Data.decodeC1BmsInfoPayload();
                        if (!infoC1BmsMsg.isBlank()) {
                            log.info("""
                                    {}
                                    """, infoC1BmsMsg.trim());
                        }
                        // write to file error history C1 - unBalance = delta + min/max + level_code
                        // 764862063274;8897;C1;len;c1Data.balanceS
                        if (c1Data.getBalanceS() != null &&
                                (c1Data.getBalanceS().equals(CRITICAL_LIMIT) || c1Data.getBalanceS().equals(EMERGENCY_MAX))) {
                            byte[] errorMsgBalance = c1Data.getBalanceS().getDescription().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                            pendingErrorRecords.add(
                                    new UsrTcpWiFiPacketRecord(
                                            timestamp,
                                            port,
                                            ErrorLogType.B1.name(),
                                            errorMsgBalance.length,
                                            errorMsgBalance
                                    )
                            );

                        }

                        // write to file error history C1 - errors
                        // 1764862063274;8897;C1;len;2008
                        // 1764862063274;8897;C1;len;1007 => c1Data.errOutput
                        if (c1Data.getErrorInfoData() != null && c1Data.getErrorInfoData() > 0) {
                            byte[] errorMsgInfoData = c1Data.getErrorOutput().getBytes();
                            pendingErrorRecords.add(
                                    new UsrTcpWiFiPacketRecord(
                                            timestamp,
                                            port,
                                            ErrorLogType.E1.name(),
                                            errorMsgInfoData.length,
                                            errorMsgInfoData
                                    )
                            );

                        }
                        long now = System.currentTimeMillis();

                        // --- Write ALL collected errors every 4 minutes ---
                        if (!pendingErrorRecords.isEmpty() && now - lastWriteTimeError >= WRITE_INTERVAL) {
                            for (UsrTcpWiFiPacketRecord rec : pendingErrorRecords) {
                                logWriter.writeError(rec);
                            }
                            pendingErrorRecords.clear();
                            lastWriteTimeError = now;
                        }
                    }
                    // write to file last
                    // 1764862062785;8895;C0;21;140014AAFFF75A00040000000A0000000500000000
                    // 1764862063274;8897;C1;43;28100CDF0CD50CDF0CDB0CEA0CDB0CE80CEB0CF00CE20CE70CEB0CEA0CF50CF90CFC03F25F000000000C10
                    UsrTcpWiFiPacketRecord lastLastRecord = new UsrTcpWiFiPacketRecord(
                            timestamp,
                            port,
                            typeFrameName,
                            payloadBytes.length,
                            payloadBytes);
                    long now = System.currentTimeMillis();
                    // ----- write Last every 4 minutes -----
                    if (now - lastWriteTimeLast >= WRITE_INTERVAL) {
                        logWriter.writeLast(lastLastRecord);
                        lastWriteTimeLast = now;
                    }
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

    public String getBmsSummary(int port){
        UsrTcpWifiC0Data c0Data = this.batteryRegistry.getBattery(port).getC0Data();
        UsrTcpWifiC1Data c1Data = this.batteryRegistry.getBattery(port).getC1Data();
        if (c0Data.getTimestamp() == null || c1Data.getTimestamp() == null) return null;
        try {
            StringBuilder out = new StringBuilder();
            out.append(String.format("- HostAddress: %s\n", c0Data.getHostAddress()));
            out.append(String.format("- SOC: %d %%\n", c0Data.getSocPercent()));
            out.append(String.format("- Voltage: %.2f V\n", c0Data.getVoltageCurV()));
            out.append(String.format("- Current: %.2f A\n", c0Data.getCurrentCurA()));
            out.append(String.format("- BMS status %s\n", c0Data.getBmsStatusStr()));
            out.append(String.format("- Cells delta: %.3f V\n", c1Data.getDeltaMv() / 1000.0));
            if (c0Data.getErrorInfoData() > 0) {
                out.append(String.format("- Error info Data:  | 0x%s\n", Integer.toHexString(c0Data.getErrorInfoData()).toUpperCase()));
            }
            return out.toString();
        } catch (Exception e) {
            log.error("CRITICAL DECODE ERROR C0", e);
            return null;
        }
    }
}
