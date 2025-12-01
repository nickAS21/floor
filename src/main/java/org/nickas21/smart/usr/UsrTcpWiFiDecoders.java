package org.nickas21.smart.usr;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.unit.UsrTcpWifiStatus;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.nickas21.smart.usr.unit.UsrTcpWiFiUtils.bytesToHex;
import static org.nickas21.smart.usr.unit.UsrTcpWifiBalanceThreadholds.getBalanceStatus;
import static org.nickas21.smart.usr.unit.UsrTcpWifiError.decodeErrorFlags;
import static org.nickas21.smart.usr.unit.UsrTcpWifiError.formatErrorCodeOutput;

@Slf4j
public class UsrTcpWiFiDecoders {

    // Constants and maps translated from Python
    private static final int BMS_LIFEPO4_CYCLES_TO_80_SOH = 6000;
    private static final int SOH_TERMINAL_PERCENT = 80;
    private static final int SOH_DEGRADATION_RANGE = 100 - SOH_TERMINAL_PERCENT;
    private static final double SOH_LOSS_PER_CYCLE = (double) SOH_DEGRADATION_RANGE / BMS_LIFEPO4_CYCLES_TO_80_SOH;
    // C0
    private static final int lenVoltageVMin = 2;
    private static final int lenVoltageVCur = 2;
    private static final int lenCurrentACur = 2;
    private static final int lenSoc = 1;
    private static final int lenBmsStatus = 2;
    private static final int lenBmsStatus1_2 = 4;
    private static final int lenErrorInfoData = 3;
    private static final int lenReserve = 1;
    // C1
    private static final int lenCellsAllLen = 1;
    private static final int lenCellsCnt = 1;
    private static final int lenLifeCycles = 2;
    private static final int lenVerM = 1;



    public static String decodeC0Payload(byte[] payloadBytes) {
        try {
            int i = 0;
            ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
            double voltageVMin = ((bb.getShort(i) & 0xFFFF)) / 100.0;
            i += lenVoltageVMin;
            double voltageVCur = ((bb.getShort(i) & 0xFFFF) ) / 100.0;
            i += lenVoltageVCur;
            double currentACur = (bb.getShort(i)) / 10.0; // signed
            i += lenCurrentACur;
            int soc = payloadBytes[i] & 0xFF;
            i += lenSoc;
            int bmsStatus = (bb.getShort(i) & 0xFFFF);
            String bmsStatusStr = UsrTcpWifiStatus.fromCode(bmsStatus).getStatus();
            i += lenBmsStatus;
            int bmsStatus1 = (bb.getInt(i));
            i += lenBmsStatus1_2;
            int bmsStatus2 = (bb.getInt(i));
            i += lenBmsStatus1_2;
            int errorInfoData = (bb.getShort(i) & 0xFFFFFF);
            i += lenErrorInfoData;
            int reserve = payloadBytes[i] & 0xFF;
            String errorOutput = formatErrorCodeOutput(errorInfoData, 9);

            StringBuilder out = new StringBuilder();
            out.append("\n--- DETAILS DECODE C0 (BMS General Status) ---\n");
            out.append(String.format("1  | Voltage Min (V)  | %.2f V\n", voltageVMin));
            out.append(String.format("2  | Voltage (V)      | %.2f V\n", voltageVCur));
            out.append(String.format("3  | Current (A)      | %.2f A\n", currentACur));
            out.append(String.format("4  | SOC (%%)          | %d %%\n", soc));
            out.append(String.format("5  | BMS status       | %s\n", bmsStatusStr));
            out.append(String.format("6  | BMS status1      | 0x%s (%dB)\n", Integer.toHexString(bmsStatus1).toUpperCase(), lenBmsStatus1_2));
            out.append(String.format("7  | BMS status2      | 0x%s (%dB)\n", Integer.toHexString(bmsStatus2).toUpperCase(), lenBmsStatus1_2));
            out.append(String.format("8  | Error info Data  | 0x%s (%dB)\n", Integer.toHexString(errorInfoData).toUpperCase(), lenErrorInfoData));
            out.append(errorOutput).append("\n");
            out.append("------------------------------------------------\n");
            return out.toString();

        } catch (Exception e) {
            log.error("CRITICAL DECODE ERROR C0", e);
            return "\n--- CRITICAL DECODE ERROR C0 ---\n" + e.getMessage() + "\n";
        }
    }

