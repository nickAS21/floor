package org.nickas21.smart.usr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.stream.IntStream;

@Data
@ConfigurationProperties("usr.tcp")
public class UsrTcpWiFiProperties {

    private Integer portStart = 8891;
    private Integer portsCnt = 14;
    private Integer portBatMasterGolego = 8891;
//    private Integer portBatGolegoCount = 8;
    private Integer portBatGolegoCount = 1;
    private Integer portInverterGolego = 8899;
    private Integer portInverterDacha = 8900;
    private Integer portInverterDachaCount = 2;
    private Integer portBatMasterDacha = 8902;
    private Integer portBatDachaCount = 3;

    public List<Integer> getAllPortsBatGolego() {
        return IntStream.range(0, portBatGolegoCount)
                .map(i -> portBatMasterGolego + i)
                .boxed()
                .toList();
    }

    public List<Integer> getAllPortsInverterDacha() {
        return IntStream.range(0, portInverterDachaCount)
                .map(i -> portInverterDacha + i)
                .boxed()
                .toList();
    }

    public List<Integer> getAllPortsBatDacha() {
        return IntStream.range(0, portBatDachaCount)
                .map(i -> portBatMasterDacha + i)
                .boxed()
                .toList();
    }

    // monitoring connect by port
    private Long monitorInactivityTimeOut = 1200000L; //20 * 60 * 1000; // 20 хвилин == 1200000
    private Long marginMs = 3660000L; //3660 * 1000L;
    private String checkRate = "60000";               // Частота перевірки (1 хв)
}
