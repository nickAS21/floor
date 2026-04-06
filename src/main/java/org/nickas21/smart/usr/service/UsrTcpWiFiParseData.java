package org.nickas21.smart.usr.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nickas21.smart.data.dataEntityDto.DataErrorInfoDto;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.nickas21.smart.usr.data.InvertorGolegoDecoders;
import org.nickas21.smart.usr.data.UsrTcpWiFiDecoders;
import org.nickas21.smart.usr.data.UsrTcpWiFiMessageType;
import org.nickas21.smart.usr.data.UsrTcpWifiCrcUtilities;
import org.nickas21.smart.usr.entity.InverterDataGolego;
import org.nickas21.smart.usr.entity.InvertorGolegoData32;
import org.nickas21.smart.usr.entity.InvertorGolegoData90;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBattery;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBmsSummary;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiC1Data;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.ID_BMS_END;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.MIN_PACKET_BMS_USR_LENGTH;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.PACKET_DEYE_SERVICE_LENGTH;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.START_SIGN_01_03;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.START_SIGN_5E;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.START_SIGN_AA;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.T_C0;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.T_C1;
import static org.nickas21.smart.usr.data.UsrTcpWifiCrcUtilities.isValidInverterGolegoCrc;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds.AUTO_RECOVERABLE_MAX;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds.SERVICE_REQUIRED_MAX;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiFaultLogType.B1;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiFaultLogType.E1;
import static org.nickas21.smart.util.LocationType.GOLEGO;
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
    @Getter
    public final UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry;


    // --- Throttling for writing every 4 min ---
    private final Map<List<String>, String> lastErrorRecords = new ConcurrentHashMap<>();
    private long lastWriteTime = 0;

    public static final String datePattern = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(datePattern)
            .withZone(ZoneOffset.UTC); // або ZoneOffset.UTC, залежно від вашого parseDeyeRtcToMillis
