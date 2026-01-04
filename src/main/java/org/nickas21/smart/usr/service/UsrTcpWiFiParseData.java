package org.nickas21.smart.usr.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.data.UsrTcpWiFiDecoders;
import org.nickas21.smart.usr.data.UsrTcpWiFiMessageType;
import org.nickas21.smart.usr.data.UsrTcpWifiCrcUtilities;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBmsSummary;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiC1Data;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecord;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.ID_END;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.ID_START;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.MIN_PACKET_LENGTH;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.START_SIGN;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.A2;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C0;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C1;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.D0;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds.AUTO_RECOVERABLE_MAX;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds.SERVICE_REQUIRED_MAX;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiFaultLogType.B1;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiFaultLogType.E1;
import static org.nickas21.smart.util.StringUtils.bytesToHex;
import static org.nickas21.smart.util.StringUtils.getCurrentTimeString;
import static org.nickas21.smart.util.StringUtils.intToHex;

@Slf4j
@Service
public class UsrTcpWiFiParseData {

    @Getter
    public final UsrTcpWiFiLogWriter logWriter;
    @Getter
    public final UsrTcpWiFiProperties usrTcpWiFiProperties;
    @Getter
    public final UsrTcpLogsWiFiProperties usrTcpLogsWiFiProperties;
    public final UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry;


    // --- Throttling for writing every 4 min ---
    private final Map<List<String>, String> lastErrorRecords = new ConcurrentHashMap<>();
    private long lastWriteTime = 0;

    public UsrTcpWiFiParseData(UsrTcpWiFiLogWriter logWriter, UsrTcpWiFiProperties usrTcpWiFiProperties,
                               UsrTcpLogsWiFiProperties usrTcpLogsWiFiProperties,
                               UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry) {
        this.logWriter = logWriter;
        this.usrTcpWiFiProperties = usrTcpWiFiProperties;
        this.usrTcpLogsWiFiProperties = usrTcpLogsWiFiProperties;
        this.usrTcpWiFiBatteryRegistry = usrTcpWiFiBatteryRegistry;
    }

