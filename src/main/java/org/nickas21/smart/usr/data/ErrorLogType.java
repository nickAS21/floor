package org.nickas21.smart.usr.data;

import lombok.Getter;

@Getter
public enum ErrorLogType {
    B1("BALANCE_ERROR"),
    E1("ERROR_CODE");

    private final String desc;

    ErrorLogType(String desc) {
        this.desc = desc;
    }

}

