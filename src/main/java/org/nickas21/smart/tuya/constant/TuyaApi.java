package org.nickas21.smart.tuya.constant;

import java.util.Map;

public class TuyaApi {

    public static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    public  final static int TOKEN_GRANT_TYPE = 1;
    public  final static String GET_TOKEN_URL_PATH = "/v1.0/token";
    public  final static String GET_REFRESH_TOKEN_URL_PATH = "/v1.0/token/%s";
    public  final static String GET_DEVICES_ID_URL_PATH = "/v1.1/iot-03/devices/%s";
    public  final static String GET_STATUS_URL_PATH = "/v1.0/iot-03/devices/%s/status";
//    public  final static String GET_ALL_DEVICE_IDS = "/v1.0/users/%s/devices";
//    public  final static String GET_ALL_DEVICE_IDS = "/v1.0/iot-03/devices";
    public  final static String QUERY_DEVICE_IDS = "device_ids";
//    public  final static String GET_ALL_DEVICES_IDS = "/v1.3/iot-03/devices";
//    public  final static String GET_ALL_DEVICES_IDS = "/v1.0/iot-03/devices/status";
//    public  final static String GET_ALL_DEVICES_IDS = "/v1.0/iot-03/device-groups";
//    public  final static String GET_ALL_DEVICES_IDS = "/v2.0/devices";
    public  final static String POST_COMMANDS_URL_PATH = "/v1.0/iot-03/devices/%s/commands";

    public  final static String GET_CATEGORY_URL_PATH = "/v1.0/iot-03/categories/%s/status";
    public  final static String GET_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/logs";
    public  final static String GET_REPORT_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/report-logs";
    public  final static String GET_SPECIFICATION_URL_PATH = "/v1.0/iot-03/devices/%s/specification";
    public  final static String GET_FUNCTIONS_URL_PATH = "/v1.0/iot-03/devices/%s/functions";
    public final static Map<String, String> envSystem = System.getenv();
}
