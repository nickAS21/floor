package org.nickas21.smart.usr.data;

public enum ErrorCode {
    TEMP_LOW(ErrorLevel.CRITICAL),
    SOC_LOW(ErrorLevel.WARN),
    SOC_CRITICAL(ErrorLevel.CRITICAL),
    CELL_IMBALANCE(ErrorLevel.ALARM),
    COMMUNICATION_LOST(ErrorLevel.ALARM),
    SYSTEM_OVERLOAD(ErrorLevel.CRITICAL);

    private final ErrorLevel level;
    ErrorCode(ErrorLevel level) { this.level = level; }
    public ErrorLevel getLevel() { return level; }
}
