package org.nickas21.smart.usr.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.data.InvertorGolegoDecoders;
import org.nickas21.smart.usr.data.UsrTcpWiFiDecoders;
import org.nickas21.smart.usr.data.UsrTcpWiFiMessageType;
import org.nickas21.smart.usr.data.UsrTcpWifiCrcUtilities;
import org.nickas21.smart.usr.entity.InverterData;
import org.nickas21.smart.usr.entity.InvertorGolegoData32;
import org.nickas21.smart.usr.entity.InvertorGolegoData90;
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
import static org.nickas21.smart.usr.data.UsrTcpWifiCrcUtilities.isValidInverterGolegoCrc;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds.AUTO_RECOVERABLE_MAX;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds.SERVICE_REQUIRED_MAX;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiFaultLogType.B1;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiFaultLogType.E1;
import static org.nickas21.smart.util.StringUtils.bytesToHex;
import static org.nickas21.smart.util.StringUtils.getCurrentTimeString;
import static org.nickas21.smart.util.StringUtils.intToHex;
import static org.nickas21.smart.util.StringUtils.payloadToHexString;

@Slf4j
@Service
public class UsrTcpWiFiParseData {

    @Getter
    public final UsrTcpWiFiLogWriter logWriter;
    @Getter
    public final UsrTcpWiFiProperties usrTcpWiFiProperties;
    @Getter
    public final UsrTcpLogsWiFiProperties usrTcpLogsWiFiProperties;
    @Getter
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
        if (port == (usrTcpWiFiProperties.getPortInverterGolego())) {
            return parseAndProcessInverterGolego(buffer);
        } else {
            return parseAndProcessBmsGolego(buffer, port);
        }
    }

    protected byte[] parseAndProcessBmsGolego(byte[] buffer, int port) {

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
                            for (Map.Entry<Integer, UsrTcpWiFiBattery> entry : usrTcpWiFiBatteryRegistry.getBatteriesAll().entrySet()) {
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
            log.error("Check the data on port {} it is not in use. Size BatteryRegistry {}", portMaster, this.usrTcpWiFiBatteryRegistry.getBatteriesAll().size());
            return null;
        }
    }

    public UsrTcpWiFiBattery getBattery(int port){
        return this.usrTcpWiFiBatteryRegistry.getBattery(port);
    }


    @NotNull
    private StringBuilder getStringBuilderError() {
        StringBuilder errorBuilder = new StringBuilder();
        for (Map.Entry<Integer, UsrTcpWiFiBattery> batteryEntry  : this.usrTcpWiFiBatteryRegistry.getBatteriesAll().entrySet()) {
            if (batteryEntry.getValue().getErrRecordE1() != null){
                errorBuilder.append(batteryEntry.getValue().getErrRecordE1().toMsgForBot());
            }
            if (batteryEntry.getValue().getErrRecordB1() != null){
                errorBuilder.append(batteryEntry.getValue().getErrRecordB1().toMsgForBot());
            }
        }
        return errorBuilder;
    }

    protected byte[] parseAndProcessInverterGolego(byte[] buffer) {
        int currentIndex = 0;

        // Нам треба мінімум 5 байт (Addr + Func + Len + CRC_low + CRC_high), щоб почати
        while (currentIndex + 5 <= buffer.length) {

            // 1. Отримуємо довжину даних (Payload) з 3-го байта
            int payloadLen = buffer[currentIndex + 2] & 0xFF;

            // Перевірка на адекватність (Modbus RTU Payload зазвичай не більше 250 байт)
            if (payloadLen > 250) {
                currentIndex++; // Це явно не початок пакета, зсуваємося
                continue;
            }

            int fullPacketLen = payloadLen + 5; // 3 заголовок + N дані + 2 CRC

            // 2. Перевіряємо, чи весь пакет долетів
            if (currentIndex + fullPacketLen <= buffer.length) {
                byte[] packet = Arrays.copyOfRange(buffer, currentIndex, currentIndex + fullPacketLen);

                try {
                    // 3. Валідація CRC (тепер передаємо ПОВНИЙ пакет 95 байт)
                    if (isValidInverterGolegoCrc(packet)) {
                        // Пакет валідний — обробляємо
                        this.processInverterGolego(packet);

                        // Зсуваємо індекс на всю довжину обробленого пакета
                        currentIndex += fullPacketLen;
                    } else {
                        // CRC не збіглося — можливо, ми знайшли випадковий байт 0x05 в даних.
                        // Зсуваємося на 1 байт, щоб шукати справжній початок далі.
                        currentIndex++;
                    }
                } catch (Exception e) {
                    log.error("Error decoding Inverter packet at port {}: {}", usrTcpWiFiProperties.getPortInverterGolego(), e.getMessage());
                    currentIndex++;
                }
            } else {
                // Пакет ще не повний (наприклад, прийшло 40 байт з 95)
                // Виходимо з циклу і чекаємо решту байтів
                break;
            }
        }

        // Повертаємо те, що залишилося в буфері і не було оброблено
        if (currentIndex >= buffer.length) return new byte[0];
        return Arrays.copyOfRange(buffer, currentIndex, buffer.length);
    }

    /**
     * 05 03 5A  03 00  00 00   00 00  00 00  00 00  0B 02  29 00  00 00  05 00 FC 08 F4 01 58 01 AD 00 05 00 05 00 01 00 B8 01 F2 1C 00 00 02 00 38 18 38 18 E6 00 1A 00 E0 01 E6 00 F4 01 1A 00 00 00 00 00 9F 15 9F 15 9F 15 37 00 ED 01 01 00 00 00 01 00 02 00 00 00 28 00 E6 00 28 00 EA 01 1C 02 96 AE
     * 05 - це адреса
     * 03 - це тип
     * 5A -> 90  len
     * [0:1] 04 00: 4 -> Статус (Заряд)
     * [2:3] C4 08: 2244 -> 224.4 Ac Input Voltage
     * [4:5] F3 01: 499 -> 49,9 Hz AC Input Frewquency
     *  [6:7] 00 00: 0 -> Rezerv_06
     *  [8:9] 00 00: 0  -> Rezerv_08
     * [10:11] 1C 02: 540 -> V 54.0 (Напруга АКБ)
     * [12:13] 5F 00: 95 -> Soc 95%
     * [14:15] 28 00: 40 -> 40 A Currency
     * [16:17] 00 00: 0 -> Rezerv_16
     *  [18:19] C9 08: 2249 -> AC  Input Voltage 224.9 V
     *  [20:21] F4 01: 500 -> AC  Input  Frequency 50.0Hz
     * [22:23] 0D 01: 269 -> 269 VA OutPut Apparent Power
     * [24:25] FD 00: 253 -> 253 W OutPut Active Power
     * [26:27] 04 00: 4 -> 4% AC Out Put Load
     * [28:29] 04 00: 4 ->  Rezerv _28
     *  [30:31] 01 00: 1 -> Rezerv _30
     * [32:33] B8 01: 440 -> Rezerv _32
     * [34:35] F2 1C: 7410 -> 7410  Main CPU Version
     * [36:37] 00 00: 0  -> Rezerv_36
     *[38:39] 02 00: 2   -> Rezerv_38
     * [40:41] 38 18: 6200 ->  6200 VA Nominal Output Apparent Power
     * [42:43] 38 18: 6200 ->  6200 W Nominal Output Active Power
     * [44:45] E6 00: 230 -> 230 V Nominal Output Voltage
     * [46:47] 1A 00: 26 -> 26A Nominal AC current
     * [48:49] E0 01: 480 -> 48.0 V Rated Battery Voltage
     * [50:51] E6 00: 230 -> 230V Nominal OutPut Voltage
     * [52:53] F4 01: 500 -> 50.0 Hz Nominal OutPut Frequency
     * [54:55] 1A 00: 26 -> 26A Nominal OutPut Current
     * [56:57] 00 00: 0 ->  Rezerv_56
     * [58:59] 00 00: 0 ->  Rezerv_58
     * [60:61] 9F 15: 5535 -> 55.35 V  Rezerv_60
     *  [62:63] 9F 15: 5535 -> 55.35 V  Rezerv_62
     *  [64:65] 9F 15: 5535 -> 55.35 V  Rezerv_64
     *  [66:67] 37 00: 55-> 55.00 V  Rezerv_66
     *  [68:69] ED 01: 493 -> 49.3 V Rezerv_68
     *  [70:71] 01 00: 1 -> 1 Rezerv_70
     *  [72:73] 00 00: 0 -> 0 Rezerv_72
     *   [76:77] 02 00: 2  -> 2 Rezerv_76
     *    [78:79] 00 00: 0  -> 0 Rezerv_78
     * [80:81] 28 00: 40 -> 40A Max Total Charge Current
     * [82:83] E6 00: 230 -> 230 V Rezerv_82
     * [84:85] 28 00: 40 -> 40A Max Utility Charge Current
     * [86:87] EA 01: 490 -> 49.0 V Rezerv_86
     *  [88:89] 1C 02: 540 -> 54.0 V Rezerv_88
     */

    /**
     * 32 ==> Type [03], len [32], payload [21 02 21 02 E0 01 21 02 05 00 84 03 5A 00 62 05 02 00 0C 00 00 00 14 00 00 00 00 00 05 00 01 00]
     * 32 ==> Type [03], len [32], payload [         05 00 01 00]
     * [00:01] 21 02: 545 -> 54.5 V (Напруга АКБ)
     * [02:03] 21 02: 545 -> 54.5 V (Напруга АКБ)
     *  [04:05] E0 01: 480 -> 48.0 V Rated Battery Voltage
     *  [06:07] 21 02: 545 -> 54.5 V (Напруга АКБ)
     *   [08:09] 05 00: 5 -> 5 V (Напруга АКБ)
     *   [10:11] 84 03: 900 -> 900 min (Battery Equalyze Timeout)
     *   [12:13] 5A 00: 90 -> 90 day (Battery Equalyze Intervsal)
     *   [14:15 62 05: 1378 -> Rezerv_14
     *   [16:17] 02 00: 2 -> Rezerv_16
     *   [18:19] 0C 00: 12 -> Rezerv_18
     *   [20:21`] 14 00: 16 -> Rezerv_20
     *   [22:23] 00 00: 0 -> Rezerv_22
     *   [24:25] 00 00: 0 -> Rezerv_24
     *   [26:27] 00 00: 0 -> Rezerv_26
     *   [28:29] 05 00: 0 -> Rezerv_28
     *   [30:31] 01 00: 1 -> Rezerv_30
     */
    private void processInverterGolego(byte[] packet) {
        int payloadLen = packet[2] & 0xFF;
        byte[] payload = Arrays.copyOfRange(packet, 3, 3 + payloadLen);
        String typeHex = String.format("%02X", packet[1]);
        log.info("Type [{}], len [{}], payload [{}]", typeHex, payloadLen, payloadToHexString(payload));
        InverterData inverterData = usrTcpWiFiBatteryRegistry.getInverter(usrTcpWiFiProperties.getPortInverterGolego());
        if (payloadLen == 90) {
            InvertorGolegoData90 entity90 = InvertorGolegoDecoders.decodeInverterGolegoPayload90(packet, usrTcpWiFiProperties.getPortInverterGolego());
            if (entity90 != null) {
                inverterData.inverterDataUpdate(entity90);
                log.info(entity90.toString());
            }
        } else if (payloadLen == 32) {
            InvertorGolegoData32 entity32 = InvertorGolegoDecoders.decodeInverterGolegoPayload32(packet, usrTcpWiFiProperties.getPortInverterGolego());
            if (entity32 != null) {
                inverterData.inverterDataUpdate(entity32);
                log.info(entity32.toString());
            }
        }
    }
}
