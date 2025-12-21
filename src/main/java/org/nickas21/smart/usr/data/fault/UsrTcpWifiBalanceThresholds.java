package org.nickas21.smart.usr.data.fault;

import lombok.Getter;

@Getter
public enum UsrTcpWifiBalanceThresholds {

    EXCELLENT_MAX(20, "Excellent", "BALANCE CELLS is very good"),
    GOOD_MAX(40, "Good", "BALANCE CELLS is good"),
    WARN_MAX(80, "Warning", "WARNING: BALANCE CELLS is not good"),
    CRITICAL_MAX(100, "Critical", "BALANCE CELLS deviation approaching recovery limit"),
    AUTO_RECOVERABLE_MAX(200, "Auto-Recoverable", "0x1xxx warning, issue will recover by itself"),
    SERVICE_REQUIRED_MAX(Integer.MAX_VALUE, "Service Required", "0x2xxx / 0x3xxx, contact service");

    private final int maxMv;
    private final String state;
    private final String description;

    UsrTcpWifiBalanceThresholds(int maxMv, String label, String description) {
        this.maxMv = maxMv;
        this.state = label;
        this.description = description;
    }

    /**
     * Selects the severity level based on mV difference.
     */
    public static UsrTcpWifiBalanceThresholds getBalanceStatus(int delta_mV) {
        if (delta_mV < 0) {
            throw new IllegalArgumentException("delta_mV must be non-negative");
        }

        if (delta_mV <= EXCELLENT_MAX.maxMv) return EXCELLENT_MAX;
        if (delta_mV <= GOOD_MAX.maxMv) return GOOD_MAX;
        if (delta_mV <= WARN_MAX.maxMv) return WARN_MAX;
        if (delta_mV <= CRITICAL_MAX.maxMv) return CRITICAL_MAX;
        if (delta_mV <= AUTO_RECOVERABLE_MAX.maxMv) return AUTO_RECOVERABLE_MAX;

        return SERVICE_REQUIRED_MAX;
    }
}
