# Smart Tuya and Solarman Control

This program is designed for the optimal consumption of solar energy for heating a country house, heating water and watering the garden with optimal battery discharge (no more than 50%) during the day.

The system operates on the basis of the service protocols of the Tuya sensors and the service protocol of the Solarman type recorder (On the example of a hybrid station of the Deye model: SUN-12KSG04LP3-EU)

Integration testing of the code was done on the basis of the following equipment:

- hybrid  solar station of the Deye model: SUN-12KSG04LP3-EU with solar panels
- battery: GBL2.45K3(LFP) 4 pcs
- Tuya Smart Life WiFi Thermostat Electric Floor Temperature Controller 5 pcs
- Tuya EU Smart Plug,16A WIFI Smart Socket 4 pcs
- Tuya Wifi Temperature Humidity Sensor 2 pcs
- 1P+N WiFi Smart Circuit Breaker Voltage Energy Power kWh Meter Time Relay Switch 1 pcs

## Parameters of application:

- spring.profiles.active: "${SPRING_PROFILES_ACTIVE:dev}":
```text
-- dev: log info to "CONSOLE"
-- prod: log info to "FILE"
```
- logging: (log info to "FILE")
```text
-- file: "${LOG_FILE:smart-floor}"
-- folder: "${LOG_PATH:./logs}"
```
**After create Cloud project on the Tuya website** https://iot.tuya.com/cloud

- connector.tuya:

-- ak: "${TUYA_AK:}" - Authorization Key -> Access ID/Client ID:

-- sk: "${TUYA_SK:}" - Authorization Key -> Access Secret/Client Secret:

-- region: "${TUYA_REGION:EU}" - for subscribe
```text
  /**
     * China
     */
    CN("https://openapi.tuyacn.com", "pulsar+ssl://mqe.tuyacn.com:7285/"),
    /**
     * US WEST
     */
    US("https://openapi.tuyaus.com", "pulsar+ssl://mqe.tuyaus.com:7285/"),
    /**
     * US EAST
     */
    US_EAST("https://openapi-ueaz.tuyaus.com", "pulsar+ssl://mqe.tuyaus.com:7285/"),
    /**
     * European
     */
    EU("https://openapi.tuyaeu.com", "pulsar+ssl://mqe.tuyaeu.com:7285/"),
    /**
     * Europe West
     */
    EU_WEST("https://openapi-weaz.tuyaeu.com", "pulsar+ssl://mqe.tuyaeu.com:7285/"),
    /**
     * India
     */
    IN("https://openapi.tuyain.com", "pulsar+ssl://mqe.tuyain.com:7285/");

```

-- device_ids: "${TUYA_DEVICE_IDS:}" - device lists with its id and power [Wth], from Tuya Cloud project

Example:
```text
TUYA_DEVICE_IDS=bf328d7e1327e7c600ncnf:70, bf8ba5a92ae00cb510nat5:70, bf11fce4b500291373jnn2:1600,bfa715581477683002qb4l:1700,bfc99c5e1b444322eaaqgu:1600,bf46c12380a94bb009ngxx:40;
```

-- user_uid: "${TUYA_USER_UID:}" Optional

**After registering on the Solarman website** https://home.solarmanpv.com/account

- solarman:

-- appid: "${SOLARMAN_APP_ID:}"

-- secret: "${SOLARMAN_SECRET:}"

-- username: "${SOLARMAN_USER_NAME:}"

-- password: "${SOLARMAN_PASS:}"
passhash: "${SOLARMAN_PASS_HASH:}"
logger_sn: "${SOLARMAN_LOGGER_SN:}"
smart:
tuya:
temp_set:
min: "${TUYA_TEMP_SET_MIN:5}"
max: "${TUYA_TEMP_SET_MAX:24}"
category_for_control_powers: "${TUYA_CATEGORY_FOR_CONTROL_POWERS:wk}"
solarman:
timeout_sec: "${SOLARMAN_TIMEOUT_SEC:600}"
bms_soc:
min: "${SOLARMAN_BMS_SOC_MIN:87.0}"
alarm_warn: "${SOLARMAN_BMS_SOC_ALARM_WARN:80.0}"
alarm_error: "${SOLARMAN_BMS_SOC_ALARM_ERROR:59.0}"



## Running the application as docker container

```bash
docker run -e SPARKPLUG_SERVER_URL='tcp://thingsboard.cloud:1883' -e SPARKPLUG_CLIENT_MQTT_USERNAME='YOUR_THINGSBOARD_DEVICE_TOKEN' thingsboard/tb-sparkplug-emulator:latest
```

### Building from sources

```shell
mvn clean install
```

### Running as plain java application

```shell
java -jar sparkplug-1.0-SNAPSHOT-jar-with-dependencies.jar
```


