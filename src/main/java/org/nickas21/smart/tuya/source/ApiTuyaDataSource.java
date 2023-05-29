package org.nickas21.smart.tuya.source;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.constant.TuyaRegion;
import org.nickas21.smart.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.nickas21.smart.util.EnvConstant.ENV_REGION;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_AK;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_CATEGORY_FOR_CONTROL_POWERS;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_DEVICE_IDS;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_SK;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_TEMP_SET_MAX;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_TEMP_SET_MIN;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_USER_UID;
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
    @Value("${connector.tuya.region:}")
    private String tuyaRegion;

    @Value("${connector.tuya.ak:}")
    private String tuyaAk;

    @Value("${connector.tuya.sk:}")
    private String tuyaSk;

    @Value("${connector.tuya.device_ids:}")
    private String tuyaDeviceIds;

    @Value("${connector.tuya.user_uid:}")
    private String tuyaUserUid;

    @Value("${smart.tuya.temp_set.min:5}")
    private int tuyaTempSetMin;

    @Value("${smart.tuya.temp_set.max:24}")
    private int tuyaTempSetMax;

    @Value("${smart.tuya.category_for_control_powers:}")
    private String tuyaCategoryForControlPowers;

    private TuyaMessageDataSource tuyaMessageDataSource;


    public TuyaMessageDataSource getTuyaConnectionConfiguration() {
        String akConf = null;
        String skConf = null;
        String[] deviceIds = null;
        String userUidConf = null;
        TuyaRegion region = null;
        if (tuyaMessageDataSource != null) {
            return tuyaMessageDataSource;
        } else {
            try {
                akConf = envSystem.get(ENV_TUYA_AK);
                akConf = StringUtils.isBlank(akConf) ? this.tuyaAk : akConf;
                skConf = envSystem.get(ENV_TUYA_SK);
                skConf = StringUtils.isBlank(skConf) ? this.tuyaSk : skConf;
                userUidConf = envSystem.get(ENV_TUYA_USER_UID);
                userUidConf = StringUtils.isBlank(userUidConf) ? this.tuyaUserUid : userUidConf;

                String reConf = envSystem.get(ENV_REGION);
                reConf = StringUtils.isBlank(reConf) ? tuyaRegion : reConf;
                region = StringUtils.isNotBlank(reConf) ? TuyaRegion.valueOf(reConf) : null;

                String devIdsConf = envSystem.get(ENV_TUYA_DEVICE_IDS);
                devIdsConf = StringUtils.isNotBlank(devIdsConf) ?devIdsConf : this.tuyaDeviceIds;
                deviceIds = StringUtils.isNotBlank(devIdsConf) ? StringUtils.split(devIdsConf, ",     ") : null;

                String tempSetMinConfStr = envSystem.get(ENV_TUYA_TEMP_SET_MIN);
                Integer tempSetMinConf = StringUtils.isBlank(tempSetMinConfStr) ? this.tuyaTempSetMin : Integer.valueOf(tempSetMinConfStr);

                String tempSetMaxConfStr = envSystem.get(ENV_TUYA_TEMP_SET_MAX);
                Integer tempSetMaxConf = StringUtils.isBlank(tempSetMaxConfStr) ? this.tuyaTempSetMax : Integer.valueOf(tempSetMaxConfStr);

                String categoryForControlPowersConf = envSystem.get(ENV_TUYA_CATEGORY_FOR_CONTROL_POWERS);
                categoryForControlPowersConf = StringUtils.isNotBlank(categoryForControlPowersConf) ? categoryForControlPowersConf : this.tuyaCategoryForControlPowers;
                String [] categoryForControlPowers = StringUtils.isNotBlank(categoryForControlPowersConf) ? StringUtils.split(categoryForControlPowersConf, ",") : null;

                if (StringUtils.isNotBlank(akConf) && StringUtils.isNotBlank(skConf)
                        && StringUtils.isArrayNoneBlank(deviceIds) && region != null) {
                    tuyaMessageDataSource = TuyaMessageDataSource.builder()
                            .region(region)
                            .ak(akConf)
                            .sk(skConf)
                            .deviceIds(deviceIds)
                            .userUid(userUidConf)
                            .tempSetMin(tempSetMinConf)
                            .tempSetMax(tempSetMaxConf)
                            .categoryForControlPowers(categoryForControlPowers)
                            .build();
                    return tuyaMessageDataSource;
                } else {
                    log.error("Incorrect or null parameters when processing Tuya. {}:[{}], {}:[{}], {}:[{}], {}:[{}], {}:[{}]", ENV_TUYA_AK, akConf,
                            ENV_TUYA_SK, skConf, ENV_TUYA_DEVICE_IDS, deviceIds, ENV_TUYA_USER_UID, userUidConf, ENV_REGION, region);
                    return null;
                }
            } catch (Exception e) {
                log.error("Invalid parameters when processing Tuya. {}:[{}], {}:[{}], {}:[{}], {}:[{}], {}:[{}]", ENV_TUYA_AK, akConf,
                        ENV_TUYA_SK, skConf, ENV_TUYA_DEVICE_IDS, deviceIds, ENV_TUYA_USER_UID, userUidConf, ENV_REGION, region);
                log.error("During processing Tuya connection error.[{}]", e.getMessage());
                return null;
            }
        }
    }
}