    // ------------------ parse & process (core) ------------------
    protected byte[] parseAndProcessData(byte[] buffer, int port) {
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
                    String timestampStr = getCurrentTimeString(nowInstant);
                    String fullPacketHex = bytesToHex(packet);
                    String output = String.format(
                        """
                         [%s] %s => %s
                        """, timestampStr, fullPacketHex, crcMessage
                    );

                    // Extra BMS info for C0 and update C0/C1
                     if (C0.equals(msgType)) {
                        UsrTcpWifiC0Data c0Data = this.usrTcpWiFiBatteryRegistry.getBattery(port).getC0Data();
                        UsrTcpWiFiDecoders.decodeC0Payload(payloadBytes, c0Data, nowInstant, port);

                        this.usrTcpWiFiBatteryRegistry.getBattery(port).setLastTime(c0Data.getTimestamp());
//                        if (testFront) {
//                            String infoC0BmsMsg = c0Data.decodeC0BmsInfoPayload(output);
//                            if (!infoC0BmsMsg.isBlank()) {
//                                log.info("""
//                                        {}
//                                        """, infoC0BmsMsg.trim());
//                            }
//                        }
                    } else {
                        UsrTcpWifiC1Data c1Data = this.usrTcpWiFiBatteryRegistry.getBattery(port).getC1Data();
                        UsrTcpWiFiDecoders.decodeC1Payload(payloadBytes, c1Data, nowInstant);
                        this.usrTcpWiFiBatteryRegistry.getBattery(port).setLastTime(c1Data.getTimestamp());
                        // write to file error history C1 - unBalance = delta + min/max + level_code
                        // 764862063274;8897;C1;len;c1Data.balanceS
                        List<String> key = List.of(String.valueOf(port), B1.name());
                        if (c1Data.getBalanceS() != null &&
                                (c1Data.getBalanceS().equals(AUTO_RECOVERABLE_MAX) || c1Data.getBalanceS().equals(SERVICE_REQUIRED_MAX))) {
                            String newValue =  c1Data.getBalanceS().name() + ": " + intToHex(c1Data.getErrorInfoData());
                            String oldValue =  lastErrorRecords.get(key);
                            if (!newValue.equals(oldValue)) {
                                this.usrTcpWiFiBatteryRegistry.getBattery(port).setErrRecordB1(c1Data.getErrorUnbalanceForRecords(port));
                                lastErrorRecords.compute(key, (k, v) -> newValue);
                                logWriter.writeError(port, this.usrTcpWiFiBatteryRegistry.getBattery(port).getErrRecordB1());
                            }
                        } else {
                            this.usrTcpWiFiBatteryRegistry.getBattery(port).setErrRecordB1(null);
                            lastErrorRecords.compute(key, (k, v) -> null);
                        }
//                        if (testFront) {
//                            String infoC1BmsMsg = c1Data.decodeC1BmsInfoPayload(output);
//                            if (!infoC1BmsMsg.isBlank()) {
//                                log.info("""
//                                        {}
//                                        """, infoC1BmsMsg.trim());
//                            }
//                        }

                        // write to file error history C1 - errors
                        // 1764862063274;8897;C1;len;2008
                        // 1764862063274;8897;C1;len;1007 => c1Data.errOutput
                        key = List.of(String.valueOf(port), E1.name());
                        Integer errorInfoData = c1Data.getErrorInfoData();
                        if (errorInfoData != null && errorInfoData > 0) {
                            String newValue =  intToHex(errorInfoData);
                            String oldValue =  lastErrorRecords.get(key);
                            if (!newValue.equals(oldValue)) {
                                this.usrTcpWiFiBatteryRegistry.getBattery(port).setErrRecordE1(c1Data.getErrorOutputForRecords(port));
                                lastErrorRecords.put(key, newValue);
                                logWriter.writeError(port, this.usrTcpWiFiBatteryRegistry.getBattery(port).getErrRecordE1());
                            }
                        } else {
                            this.usrTcpWiFiBatteryRegistry.getBattery(port).setErrRecordE1(null);
                            lastErrorRecords.compute(key, (k, v) -> null);
                        }
                    }

                    synchronized (this) {
                        // --- Write ALL last info c0/c1 by port every 30 minutes ---
                        // write to file today
                        // 1764862062785;8895;C0;21;140014AAFFF75A00040000000A0000000500000000
                        // 1764862063274;8897;C1;43;28100CDF0CD50CDF0CDB0CEA0CDB0CE80CEB0CF00CE20CE70CEB0CEA0CF50CF90CFC03F25F000000000C10
                        long now = System.currentTimeMillis();
                        if (now - lastWriteTime >= this.usrTcpLogsWiFiProperties.getWriteInterval()) {
                            for (Map.Entry<Integer, UsrTcpWiFiBattery> entry : usrTcpWiFiBatteryRegistry.getAll().entrySet()) {
                                int portWrite = entry.getKey();
                                UsrTcpWiFiBattery battery = entry.getValue();

                                UsrTcpWiFiPacketRecord lastRecordC0 = battery.getC0Data().getInfoForRecords(portWrite);
                                UsrTcpWiFiPacketRecord lastRecordC1 = battery.getC1Data().getInfoForRecords(portWrite);

                                if (lastRecordC0 != null) logWriter.writeToday(portWrite, lastRecordC0);
                                if (lastRecordC1 != null) logWriter.writeToday(portWrite, lastRecordC1);
                            }
                            lastWriteTime = now;
                        }
                    }
                } else if (!A2.equals(msgType) && !D0.equals(msgType)) {
                    log.error("UNKNOWN message type: 0x{}", String.format("%02X", packet[2]));
                }
            } catch (Exception e) {
                String msgError = String.format("Error processing packet at %d. Packet (HEX): [%s]", port, bytesToHex(packet));
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

    public UsrTcpWiFiBmsSummary getBmsSummary(int portMaster){
        UsrTcpWiFiBattery usrTcpWiFiBattery = this.getBattery(portMaster);
        if (usrTcpWiFiBattery != null) {
            UsrTcpWifiC0Data c0Data = usrTcpWiFiBattery.getC0Data();
            UsrTcpWifiC1Data c1Data = usrTcpWiFiBattery.getC1Data();
            String bmsErrors = null;
            if (c0Data.getTimestamp() == null || c1Data.getTimestamp() == null) return null;
            try {
                double batteryCurrentAll = 0;
                double batterySocMax = c0Data.getSocPercent();
                for (int i = 0; i < this.usrTcpWiFiProperties.getBatteriesCnt(); i++) {
                    int portOut = this.usrTcpWiFiProperties.getPortStart() + i;
                    UsrTcpWiFiBattery usrTcpWiFiBatteryA = this.getBattery(portOut);
                    if (usrTcpWiFiBatteryA != null && usrTcpWiFiBatteryA.getC0Data() != null) {
                        log.warn("port [{}] batteryCurrent [{}] soc [{}]", portOut, usrTcpWiFiBatteryA.getC0Data().getCurrentCurA(), usrTcpWiFiBatteryA.getC0Data().getSocPercent());
                        batteryCurrentAll += usrTcpWiFiBatteryA.getC0Data().getCurrentCurA();
                        // TODO - 8894 - 20% this is bad then only master
                        batterySocMax = usrTcpWiFiBatteryA.getC0Data().getSocPercent() != 0 ? Math.max(batterySocMax, usrTcpWiFiBatteryA.getC0Data().getSocPercent()) : batterySocMax;
                    }
                }

                StringBuilder out = new StringBuilder();
                out.append(String.format("- BMS status %s\n", c0Data.getBmsStatusStr()));
                out.append(String.format("- Voltage: %.2f V\n", c0Data.getVoltageCurV()));
                out.append(String.format("- Current: %.2f A\n", batteryCurrentAll));
                out.append(String.format("- Cells delta: %.3f V\n", c1Data.getDeltaMv() / 1000.0));
                StringBuilder errorBuilder = getStringBuilderError();
                if (!errorBuilder.toString().isEmpty()) {
                    bmsErrors = (String.format("Error info Data:\n%s", errorBuilder));
                }
                return new UsrTcpWiFiBmsSummary(c0Data.getTimestamp(), batterySocMax, bmsErrors, out.toString());
            } catch (Exception e) {
                log.error("CRITICAL DECODE ERROR C0", e);
                return null;
            }
        } else {
            log.error("Check the data on port {} it is not in use. Size BatteryRegistry {}", portMaster, this.usrTcpWiFiBatteryRegistry.getAll().size());
            return null;
        }
    }

    public UsrTcpWiFiBattery getBattery(int port){
        return this.usrTcpWiFiBatteryRegistry.getBattery(port);
    }


    @NotNull
    private StringBuilder getStringBuilderError() {
        StringBuilder errorBuilder = new StringBuilder();
        for (Map.Entry<Integer, UsrTcpWiFiBattery> batteryEntry  : this.usrTcpWiFiBatteryRegistry.getAll().entrySet()) {
            if (batteryEntry.getValue().getErrRecordE1() != null){
                errorBuilder.append(batteryEntry.getValue().getErrRecordE1().toMsgForBot());
            }
            if (batteryEntry.getValue().getErrRecordB1() != null){
                errorBuilder.append(batteryEntry.getValue().getErrRecordB1().toMsgForBot());
            }
        }
        return errorBuilder;
    }
}
