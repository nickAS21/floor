package org.nickas21.smart.usr.data;

public enum ErrorLogType {
    B1("BALANCE_ERROR"),
    E1("ERROR_CODE");

    private final String desc;

    ErrorLogType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}

