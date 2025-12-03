package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiC1Data {

    private int cellsCount;
    private List<Float> cellVoltagesV = new ArrayList<>();

    private double lifeCyclesCount;
    private int socPercent;

    private long errorData;

    private int majorVersion;
    private int minorVersion;

    private Instant timestamp;  // отримано разом з пакетом

    public String getVersionString() {
        return "V%02d%02d".formatted(majorVersion, minorVersion);
    }
}
