package org.nickas21.smart.usr.unit;

public final class UsrTcpWiFiUtils {


    // Defaults & constants (as in Python)
    public static final int DEFAULT_PORT = 8898;
    public static final byte[] START_SIGN = new byte[]{ (byte)0xAA, 0x55 };
    public static final int ID_LENGTH = 19;
    public static final int MIN_PACKET_LENGTH = START_SIGN.length + 1 + ID_LENGTH + 2;

    // You used same expected ID hex for both in Python; we keep for reference (not used for strict checking here)
    public static final String EXPECTED_ID_B_HEX = "31343133413037424C444F5047303039303031";    // 1413A07BLDOPG009001
    public static final String EXPECTED_ID_S_HEX = "31343133413037534C444F5047303039303031";    // 1413A07SLDOPG009001

    // All
    // START_SIGN [2] + TYPE[2] + EXPECTED_ID [19] + PAYLOAD_DATA + CRC [2]
    // A2, D0, C0 - EXPECTED_ID_S_HEX
    // C1 - EXPECTED_ID_B_HEX

    // D0 - Identifier
    // - Payload: null -> only  EXPECTED_ID_S_HEX

    // A2 - Ver

    //- Payload[2]: Ver[2]

    // С0 - BMS General Status - AA55 C0 31343133413037534C444F5047303039303031 1400 14AA FFF7 5A 0004 00000002 00000003 00 00 00 00 C2CB
    /* - Payload: 1400 14AA FFF7 5A 0004 00000002 00000003 000000 00
        "--- DETAILS DECODE C0 (BMS General Status) ---",
        - Voltage Min (V)  [2]V - "+"int/100
        - Voltage (V)      [2]V - "+"int/100
        - Current (A)      [2]A - "+-" int/10
        - SOC (%)          [1]%, - "+" int
        - All info Data    [14], =>  info Data [10], Error info Data  [3] - Enum, Reserve [1]
        - info Data => BMS status [2] - Enum, BMS status1 [4], BMS status2 [4]
     */

    // С1 - BMS Cell Status - AA55 C1 1343133413037424C444F5047303039303031 28100CE60CE70CE70CE90CE80CE80CE70CE70CE80CE70CE70CE80CE80CE70CE70CE703DE59000000000C0C DBD6
    /* Pyload: 28 10 0CE5 0CE6 0CE7 0CE8 0CE7 0CE8 0CE7 0CE8 0CE7 0CE6 0CE7 0CE7 0CE8 0CE7 0CE7 0CE5 03DE 59 000000 00 0C0C
        - cells_all_len     [1] = 0x28 - 40 - "+"int == start[2] + cells_v[32] + life_cycles_cnt[2] + soc[1] + Error info Data[3]
        - cells_cnt         [1] = 16        - "+"int
        - cells_v           [32]            - "+"int
        - life_cycles_cnt   [2]             - "+"int
        - soc               [1]             - "+"int
        - Error info Data   [3]             - Enum
        - Last:
        - Reserve           [1]
        - ver               [2]
     */

    public static int parseBytesToIntBigEndian(byte[] data, int offset, int length) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (data[offset + i] & 0xFF);
        }
        return value;
    }


    // Utility: bytes -> HEX uppercase
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }


}
