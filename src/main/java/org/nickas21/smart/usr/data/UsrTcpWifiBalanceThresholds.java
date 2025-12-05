package org.nickas21.smart.usr.data;

import lombok.Getter;

@Getter
public enum UsrTcpWifiBalanceThresholds {

    EXCELLENT_MAX(10, "Excellent", "BALANCE CELLS is very good"),
    GOOD_MAX(20, "Good", "BALANCE CELLS is good"),
    WARN_MAX(50, "Warning", "WARNING: BALANCE CELLS is not good"),
    CRITICAL_LIMIT(80, "Critical","CRITICAL_LIMIT: <= 80 mV, monitor for damaged cells"),
    EMERGENCY_MAX(Integer.MAX_VALUE, "Emergency", "DANGEROUS: Emergency Shutdown Recommended");


    private final int code;
    private final String state;
    private final String description;

    UsrTcpWifiBalanceThresholds(int code, String state, String description) {
        this.code = code;
        this.state = state;
        this.description = description;
    }

    /**
     * Selects the severity level based on mV difference.
     */
    public static UsrTcpWifiBalanceThresholds getBalanceStatus(int delta_mV) {
        if (delta_mV < 0) {
            throw new IllegalArgumentException("delta_mV must be non-negative");
        }

        if (delta_mV <= EXCELLENT_MAX.code) return EXCELLENT_MAX;
        if (delta_mV <= GOOD_MAX.code) return GOOD_MAX;
        if (delta_mV <= WARN_MAX.code) return WARN_MAX;
        if (delta_mV <= CRITICAL_LIMIT.code) return CRITICAL_LIMIT;

        return EMERGENCY_MAX;
    }
}
