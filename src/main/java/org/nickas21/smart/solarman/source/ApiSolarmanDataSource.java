package org.nickas21.smart.solarman.source;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.constant.SolarmanRegion;
import org.nickas21.smart.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_APP_ID;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_LOGGER_SN;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_PASS;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_PASS_HASH;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_SECRET;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_REGION;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_USER_NAME;
import static org.nickas21.smart.util.EnvConstant.envSystem;
import static org.nickas21.smart.util.HttpUtil.getBodyHash;

@Slf4j
@Component
public class ApiSolarmanDataSource {

    @Value("${connector.solarman.region}")
    public String solarmanRegion;

    @Value("${connector.solarman.appid}")
    public String solarmanAppId;

    @Value("${connector.solarman.secret}")
    public String solarmanSecret;

    @Value("${connector.solarman.password}")
    public String solarmanPassword;

    @Value("${connector.solarman.username}")
    public String solarmanUserName;

    @Value("${connector.solarman.passhash}")
    public String solarmanPassHash;

    @Value("${connector.solarman.logger_sn}")
    public String solarmanLoggerSn;

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
                String soPassConf = envSystem.get(ENV_SOLARMAN_PASS);
                soPassConf = StringUtils.isBlank(soPassConf) ? this.solarmanPassword : soPassConf;
                String soPassHashConf = envSystem.get(ENV_SOLARMAN_PASS_HASH);
                soPassHashConf = StringUtils.isBlank(soPassHashConf) ? this.solarmanPassHash : soPassHashConf;
                soPassHashConf = StringUtils.isBlank(soPassHashConf) ? getBodyHash(soPassConf) : soPassHashConf;
                String soLogSnConf = envSystem.get(ENV_SOLARMAN_LOGGER_SN);
                soLogSnConf = StringUtils.isBlank(soLogSnConf) ? this.solarmanLoggerSn : soLogSnConf;
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

