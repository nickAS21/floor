package org.nickas21.smart.util;

import java.util.Map;

public class EnvConstant {

    public static final String ENV_TUYA_AK = "TUYA_AK";

    public static final String ENV_TUYA_SK = "TUYA_SK";

    public static final String ENV_REGION = "TUYA_REGION";

    public static final String ENV_TUYA_DEVICE_IDS = "TUYA_DEVICE_IDS";

    public static final String ENV_TUYA_TEMP_SET_MIN = "TUYA_TEMP_SET_MIN";

    public static final String ENV_TUYA_TEMP_SET_MAX = "TUYA_TEMP_SET_MAX";

    public static final String ENV_TUYA_USER_UID = "TUYA_USER_UID";

    public static final String ENV_SOLARMAN_REGION = "SOLARMAN_REGION";

    public static final String ENV_SOLARMAN_APP_ID = "SOLARMAN_APP_ID";

    public static final String ENV_SOLARMAN_SECRET = "SOLARMAN_SECRET";

    public static final String ENV_SOLARMAN_USER_NAME = "SOLARMAN_USER_NAME";

    public static final String ENV_SOLARMAN_PASS= "SOLARMAN_PASS";

    public static final String ENV_SOLARMAN_PASS_HASH= "SOLARMAN_PASS_HASH";

    public static final String ENV_SOLARMAN_LOGGER_SN = "SOLARMAN_LOGGER_SN";

    public static final String ENV_SOLARMAN_TIMEOUT_SEC = "SOLARMAN_TIMEOUT_SEC";

    public static final String ENV_SOLARMAN_BMS_SOC_MIN = "SOLARMAN_BMS_SOC_MIN";

    public static final String ENV_SOLARMAN_BMS_SOC_MAX = "SOLARMAN_BMS_SOC_MAX";

    public static final String ENV_SOLARMAN_BMS_SOC_ALARM_WARN = "SOLARMAN_BMS_SOC_ALARM_WARN";

    public static final String ENV_SOLARMAN_BMS_SOC_ALARM_ERROR = "SOLARMAN_BMS_SOC_ALARM_ERROR";

    public static final String ENV_SOLARMAN_BMS_SOC_STEP_VALUE_CHANGE = "SOLARMAN_BMS_SOC_STEP_VALUE_CHANGE";

    public final static Map<String, String> envSystem = System.getenv();
}
