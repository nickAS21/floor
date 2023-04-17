package org.nickas21.smart.tuya.constant;

import java.util.Map;

public class TuyaApi {

    public  final static int TOKEN_GRANT_TYPE = 1;
    public  final static String GET_TOKEN_URL_PATH = "/v1.0/token";
    public  final static String GET_REFRESH_TOKEN_URL_PATH = "/v1.0/token/%s";
    public  final static String POST_COMMANDS_URL_PATH = "/v1.0/iot-03/devices/%s/commands";
    public  final static String GET_STATUS_URL_PATH = "/v1.0/iot-03/devices/%s/status";
    public  final static String GET_CATEGORY_URL_PATH = "/v1.0/iot-03/categories/%s/status";
    public  final static String GET_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/logs";
    public  final static String GET_REPORT_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/report-logs";
    public  final static String GET_SPECIFICATION_URL_PATH = "/v1.0/iot-03/devices/%s/specification";
    public  final static String GET_FUNCTIONS_URL_PATH = "/v1.0/iot-03/devices/%s/functions";
    public final static Map<String, String> envSystem = System.getenv();
}
