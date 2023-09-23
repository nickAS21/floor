package org.nickas21.smart.tuya;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("smart.tuya")
public class TuyaDeviceProperties {
    private Integer tempSetMin = 5;
    private Integer tempSetMax = 24;
    private String[] categoryForControlPowers = {"wk"};
}
