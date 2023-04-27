package org.nickas21.smart.tuya.source;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.util.StringUtils;
import org.nickas21.smart.tuya.constant.TuyaRegion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.nickas21.smart.util.EnvConstant.ENV_AK;
import static org.nickas21.smart.util.EnvConstant.ENV_DEVICE_IDS;
import static org.nickas21.smart.util.EnvConstant.ENV_REGION;
import static org.nickas21.smart.util.EnvConstant.ENV_SK;
import static org.nickas21.smart.util.EnvConstant.ENV_USER_UID;
import static org.nickas21.smart.util.EnvConstant.envSystem;

@Slf4j
@Component
public class ApiTuyaDataSource {

    /**
     * application.properties
     * connector.ak=
     * connector.sk=
     * connector.region=
     */
    @Value("${connector.tuya.region}")
    public String tuyaRegion;

    @Value("${connector.tuya.ak}")
    public String tuyaAk;

    @Value("${connector.tuya.sk}")
    public String tuyaSk;

    @Value("${connector.tuya.device_ids}")
    public String tuyaDeviceIds;

    @Value("${connector.tuya.user_uid}")
    public String tuyaUserUid;

    private TuyaMessageDataSource tuyaMessageDataSource;


    public TuyaMessageDataSource getTuyaConnectionConfiguration() {
        if (tuyaMessageDataSource != null) {
            return tuyaMessageDataSource;
        } else {
            try {
                String akConf = envSystem.get(ENV_AK);
                akConf = StringUtils.isBlank(akConf) ? this.tuyaAk : akConf;
                String skConf = envSystem.get(ENV_SK);
                skConf = StringUtils.isBlank(skConf) ? this.tuyaSk : skConf;
                String userUidConf = envSystem.get(ENV_USER_UID);
                userUidConf = StringUtils.isBlank(userUidConf) ? this.tuyaUserUid : userUidConf;

                String reConf = envSystem.get(ENV_REGION);
                reConf = StringUtils.isBlank(reConf) ? this.tuyaUserUid : reConf;
                TuyaRegion region = StringUtils.isNoneBlank(reConf) ? TuyaRegion.valueOf(reConf) : null;

                String devIdsConf = envSystem.get(ENV_DEVICE_IDS);
                String[] deviceIds = StringUtils.isNoneBlank(devIdsConf) ? StringUtils.split(devIdsConf, ",     ") : null;
                if (StringUtils.isArrayBlank(deviceIds)) {
                    devIdsConf = this.tuyaDeviceIds;
                    deviceIds = (devIdsConf != null && devIdsConf.isBlank()) ? StringUtils.split(devIdsConf, ",") : null;
                }

                if (StringUtils.isNoneBlank(akConf) && StringUtils.isNoneBlank(skConf)
                        && StringUtils.isArrayNoneBlank(deviceIds) && StringUtils.isNoneBlank(userUidConf) && region != null) {
                    tuyaMessageDataSource = TuyaMessageDataSource.builder()
                            .region(region)
                            .ak(akConf)
                            .sk(skConf)
                            .deviceIds(deviceIds)
                            .userUid(userUidConf)
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
