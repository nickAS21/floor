package org.nickas21.smart.tuya.sourece;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.util.StringUtils;
import org.nickas21.smart.tuya.TuyaMessageDataSource;
import org.nickas21.smart.tuya.constant.TuyaRegion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_AK;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_DEVICE_IDS;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_REGION;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_SK;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_USER_UID;
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

    @Value("${connector.device_ids:}")
    public String deviceIds;

    @Value("${connector.user_uid:}")
    public String userUid;

    private TuyaMessageDataSource tuyaMessageDataSource;


    public TuyaMessageDataSource getTuyaConnectionConfiguration() {
        if (tuyaMessageDataSource != null) {
            return tuyaMessageDataSource;
        } else {
            try {
                String akConf = envSystem.get(ENV_AK);
                String skConf = envSystem.get(ENV_SK);
                String reConf = envSystem.get(ENV_REGION);
                String devIdsConf = envSystem.get(ENV_DEVICE_IDS);
                String userUidConf = envSystem.get(ENV_USER_UID);

                TuyaRegion region = StringUtils.isNoneBlank(reConf) ? TuyaRegion.valueOf(reConf) : null;
                String [] deviceIds = StringUtils.isNoneBlank(devIdsConf) ? StringUtils.split(devIdsConf, ",     ") : null;

                if (StringUtils.isBlank(akConf) || StringUtils.isBlank(skConf)
                        || StringUtils.isArrayBlank(deviceIds) || StringUtils.isBlank(userUidConf) || region == null) {
                    akConf = this.ak;
                    skConf = this.sk;
                    reConf = this.region;
                    devIdsConf = this.deviceIds;
                    userUidConf = this.userUid;
                    region = (reConf != null && !reConf.isEmpty()) ? TuyaRegion.valueOf(reConf) : null;
                    deviceIds = (devIdsConf != null && devIdsConf.isBlank()) ? StringUtils.split(devIdsConf, ",") : null;
                }
                if (StringUtils.isNoneBlank(akConf) && StringUtils.isNoneBlank(skConf)
                        && StringUtils.isArrayNoneBlank(deviceIds) && StringUtils.isNoneBlank(userUidConf) && region != null) {
                    tuyaMessageDataSource = TuyaMessageDataSource.builder()
                            .region(region)
                            .ak(akConf)
                            .sk(skConf)
                            .deviceIds(deviceIds)
                            .userUid(userUidConf )
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
