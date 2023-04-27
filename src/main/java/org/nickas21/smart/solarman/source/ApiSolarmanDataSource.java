package org.nickas21.smart.solarman.source;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.constant.SolarmanRegion;
import org.nickas21.smart.tuya.constant.TuyaRegion;
import org.nickas21.smart.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;
import static org.nickas21.smart.util.EnvConstant.ENV_REGION;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_APP_ID;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_INVERTER_ID;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_LOGGER_ID;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_MQTT_PASSWORD;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_MQTT_PORT;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_MQTT_TOPIC;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_MQTT_USER_NAME;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_PASS;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_PASS_HASH;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_PLANT_NAME;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_SECRET;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_STATION_ID;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_REGION;
import static org.nickas21.smart.util.EnvConstant.ENV_SOLARMAN_USER_NAME;
import static org.nickas21.smart.util.EnvConstant.envSystem;
import static org.nickas21.smart.util.HttpUtil.getBodyHash;

@Slf4j
@Component
public class ApiSolarmanDataSource {

    @Value("${connector.solarman.plant_mame}:")
    public String solarmanName;

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

    @Value("${connector.solarman.stationid}")
    public String solarmanStationId;

    @Value("${connector.solarman.inverterid}")
    public String solarmanInverterId;

    @Value("${connector.solarman.loggerid}")
    public String solarmanLoggerId;

    @Value("${connector.solarman.mqtt.port}")
    public int solarmanMqttPort;

    @Value("${connector.solarman.mqtt.topic}")
    public String solarmanMqttTopic;

    @Value("${connector.solarman.mqtt.username}")
    public String solarmanMqttUserName;

    @Value("${connector.solarman.mqtt.password}")
    public String solarmanMqttPassword;

    private SolarmanMqttDataSource solarmanMqttDataSource;

    public SolarmanMqttDataSource getSolarmanMqttDataSource () {
        if (this.solarmanMqttDataSource == null) {
            try {
                String soNameConf = envSystem.get(ENV_SOLARMAN_PLANT_NAME );
                soNameConf = StringUtils.isBlank(soNameConf) ? this.solarmanName : soNameConf;
                String soRegionConf = envSystem.get(ENV_SOLARMAN_REGION);
                soRegionConf = StringUtils.isBlank(soRegionConf) ? this.solarmanRegion : soRegionConf;
                SolarmanRegion region = StringUtils.isNoneBlank(soRegionConf) ? SolarmanRegion.valueOf(soRegionConf) : null;
                String soAppIdConf = envSystem.get(ENV_SOLARMAN_APP_ID);
                soAppIdConf = StringUtils.isBlank(soAppIdConf) ? this.solarmanAppId :  soAppIdConf;
                String soSecretConf = envSystem.get(ENV_SOLARMAN_SECRET);
                soSecretConf = StringUtils.isBlank(soSecretConf) ? this.solarmanSecret : soSecretConf;
                String soUserNameConf = envSystem.get(ENV_SOLARMAN_USER_NAME);
                String soMqttUserNameConf = envSystem.get(ENV_SOLARMAN_MQTT_USER_NAME);
                soUserNameConf = StringUtils.isBlank(soUserNameConf) ? this.solarmanUserName : soUserNameConf;
                soMqttUserNameConf = StringUtils.isBlank(soMqttUserNameConf) ? this.solarmanMqttUserName : soMqttUserNameConf;
                soMqttUserNameConf = StringUtils.isBlank(soMqttUserNameConf) ? soUserNameConf : soMqttUserNameConf;
                soUserNameConf = StringUtils.isBlank(soUserNameConf) ? soMqttUserNameConf : soUserNameConf;
                String soPassConf = envSystem.get(ENV_SOLARMAN_PASS);
                soPassConf = StringUtils.isBlank(soPassConf) ? this.solarmanPassword : soPassConf;
                String soPassHashConf = envSystem.get(ENV_SOLARMAN_PASS_HASH);
                soPassHashConf = StringUtils.isBlank(soPassHashConf) ? this.solarmanPassHash : soPassHashConf;
                soPassHashConf = StringUtils.isBlank(soPassHashConf) ? getBodyHash(soPassConf) : soPassHashConf;
                String soStationIdConf = envSystem.get(ENV_SOLARMAN_STATION_ID);
                soStationIdConf = StringUtils.isBlank(soStationIdConf) ? this.solarmanStationId : soStationIdConf;
                String soInvIdConf = envSystem.get(ENV_SOLARMAN_INVERTER_ID);
                soInvIdConf = StringUtils.isBlank(soInvIdConf) ? this.solarmanInverterId : soInvIdConf;
                String soLogIdConf = envSystem.get(ENV_SOLARMAN_LOGGER_ID);
                soLogIdConf = StringUtils.isBlank(soLogIdConf) ? this.solarmanLoggerId : soLogIdConf;
                String soMqttPortConf = envSystem.get(ENV_SOLARMAN_MQTT_PORT);
                int mqttPort = StringUtils.isBlank(soMqttPortConf) ? this.solarmanMqttPort : Integer.valueOf(soMqttPortConf);
                String soMqttTopicConf = envSystem.get(ENV_SOLARMAN_MQTT_TOPIC);
                soMqttTopicConf = StringUtils.isBlank(soMqttTopicConf) ? this.solarmanMqttTopic : soMqttTopicConf;
                String soMqttPassConf = envSystem.get(ENV_SOLARMAN_MQTT_PASSWORD);
                soMqttPassConf = StringUtils.isBlank(soMqttPassConf) ? this.solarmanMqttPassword : soMqttPassConf;
                if (StringUtils.isBlank(soAppIdConf) || StringUtils.isBlank(soLogIdConf) || StringUtils.isBlank(soSecretConf) ||
                        StringUtils.isBlank(soMqttUserNameConf) || soPassHashConf.equals(EMPTY_HASH) || region == null) {
                    log.error("During processing Solarman connection data source error. One of parameters is null");
                    return null;
                } else {
                    this.solarmanMqttDataSource = SolarmanMqttDataSource.builder()
                            .name(soNameConf)
                            .region(region)
                            .appId(soAppIdConf)
                            .secret(soSecretConf)
                            .userName(soUserNameConf)
                            .passHash(soPassHashConf)
                            .stationId(soStationIdConf)
                            .inverterId(soInvIdConf)
                            .loggerId(soLogIdConf)
                            .mqttPort(mqttPort)
                            .topic(soMqttTopicConf)
                            .mqttUsername(soMqttUserNameConf)
                            .passWord(soMqttPassConf)
                            .build();
                }
            } catch (Exception e) {
                log.error("During processing Solarman connection data source error.[{}]", e.getMessage());
                return null;
            }
        }
        return this.solarmanMqttDataSource;
    }

}
