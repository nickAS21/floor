package org.nickas21.smart.usr.data;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiC1Data;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.usr.data.UsrTcpWifiStatus.fromCode;

@Slf4j
public class UsrTcpWiFiDecoders {

    // Constants and maps translated from Python
    private static final int BMS_LIFEPO4_CYCLES_TO_80_SOH = 6000;
    private static final int SOH_TERMINAL_PERCENT = 80;
    private static final int SOH_DEGRADATION_RANGE = 100 - SOH_TERMINAL_PERCENT;
    private static final double SOH_LOSS_PER_CYCLE = (double) SOH_DEGRADATION_RANGE / BMS_LIFEPO4_CYCLES_TO_80_SOH;
    public static final byte[] START_SIGN = { (byte) 0xAA, (byte) 0x55 };
    private static final int ID_LEN = 19;
    public static final int ID_START = 3;
    public static final int ID_END = ID_START + ID_LEN;
    private static final int CRC_LEN = 2;
    public static final int MIN_PACKET_LENGTH = ID_END + CRC_LEN;

    // C0
    private static final int lenVoltageMinV = 2;
    private static final int lenVoltageCurV = 2;
    private static final int lenCurrentACur = 2;
    private static final int lenSocPercent = 1;
    private static final int lenBmsStatus = 2;
    private static final int lenBmsStatus1_2 = 4;
    public static final int lenErrorInfoData = 2;
    private static final int lenReserve = 2;
    // C1
    private static final int lenCellsAllLen = 1;
    private static final int lenCellsCnt = 1;
    private static final int lenLifeCyclesCount = 2;
    private static final int lenVerM = 1;
    public static String keyIdx = "idx";
    public static String keyVoltage = "voltage";


    public static void decodeC0Payload(byte[] payloadBytes, UsrTcpWifiC0Data c0Data, Instant timestamp) {
        try {
            int i = 0;
            ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
            double voltageMinV = ((bb.getShort(i) & 0xFFFF)) / 100.0;
            i += lenVoltageMinV;
            double voltageCurV = ((bb.getShort(i) & 0xFFFF) ) / 100.0;
            i += lenVoltageCurV;
            double currentCurA = (bb.getShort(i)) / 10.0; // signed
            i += lenCurrentACur;
            int socPercent = payloadBytes[i] & 0xFF;
            i += lenSocPercent;
            int bmsStatus = (bb.getShort(i) & 0xFFFF);
            String bmsStatusStr = fromCode(bmsStatus).getStatus();
            i += lenBmsStatus;
            int bmsStatus1 = (bb.getInt(i));
            i += lenBmsStatus1_2;
            int bmsStatus2 = (bb.getInt(i));
            i += lenBmsStatus1_2;
            int errorInfoData = bb.getShort(i) & 0xFFFF;
            i += lenErrorInfoData;

            c0Data.updateC0Data(voltageMinV, voltageCurV, currentCurA, socPercent, bmsStatus,
                                bmsStatus1, bmsStatus2, errorInfoData, timestamp, payloadBytes);
        } catch (Exception e) {
            log.error("CRITICAL DECODE ERROR C0", e);
        }
    }

    public static void decodeC1Payload(byte[] payloadBytes, UsrTcpWifiC1Data c1Data, Instant timestamp) {
        try {
            int i = 0;
            ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
            int cellsAllLen = payloadBytes[i] & 0xFF;
            i++;
            byte[] cellsDataAll = new byte[cellsAllLen];
            System.arraycopy(payloadBytes, 0, cellsDataAll, 0, cellsAllLen);
            i += cellsAllLen;
            int majorVersion = payloadBytes[i] & 0xFF;
            i += lenVerM;
            int minorVersion = payloadBytes[i] & 0xFF;

            ByteBuffer bbCellsAll = ByteBuffer.wrap(cellsDataAll);
            i = 0;
            i += lenCellsAllLen;
            int cellsCount = cellsDataAll[i] & 0xFF;
            i += lenCellsCnt;
            int lenCells = cellsCount * 2;
            byte[] cellsData = new byte[lenCells];
            System.arraycopy(cellsDataAll, i, cellsData, 0, lenCells);
            i += lenCells;
            int lifeCyclesCount = (bbCellsAll.getShort(i) & 0xFFFF);
            i += lenLifeCyclesCount;
            int socPercent = cellsDataAll[i] & 0xFF;
            i += lenSocPercent;
            int errorInfoData = bb.getShort(i) & 0xFFFF;
            i += lenErrorInfoData;

            // Voltages
            Map<Integer, Float> cellVoltagesV = new ConcurrentHashMap<>();
            for (i = 0; i < cellsCount; i++) {
                int offset = i * 2;
                int raw = ((cellsData[offset] & 0xFF) << 8) | (cellsData[offset + 1] & 0xFF);
                float voltage = raw / 1000.0f;
                cellVoltagesV.put(i, voltage);
            }
            c1Data.updateC1Data(cellsCount, cellVoltagesV, lifeCyclesCount, socPercent,
                                errorInfoData, majorVersion, minorVersion, timestamp, payloadBytes);
        } catch (Exception e) {
            log.error("CRITICAL ERROR C1", e);
        }
    }
}