//            .withZone(ZoneId.systemDefault()); // або ZoneOffset.UTC, залежно від вашого parseDeyeRtcToMillis

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
        if (port < (usrTcpWiFiProperties.getPortInverterGolego())) {
            //            log.warn("Start Golego acum decoder byteArray from port [{}]", port);
            return parseAndProcessBmsGolego(buffer, port);
        } else if (port == (usrTcpWiFiProperties.getPortInverterGolego())) {
//            log.warn("Start Golegos inv decoder byteArray from ports [{}]", port);
            return parseAndProcessInverterGolego(buffer);
        }
        else if (port == (usrTcpWiFiProperties.getPortInverterDacha())) {

            return parseAndProcessInverterDyey(buffer);
        }
        return new byte[0];
    }

    protected byte[] parseAndProcessBmsGolego(byte[] buffer, int port) {
        int currentIndex = 0;
        int endIndex = -1;
        List<byte[]> packets = new ArrayList<>();

        // 1. Пошук пакетів у буфері
        while (true) {
            // Шукаємо найближчий маркер AA AA або 5E 5E
            int idxAA = indexOf(buffer, START_SIGN_AA, currentIndex);
            int idx5E = indexOf(buffer, START_SIGN_5E, currentIndex);

            int startIndex;
            byte[] currentSign;

            // Визначаємо, який маркер зустрівся першим у потоці
            if (idxAA != -1 && (idx5E == -1 || idxAA < idx5E)) {
                startIndex = idxAA;
                currentSign = START_SIGN_AA;
            } else if (idx5E != -1) {
                startIndex = idx5E;
                currentSign = START_SIGN_5E;
            } else {
                break; // Маркерів більше немає
            }

            // Шукаємо кінець пакета за таким самим маркером
            int foundEnd = indexOf(buffer, currentSign, startIndex + currentSign.length);

            if (foundEnd == -1) {
                // Пакет неповний, зберігаємо залишок для наступного читання
                if (buffer.length - startIndex >= MIN_PACKET_BMS_USR_LENGTH) {
                    packets.add(Arrays.copyOfRange(buffer, startIndex, buffer.length));
                    currentIndex = buffer.length;
                }
                break;
            } else {
                // Знайдено повний пакет
                byte[] pkt = Arrays.copyOfRange(buffer, startIndex, foundEnd);
                if (pkt.length >= MIN_PACKET_BMS_USR_LENGTH) {
                    packets.add(pkt);
                }
                currentIndex = foundEnd;
                endIndex = foundEnd;
            }
        }

        // 2. Обробка знайдених пакетів
        for (byte[] packet : packets) {
            try {
                // Отримуємо тип повідомлення з 3-го байта
                UsrTcpWiFiMessageType msgType = UsrTcpWiFiMessageType.fromByte(packet[2]);

                // ПРАВИЛО: Група 1 — ігноруємо повністю без перевірок
                if (msgType.getGroupBms() == 1) {
                    continue;
                }

                // Для груп 2, 3, 4, 5 — виконуємо валідацію (CRC, сигнатура тощо)
                byte[] sign = Arrays.copyOfRange(packet, 0, 2);
                if (!validationPacket(packet, sign, msgType)) {
                    log.warn("Port [{}]: Validation failed for type [{}]", port, msgType.name());
                    continue;
                }

                // Обробка за алгоритмом залежно від групи
                if (msgType.getGroupBms() >= 2 && msgType.getGroupBms() <= 5) {
                    if (T_C0.equals(msgType) || T_C1.equals(msgType)) {
                        decodeTypeC0_C1(msgType, packet, port);
                    } else {
                        // Групи 2 та 5 (T_00, T_02, T_05 тощо), які пройшли валідацію
                        log.info("Port [{}]: Received service packet Type: [{}],\n Hex: [{}]",
                                port, msgType.name(), bytesToHex(packet));
                    }
                }

            } catch (Exception e) {
                log.error("Error processing packet at port {}: {}", port, e.getMessage());
            }
        }

        // 3. Управління буфером: повертаємо необроблені дані
        if (packets.isEmpty()) return buffer;
        if (endIndex == -1) return buffer;
        if (currentIndex >= buffer.length) return new byte[0];

        return Arrays.copyOfRange(buffer, currentIndex, buffer.length);
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

    private void decodeTypeC0_C1(UsrTcpWiFiMessageType msgType, byte[] packet, int port) throws IOException {
        String typeFrameName = msgType.name();

        byte[] payloadWithCrc = Arrays.copyOfRange(packet, ID_BMS_END, packet.length);
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
        if (T_C0.equals(msgType)) {
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
                String newValue = c1Data.getBalanceS().name() + ": " + intToHex(c1Data.getErrorInfoData());
                String oldValue = lastErrorRecords.get(key);
                if (!newValue.equals(oldValue)) {
                    this.usrTcpWiFiBatteryRegistry.getBattery(port).setErrRecordB1(c1Data.getErrorUnbalanceForRecords(port));
                    lastErrorRecords.compute(key, (k, v) -> newValue);
                    DataErrorInfoDto dataErrorInfoDto = new DataErrorInfoDto(this.usrTcpWiFiBatteryRegistry.getBattery(port).getErrRecordB1());
                    logWriter.writeError(GOLEGO, dataErrorInfoDto);
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
                String newValue = intToHex(errorInfoData);
                String oldValue = lastErrorRecords.get(key);
                if (!newValue.equals(oldValue)) {
                    this.usrTcpWiFiBatteryRegistry.getBattery(port).setErrRecordE1(c1Data.getErrorOutputForRecords(port));
                    lastErrorRecords.put(key, newValue);
                    DataErrorInfoDto dataErrorInfoDto = new DataErrorInfoDto(this.usrTcpWiFiBatteryRegistry.getBattery(port).getErrRecordE1());
                    logWriter.writeError(GOLEGO, dataErrorInfoDto);
                }
            } else {
                this.usrTcpWiFiBatteryRegistry.getBattery(port).setErrRecordE1(null);
                lastErrorRecords.compute(key, (k, v) -> null);
            }
        }

        synchronized (this) {
            long now = System.currentTimeMillis();
            if (now - lastWriteTime >= this.usrTcpLogsWiFiProperties.getActiveInterval()) {
                lastWriteTime = now;
            }
        }
    }

    private boolean validationPacket(byte[] packet, byte[] startSign, UsrTcpWiFiMessageType msgType) {
        if (msgType.getGroupBms() == 1){    // not parse
            return false;
        }
        if (packet.length < MIN_PACKET_BMS_USR_LENGTH) {
            log.warn("DECODER ERROR: Packet too short ({} bytes)", packet.length);
            return false;
        }
        // TODO double
        if (packet[0] != startSign[0] || packet[1] != startSign[1]) {
            // Тут ми бачимо, що дані прийшли, але вони не BMS Golego
            log.error("DECODER ERROR: Invalid signature 0x{} 0x{} on port. Check device protocol!",
                    String.format("%02X", packet[0]), String.format("%02X", packet[1]));
            return false;
        }
        if (UsrTcpWiFiMessageType.UNKNOWN == msgType) {
            log.error("UNKNOWN message type: 0x{}", String.format("%02X", packet[2]));
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
    public UsrTcpWiFiBattery getInverter(int port){
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
                    log.error("Error decoding InverterInfo packet at port {}: {}", usrTcpWiFiProperties.getPortInverterGolego(), e.getMessage());
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
        InverterDataGolego inverterData = usrTcpWiFiBatteryRegistry.getInverter(usrTcpWiFiProperties.getPortInverterGolego());
        if (payloadLen == 90) {
            InvertorGolegoData90 entity90 = InvertorGolegoDecoders.decodeInverterGolegoPayload90(packet, usrTcpWiFiProperties.getPortInverterGolego());
            if (entity90 != null) {
                inverterData.inverterDataUpdate(entity90);
            }
        } else if (payloadLen == 32) {
            InvertorGolegoData32 entity32 = InvertorGolegoDecoders.decodeInverterGolegoPayload32(packet, usrTcpWiFiProperties.getPortInverterGolego());
            if (entity32 != null) {
                inverterData.inverterDataUpdate(entity32);
            }
        }
    }

    /**
     * BitRate - 9600
     * Логіка для Deye (Modbus RTU):
     * 1. Шукаємо заголовок 01 03.
     * 2. Читаємо третій байт (dataLength).
     * 3. Вираховуємо повну довжину пакета: 3 (header) + dataLength + 2 (CRC).
     * 4. Перевіряємо цілісність через CRC16 Modbus.
     */
    public byte[] parseAndProcessInverterDyey(byte[] buffer) {
        int currentIndex = 0;
        int lastProcessedIndex = 0; // Краще використовувати цей індекс для залишку

        while (true) {
            int startIndex = indexOf(buffer, START_SIGN_01_03, currentIndex);

            // Якщо маркер не знайдено або недостатньо байтів для читання довжини
            if (startIndex == -1 || startIndex + 2 >= buffer.length) break;

            int dataLength = buffer[startIndex + 2] & 0xFF;

            // САНИТАРНА ПЕРЕВІРКА: щоб не чекати "нескінченний" пакет
            if (dataLength > 200) {
                currentIndex = startIndex + 1;
                continue;
            }

            int expectedFullLength = dataLength + PACKET_DEYE_SERVICE_LENGTH;

            // ПЕРЕВІРКА: Чи весь пакет вже в буфері?
            if (startIndex + expectedFullLength <= buffer.length) {
                try {
                    byte[] payload = new byte[dataLength];
                    System.arraycopy(buffer, startIndex + 3, payload, 0, dataLength);
                    processInverterDeye(payload);
                    // Зсуваємось за межі обробленого пакета
                    currentIndex = startIndex + expectedFullLength;
                    lastProcessedIndex = currentIndex;

                } catch (Exception e) {
                    log.error("Error processing Deye payload: " + e.getMessage());
                    // Якщо всередині process сталася біда — пробуємо шукати далі з наступного байта
                    currentIndex = startIndex + 1;
                }
            } else {
                // Пакет знайдено, але він ще не повний. Виходимо, щоб дочекатися решти.
                break;
            }
        }

        // Якщо нічого не обробили, повертаємо весь буфер
        if (lastProcessedIndex == 0 && currentIndex == 0) return buffer;

        // Якщо обробили все під нуль
        if (lastProcessedIndex >= buffer.length) return new byte[0];

        // Повертаємо залишок (хвіст останнього незавершеного пакета)
        return Arrays.copyOfRange(buffer, lastProcessedIndex, buffer.length);
    }
//    public static boolean isFrameDyeyCrcValid(byte[] buffer, int startIndex, int fullLength) {
//        // Довжина даних для розрахунку (весь пакет мінус 2 останні байти CRC)
//        int dataLengthForCrc = fullLength - 2;
//
//        // Зчитуємо те, що прийшло в пакеті (Little-endian: спочатку Low, потім High)
//        int low = buffer[startIndex + dataLengthForCrc] & 0xFF;
//        int high = buffer[startIndex + dataLengthForCrc + 1] & 0xFF;
//        int expectedCrc = (high << 8) | low;
//
//        // Рахуємо актуальний CRC від першого байта пакета
//        int actualCrc = calculateCrc16Modbus(buffer, startIndex, dataLengthForCrc);
//
//        if (actualCrc != expectedCrc) {
//            throw new RuntimeException(
//                    "CRC mismatch: expected " + String.format("%04X", expectedCrc) +
//                            " but got " + String.format("%04X", actualCrc)
//            );
//        }
//        return true;
//    }

    public static boolean isFrameDyeyCrcValid(byte[] buffer, int startIndex, int fullLength) {
        int dataLengthForCrc = fullLength - 2;

        // Створюємо чистий масив тільки з тих байтів, що мають йти в розрахунок
        byte[] dataToCalculate = new byte[dataLengthForCrc];
        System.arraycopy(buffer, startIndex, dataToCalculate, 0, dataLengthForCrc);

        // Рахуємо від 0 до кінця цього нового масиву
        int actualCrc = calculateCrc16Modbus(dataToCalculate, 0, dataLengthForCrc, dataLengthForCrc);

        // Зчитуємо CRC з ОРИГІНАЛЬНОГО буфера
        int low = buffer[startIndex + dataLengthForCrc] & 0xFF;
        int high = buffer[startIndex + dataLengthForCrc + 1] & 0xFF;
        int expectedCrc = (high << 8) | low;

        if (actualCrc != expectedCrc) {
            // Виводимо в HEX, що ми реально намагалися порахувати
            String hexParsed = org.nickas21.smart.util.StringUtils.bytesToHex(dataToCalculate);
            throw new RuntimeException(String.format(
                    "CRC mismatch! Expected: %04X, Got: %04X. Data used: %s",
                    expectedCrc, actualCrc, hexParsed));
        }
        return true;
    }

//    public static int calculateCrc16Modbus(byte[] buffer, int offset, int length) {
//        int crc = 0xFFFF;
//
//        for (int i = offset; i < offset + length; i++) {
//            // КРИТИЧНО: & 0xFF перетворює знаковий byte на беззнаковий int
//            crc ^= (buffer[i] & 0xFF);
//
//            for (int j = 0; j < 8; j++) {
//                if ((crc & 0x0001) != 0) {
//                    // Використовуємо >>> (логічний зсув)
//                    crc = (crc >>> 1) ^ 0xA001;
//                } else {
//                    crc >>>= 1;
//                }
//            }
//        }
//        return crc & 0xFFFF;
//    }

    public static int calculateCrc16Modbus(byte[] buffer, int offset, int length, int expectedFinalCrc) {
        int crc = 0xFFFF;

        for (int i = offset; i < offset + length; i++) {
            int currentByte = buffer[i] & 0xFF;
            crc ^= currentByte;

            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc >>>= 1;
                }
                crc &= 0xFFFF; // Тримаємо в межах 16 біт
            }

            // ПЕРЕВІРКА: чи не став наш проміжний CRC рівним очікуваному?
            // (Хоча очікуваний зазвичай збігається лише в кінці, цей лог покаже динаміку)
            if (i > offset + length - 5) { // Дивимось останні 5 кроків
                System.out.printf("Step %d | Byte: %02X | Current CRC: %04X%n", i, currentByte, crc);
            }
        }
        return crc;
    }

    private void processInverterDeye(byte[] packet) {
        // 1. Пропускаємо пакети, заповнені однаковими 00 або FF
        int payloadLen = packet.length;
        if (payloadLen <=2 || isGarbagePacket(packet) || payloadLen == 48) {
            return;
        }
        InverterDataGolego inverterData = this.usrTcpWiFiBatteryRegistry.getInverter(this.usrTcpWiFiProperties.getPortInverterDacha());
        log.info("Deye: len: [{}] hex: [{}]", payloadLen, bytesToHex(packet));
        if (payloadLen == 6) {
            long timeMillis = parseDeyeRtcToMillis(packet);
            String formattedTime = DATE_FORMATTER.format(Instant.ofEpochMilli(timeMillis));
            log.info("Deye RTC time: [{}] hex: [{}]", formattedTime, bytesToHex(packet));
        } else if (payloadLen == 8) {
            parseDeyeHomeBlock8(packet, inverterData);
        } else if (payloadLen == 16) {
            parseDeyeBmsBlock16(packet, inverterData);
        } else if (payloadLen == 80) {
            parseDeyeInverterOutDcBlock80(packet, inverterData);
        } else if (payloadLen == 106) {
            parseDeyBatteryBlock106(packet, inverterData);
        }
//        else if (payloadLen == 118) {
//            parseDeyeTilyBlock118(packet, inverterData);
//        }
        else  {
            parseDeyeBlock_NN(packet, inverterData);
        }
    }

    private long parseDeyeRtcToMillis(byte[] packet) {
        try {
            // Отримуємо значення з байтів
            int year   = (packet[0] & 0xFF) + 2000;
            int month  = (packet[1] & 0xFF);
            int day    = (packet[2] & 0xFF);
            int hour   = (packet[3] & 0xFF);
            int minute = (packet[4] & 0xFF);
            int second = (packet[5] & 0xFF);

            // Валідація базових меж (щоб не отримати помилку при створенні дати)
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return 0L;
            }

            // Створюємо об'єкт дати та часу
            LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, second);

            // Перетворюємо в мілісекунди (UTC або системний часовий пояс)
            return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();

        } catch (Exception e) {
            log.error("Failed to parse RTC bytes to millis: " + e.getMessage());
            return 0L;
        }
    }

    private boolean isGarbagePacket(byte[] packet) {
        for (byte b : packet) {
            // Якщо знайшли хоча б один байт, який НЕ 0x00 і НЕ 0xFF
            if (b != 0x00 && b != (byte) 0xFF) {
                return false; // Пакет містить корисні дані
            }
        }
        // Якщо цикл пройшов до кінця, значить у пакеті лише 00 та FF
        return true;
    }

    private int getUint16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private short getSignedShort(byte[] data, int offset) {
        return (short) (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF));
    }

    /**
     * 2026-04-04T02:18:40.795+03:00  INFO 1 --- [p-listener-8900] o.n.s.usr.service.UsrTcpWiFiParseData    : Deye: len: [16] hex: [16440000019A019A003A147DFFFB048F]
     * 2026-04-04T02:18:40.795+03:00  INFO 1 --- [p-listener-8900] o.n.s.usr.service.UsrTcpWiFiParseData    : Deye RAW BLOCK_16:
     *           1644          0000          019A          019A          003A          147D          FFFB          048F
     *     [000]:5700  | [002]:0     | [004]:410   | [006]:410   | [008]:58    | [010]:5245  | [012]:65531 | [014]:1167  |
     *    BMS Charge			Charge		Discharge) SOC %	 BMS     	BMS
     *      Voltage(V)		        Current Limit(A) Current Limit(A	 Voltage(V) 	Current(A) - *1
     *      									*0.01
     * @param data
     * @param inverterData
     */

    /**
     * Парсинг великого блоку даних (106 байт)
     * BMS
     */
    private void parseDeyeHomeBlock8(byte[] data, InverterDataGolego inverterData) {
        StringBuilder sb = new StringBuilder();

        // Масив назв відповідно до офсетів 0, 2, 4, 6, 8, 10, 12, 14
        String[] labels = {
                "Load  Power L1(W)",            // 000
                "Load  Power L2(W)",            // 002
                "Load  Power L3(W)",            // 004
                "Total Consumption Power(W)",   // 006
        };

        for (int i = 0; i < data.length; i += 2) {
            short val = getSignedShort(data, i);
            String hex = String.format("0x%02X%02X", data[i], data[i+1]);
            String label = labels[i / 2]; // Отримуємо назву за індексом регістру

            String dec = String.valueOf(val); // SOC та ліміти

            // Формат: [Назва][Індекс]:HEX:DEC
            sb.append(String.format("[%s][%03d]:%s:%-7s | ", label, i, hex, dec));

            // Перенос кожні 2 регістри (так краще влізе в лог з довгими назвами)
            if ((i + 2) % 4 == 0) sb.append("\n    ");
        }

        log.info("\nDeye Home BLOCK_8:\n    {}", sb.toString());

        // Оновлення об'єкта
        if (inverterData != null) {

        }
    }


    /**
     * Парсинг великого блоку даних (106 байт)
     * BMS
     */
    private void parseDeyeBmsBlock16(byte[] data, InverterDataGolego inverterData) {
        StringBuilder sb = new StringBuilder();

        // Масив назв відповідно до офсетів 0, 2, 4, 6, 8, 10, 12, 14
        String[] labels = {
                "ChargeVoltage(V)",      // 000
                "BMS Discharge Voltage(V)",       // 002
                "ChargeCurrent Limit(A)",// 004
                "DischargeCurrent Limit(A)", // 006
                "SOC(%)",                // 008
                "Voltage(V)",            // 010
                "Current(A)",            // 012
                "Temperature"            // 014
        };

        for (int i = 0; i < data.length; i += 2) {
            short val = getSignedShort(data, i);
            String hex = String.format("0x%02X%02X", data[i], data[i+1]);
            String label = labels[i / 2]; // Отримуємо назву за індексом регістру

            String dec;
            if (i == 12)      dec = String.valueOf(val); // Струм як є
            else if (i == 14) dec = String.format("%.1f", (val - 1000) * 0.1); // Температура
            else if (i == 0 || i == 2 || i == 10) dec = String.format("%.2f", val * 0.01); // Напруги
            else              dec = String.valueOf(val); // SOC та ліміти

            // Формат: [Назва][Індекс]:HEX:DEC
            sb.append(String.format("[%s][%03d]:%s:%-7s | ", label, i, hex, dec));

            // Перенос кожні 2 регістри (так краще влізе в лог з довгими назвами)
            if ((i + 2) % 4 == 0) sb.append("\n    ");
        }

        log.info("\nDeye BMS BLOCK_16:\n    {}", sb.toString());

        // Оновлення об'єкта
        if (inverterData != null) {

        }
    }

    private void parseDeyeInverterOutDcBlock80(byte[] data, InverterDataGolego inverterData) {
        StringBuilder sb = new StringBuilder();
        int totalDcPower = 0;

        // Ініціалізуємо масив на 40 елементів (80 байт / 2)
        String[] labels = new String[40];
        for (int j = 0; j < labels.length; j++) {
            labels[j] = String.format("Nothing_%03d", j * 2);
        }

        // Заповнюємо розпізнані назви
        labels[0] = "Load Voltage L1(V)";      // 000
        labels[1] = "Load Voltage L2(V)";      // 002
        labels[2] = "Load Voltage L3(V)";      // 004
        // labels[3-5] - Nothing_006...Nothing_010
        labels[6] = "Load Power L1(W)";        // 012
        labels[7] = "Load Power L2(W)";        // 014
        labels[8] = "Load Power L3(W)";        // 016
        labels[9] = "Total Consumption Power(W)"; // 018
        labels[10] = "Total Consumption Apparent Power(VA)"; // 020
        labels[11] = "Load Frequency(Hz)";     // 022

        // ... проміжні Nothing ...

        labels[28] = "DC Power PV1(W)";        // 056
        labels[29] = "DC Power PV2(W)";        // 058
        labels[30] = "DC Power PV3(W)";        // 060
        labels[31] = "DC Power PV4(W)";        // 062

        for (int i = 0; i < data.length; i += 2) {
            short val = getSignedShort(data, i);
            String hex = String.format("0x%02X%02X", data[i], data[i+1]);
            String label = labels[i / 2];

            String dec;
            // Специфічна логіка множників по офсету
            if (i <= 4) {
                dec = String.format("%.1f", val * 0.1); // Напруги L1-L3
            } else if (i == 22) {
                dec = String.format("%.2f", val * 0.01); // Частота Hz
            } else if (i >= 56 && i <= 62) {
                dec = String.valueOf(val);
                totalDcPower += val; // Рахуємо суму PV
            } else {
                dec = String.valueOf(val); // Потужності та інше
            }

            // Додаємо в StringBuilder
            sb.append(String.format("[%s][%03d]:%s:%-7s | ", label, i, hex, dec));

            // Перенос рядка кожні 2 елемента (після 0, 4, 8 і т.д. байт)
            if ((i + 2) % 4 == 0) {
                sb.append("\n    ");
            }
        }

        sb.append(String.format("\n    [Total DC Power Sum PV][---]:SUM:%-7d | ", totalDcPower));

        log.info("\nDeye RAW BLOCK_InverterOutDc_80:\n    {}", sb.toString());
    }

    private void parseDeyBatteryBlock106(byte[] data, InverterDataGolego inverterData) {
        StringBuilder sb = new StringBuilder();

        // 1. Універсальний масив (можна зробити 60, як обговорювали)
        String[] labels = new String[60];
        for (int j = 0; j < labels.length; j++) {
            labels[j] = String.format("Nothing_%03d", j * 2);
        }

        // 2. Заповнення імен
        labels[0] = "Temperature- Battery(℃)";
        labels[1] = "Battery Voltage(V)";
        labels[2] = "SoC(%)";
        labels[4] = "Battery Power(W)";
        labels[5] = "Battery current 1(A)";
        // ... (інші лейбли без змін)
        labels[52] = "AC Output Frequency R(Hz)";

        // 3. Цикл
        for (int i = 0; i < data.length; i += 2) {
            short val = getSignedShort(data, i);
            String hex = String.format("0x%02X%02X", data[i], data[i+1]);
            String label = labels[i / 2];

            String dec;

            // ГРУПУЄМО ЛОГІКУ ОБРОБКИ
            if (i == 0) {
                // Специфічний розрахунок для температури
                dec = String.format("%.1f", (val - 1000) * 0.1);
            } else if (i == 82 || i == 84 || i == 86) {
                // Крок 0.1 (Напруги AC)
                dec = String.format("%.1f", val * 0.1);
            } else if (i == 2 || i == 10 || (i >= 88 && i <= 92) || i == 104) {
                // Крок 0.01 (Напруга батареї, струми AC, частота)
                dec = String.format("%.2f", val * 0.01);
            } else {
                // Цілі числа (SoC, Power)
                dec = String.valueOf(val);
            }

            sb.append(String.format("[%-35s][%03d]:%s:%-7s | ", label, i, hex, dec));
            if ((i + 2) % 4 == 0) sb.append("\n    ");
        }

        log.info("\nDeye RAW BLOCK_106:\n    {}", sb.toString());
    }

    private void parseDeyeBlock_NN(byte[] packet, InverterDataGolego inverterData) {
        int payloadLen = packet.length;
        StringBuilder sb = new StringBuilder();

        // 2. Проходимо по всьому масиву з кроком 2 (Modbus-регістри)
        for (int i = 0; i < payloadLen; i += 2) {
            if (i + 1 < payloadLen) {
                // Отримуємо значення байтів для HEX представлення
                int b1 = packet[i] & 0xFF;
                int b2 = packet[i + 1] & 0xFF;

                // Розраховуємо 16-бітне значення (Big-Endian)
                int regValue = (b1 << 8) | b2;

                // Форматування: [Індекс]:0xHEX:DEC
                // %02X%02X - виводить два байти в HEX (наприклад 1644)
                // %-5d - десяткове число з вирівнюванням
                sb.append(String.format("[%03d]:0x%02X%02X:%-5d | ", i, b1, b2, regValue));

                // Перенос рядка кожні 6 регістрів (для кращої читабельності при довшому форматі)
                if ((i + 2) % 12 == 0) {
                    sb.append("\n    ");
                }
            }
        }

        // 3. Вивід у лог
        log.info("Deye RAW BLOCK_{}:\n    {}", payloadLen, sb.toString());
    }
}
