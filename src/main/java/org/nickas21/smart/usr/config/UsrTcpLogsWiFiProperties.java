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
    private Long writeInterval = 1800000L;
    private Integer errorLimit = 2000;
}
