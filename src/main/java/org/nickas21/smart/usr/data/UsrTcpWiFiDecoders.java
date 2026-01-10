package org.nickas21.smart.usr.data;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.entity.UsrTcpWifiC0Data;
import org.nickas21.smart.usr.entity.UsrTcpWifiC1Data;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UsrTcpWiFiDecoders {

    // Constants
    // 1416115SLGOPG009251 - ident SN BMS
    // 1416115SLGOPGA09113 - ident SN BMS
    private static final int BMS_LIFEPO4_CYCLES_TO_80_SOH = 6000;
    private static final int SOH_TERMINAL_PERCENT = 80;
    private static final int SOH_DEGRADATION_RANGE = 100 - SOH_TERMINAL_PERCENT;
    private static final double SOH_LOSS_PER_CYCLE = (double) SOH_DEGRADATION_RANGE / BMS_LIFEPO4_CYCLES_TO_80_SOH;
    public static final byte[] START_SIGN_AA = { (byte) 0xAA, (byte) 0x55 };
    public static final byte[] START_SIGN_5E = { (byte) 0x5E, (byte) 0x10 };
    private static final int ID_BMS_LEN = 19;
    public static final int ID_BMS_START = 3;
    public static final int ID_BMS_END = ID_BMS_START + ID_BMS_LEN;
    private static final int CRC_LEN = 2;
    public static final int MIN_PACKET_LENGTH = ID_BMS_END + CRC_LEN;

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

    public static void decodeC0Payload(byte[] payloadBytes, UsrTcpWifiC0Data c0Data, Instant timestamp, int port) {
        double voltageMinV = 0, voltageCurV = 0, currentCurA = 0, socPercent = 0;
        int bmsStatus = 0, bmsStatus1 = 0, bmsStatus2 = 0, errorInfoData = 0, reserveData = 0;

        try {
            if (payloadBytes != null && payloadBytes.length > 0) {
//                // TODO for test
//                String testHex =  "140015040001640004000000040000000020080000";
//                byte[] testBB = hexToBytes(testHex);
                // 1400 1504 0001 64 0004 00 000004 00000000 2008 0000
//                ByteBuffer bb = ByteBuffer.wrap(testBB);
                ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
                if (bb.remaining() >= lenVoltageMinV) voltageMinV = (bb.getShort() & 0xFFFF) / 100.0;
                if (bb.remaining() >= lenVoltageCurV) voltageCurV = (bb.getShort() & 0xFFFF) / 100.0;
                if (bb.remaining() >= lenCurrentACur) currentCurA = bb.getShort() / 10.0; // signed short
                if (bb.remaining() >= lenSocPercent) socPercent = bb.get() & 0xFF;       // unsigned byte
                if (bb.remaining() >= lenBmsStatus) bmsStatus = bb.getShort() & 0xFFFF;
                if (bb.remaining() >= lenBmsStatus1_2) bmsStatus1 = bb.getInt();
                if (bb.remaining() >= lenBmsStatus1_2) bmsStatus2 = bb.getInt();
                if (bb.remaining() >= lenErrorInfoData) errorInfoData = bb.getShort() & 0xFFFF;
                if (bb.remaining() >= lenReserve) reserveData = bb.getShort() & 0xFFFF;
            } else {
                log.warn("Port: [{}]. Payload is empty or null, updating with zeros", port);
            }
        } catch (Exception e) {
            log.error("Port: [{}]. Partial decode error for C0 payload", port, e);
        } finally {
            c0Data.updateC0Data(voltageMinV, voltageCurV, currentCurA, socPercent, bmsStatus,
                    bmsStatus1, bmsStatus2, errorInfoData, timestamp, payloadBytes);
        }
    }

    public static void decodeC1Payload(byte[] payloadBytes, UsrTcpWifiC1Data c1Data, Instant timestamp) {
        double socPercent = 0;
        int cellsCount = 0,  lifeCyclesCount = 0, errorInfoData = 0, reserveData = 0, majorVersion = 0, minorVersion = 0, cellsAllLen = 0;

        try {
            // TODO for test
//            String testHex =  "28100D100D140D120D140D110D140D130D120D140D140D130D130D130D150D9F0DA5040664200800000C10";
//            byte[] testBB = hexToBytes(testHex);
//            // 28   <10 <<0D10 0D14 0D12 0D14 0D11 0D14 0D13 0D12 0D14 0D14 0D13 0D13 0D13 0D15 0D9F 0DA5>> 0406 <<64>> <<2008>> 0000> 0C10
//            ByteBuffer bb = ByteBuffer.wrap(testBB);
            ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
            if (bb.remaining() >= lenCellsAllLen) cellsAllLen = bb.get() & 0xFF;
            byte[] cellsDataAll = new byte[cellsAllLen];
            if (bb.remaining() >= cellsAllLen) bb.get(cellsDataAll);
            if (bb.remaining() >= lenVerM) majorVersion = bb.get() & 0xFF;
            if (bb.remaining() >= lenVerM) minorVersion = bb.get() & 0xFF;

            ByteBuffer bbCellsAll = ByteBuffer.wrap(cellsDataAll);
            if (bbCellsAll.remaining() >= lenCellsCnt) cellsCount = bbCellsAll.get() & 0xFF;
            int lenCells = cellsCount * 2;
            byte[] cellsData = new byte[lenCells];
            if (bbCellsAll.remaining() >= lenCells) bbCellsAll.get(cellsData);
            if (bbCellsAll.remaining() >= lenLifeCyclesCount) lifeCyclesCount = bbCellsAll.getShort() & 0xFFFF;
            if (bbCellsAll.remaining() >= lenSocPercent) socPercent = bbCellsAll.get() & 0xFF;
            if (bbCellsAll.remaining() >= lenErrorInfoData) errorInfoData = bbCellsAll.getShort() & 0xFFFF;
            if (bbCellsAll.remaining() >= lenReserve) reserveData = bbCellsAll.getShort() & 0xFFFF;

            // Voltages cells
            Map<Integer, Float> cellVoltagesV = new ConcurrentHashMap<>();
            for (int i = 0; i < cellsCount; i++) {
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

//    public static void decodeC1Payload(byte[] payloadBytes, UsrTcpWifiC1Data c1Data, Instant timestamp) {
//
//        try {
//
//            int i = 0;
//
//            ByteBuffer bb = ByteBuffer.wrap(payloadBytes);
//
//            int cellsAllLen = payloadBytes[i] & 0xFF;
//
//            i++;
//
//            byte[] cellsDataAll = new byte[cellsAllLen];
//
//            System.arraycopy(payloadBytes, 0, cellsDataAll, 0, cellsAllLen);
//
//            i += cellsAllLen;
//
//            int majorVersion = payloadBytes[i] & 0xFF;
//
//            i += lenVerM;
//
//            int minorVersion = payloadBytes[i] & 0xFF;
//
//
//
//            ByteBuffer bbCellsAll = ByteBuffer.wrap(cellsDataAll);
//
//            i = 0;
//
//            i += lenCellsAllLen;
//
//            int cellsCount = cellsDataAll[i] & 0xFF;
//
//            i += lenCellsCnt;
//
//            int lenCells = cellsCount * 2;
//
//            byte[] cellsData = new byte[lenCells];
//
//            System.arraycopy(cellsDataAll, i, cellsData, 0, lenCells);
//
//            i += lenCells;
//
//            int lifeCyclesCount = (bbCellsAll.getShort(i) & 0xFFFF);
//
//            i += lenLifeCyclesCount;
//
//            double socPercent = cellsDataAll[i] & 0xFF;
//
//            i += lenSocPercent;
//
//            int errorInfoData = bb.getShort(i) & 0xFFFF;
//
//            i += lenErrorInfoData;
//
//
//
//// Voltages
//
//            Map<Integer, Float> cellVoltagesV = new ConcurrentHashMap<>();
//
//            for (i = 0; i < cellsCount; i++) {
//
//                int offset = i * 2;
//
//                int raw = ((cellsData[offset] & 0xFF) << 8) | (cellsData[offset + 1] & 0xFF);
//
//                float voltage = raw / 1000.0f;
//
//                cellVoltagesV.put(i, voltage);
//
//            }
//
//            c1Data.updateC1Data(cellsCount, cellVoltagesV, lifeCyclesCount, socPercent,
//
//                    errorInfoData, majorVersion, minorVersion, timestamp, payloadBytes);
//
//        } catch (Exception e) {
//
//            log.error("CRITICAL ERROR C1", e);
//
//        }
//
//    }
}

