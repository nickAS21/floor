package org.nickas21.smart.util;

import java.util.Map;

public class EnvConstant {

    public static final String ENV_AK = "TUYA_AK";

    public static final String ENV_SK = "TUYA_SK";

    public static final String ENV_REGION = "TUYA_REGION";

    public static final String ENV_DEVICE_IDS = "TUYA_DEVICE_IDS";

    public static final String ENV_USER_UID = "TUYA_USER_UID";

    public static final String ENV_SOLARMAN_REGION = "SOLARMAN_REGION";

    public static final String ENV_SOLARMAN_APP_ID = "SOLARMAN_APP_ID";

    public static final String ENV_SOLARMAN_SECRET = "SOLARMAN_SECRET";

    public static final String ENV_SOLARMAN_USER_NAME = "SOLARMAN_USER_NAME";

    public static final String ENV_SOLARMAN_PASS= "SOLARMAN_PASS";

    public static final String ENV_SOLARMAN_PASS_HASH= "SOLARMAN_PASS_HASH";

    public static final String ENV_SOLARMAN_LOGGER_SN = "SOLARMAN_LOGGER_SN";

    public final static Map<String, String> envSystem = System.getenv();
}
