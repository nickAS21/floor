package org.nickas21.smart.usr.data;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum UsrTcpWiFiMessageType {

    A2((byte) 0xA2, "BMS Version"),
    D0((byte) 0xD0, "BMS Id Ident"),
    C0((byte) 0xC0, "BMS General Status"),
    C1((byte) 0xC1, "BMS Cell Voltages"),
    UNKNOWN((byte) 0x00, "Unknown"); // код не використовується

    private final byte code;
    private final String description;

    UsrTcpWiFiMessageType(byte code, String description) {
        this.code = code;
        this.description = description;
    }

    private static final Map<Byte, UsrTcpWiFiMessageType> LOOKUP = new HashMap<>();

    static {
        for (UsrTcpWiFiMessageType t : values()) {
            if (t != UNKNOWN) {
                LOOKUP.put(t.code, t);
            }
        }
    }

    public static UsrTcpWiFiMessageType fromByte(byte b) {
        return LOOKUP.getOrDefault(b, UNKNOWN);
    }
}
