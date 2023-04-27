package org.nickas21.smart.tuya.constant;


public class TuyaApi {

    public static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    public  final static int TOKEN_GRANT_TYPE = 1;
    public  final static String GET_TUYA_TOKEN_URL_PATH = "/v1.0/token";
    public  final static String GET_TUYA_REFRESH_TOKEN_URL_PATH = "/v1.0/token/%s";
    public  final static String GET_DEVICES_ID_URL_PATH = "/v1.1/iot-03/devices/%s";
    // device_ids=bf11fce4b500291373jnn2,bfa715581477683002qb4l
    public  final static String GET_DEVICES_IDS_URL_PATH = "/v1.0/iot-03/devices";
    public  final static String GET_DEVICE_STATUS_URL_PATH = "/v1.0/iot-03/devices/%s/status";
    // --data "{"commands":[{"code":"temp_set","value":6}]}"
    public  final static String POST_DEVICE_COMMANDS_URL_PATH = "/v1.0/iot-03/devices/%s/commands";
    //  --data "{"name":"Sun Usel2"}"
    public  final static String POST_DEVICE_RENAME_URL_PATH = "/v1.0/iot-03/devices/%s";
    public  final static String GET_DEVICE_SPECIFICATION_URL_PATH = "/v1.0/iot-03/devices/%s/specification";
    public  final static String GET_DEVICE_FUNCTIONS_URL_PATH = "/v1.0/iot-03/devices/%s/functions";
    public  final static String GET_DEVICE_CATEGORIES_URL_PATH = "/v1.0/iot-03/device-categories";
    // state	Integer   0: normal  1: frozen
    // {
    //  "state": 1
    //}
    //  --data "{"state":0}"
    public  final static String GET_POST_DEVICE_FREEZE_STATE_URL_PATH = "/v1.0/iot-03/devices/%s/freeze-state";

    public  final static String GET_CATEGORY_STATUS_URL_PATH = "v1.0/iot-03/categories/%s/status";
    public  final static String GET_CATEGORY_FUNCTIONS_URL_PATH = "/v1.0/iot-03/categories/%s/functions";

    public  final static String COMMANDS = "commands";
    public  final static String CODE = "code";
    public  final static String VALUE = "value";



    public  final static String GET_CATEGORY_URL_PATH = "/v1.0/iot-03/categories/%s/status";
//    public  final static String GET_LOGS_URL_PATH = "/v1.0/devices/%s/logs?start_row_key=''&type=1,2&start_time=%d&end_time=%d&size=20";
    public  final static String GET_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/logs";
    public  final static String GET_REPORT_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/report-logs";
//    public  final static String GET_REPORT_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/report-logs?codes=&start_time=1545898159931&end_time=1545898159935&size=20";


}
