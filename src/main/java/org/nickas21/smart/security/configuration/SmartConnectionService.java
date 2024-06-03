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


    public String getSmartLogin() {
        return StringUtils.hasLength(this.smartConnectionProperties.getLogin()) ? this.smartConnectionProperties.getLogin() : this.solarmanConnectionProperties.getUsername();
    }

    public String getSmartPassword() {
        return StringUtils.hasLength(this.smartConnectionProperties.getPassword()) ? this.smartConnectionProperties.getPassword() : this.solarmanConnectionProperties.getSecret();
    }

}


