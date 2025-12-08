package org.nickas21.smart.usr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("usr.tcp")
public class UsrTcpWiFiProperties {
    private Integer portStart = 8891;
    private Integer batteriesCnt = 8;
    private String logsDir = "./logs/";
    private String logsErrorPrefix = "usrTcpWiFiError";
    private String logsTodayPrefix = "usrTcpWiFiToday";
    private String logsYesterdayPrefix = "usrTcpWiFiYesterday";
    private Integer logsErrorLimit = 2000;
    private Integer offline = 120;
    private Integer removeAll = 259200; //   259200 - 3 days
}
