package org.nickas21.smart.usr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("usr.tcp")
public class UsrTcpWiFiProperties {

    private Integer portStart = 8891;
    private Integer batteriesCnt = 8;
    private Integer portMaster = 8898;
}
