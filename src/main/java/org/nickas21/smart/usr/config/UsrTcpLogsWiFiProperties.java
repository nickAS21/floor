package org.nickas21.smart.usr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("usr.tcp.logs")
public class UsrTcpLogsWiFiProperties {
    private String dir = "./logs/";
    private String errorPrefix = "usrTcpWiFiError";
    private String todayPrefix = "usrTcpWiFiToday";
    private String yesterdayPrefix = "usrTcpWiFiYesterday";
    private Long writeInterval = 480000L;   // 8 min
    private Long activeInterval = 1800000L; // 30 min
    private Integer errorLimit = 2000;
}
