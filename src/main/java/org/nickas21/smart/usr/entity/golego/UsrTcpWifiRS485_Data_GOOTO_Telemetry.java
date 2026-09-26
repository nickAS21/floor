package org.nickas21.smart.usr.entity.golego;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.data.UsrWifiBmsBatteryStatus;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.nickas21.smart.util.StringUtils.isNotBlank;

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

    // Поля телеметрії 0x42H
    private Integer numCells;
    private List<Double> cellVoltages = new ArrayList<>();
    private List<Double> temperatures = new ArrayList<>();

    private double socPercent;       // Розрахований SOC (%)
    private double sohPercent;       // SOH (%)

    private double currentCurA;      // Струм (А)
    private double voltageCurV;      // Загальна напруга (В)
    private double remainAh;         // Залишкова ємність (Ah)
    private double fullAh;           // Повна актуальна ємність (Ah)
    private double designAh;         // Базова (паспортна) ємність (Ah)
    private Integer cyclesCount;     // Цикли

    // Статуси BMS
    private int bmsStatus = UsrWifiBmsBatteryStatus.UNKNOWN.getCode();
    private String bmsStatusStr = UsrWifiBmsBatteryStatus.UNKNOWN.getStatus();

    // --- Поля помилок та алармів ---
    private String errorInfoDataHex;   // Код помилки  весь payload в Hex якщо є хоть одна помилка і == "0x0000" - якщо все чисто
    private String errorOutput;      // Текстовий опис помилки

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
     * ~22014A84E00201FD22
     * ~2001460070720001100CEF...
     * ~22024A84E00202FD20
     * ~ 20 01 46 00 70 72 ...
     *   │  │  │  │
     *   │  │  │  └─ RTN = 00 → NORMAL
     *   │  │  └──── CID1 = 46
     *   │  └─────── ADR = 01
     *   └────────── VER = 20
     */
    public boolean parse42(String asciiFrame42) {
        try {
            String dataFrame = validateFindFrame (asciiFrame42);
            if (dataFrame == null) {
                log.warn("RS485 0x42 NORMAL response frame not found in: {}", asciiFrame42);
                return false;
            }

            // ---------------------------------------------------------
            // Parse INFO Payload according to Pylontech ASCII Standard
            // ---------------------------------------------------------

            // 1. Parse LENGTH (4 HEX chars at indices 9..13)
            String lenHex = dataFrame.substring(9, 13);

            // LenID mask (0x7072 & 0x0FFF -> 0x072 = 114 ASCII chars / 57 bytes)
            int infoCharCount = Integer.parseInt(lenHex, 16) & 0x0FFF;

            // Чиста вирізка INFO: початок з 13, довжина infoCharCount
            int payloadStart = 13;
            String payload = dataFrame.substring(payloadStart, payloadStart + infoCharCount);
            if (payloadStart >= dataFrame.length()) {
                log.warn("Payload missing in RS485 0x42 frame: {}", dataFrame);
                return false;
            }

            // 2. Cell Voltages (16S)
            // Python equivalent: num_cells = int(payload[4:6], 16)
            if (payload.length() < 6) return false;
            this.numCells = Integer.parseInt(payload.substring(4, 6), 16);

            int cellsStart = 6;
            this.cellVoltages = new ArrayList<>();

            for (int i = 0; i < this.numCells; i++) {
                int pos = cellsStart + i * 4;
                if (pos + 4 > payload.length()) break;

                int mV = Integer.parseInt(payload.substring(pos, pos + 4), 16);
                this.cellVoltages.add(mV / 1000.0);
            }

            // 3. Temperature Sensors
            int tempsCountPos = cellsStart + this.numCells * 4;

            if (tempsCountPos + 2 <= payload.length()) {
                int numTemps = Integer.parseInt(payload.substring(tempsCountPos, tempsCountPos + 2), 16);
                int tempsStart = tempsCountPos + 2;

                this.temperatures = new ArrayList<>();

                for (int i = 0; i < numTemps; i++) {
                    int pos = tempsStart + i * 4;
                    if (pos + 4 > payload.length()) break;

                    int deciKelvin = Integer.parseInt(payload.substring(pos, pos + 4), 16);
                    double celsius = Math.round(((deciKelvin - 2731) / 10.0) * 10.0) / 10.0;
                    this.temperatures.add(celsius);
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

                // SOC & SOH Calculations
                if (this.fullAh > 0 && this.remainAh > 0) {
                    this.socPercent = Math.min(100.0, Math.max(0.0, (this.remainAh / this.fullAh) * 100.0));
                }

                this.designAh = 300.0;
                if (this.fullAh > 0) {
                    this.sohPercent = Math.round((this.fullAh / 300.0) * 10000.0) / 100.0;
                }
            }

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

            // 1. Вирізка LENGTH & INFO
            String lenHex = dataFrame.substring(9, 13);
            int infoCharCount = Integer.parseInt(lenHex, 16) & 0x0FFF;

            int payloadStart = 13;
            if (payloadStart + infoCharCount > dataFrame.length()) {
                log.warn("Payload overflow in RS485 0x44 frame: {}", dataFrame);
                return false;
            }
            String payload = dataFrame.substring(payloadStart, payloadStart + infoCharCount);

            if (payload.length() < 4) return false;

            // 2. Кількість комірок та їх аларми (1 byte count + N bytes alarms)
            int numCells44 = Integer.parseInt(payload.substring(2, 4), 16);
            int pos = 4;

            this.cellAlarms = new ArrayList<>();
            for (int i = 0; i < numCells44; i++) {
                if (pos + 2 > payload.length()) break;
                int alarmCode = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                this.cellAlarms.add(alarmCode);
                pos += 2;
            }

            // 3. Кількість датчиків температури та їх аларми
            if (pos + 2 <= payload.length()) {
                int numTemps44 = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;

                this.tempAlarms = new ArrayList<>();
                for (int i = 0; i < numTemps44; i++) {
                    if (pos + 2 > payload.length()) break;
                    int alarmCode = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                    this.tempAlarms.add(alarmCode);
                    pos += 2;
                }
            }

            // 4. Загальні аларми (Струм заряду, Напруга паку, Струм розряду)
            if (pos + 2 <= payload.length()) {
                this.chargeCurrentAlarm = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }
            if (pos + 2 <= payload.length()) {
                this.totalVoltageAlarm = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }
            if (pos + 2 <= payload.length()) {
                this.dischargeCurrentAlarm = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }

            // 5. Системні прапори та стан MOSFET
            if (pos + 2 <= payload.length()) {
                this.systemStatusFlags = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                pos += 2;
            }

            if (pos + 2 <= payload.length()) {
                int mosfetFlags = Integer.parseInt(payload.substring(pos, pos + 2), 16);
                // За специфікацією: біт 0 - Charge FET, біт 1 - Discharge FET
                this.chargeMosfetEnabled = (mosfetFlags & 0x01) == 0;
                this.dischargeMosfetEnabled = (mosfetFlags & 0x02) == 0;
            }

            // 6. Формування errorOutput, errorInfoDataHex та errorInfoData
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

            if (activeAlarms.isEmpty()) {
                this.errorInfoDataHex = "0x0000";
                 this.errorOutput = null;
            } else {
                this.errorInfoDataHex = payload;
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


    /**
     * Маппінг коду помилки у текстовий опис
     */
    /**
     * Розширений маппінг кодів помилок, статусів та захистів BMS (Pylontech / Daren / GOOTO)
     */
    private String decodeErrorCode(int code) {
        return switch (code) {
            case 0x0000 -> "Normal (No Error)";

            // --- Системні відповіді RTN / Заперечення протоколу ---
            case 0x0001 -> "VER Error / Protocol Version Invalid (0x0001)";
            case 0x0002 -> "CHKSUM Error / Checksum Invalid (0x0002)";
            case 0x0003 -> "LCHKSUM Error / Length Checksum Invalid (0x0003)";
            case 0x0004 -> "CID2 Invalid / Command Not Supported (0x0004)";
            case 0x0005 -> "Command Format Error (0x0005)";
            case 0x0006 -> "INFO Data Invalid / Parameter Error (0x0006)";

            // --- Захисти по напрузі (System & Cell) ---
            case 0x1001 -> "System Overvoltage Protection (0x1001)";
            case 0x1002 -> "System Undervoltage Protection (0x1002)";
            case 0x2007 -> "Cell Overvoltage Protection (0x2007)";
            case 0x2008 -> "Cell Undervoltage Protection (0x2008)";
            case 0x2009 -> "Cell High Voltage Warning (0x2009)";
            case 0x200A -> "Cell Low Voltage Warning (0x200A)";

            // --- Захисти по струму (Current) ---
            case 0x1007 -> "Charge Overcurrent Protection (0x1007)";
            case 0x1008 -> "Discharge Overcurrent Protection (0x1008)";
            case 0x1009 -> "Charge Overcurrent Warning (0x1009)";
            case 0x100A -> "Discharge Overcurrent Warning (0x100A)";
            case 0x100B -> "Short Circuit Protection / КЗ (0x100B)";

            // --- Захисти по температурі (Temperatures) ---
            case 0x2001 -> "Charge Over Temperature Protection (0x2001)";
            case 0x2002 -> "Charge Under Temperature Protection (0x2002)";
            case 0x2003 -> "Discharge Over Temperature Protection (0x2003)";
            case 0x2004 -> "Discharge Under Temperature Protection (0x2004)";
            case 0x2005 -> "MOSFET Over Temperature Protection (0x2005)";
            case 0x2006 -> "Environment Over Temperature Protection (0x2006)";

            // --- Апаратні та системні збої (Hardware & Sensor Faults) ---
            case 0x3001 -> "Cell Voltage Sensor Error / Break (0x3001)";
            case 0x3002 -> "Temperature Sensor Error / Break (0x3002)";
            case 0x3003 -> "Current Sensor Failure (0x3003)";
            case 0x3004 -> "Charge MOSFET Failure (0x3004)";
            case 0x3005 -> "Discharge MOSFET Failure (0x3005)";
            case 0x3006 -> "EEPROM / Memory Fault (0x3006)";
            case 0x3007 -> "Severe Differential Voltage / Unbalance (0x3007)";

            default -> String.format("Protection Code 0x%04X (%d)", code, code);
        };
    }

    public String decodeBmsGOOTO_InfoPayload(String output) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n--- DETAILS DECODE RS485 42 ---\n")
                    .append(output).append("\n")
                    .append("#  | Name             | Value\n")
                    .append("---|------------------|------------\n")
                    .append(String.format("1  | Total Voltage (V)| %.2f V\n", this.voltageCurV))
                    .append(String.format("2  | Current (A)      | %.2f A\n", this.currentCurA))
                    .append(String.format("3  | Status Code      | %d (%s)\n", this.bmsStatus, this.bmsStatusStr))
                    .append(String.format("4  | Error Info       | 0x%04X (%d) -> %s\n",
                            this.errorInfoDataHex != null ? this.errorInfoDataHex : 0,
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

            if (this.cellVoltages != null) {
                for (int i = 0; i < this.cellVoltages.size(); i++) {
                    sb.append(String.format("Cell %02d | %.3f V\n", i + 1, this.cellVoltages.get(i)));
                }
            }

            sb.append("------------------------------------------------\n")
                    .append("TEMPERATURES:\n");

            if (this.temperatures != null) {
                for (int i = 0; i < this.temperatures.size(); i++) {
                    sb.append(String.format("Temp %d  | %.1f °C\n", i + 1, this.temperatures.get(i)));
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
        // Split concatenated stream by SOI ('~') to find valid 0x42 response
        String dataFrame = null;
        if (isNotBlank (asciiFrames)) {
            for (String frame : asciiFrames.split("(?=~)")) {
                frame = frame.trim();

                if (frame.length() < 13 || !frame.startsWith("~")) {
                    continue;
                }

                // Validation of header: SOI(~) VER(20) ADR(01) CID1(46) RTN(00)
                // Indices:              0      1-2     3-4     5-6     7-8
                if (frame.startsWith("~20")
                        && "01".equals(frame.substring(3, 5))
                        && "46".equals(frame.substring(5, 7))
                        && "00".equals(frame.substring(7, 9))) {

                    dataFrame = frame;
                    break;
                }
            }
        }
        return dataFrame;
    }
}