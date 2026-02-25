package org.nickas21.smart.usr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("usr.tcp")
public class UsrTcpWiFiProperties {

    private Integer portStart = 8891;
    private Integer batteriesCnt = 14;
    private Integer portMaster = 8898;
    private Integer portInverterGolego = 8899;
    private Integer portInverterDacha = 8900;

    // monitoring connect by port
    private Long monitorInactivityTimeOut = 1200000L; //20 * 60 * 1000; // 20 хвилин == 1200000
    private Long marginMs = 3660000L; //3660 * 1000L;
    private String checkRate = "60000";               // Частота перевірки (1 хв)
}
