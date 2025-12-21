package org.nickas21.smart.usr.data.fault;

import lombok.Getter;

@Getter
public enum UsrTcpWifiFaultLogType {
    B1("BALANCE_ERROR"),
    E1("ERROR_CODE");

    private final String desc;

    UsrTcpWifiFaultLogType(String desc) {
        this.desc = desc;
    }

}

