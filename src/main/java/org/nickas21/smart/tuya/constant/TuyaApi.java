package org.nickas21.smart.tuya.constant;


public class TuyaApi {

    public static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    public  final static int TOKEN_GRANT_TYPE = 1;
    public  final static String GET_TUYA_TOKEN_URL_PATH = "/v1.0/token";
    public  final static String GET_TUYA_REFRESH_TOKEN_URL_PATH = "/v1.0/token/%s";
    public  final static int  GET_TUYA_REFRESH_TOKEN_ERROR_1010 = 1010; // "token is expired"
    public  final static String GET_DEVICES_ID_URL_PATH = "/v1.1/iot-03/devices/%s";
//    public  final static String GET_DEVICES_IDS_URL_PATH = "/v1.3/iot-03/devices";
//    public  final static String GET_DEVICES_IDS_URL_PATH = "/v1.3/iot-03/devices?category=xx&product_id=xx&name=xx&device_ids=xx&last_row_key=xx&page_size=xx";
    public  final static String GET_DEVICES_IDS_URL_PATH = "/v1.3/iot-03/devices?category=&product_id=&name=&device_ids=&last_row_key=bfa715581477683002qb4l&page_size=100";
    public  final static String GET_DEVICE_STATUS_URL_PATH = "/v1.0/iot-03/devices/%s/status";
    public  final static String POST_DEVICE_COMMANDS_URL_PATH = "/v1.0/iot-03/devices/%s/commands";
    public  final static String POST_DEVICE_RENAME_URL_PATH = "/v1.0/iot-03/devices/%s";
    public  final static String GET_DEVICE_SPECIFICATION_URL_PATH = "/v1.0/iot-03/devices/%s/specification";
    public  final static String GET_DEVICE_FUNCTIONS_URL_PATH = "/v1.0/iot-03/devices/%s/functions";
    public  final static String GET_DEVICE_CATEGORIES_URL_PATH = "/v1.0/iot-03/device-categories";
    public  final static String GET_POST_DEVICE_FREEZE_STATE_URL_PATH = "/v1.0/iot-03/devices/%s/freeze-state";

    public  final static String GET_CATEGORY_STATUS_URL_PATH = "v1.0/iot-03/categories/%s/status";
    public  final static String GET_CATEGORY_FUNCTIONS_URL_PATH = "/v1.0/iot-03/categories/%s/functions";

    public  final static String COMMANDS = "commands";
    public  final static String CODE = "code";
    public  final static String VALUE = "value";

    public  final static String GET_CATEGORY_URL_PATH = "/v1.0/iot-03/categories/%s/status";
    public  final static String GET_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/logs";
    public  final static String GET_REPORT_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/report-logs";
}
