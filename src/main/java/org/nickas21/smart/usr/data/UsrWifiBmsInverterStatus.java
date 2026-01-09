package org.nickas21.smart.usr.data;

import lombok.Getter;

@Getter
public enum UsrWifiBmsInverterStatus {

    CHARGING(4, "Charging"),
    DISCHARGING(3, "Discharging"),
    UNKNOWN(-1, "Unknown");

    private final Integer code;
    private final String status;

    UsrWifiBmsInverterStatus(Integer code, String status) {
        this.code = code;
        this.status = status;
    }

    /**
     * Linear search — OK for small enums.
     */
    public static UsrWifiBmsInverterStatus fromCode(Integer code) {
        for (UsrWifiBmsInverterStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return UNKNOWN;
    }

    /**
     * Human-friendly status lookup.
     */
    public static String fromStatusByCode(Integer code) {
        UsrWifiBmsInverterStatus s = fromCode(code);
        return s == UNKNOWN
                ? String.format("UNKNOWN_STATUS (0x%02X)", code)
                : s.status;
    }
}
