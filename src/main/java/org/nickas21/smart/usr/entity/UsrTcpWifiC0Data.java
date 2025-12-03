package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiC0Data {

    private double voltageMinV;
    private double voltageCurV;
    private double currentA;
    private int soc;

    private int bmsStatus;
    private long bmsStatus1;
    private long bmsStatus2;
    private long errorInfoData;
    private String errorOutput;

    private Instant timestamp;
}
