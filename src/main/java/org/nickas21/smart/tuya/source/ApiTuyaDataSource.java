package org.nickas21.smart.tuya.source;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.util.StringUtils;
import org.nickas21.smart.tuya.constant.TuyaRegion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_BMS_SOC_ALARM_ERROR;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_AK;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_CATEGORY_FOR_CONTROL_POWERS;
import static org.nickas21.smart.util.EnvConstant.ENV_TUYA_DEVICE_IDS;
import static org.nickas21.smart.util.EnvConstant.ENV_REGION;
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
    @Value("${connector.tuya.region}")
    private String tuyaRegion;

    @Value("${connector.tuya.ak}")
    private String tuyaAk;

    @Value("${connector.tuya.sk}")
    private String tuyaSk;

    @Value("${connector.tuya.device_ids}")
    private String tuyaDeviceIds;

    @Value("${connector.tuya.user_uid}")
    private String tuyaUserUid;

    @Value("${smart.tuya.temp_set.min}")
    private Integer tuyaTempSetMin;

    @Value("${smart.tuya.temp_set.max}")
    private Integer tuyaTempSetMax;

    @Value("${smart.tuya.category_for_control_powers}")
    private String tuyaCategoryForControlPowers;

    private TuyaMessageDataSource tuyaMessageDataSource;


    public TuyaMessageDataSource getTuyaConnectionConfiguration() {
        if (tuyaMessageDataSource != null) {
            return tuyaMessageDataSource;
        } else {
            try {
                String akConf = envSystem.get(ENV_TUYA_AK);
                akConf = StringUtils.isBlank(akConf) ? this.tuyaAk : akConf;
                String skConf = envSystem.get(ENV_TUYA_SK);
                skConf = StringUtils.isBlank(skConf) ? this.tuyaSk : skConf;
                String userUidConf = envSystem.get(ENV_TUYA_USER_UID);
                userUidConf = StringUtils.isBlank(userUidConf) ? this.tuyaUserUid : userUidConf;

                String reConf = envSystem.get(ENV_REGION);
                reConf = StringUtils.isBlank(reConf) ? this.tuyaUserUid : reConf;
                TuyaRegion region = StringUtils.isNoneBlank(reConf) ? TuyaRegion.valueOf(reConf) : null;

                String devIdsConf = envSystem.get(ENV_TUYA_DEVICE_IDS);
                devIdsConf = StringUtils.isNoneBlank(devIdsConf) ?devIdsConf : this.tuyaDeviceIds;
                String[] deviceIds = StringUtils.isNoneBlank(devIdsConf) ? StringUtils.split(devIdsConf, ",     ") : null;

                String tempSetMinConfStr = envSystem.get(ENV_TUYA_TEMP_SET_MIN);
                Integer tempSetMinConf = StringUtils.isBlank(tempSetMinConfStr) ? this.tuyaTempSetMin : Integer.valueOf(tempSetMinConfStr);

                String tempSetMaxConfStr = envSystem.get(ENV_TUYA_TEMP_SET_MAX);
                Integer tempSetMaxConf = StringUtils.isBlank(tempSetMaxConfStr) ? this.tuyaTempSetMax : Integer.valueOf(tempSetMaxConfStr);

                String categoryForControlPowersConf = envSystem.get(ENV_TUYA_CATEGORY_FOR_CONTROL_POWERS);
                categoryForControlPowersConf = StringUtils.isNoneBlank(categoryForControlPowersConf) ? categoryForControlPowersConf : this.tuyaCategoryForControlPowers;
                String [] categoryForControlPowers = StringUtils.isNoneBlank(categoryForControlPowersConf) ? StringUtils.split(categoryForControlPowersConf, ",") : null;

                if (StringUtils.isNoneBlank(akConf) && StringUtils.isNoneBlank(skConf)
                        && StringUtils.isArrayNoneBlank(deviceIds) && StringUtils.isNoneBlank(userUidConf) && region != null) {
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
                    return null;
                }
            } catch (Exception e) {
                log.error("During processing Tuya connection error.[{}]", e.getMessage());
                return null;
            }
        }
    }
}

