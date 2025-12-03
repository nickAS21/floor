package org.nickas21.smart.usr.data;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UsrTcpWifiCrcUtilities {

    /**
     * Converts integer to a 4-digit uppercase hex string (zero-padded).
     */
    public static String toHex16(int value) {
        return String.format("%04X", value & 0xFFFF);
    }

    /**
     * Calculates CRC-16/MODBUS (LSB-first) as in your Python implementation.
     * Poly reflected: 0xA001 (reflected of 0x8005), init 0x0000.
     */
    public static int calculateCrc16Modbus(byte[] data) {
        int crc = 0x0000;
        int polyReflected = 0xA001;

        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ polyReflected;
                } else {
                    crc = (crc >>> 1);
                }
                crc &= 0xFFFF;
            }
        }
        return crc & 0xFFFF;
    }

    /**
     * Checks CRC for the full packet.
     * Rule: CRC computed from entire packet excluding the last 2 CRC bytes (starting from AA55).
     * typeFrameHex is used only for message formatting to match original behaviour.
     */
    public static String checkPacketCrc(byte[] fullPacket, String typeFrameHex) {
        if (fullPacket == null || fullPacket.length < 4) {
            return "CRC Status: BAD. Message: Packet is too short for CRC.";
        }
        int len = fullPacket.length;
        byte[] expectedCrcBytes = new byte[2];
        expectedCrcBytes[0] = fullPacket[len - 2];
        expectedCrcBytes[1] = fullPacket[len - 1];

        String expectedCrcHex = toHex16(((expectedCrcBytes[0] & 0xFF) << 8) | (expectedCrcBytes[1] & 0xFF));

        // data for CRC = fullPacket without last 2 CRC bytes
        byte[] dataForCrc = new byte[len - 2];
        System.arraycopy(fullPacket, 0, dataForCrc, 0, len - 2);

        if (dataForCrc.length == 0) {
            return "CRC Status: N/A. Message: Payload/ID is empty.";
        }

        int calcCrcInt = calculateCrc16Modbus(dataForCrc);
        byte[] calcCrcBytes = new byte[2];
        // Python code compared big-endian bytes (presented big-endian in AoBo).
        calcCrcBytes[0] = (byte) ((calcCrcInt >> 8) & 0xFF);
        calcCrcBytes[1] = (byte) (calcCrcInt & 0xFF);
        String calcCrcHex = toHex16(calcCrcInt);

        boolean equal = (calcCrcBytes[0] == expectedCrcBytes[0] && calcCrcBytes[1] == expectedCrcBytes[1]);

        if (equal) {
            return "CRC Status: OK. Calc/Expected: " + calcCrcHex;
        } else {
            return "CRC Status: BAD. Calc: " + calcCrcHex + " != Expected: " + expectedCrcHex;
        }
    }
}