    public static String decodeC1Payload(byte[] payloadBytes) {
        try {
            int i = 0;
            ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
            int cellsAllLen = payloadBytes[i] & 0xFF;
            byte[] cellsDataAll = new byte[cellsAllLen];
            System.arraycopy(payloadBytes, 0, cellsDataAll, 0, cellsAllLen);
            i += cellsAllLen;
            int reserve = payloadBytes[i] & 0xFF;
            i += lenReserve;
            int majorVersion = payloadBytes[i] & 0xFF;
            i += lenVerM;
            int minorVersion = payloadBytes[i] & 0xFF;

            ByteBuffer bbCellsAll = ByteBuffer.wrap(cellsDataAll);
            i = 0;
            i += lenCellsAllLen;
            int cellsCnt = cellsDataAll[i] & 0xFF;
            i += lenCellsCnt;
            int lenCells = cellsCnt * 2;
            byte[] cellsData = new byte[lenCells];
            System.arraycopy(cellsDataAll, i, cellsData, 0, lenCells);
            i += lenCells;
            int lifeCycles = ((bbCellsAll.getShort(i) & 0xFFFF));
            i += lenLifeCycles;
            int soc = cellsDataAll[i] & 0xFF;
            i += lenSoc;
            int errorInfoData = (bb.getShort(i) & 0xFFFFFF);
            int soh = (int) Math.round(100 - (lifeCycles * SOH_LOSS_PER_CYCLE));

            // Voltages
            List<Integer> voltages = new ArrayList<>();
            for (i = 0; i < cellsCnt; i++) {
                int offset = i * 2;
                int mv = ((cellsData[offset] & 0xFF) << 8) | (cellsData[offset + 1] & 0xFF);
                voltages.add(mv);
            }
            int minMv = voltages.stream().min(Integer::compareTo).orElse(0);
            int maxMv = voltages.stream().max(Integer::compareTo).orElse(0);
            int deltaMv = maxMv - minMv;
            String balance = getBalanceStatus(deltaMv);
            int sumMv = voltages.stream().mapToInt(Integer::intValue).sum();
            double sumV = sumMv / 1000.0;
            int idxMin = voltages.indexOf(minMv) + 1;
            int idxMax = voltages.indexOf(maxMv) + 1;

            StringBuilder out = new StringBuilder();
            out.append("\n--- DETAILS DECODE C1 ---\n");
            out.append("1.3) Cells Info Table:\n");
            out.append("#  | Name             | Value\n");
            out.append("---|------------------|------------\n");
            out.append(String.format("1  | Ver:             | V%02d%02d\n", majorVersion, minorVersion));
            out.append(String.format("2  | Life Cycles:     | %d\n", lifeCycles));
            out.append(String.format("3  | SOC:             | %d %%\n",soc));
            out.append(String.format("4  | SOH:             | %d %%\n", soh));
            out.append(String.format("5  | SUM_V:           | %.2f V\n", sumV));
            out.append(String.format("6  | Cell%02d_MIN:      | %.3f V\n", idxMin, minMv / 1000.0));
            out.append(String.format("7  | Cell%02d_MAX:      | %.3f V\n", idxMax, maxMv / 1000.0));
            out.append(String.format("8  | DELTA:           | %.3f V\n", deltaMv / 1000.0));
            out.append(String.format("9  | Balance:         | %s\n", balance));

            String errOutput = formatErrorCodeOutput(errorInfoData, 10);
            out.append(errOutput).append("\n\n");

            out.append("1.4) Cells Table:\n#\tHEX     mV      V\n");
            for (i = 0; i < cellsCnt; i++) {
                int mv = voltages.get(i);
                double v = mv / 1000.0;
                byte[] vb = new byte[]{ cellsData[i*2], cellsData[i*2 + 1] };
                out.append(String.format("%02d\t%s\t%d\t%.3f V\n", i+1, bytesToHex(vb), mv, v));
            }

            return out.toString();

        } catch (Exception e) {
            log.error("CRITICAL ERROR C1", e);
            return "\n--- CRITICAL ERROR C1 ---\n" + e.getMessage() + "\n";
        }
    }

    public static String decodeC0BmsInfoPayload(byte[] payloadBytes) {
        try {
            int i = 0;
            ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
            double voltageVMin = ((bb.getShort(i) & 0xFFFF)) / 100.0;
            i += lenVoltageVMin;
            double voltageVCur = ((bb.getShort(i) & 0xFFFF) ) / 100.0;
            i += lenVoltageVCur;
            double currentACur = (bb.getShort(i)) / 10.0; // signed
            i += lenCurrentACur;
            int soc = payloadBytes[i] & 0xFF;
            i += lenSoc;
            int bmsStatus = (bb.getShort(i) & 0xFFFF);
            String bmsStatusStr = UsrTcpWifiStatus.fromCode(bmsStatus).getStatus();
            i += lenBmsStatus;
            i += lenBmsStatus1_2;
            i += lenBmsStatus1_2;
            int errorInfoData = (bb.getShort(i) & 0xFFFFFF);
            List<String> errors = decodeErrorFlags(errorInfoData);
            i += lenErrorInfoData;
            int reserve = payloadBytes[i] & 0xFF;
            String errorOutput = formatErrorCodeOutput(errorInfoData, 9);

            StringBuilder out = new StringBuilder();
            out.append("\n--- DETAILS DECODE C0 (BMS General Status - INFO) ---\n");
            out.append(String.format("1  | Voltage Min (V)  | %.2f V\n", voltageVMin));
            out.append(String.format("2  | Voltage (V)      | %.2f V\n", voltageVCur));
            out.append(String.format("3  | Current (A)      | %.2f A\n", currentACur));
            out.append(String.format("4  | SOC (%%)         | %d %%\n", soc));
            out.append(String.format("5  | BMS status       | %s\n", bmsStatusStr));
            out.append(String.format("8  | Error info Data  | 0x%s (%dB)\n", Integer.toHexString(errorInfoData).toUpperCase(), lenErrorInfoData));
            out.append(errorOutput).append("\n");
            out.append("------------------------------------------------\n");
            return out.toString();

        } catch (Exception e) {
            log.error("CRITICAL DECODE ERROR C0", e);
            return "\n--- CRITICAL DECODE ERROR C0 ---\n" + e.getMessage() + "\n";
        }    }


    public static String decodeA2Payload(byte[] payloadBytes) {
        if (payloadBytes == null || payloadBytes.length != 2) {
            return "\n--- Error A2: Incorrect data length ---";
        }
        int major = payloadBytes[0] & 0xFF;
        int minor = payloadBytes[1] & 0xFF;
        return String.format("\nVer: V%02d%02d\n", major, minor);
    }


}
