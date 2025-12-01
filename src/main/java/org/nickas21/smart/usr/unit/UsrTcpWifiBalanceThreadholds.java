package org.nickas21.smart.usr.unit;

public enum UsrTcpWifiBalanceThreadholds {

    EXCELLENT_MAX(10, "Excellent", "BALANCE CELLS is very good"),
    GOOD_MAX(20, "Good", "BALANCE CELLS is good"),
    WARN_MAX(50,  "Warning", "WARNING: BALANCE CELLS is not good"),
    CRITICAL_MAX(80, "Critical","CRITICAL: <= 80 mV, monitor for damaged cells");


    private final int code;
    private final String state;
    private final String description;



    // Constructor
    UsrTcpWifiBalanceThreadholds(int code, String state, String description) {
        this.code = code;
        this.state = state;
        this.description = description;
    }

    /**
     * Finds the enum member that matches the exact code value.
     * NOTE: This method is O(N) complexity as it iterates through all members.
     */
    public static UsrTcpWifiBalanceThreadholds fromCode(int code) {
        for (UsrTcpWifiBalanceThreadholds to : UsrTcpWifiBalanceThreadholds.values()) {
            if (to.code == code) {
                return to;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }


    public String getDescription() {
        return description;
    }

    /**
     * Determines the cell balance status based on the voltage difference (V_max - V_min).
     */
    public static String getBalanceStatus(int delta_mV) {
        if (delta_mV < 0) return "ERROR - Invalid delta_mV (must be non-negative)";
        else if (delta_mV <= EXCELLENT_MAX.code) return EXCELLENT_MAX.state;
        else if (delta_mV <= GOOD_MAX.code) return GOOD_MAX.state;
        else if (delta_mV <= WARN_MAX.code) return WARN_MAX.state;
        else if (delta_mV <= CRITICAL_MAX.code) return CRITICAL_MAX.state;
        else return "Dangerous, Emergency Shutdown";
    }
}