package org.nickas21.smart.tuya.sourece;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.TuyaMessageDataSource;
import org.nickas21.smart.tuya.constant.TuyaRegion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_AK;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_REGION;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_SK;
import static org.nickas21.smart.tuya.constant.TuyaApi.envSystem;

@Slf4j
@Configuration
public class ApiDataSource {

    /**
     * application.properties
     * connector.ak=
     * connector.sk=
     * connector.region=
     */
    @Value("${connector.region:}")
    public String region;

    @Value("${connector.ak:}")
    public String ak;

    @Value("${connector.sk:}")
    public String sk;

    private TuyaMessageDataSource tuyaMessageDataSource;


    public TuyaMessageDataSource getTuyaConnectionConfiguration() {
        if (tuyaMessageDataSource != null) {
            return tuyaMessageDataSource;
        } else {
            try {
                String akConf = envSystem.get(ENV_AK);
                String skConf = envSystem.get(ENV_SK);
                String reConf = envSystem.get(ENV_REGION);

                TuyaRegion region = (reConf != null && reConf.isBlank()) ? TuyaRegion.valueOf(reConf) : null;
                if (akConf == null || akConf.isEmpty()
                        || skConf == null || skConf.isEmpty() || region == null) {
                    akConf = this.ak;
                    skConf = this.sk;
                    reConf = this.region;
                    region = (reConf != null && !reConf.isEmpty()) ? TuyaRegion.valueOf(reConf) : null;
                }
                if (akConf != null && !akConf.isEmpty() && skConf != null && !skConf.isEmpty() && region != null) {
                    String url = region.getMsgUrl();
                    tuyaMessageDataSource = TuyaMessageDataSource.builder()
                            .url(url)
                            .ak(akConf)
                            .sk(skConf)
                            .build();
                    return tuyaMessageDataSource;
                } else {
                    return null;
                }
            } catch (Exception e) {
                log.error("During processing Tuya connection error.[{}]", e.getMessage());
                return null;
            }
        }
    }

}
