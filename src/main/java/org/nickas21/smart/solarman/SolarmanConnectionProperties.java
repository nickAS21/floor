package org.nickas21.smart.solarman;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import static org.nickas21.smart.util.HttpUtil.getBodyHash;

@Data
@ConfigurationProperties("connector.solarman")
public class SolarmanConnectionProperties {
    private SolarmanRegion region = SolarmanRegion.IN;
    private String appid;
    private String secret;
    private String username;
    private String password;
    private String passwordHash;
    private String loggerSn;

    public String getPasswordHash() {
        return StringUtils.hasLength(passwordHash) ? passwordHash : getBodyHash(password);
    }

    public enum SolarmanRegion {

        /**
         * China
         */
        CN("https://api.solarmanpv.com"),
        /**
         * International
         */
        IN("https://globalapi.solarmanpv.com");

        private final String apiUrl;

        SolarmanRegion(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getApiUrl() {
            return apiUrl;
        }
    }
}
