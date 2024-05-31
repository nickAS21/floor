package org.nickas21.smart.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.SolarmanConnectionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@EnableConfigurationProperties({SolarmanConnectionProperties.class, SmartConnectionProperties.class})
public class SmartConnectionService {
    private final SolarmanConnectionProperties solarmanConnectionProperties;
    private final SmartConnectionProperties smartConnectionProperties;

    public SmartConnectionService(SolarmanConnectionProperties solarmanConnectionProperties, SmartConnectionProperties smartConnectionProperties) {
        this.solarmanConnectionProperties = solarmanConnectionProperties;
        this.smartConnectionProperties = smartConnectionProperties;
     }


    public String getUserLogin() {
        return StringUtils.hasLength(this.smartConnectionProperties.getUserLogin()) ? this.smartConnectionProperties.getUserLogin() : this.solarmanConnectionProperties.getUsername();
    }

    public String getUserPassword() {
        return StringUtils.hasLength(this.smartConnectionProperties.getUserPassword()) ? this.smartConnectionProperties.getUserPassword() : this.solarmanConnectionProperties.getSecret();
    }

    public String getAdminLogin() {
        return StringUtils.hasLength(this.smartConnectionProperties.getAdminLogin()) ? this.smartConnectionProperties.getAdminLogin() : this.solarmanConnectionProperties.getUsername();
    }

    public String getAdminPassword() {
        return StringUtils.hasLength(this.smartConnectionProperties.getAdminPassword()) ? this.smartConnectionProperties.getAdminPassword() : this.solarmanConnectionProperties.getSecret();
    }

}


