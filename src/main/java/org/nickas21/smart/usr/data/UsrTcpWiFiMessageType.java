package org.nickas21.smart.usr.data;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum UsrTcpWiFiMessageType {

    //  no parse
    T_A2((byte) 0xA2, "BMS Version A2",1),
    T_D0((byte) 0xD0, "BMS Id Ident D0",1),
    T_21((byte) 0x21, "BMS Version 21",1),


    T_02((byte) 0x02, "BMS Id Ident 02",2),
    T_03((byte) 0x03, "BMS Id Ident 03",2),


    T_C0((byte) 0xC0, "BMS General Status1", 3),

    T_C1((byte) 0xC1, "BMS Cell Voltages", 4),

    T_00((byte) 0x00, "BMS Version 00",5),
    T_01((byte) 0x01, "BMS Version 01",5),

    T_05((byte) 0x05, "BMS Version 05",5),

    T_32((byte) 0x32, "BMS Version 32",5),

    UNKNOWN((byte) 0xFF, "Unknown", 0);

    private final byte code;
    private final String description;
    private final int groupBms;

    UsrTcpWiFiMessageType(byte code, String description, int groupBms) {
        this.code = code;
        this.description = description;

        this.groupBms = groupBms;
    }

    private static final Map<Byte, UsrTcpWiFiMessageType> LOOKUP = new HashMap<>();

    static {
        for (UsrTcpWiFiMessageType t : values()) {
            LOOKUP.put(t.code, t);
        }
    }

    public static UsrTcpWiFiMessageType fromByte(byte b) {
        return LOOKUP.getOrDefault(b, UNKNOWN);
    }
}
