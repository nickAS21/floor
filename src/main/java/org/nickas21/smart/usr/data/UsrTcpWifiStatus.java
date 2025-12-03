package org.nickas21.smart.usr.data;

public enum UsrTcpWifiStatus {

    CHARGING(1, "Charging"),
    DISCHARGING(2, "Discharging"),
    STATIC(4, "Static"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String status;

    UsrTcpWifiStatus(int code, String status) {
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Linear search — OK for small enums.
     */
    public static UsrTcpWifiStatus fromCode(int code) {
        for (UsrTcpWifiStatus s : values()) {
            if (s.code == code) return s;
        }
        return UNKNOWN;
    }

    /**
     * Human-friendly status lookup.
     */
    public static String fromStatusByCode(int code) {
        UsrTcpWifiStatus s = fromCode(code);
        return s == UNKNOWN
                ? String.format("UNKNOWN_STATUS (0x%02X)", code)
                : s.status;
    }
}
