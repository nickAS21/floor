package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.data.UsrWifiBmsBatteryStatus;
import org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.keyIdx;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.keyVoltage;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiBalanceThresholds.getBalanceStatus;
import static org.nickas21.smart.util.JacksonUtil.newObjectNode;
import static org.nickas21.smart.util.StringUtils.isBlank;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiRS485_Data_GOOTO_Telemetry {

    public static final int CMD_VER = 0x20;  // VER = 20
    public static final int CMD_CID1 = 0x46; // CID1 = 46 (Lithium Battery)
    public static final int CMD_CID2_42 = 0x42;
    public static final int CMD_CID2_44 = 0x44;
    public static final int CMD_ADR_01 = 0x01;
    public static final String CMD_INFO_HEX = "";
    public static final int PAYLOAD_START = 13;

    // Поля телеметрії 0x42H
    private Integer numCells;
    private Map<Integer, Float> cellVoltagesV = new HashMap<>();
    private Float sumCellsV;
    private int deltaMv;
    private JsonNode minCellV;
    private JsonNode maxCellV;
    private Integer minCellIdx;
    private Integer maxCellIdx;
    private UsrTcpWifiBalanceThresholds balanceS;
    private Map<Integer, Float> temperatures = new HashMap<>();
    private Double bmsTempValue;

    private double socPercent;       // Розрахований SOC (%)
    private double sohPercent = 100.0;       // SOH (%)

    private double currentCurA;      // Струм (А)
    private double voltageCurV;      // Загальна напруга (В)
    private double remainAh;         // Залишкова ємність (Ah)
    private double fullAh;           // Повна актуальна ємність (Ah)
    private double designAh = 300.0;         // Базова (паспортна) ємність (Ah)
    private Integer cyclesCount;     // Цикли

    // Статуси BMS
    private int bmsStatus = UsrWifiBmsBatteryStatus.UNKNOWN.getCode();
    private String bmsStatusStr = UsrWifiBmsBatteryStatus.UNKNOWN.getStatus();

    // --- Поля помилок та алармів ---
    private String errorInfoDataHex;   // Код помилки: "0x0000" якщо все чисто, або код аларму (наприклад, "0x0001")
    private String errorOutput;      // Текстовий опис помилок/алармів

    private Instant timestamp;
    private byte[] payloadBytesCur;
    private byte[] payloadBytesLastSaved;

    // --- Поля алармів 0x44 ---
    private List<Integer> cellAlarms = new ArrayList<>();   // Аларми по кожній комірці (0 - OK, >0 - Аларм/Захист)
    private List<Integer> tempAlarms = new ArrayList<>();   // Аларми по кожній температурі

    private int chargeCurrentAlarm = 0;                     // Аларм струму заряду
    private int totalVoltageAlarm = 0;                      // Аларм загальної напруги
    private int dischargeCurrentAlarm = 0;                  // Аларм струму розряду

    private boolean chargeMosfetEnabled = true;             // Стан мосфета заряду (true - відкритий)
    private boolean dischargeMosfetEnabled = true;          // Стан мосфета розряду (true - відкритий)
    private int systemStatusFlags = 0;                      // Системні прапори захисту

    /**
     * Parses raw Pylontech RS485 0x42 ASCII response frame directly into entity.
     */
    public boolean parse42(String asciiFrame42) {
        try {
            String dataFrame = validateFindFrame(asciiFrame42);
            if (dataFrame == null) {
                // TODO onle debug
                log.debug("RS485 0x42 BAD valid response frame not found in: {}", asciiFrame42.split("(?=~)"));
                return false;
            }

            // 1. Parse LENGTH
            String lenHex = dataFrame.substring(9, 13);
            int infoCharCount = Integer.parseInt(lenHex, 16) & 0x0FFF;
            String payload = dataFrame.substring(PAYLOAD_START, PAYLOAD_START + infoCharCount);

            // 2. Cell Voltages
            if (payload.length() < 6) return false;
            this.numCells = Integer.parseInt(payload.substring(4, 6), 16);

            int cellsStart = 6;
            this.cellVoltagesV.clear();
            for (int i = 0; i < this.numCells; i++) {
                int pos = cellsStart + i * 4;
                if (pos + 4 > payload.length()) break;
                int mV = Integer.parseInt(payload.substring(pos, pos + 4), 16);
                this.cellVoltagesV.put(i + 1, mV / 1000.0f);
            }

            // 3. Temperature Sensors
            int tempsCountPos = cellsStart + this.numCells * 4;
            this.temperatures.clear();
            if (tempsCountPos + 2 <= payload.length()) {
                int numTemps = Integer.parseInt(payload.substring(tempsCountPos, tempsCountPos + 2), 16);
                int tempsStart = tempsCountPos + 2;
                for (int i = 0; i < numTemps; i++) {
                    int pos = tempsStart + i * 4;
                    if (pos + 4 > payload.length()) break;

                    int deciKelvin = Integer.parseInt(payload.substring(pos, pos + 4), 16);
                    float celsius = Math.round(deciKelvin - 2731) / 10.0f;
                    this.temperatures.put(i + 1, celsius);
                }
                if (this.temperatures != null && !this.temperatures.isEmpty()) {
                    Float temp1 = this.temperatures.get(1);
                    this.bmsTempValue = (temp1 != null) ? temp1.doubleValue() : 0;
                }

                // 4. Analog Tail (Current, Total Voltage, Capacities, Cycles)
                int p = tempsStart + numTemps * 4;

                // Current (Signed 16-bit Int)
                if (p + 4 <= payload.length()) {
                    int rawCurrent = Integer.parseInt(payload.substring(p, p + 4), 16);
                    if (rawCurrent > 0x7FFF) {
                        rawCurrent -= 0x10000;
                    }
                    this.currentCurA = rawCurrent / 10.0;

                    int statusCode;
                    if (this.currentCurA > 0.1) {
                        statusCode = UsrWifiBmsBatteryStatus.CHARGING.getCode();
                    } else if (this.currentCurA < -0.1) {
                        statusCode = UsrWifiBmsBatteryStatus.DISCHARGING.getCode();
                    } else {
                        statusCode = UsrWifiBmsBatteryStatus.STATIC.getCode();
                    }

                    this.bmsStatus = statusCode;
                    this.bmsStatusStr = UsrWifiBmsBatteryStatus.fromCode(statusCode).getStatus();
                }

                // Total Pack Voltage
                if (p + 8 <= payload.length()) {
                    this.voltageCurV = Integer.parseInt(payload.substring(p + 4, p + 8), 16) / 1000.0;
                }

                // Remaining Capacity (Ah)
                if (p + 12 <= payload.length()) {
                    this.remainAh = Integer.parseInt(payload.substring(p + 8, p + 12), 16) / 100.0;
                }

                // Full Capacity (Ah)
                if (p + 18 <= payload.length()) {
                    String rawFullHex = payload.substring(p + 14, p + 18);
                    this.fullAh = "0000".equals(rawFullHex)
                            ? 300.0
                            : Integer.parseInt(rawFullHex, 16) / 100.0;
                } else {
                    this.fullAh = 300.0;
                }

                // Cycle Count
                if (p + 22 <= payload.length()) {
                    String rawCycHex = payload.substring(p + 18, p + 22);
                    this.cyclesCount = Integer.parseInt(rawCycHex, 16);
                }

                // SOC Calculation
                if (this.fullAh > 0 && this.remainAh >= 0) {
                    double rawSoc = Math.clamp((this.remainAh / this.fullAh) * 100.0, 0.0, 100.0);
                    this.socPercent = Math.round(rawSoc * 100.0) / 100.0;
                }

                // SOH Calculation
                if (this.designAh > 0 && this.fullAh >= 0) {
                    double rawSoh = Math.clamp((this.fullAh / this.designAh) * 100.0, 0.0, 100.0);
                    this.sohPercent = Math.round(rawSoh * 100.0) / 100.0;
                }
            }

            this.sumCellsV = computeSumCellsV();
            this.minCellV = computeMinCell();
            this.maxCellV = computeMaxCell();
            this.deltaMv = computeDeltaMv();
            this.balanceS = computeBalanceStatus();

            this.timestamp = Instant.now();
            this.payloadBytesCur = dataFrame.getBytes(StandardCharsets.US_ASCII);

            return true;

        } catch (Exception e) {
            log.error("Failed to parse42 RS485 0x42 frame: {}", asciiFrame42, e);
            return false;
        }
    }

    public boolean parse44(String asciiFrame44) {
        try {
            String dataFrame = validateFindFrame(asciiFrame44);
            if (dataFrame == null) {
                log.warn("RS485 0x44 NORMAL response frame not found in: {}", asciiFrame44);
                return false;
            }

            // 1. Вирізаємо INFO Payload
            String lenHex = dataFrame.substring(9, 13);
            int infoCharCount = Integer.parseInt(lenHex, 16) & 0x0FFF;
            String payload = dataFrame.substring(PAYLOAD_START, PAYLOAD_START + infoCharCount);
            if (payload.length() < 6) return false;

            // -----------------------------------------------------------------
            // Побайтний розбір payload (1 байт = 2 HEX символи)
            // -----------------------------------------------------------------

            // Байти 0..1: Flag & Pack Index (наприклад, "0001")
            int pos = 4;

            // Байт 2: Кількість комірок (наприклад, "10" -> 16)
            int numCells44 = Integer.parseInt(payload.substring(pos, pos + 2), 16);
            pos += 2;

            // Байти 3 .. 3 + numCells44: Аларми комірок (16 байт)
            this.cellAlarms = new ArrayList<>();
            for (int i = 0; i < numCells44; i++) {
                if (pos + 2 > payload.length()) break;
                int alarmCode = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                this.cellAlarms.add(alarmCode);

                pos += 2;
            }

            // Байт 19: Кількість датчиків температури (наприклад, "04" -> 4)
            if (pos + 2 <= payload.length()) {
                int numTemps44 = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;

                // Байти 20..23: Аларми датчиків температури (4 байти)
                this.tempAlarms = new ArrayList<>();
                for (int i = 0; i < numTemps44; i++) {
                    if (pos + 2 > payload.length()) break;
                    int alarmCode = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                    // TODO: Verify GOTO RS485 0x44 temperature alarm code 0x02.
                    // Currently 0x02 appears on Temp #2 without an actual temperature alarm.
                    if (alarmCode != 0x02) {
                        this.tempAlarms.add(alarmCode);
                    }
                    pos += 2;
                }
            }

            // Байт 24: Загальний аларм струму заряду
            if (pos + 2 <= payload.length()) {
                this.chargeCurrentAlarm = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }
            // Байт 25: Загальний аларм напруги
            if (pos + 2 <= payload.length()) {
                this.totalVoltageAlarm = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }
            // Байт 26: Загальний аларм струму розряду
            if (pos + 2 <= payload.length()) {
                this.dischargeCurrentAlarm = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }

            // Байт 27 (Резервний) - пропускаємо
            if (pos + 2 <= payload.length()) {
                pos += 2;
            }

            // Байт 28: Системні прапори роботи BMS (наприклад, 0x0E)
            if (pos + 2 <= payload.length()) {
                this.systemStatusFlags = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }

            // Байт 29: Ключі MOSFET (0x00 = Charge & Discharge Enabled)
            if (pos + 2 <= payload.length()) {
                int mosfetFlags = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                this.chargeMosfetEnabled = (mosfetFlags & 0x01) == 0;
                this.dischargeMosfetEnabled = (mosfetFlags & 0x02) == 0;
            }

            // -----------------------------------------------------------------
            // Формування висновку: шукаємо ТІЛЬКИ байти алармів, які > 0
            // -----------------------------------------------------------------
            List<String> activeAlarms = new ArrayList<>();

            if (this.cellAlarms != null) {
                for (int i = 0; i < this.cellAlarms.size(); i++) {
                    int code = this.cellAlarms.get(i);
                    if (code > 0) {
                        activeAlarms.add(String.format("Cell %d Alarm (0x%02X)", i + 1, code));
                    }
                }
            }

            if (this.tempAlarms != null) {
                for (int i = 0; i < this.tempAlarms.size(); i++) {
                    int code = this.tempAlarms.get(i);
                    if (code > 0) {
                        activeAlarms.add(String.format("Temp %d Alarm (0x%02X)", i + 1, code));
                    }
                }
            }

            if (this.chargeCurrentAlarm > 0) {
                activeAlarms.add(String.format("Charge Overcurrent (0x%02X)", this.chargeCurrentAlarm));
            }
            if (this.totalVoltageAlarm > 0) {
                activeAlarms.add(String.format("Total Voltage Alarm (0x%02X)", this.totalVoltageAlarm));
            }
            if (this.dischargeCurrentAlarm > 0) {
                activeAlarms.add(String.format("Discharge Overcurrent (0x%02X)", this.dischargeCurrentAlarm));
            }

                // 7. Фінальне встановлення статусу помилок
            if (activeAlarms.isEmpty()) {
                this.errorInfoDataHex = "0x0000";
                this.errorOutput = null;
            } else {
                // Якщо аларми ДІЙСНО є в розпарсеному кадрі:
                int firstAlarmCode = 0;

                for (int code : this.cellAlarms) {
                    if (code > 0) { firstAlarmCode = code; break; }
                }
                if (firstAlarmCode == 0) {
                    for (int code : this.tempAlarms) {
                        if (code > 0) { firstAlarmCode = code; break; }
                    }
                }
                if (firstAlarmCode == 0) {
                    if (this.chargeCurrentAlarm > 0) firstAlarmCode = this.chargeCurrentAlarm;
                    else if (this.totalVoltageAlarm > 0) firstAlarmCode = this.totalVoltageAlarm;
                    else if (this.dischargeCurrentAlarm > 0) firstAlarmCode = this.dischargeCurrentAlarm;
                }

                // Якщо з якихось причин код 0, виводимо 0x0000, а не видумуємо 1 чи 2!
                this.errorInfoDataHex = (firstAlarmCode > 0) ? String.format("0x%04X", firstAlarmCode) : "0x0000";
                this.errorOutput = String.join("; ", activeAlarms);
            }

            if (this.timestamp == null) {
                this.timestamp = Instant.now();
            }
            return true;

        } catch (Exception e) {
            log.error("Failed to parse RS485 0x44 frame: {}", asciiFrame44, e);
            return false;
        }
    }

//    public String decodeBmsGOOTO_InfoPayload(String output) {
    public String decodeBmsGOOTO_InfoPayload() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n--- DETAILS DECODE RS485 42 ---\n")
//                    .append(output).append("\n")
                    .append("#  | Name             | Value\n")
                    .append("---|------------------|------------\n")
                    .append(String.format("1  | Total Voltage (V)| %.2f V\n", this.voltageCurV))
                    .append(String.format("2  | Current (A)      | %.2f A\n", this.currentCurA))
                    .append(String.format("3  | Status Code      | %d (%s)\n", this.bmsStatus, this.bmsStatusStr))
                    .append(String.format("4  | Error Info       | %s -> %s\n",
                            this.errorInfoDataHex != null ? this.errorInfoDataHex : "0x0000",
                            this.errorOutput != null ? this.errorOutput : "N/A"))
                    .append(String.format("5  | SOC (%%)          | %.1f %%\n", this.socPercent))
                    .append(String.format("6  | SOH (%%)          | %.1f %%\n", this.sohPercent))
                    .append(String.format("7  | Remain Cap (Ah)  | %.2f Ah\n", this.remainAh))
                    .append(String.format("8  | Full Cap (Ah)    | %.2f Ah\n", this.fullAh))
                    .append(String.format("9  | Design Cap (Ah)  | %.2f Ah\n", this.designAh))
                    .append(String.format("10 | Cycles Count     | %d\n", this.cyclesCount != null ? this.cyclesCount : 0))
                    .append(String.format("11 | Cells Count      | %d\n", this.numCells != null ? this.numCells : 0))
                    .append("------------------------------------------------\n")
                    .append("CELL VOLTAGES:\n");

            if (this.cellVoltagesV != null) {
                for (int i = 0; i < this.cellVoltagesV.size(); i++) {
                    sb.append(String.format("Cell %02d | %.3f V\n", i + 1, this.cellVoltagesV.get(i + 1)));
                }
            }

            sb.append("------------------------------------------------\n")
                    .append("TEMPERATURES:\n");

            if (this.temperatures != null) {
                for (int i = 0; i < this.temperatures.size(); i++) {
                    sb.append(String.format("Temp %d  | %.1f °C\n", i + 1, this.temperatures.get(i + 1)));
                }
            }

            sb.append("------------------------------------------------\n");
            return sb.toString();

        } catch (Exception e) {
            log.error("CRITICAL DECODE ERROR RS485 42", e);
            return "\n--- CRITICAL DECODE ERROR RS485 42 ---\n" + e.getMessage() + "\n";
        }
    }

    public UsrTcpWiFiPacketRecord getInfoForRecords(int port) {
        if (Arrays.equals(this.payloadBytesCur, this.payloadBytesLastSaved)) {
            return null;
        } else {
            this.payloadBytesLastSaved = this.payloadBytesCur;
            return new UsrTcpWiFiPacketRecord(
                    this.timestamp.toEpochMilli(),
                    port,
                    "RS485_42",
                    this.payloadBytesCur.length,
                    this.payloadBytesCur);
        }
    }

    private String validateFindFrame(String asciiFrames) {
        if (isBlank(asciiFrames)) {
            return null;
        }

        // Розбиваємо вихідний потік по стартовому символу '~'
        String[] chunks = asciiFrames.split("(?=~)");

        for (String chunk : chunks) {
            String frame = chunk.trim();

            // 1. Відсікаємо занадто короткі шматки, сміття без '~' та біті символи FFFD
            if (frame.length() < PAYLOAD_START || !frame.startsWith("~") || frame.contains("\uFFFD")) {
                continue;
            }

            // 2. Ізолюємо перший закінчений рядок до '\r' або '\n'
            int endIdx = frame.indexOf('\r');
            if (endIdx != -1) {
                frame = frame.substring(0, endIdx).trim();
            }

            // 3. Перевіряємо заголовки Pylontech (VER=20, ADR=01, CID1=46, RTN=00)
            if (frame.startsWith("~20")
                    && "01".equals(frame.substring(3, 5))
                    && "46".equals(frame.substring(5, 7))
                    && "00".equals(frame.substring(7, 9))) {

                try {
                    // 4. Перевіряємо завальний обсяг INFO payload
                    String lenHex = frame.substring(9, 13);
                    int infoCharCount = Integer.parseInt(lenHex, 16) & 0x0FFF;
                    // ВАЛІДАЦІЯ МЕЖ: Переконуємося, що заявлена довжина реально є у кадрі
                    if (PAYLOAD_START + infoCharCount <= frame.length()) {
                        return frame; // Кадр ідеальний!
                    } else {
                        log.warn("RS485 frame payload overflow: expected {}, actual len {}",
                                PAYLOAD_START + infoCharCount, frame.length());
                    }
                } catch (Exception ignored) {
                    // Помилка парсингу LEN_HEX -> пропускаємо битий шматок
                }
            }
        }

        return null;
    }

    // -----------------------------
    //   CALCULATION METHODS
    // -----------------------------
    private Float computeSumCellsV() {
        return this.cellVoltagesV.isEmpty() ? null :
                (float) this.cellVoltagesV.values().stream()
                        .mapToDouble(Float::doubleValue)
                        .sum();
    }

    private JsonNode computeMinCell() {
        if (cellVoltagesV.isEmpty()) return null;

        var minEntry = this.cellVoltagesV.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElseThrow();

        return newObjectNode()
                .put(keyIdx, minEntry.getKey())
                .put(keyVoltage, minEntry.getValue());
    }

    private JsonNode computeMaxCell() {
        if (this.cellVoltagesV.isEmpty()) return null;

        var maxEntry = this.cellVoltagesV.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        return newObjectNode()
                .put(keyIdx, maxEntry.getKey())
                .put(keyVoltage, maxEntry.getValue());
    }

    private int computeDeltaMv() {
        if (this.minCellV == null || this.maxCellV == null) return -1;

        float minV = this.minCellV.get(keyVoltage).floatValue();
        float maxV = this.maxCellV.get(keyVoltage).floatValue();

        return (int) ((maxV - minV) * 1000);
    }

    private UsrTcpWifiBalanceThresholds computeBalanceStatus() {
        if (this.deltaMv < 0) return null;
        return getBalanceStatus(this.deltaMv);
    }
}