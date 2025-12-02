package org.nickas21.smart.usr.unit;

public enum UsrTcpWifiStatus {

    CHARGING(1, "Charging"),
    DISCHARGING(2, "Discharging"),
    STATIC(4,  "Static");


    private final int code;
    private final String status;



    // Constructor
    UsrTcpWifiStatus(int code, String status) {
        this.code = code;
        this.status = status;
    }

    /**
     * Finds the enum member that matches the exact code value.
     * NOTE: This method is O(N) complexity as it iterates through all members.
     */
    public static UsrTcpWifiStatus fromCode(int code) {
        for (UsrTcpWifiStatus to : UsrTcpWifiStatus.values()) {
            if (to.code == code) {
                return to;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }


    public String getStatus() {
        return status;
    }

    public static String fromStatusByeCode(int code) {
        UsrTcpWifiStatus usrTcpWifiStatus = fromCode(code);
        return usrTcpWifiStatus == null ?  String.format("UNKNOWN_ERROR_CODE (0x%s})", Integer.toHexString(code)) : usrTcpWifiStatus.status;
    }
}
