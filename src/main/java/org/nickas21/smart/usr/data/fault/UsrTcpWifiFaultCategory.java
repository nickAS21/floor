package org.nickas21.smart.usr.data.fault;

public enum UsrTcpWifiFaultCategory {
    WARNING(0x1000, "WARNING (Auto-recoverable)"),
    PROTECTION(0x2000, "PROTECTION (Service required)"),
    COMMUNICATION(0x3000, "COMMUNICATION FAULT"),
    UNKNOWN(0x0000, "UNKNOWN CATEGORY");

    private final int mask;
    private final String description;

    UsrTcpWifiFaultCategory(int mask, String description) {
        this.mask = mask;
        this.description = description;
    }

    public static UsrTcpWifiFaultCategory fromCode(int code) {
        int highNibble = code & 0xF000;
        for (UsrTcpWifiFaultCategory c : values()) {
            if (c.mask == highNibble) {
                return c;
            }
        }
        return UNKNOWN;
    }

    public String getDescription() {
        return description;
    }
}