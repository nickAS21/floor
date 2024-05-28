package org.nickas21.smart.security.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("connector.jwt")
public class JwtConnectionProperties {
    private String expiresIn;

}
