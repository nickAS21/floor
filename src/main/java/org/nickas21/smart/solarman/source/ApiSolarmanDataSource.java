package org.nickas21.smart.solarman.source;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.constant.SolarmanRegion;
import org.nickas21.smart.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_APP_ID;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_BMS_SOC_ALARM_ERROR;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_BMS_SOC_ALARM_WARN;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_BMS_SOC_MAX;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_BMS_SOC_MIN;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_BMS_SOC_STEP_VALUE_CHANGE;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_LOGGER_SN;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_PASS;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_PASS_HASH;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_SECRET;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_REGION;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_TIMEOUT_SEC;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_USER_NAME;
import static org.nickas21.smart.util.EnvConstant.envSystem;
import static org.nickas21.smart.util.HttpUtil.getBodyHash;

@Slf4j
@Component
public class ApiSolarmanDataSource {

    @Value("${connector.solarman.region}")
    private String solarmanRegion;

    @Value("${connector.solarman.appid}")
    private String solarmanAppId;

    @Value("${connector.solarman.secret}")
    private String solarmanSecret;

    @Value("${connector.solarman.password}")
    private String solarmanPassword;

    @Value("${connector.solarman.username}")
    private String solarmanUserName;

    @Value("${connector.solarman.passhash}")
    private String solarmanPassHash;

    @Value("${connector.solarman.logger_sn}")
    private String solarmanLoggerSn;

    @Value("${smart.solarman.timeout_sec}")
    private Long solarmanTimeOutSec;

    @Value("${smart.solarman.bms_soc.min}")
    private double solarmanBmsSocMin;

    @Value("${smart.solarman.bms_soc.max}")
    private double solarmanBmsSocMax;

    @Value("${smart.solarman.bms_soc.alarm_warn}")
    private double solarmanBmsSocAlarmWarn;

    @Value("${smart.solarman.bms_soc.alarm_error}")
    private double solarmanBmsSocAlarmError;

    @Value("${smart.solarman.bms_soc.step_value_change}")
    private double solarmanBmsSocStepValueChange;

    private SolarmanDataSource solarmanDataSource;

    public SolarmanDataSource getSolarmanDataSource() {
        if (this.solarmanDataSource == null) {
            try {
                String soRegionConf = envSystem.get(ENV_SOLARMAN_REGION);
                soRegionConf = StringUtils.isBlank(soRegionConf) ? this.solarmanRegion : soRegionConf;
                SolarmanRegion region = StringUtils.isNoneBlank(soRegionConf) ? SolarmanRegion.valueOf(soRegionConf) : null;
                String soAppIdConf = envSystem.get(ENV_SOLARMAN_APP_ID);
                soAppIdConf = StringUtils.isBlank(soAppIdConf) ? this.solarmanAppId :  soAppIdConf;
                String soSecretConf = envSystem.get(ENV_SOLARMAN_SECRET);
                soSecretConf = StringUtils.isBlank(soSecretConf) ? this.solarmanSecret : soSecretConf;
                String soUserNameConf = envSystem.get(ENV_SOLARMAN_USER_NAME);
                soUserNameConf = StringUtils.isBlank(soUserNameConf) ? this.solarmanUserName : soUserNameConf;
                String soPassConf = envSystem.get(ENV_SOLARMAN_PASS);
                soPassConf = StringUtils.isBlank(soPassConf) ? this.solarmanPassword : soPassConf;
                String soPassHashConf = envSystem.get(ENV_SOLARMAN_PASS_HASH);
                soPassHashConf = StringUtils.isBlank(soPassHashConf) ? this.solarmanPassHash : soPassHashConf;
                soPassHashConf = StringUtils.isBlank(soPassHashConf) ? getBodyHash(soPassConf) : soPassHashConf;
                String soLogSnConf = envSystem.get(ENV_SOLARMAN_LOGGER_SN);
                soLogSnConf = StringUtils.isBlank(soLogSnConf) ? this.solarmanLoggerSn : soLogSnConf;
                String soTimeOutSecConfStr = envSystem.get(ENV_SOLARMAN_TIMEOUT_SEC);
                Long soTimeOutSecConf = StringUtils.isBlank(soTimeOutSecConfStr) ? this.solarmanTimeOutSec : Long.valueOf(soTimeOutSecConfStr);
                String soBmsSocMinConfStr = envSystem.get(ENV_SOLARMAN_BMS_SOC_MIN);
                double soBmsSocMinConf = StringUtils.isBlank(soBmsSocMinConfStr) ? this.solarmanBmsSocMin : Double.valueOf(soBmsSocMinConfStr);
                String soBmsSocMaxConfStr = envSystem.get(ENV_SOLARMAN_BMS_SOC_MAX);
                double soBmsSocMaxConf = StringUtils.isBlank(soBmsSocMaxConfStr) ? this.solarmanBmsSocMax : Double.valueOf(soBmsSocMaxConfStr);
                String soBmsSocAlarmWarnConfStr = envSystem.get(ENV_SOLARMAN_BMS_SOC_ALARM_WARN);
                double soBmsSocAlarmWarnConf = StringUtils.isBlank(soBmsSocAlarmWarnConfStr) ? this.solarmanBmsSocAlarmWarn : Double.valueOf(soBmsSocAlarmWarnConfStr);
                String soBmsSocAlarmErrorConfStr = envSystem.get(ENV_SOLARMAN_BMS_SOC_ALARM_ERROR);
                double soBmsSocAlarmErrorConf = StringUtils.isBlank(soBmsSocAlarmErrorConfStr) ? this.solarmanBmsSocAlarmError : Double.valueOf(soBmsSocAlarmErrorConfStr);
                String soBmsSocStepValueChangeConfStr = envSystem.get(ENV_SOLARMAN_BMS_SOC_STEP_VALUE_CHANGE);
                double soBmsSocStepValueChangeConf = StringUtils.isBlank(soBmsSocStepValueChangeConfStr) ? this.solarmanBmsSocStepValueChange : Double.valueOf(soBmsSocStepValueChangeConfStr);
                if (StringUtils.isBlank(soAppIdConf) || StringUtils.isBlank(soLogSnConf) || StringUtils.isBlank(soSecretConf)
                        || soPassHashConf.equals(EMPTY_HASH) || region == null) {
                    log.error("During processing Solarman connection data source error. One of parameters is null");
                    return null;
                } else {
                    this.solarmanDataSource = SolarmanDataSource.builder()
                            .region(region)
                            .appId(soAppIdConf)
                            .secret(soSecretConf)
                            .userName(soUserNameConf)
                            .passHash(soPassHashConf)
                            .loggerSn(soLogSnConf)
                            .passWord(soPassConf)
                            .timeOutSec(soTimeOutSecConf)
                            .bmsSocMin(soBmsSocMinConf)
                            .bmsSocMax(soBmsSocMaxConf)
                            .bmsSocAlarmWarn(soBmsSocAlarmWarnConf)
                            .bmsSocAlarmError(soBmsSocAlarmErrorConf)
                            .bmsSocStepValueChange(soBmsSocStepValueChangeConf)
                            .build();
                }
            } catch (Exception e) {
                log.error("During processing Solarman connection data source error.[{}]", e.getMessage());
                return null;
            }
        }
        return this.solarmanDataSource;
    }
}

