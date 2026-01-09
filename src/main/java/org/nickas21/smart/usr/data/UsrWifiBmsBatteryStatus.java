package org.nickas21.smart.usr.data;

import lombok.Getter;

@Getter
public enum UsrWifiBmsBatteryStatus {

    CHARGING(1, "Charging"),
    DISCHARGING(2, "Discharging"),
    STATIC(4, "Static"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String status;

    UsrWifiBmsBatteryStatus(int code, String status) {
        this.code = code;
        this.status = status;
    }

    /**
     * Linear search — OK for small enums.
     */
    public static UsrWifiBmsBatteryStatus fromCode(int code) {
        for (UsrWifiBmsBatteryStatus s : values()) {
            if (s.code == code) return s;
        }
        return UNKNOWN;
    }

    /**
     * Human-friendly status lookup.
     */
    public static String fromStatusByCode(int code) {
        UsrWifiBmsBatteryStatus s = fromCode(code);
        return s == UNKNOWN
                ? String.format("UNKNOWN_STATUS (0x%02X)", code)
                : s.status;
    }
}
