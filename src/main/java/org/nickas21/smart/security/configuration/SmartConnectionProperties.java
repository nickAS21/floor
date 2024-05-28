package org.nickas21.smart.security.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("connector.smart")
public class SmartConnectionProperties {
    private String userLogin;
    private String userPassword;
    private String adminLogin;
    private String adminPassword;


}
