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

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiRS485_42Data {

    public static final int CMD_CID2_42 = 0x42;

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
    private Integer errorInfoData;   // Код помилки (наприклад: 0x1007, 0x1008, 0x2007, 0x2008)
    private String errorOutput;      // Текстовий опис помилки

    private Instant timestamp;
    private byte[] payloadBytesCur;
    private byte[] payloadBytesLastSaved;

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
    public static UsrTcpWifiRS485_42Data parse(String asciiFrame) {
        if (asciiFrame == null || asciiFrame.isBlank()) {
            log.warn("Invalid or empty RS485 0x42 ASCII frame: {}", asciiFrame);
            return null;
        }

        try {
            // Split concatenated stream by SOI ('~') to find valid 0x42 response
            String dataFrame = null;

            for (String frame : asciiFrame.split("(?=~)")) {
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

            if (dataFrame == null) {
                log.warn("RS485 0x42 NORMAL response frame not found in: {}", asciiFrame);
                return null;
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
                return null;
            }

            UsrTcpWifiRS485_42Data data42 = new UsrTcpWifiRS485_42Data();

            // 2. Cell Voltages (16S)
            // Python equivalent: num_cells = int(payload[4:6], 16)
            if (payload.length() < 6) return null;
            data42.numCells = Integer.parseInt(payload.substring(4, 6), 16);

            int cellsStart = 6;
            data42.cellVoltages = new ArrayList<>();

            for (int i = 0; i < data42.numCells; i++) {
                int pos = cellsStart + i * 4;
                if (pos + 4 > payload.length()) break;

                int mV = Integer.parseInt(payload.substring(pos, pos + 4), 16);
                data42.cellVoltages.add(mV / 1000.0);
            }

            // 3. Temperature Sensors
            int tempsCountPos = cellsStart + data42.numCells * 4;

            if (tempsCountPos + 2 <= payload.length()) {
                int numTemps = Integer.parseInt(payload.substring(tempsCountPos, tempsCountPos + 2), 16);
                int tempsStart = tempsCountPos + 2;

                data42.temperatures = new ArrayList<>();

                for (int i = 0; i < numTemps; i++) {
                    int pos = tempsStart + i * 4;
                    if (pos + 4 > payload.length()) break;

                    int deciKelvin = Integer.parseInt(payload.substring(pos, pos + 4), 16);
                    double celsius = Math.round(((deciKelvin - 2731) / 10.0) * 10.0) / 10.0;
                    data42.temperatures.add(celsius);
                }

                // 4. Analog Tail (Current, Total Voltage, Capacities, Cycles)
                int p = tempsStart + numTemps * 4;

                // Current (Signed 16-bit Int)
                if (p + 4 <= payload.length()) {
                    int rawCurrent = Integer.parseInt(payload.substring(p, p + 4), 16);
                    if (rawCurrent > 0x7FFF) {
                        rawCurrent -= 0x10000;
                    }
                    data42.currentCurA = rawCurrent / 10.0;

                    int statusCode;
                    if (data42.currentCurA > 0.1) {
                        statusCode = UsrWifiBmsBatteryStatus.CHARGING.getCode();
                    } else if (data42.currentCurA < -0.1) {
                        statusCode = UsrWifiBmsBatteryStatus.DISCHARGING.getCode();
                    } else {
                        statusCode = UsrWifiBmsBatteryStatus.STATIC.getCode();
                    }

                    data42.bmsStatus = statusCode;
                    data42.bmsStatusStr = UsrWifiBmsBatteryStatus.fromCode(statusCode).getStatus();
                }

                // Total Pack Voltage
                if (p + 8 <= payload.length()) {
                    data42.voltageCurV = Integer.parseInt(payload.substring(p + 4, p + 8), 16) / 1000.0;
                }

                // Remaining Capacity (Ah)
                if (p + 12 <= payload.length()) {
                    data42.remainAh = Integer.parseInt(payload.substring(p + 8, p + 12), 16) / 100.0;
                }

                // Full Capacity (Ah)
                if (p + 18 <= payload.length()) {
                    String rawFullHex = payload.substring(p + 14, p + 18);
                    data42.fullAh = "0000".equals(rawFullHex)
                            ? 300.0
                            : Integer.parseInt(rawFullHex, 16) / 100.0;
                } else {
                    data42.fullAh = 300.0;
                }

                // Cycle Count
                if (p + 22 <= payload.length()) {
                    String rawCycHex = payload.substring(p + 18, p + 22);
                    data42.cyclesCount = Integer.parseInt(rawCycHex, 16);
                }

                // SOC & SOH Calculations
                if (data42.fullAh > 0 && data42.remainAh > 0) {
                    data42.socPercent = Math.min(100.0, Math.max(0.0, (data42.remainAh / data42.fullAh) * 100.0));
                }

                data42.designAh = 300.0;
                if (data42.fullAh > 0) {
                    data42.sohPercent = Math.round((data42.fullAh / 300.0) * 10000.0) / 100.0;
                }
            }

            data42.timestamp = Instant.now();
            data42.payloadBytesCur = dataFrame.getBytes(StandardCharsets.US_ASCII);

            return data42;

        } catch (Exception e) {
            log.error("Failed to parse RS485 0x42 frame: {}", asciiFrame, e);
            return null;
        }
    }
    /**
     * Встановлює код помилки та формує текстовий опис errorOutput
     */
    public void setErrorData(Integer errorInfoData) {
        this.errorInfoData = errorInfoData;
        if (errorInfoData == null || errorInfoData == 0) {
            this.errorOutput = "Normal (No Error)";
            return;
        }

        this.errorOutput = decodeErrorCode(errorInfoData);
    }

    /**
     * Маппінг коду помилки у текстовий опис
     */
    private String decodeErrorCode(int code) {
        return switch (code) {
            case 0x1007 -> "Charge Overcurrent Protection (0x1007)";
            case 0x1008 -> "Discharge Overcurrent Protection (0x1008)";
            case 0x2007 -> "Cell Overvoltage Protection (0x2007)";
            case 0x2008 -> "Cell Undervoltage Protection (0x2008)";
            case 0x1001 -> "System Overvoltage Protection";
            case 0x1002 -> "System Undervoltage Protection";
            case 0x2001 -> "Charge Over Temperature Protection";
            case 0x2002 -> "Charge Under Temperature Protection";
            case 0x2003 -> "Discharge Over Temperature Protection";
            case 0x2004 -> "Discharge Under Temperature Protection";
            default -> String.format("Protection Code 0x%04X (%d)", code, code);
        };
    }

    public String decode42BmsInfoPayload(String output) {
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
                            this.errorInfoData != null ? this.errorInfoData : 0,
                            this.errorInfoData != null ? this.errorInfoData : 0,
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
}