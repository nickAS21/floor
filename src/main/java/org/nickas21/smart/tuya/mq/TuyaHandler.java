package org.nickas21.smart.tuya.mq;

import org.nickas21.smart.tuya.TuyaConnection;
import org.nickas21.smart.tuya.TuyaConnectionConfiguration;
import org.nickas21.smart.tuya.util.TuyaRegion;
import org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class TuyaHandler {

    private final static int TOKEN_GRANT_TYPE = 1;
    private final static String GET_TOKEN_URL_PATH = "/v1.0/token";
    private final static String GET_REFRESH_TOKEN_URL_PATH = "/v1.0/token/%s";
    private final static String POST_COMMANDS_URL_PATH = "/v1.0/iot-03/devices/%s/commands";
    private final static String GET_STATUS_URL_PATH = "/v1.0/iot-03/devices/%s/status";
    private final static String GET_CATEGORY_URL_PATH = "/v1.0/iot-03/categories/%s/status";
    private final static String GET_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/logs";
    private final static String GET_REPORT_LOGS_URL_PATH = "/v1.0/iot-03/devices/%s/report-logs";
    private final static String GET_SPECIFICATION_URL_PATH = "/v1.0/iot-03/devices/%s/specification";
    private final static String GET_FUNCTIONS_URL_PATH = "/v1.0/iot-03/devices/%s/functions";
    private final static String CONNECTOR_AK = "connector.ak";
    private final static String CONNECTOR_SK = "connector.sk";
    private final static String CONNECTOR_RE = "connector.region";
    private static Map<String, String> envSystem = System.getenv();


    public static TuyaConnectionConfiguration getTuyaConnectionConfiguration(TuyaConnection tuyaConnection) throws IOException {

        String ak = envSystem.get(CONNECTOR_AK);
        String sk = envSystem.get(CONNECTOR_SK);
        String re = envSystem.get(CONNECTOR_RE);
        TuyaRegion region = (re != null && re.isBlank()) ? TuyaRegion.valueOf(re) : null;
        if (ak == null || ak.isEmpty()
                || sk == null || sk.isEmpty()  || region == null) {
            ak = tuyaConnection.ak;
            sk = tuyaConnection.sk;
            re = tuyaConnection.region;
            region = (re != null && !re.isEmpty()) ? TuyaRegion.valueOf(re) : null;
        }
        if (ak != null && !ak.isEmpty() && sk != null && !sk.isEmpty() && region != null) {
            return new TuyaConnectionConfiguration(ak, sk, region);
        } else {
            return null;
        }

//        if (fileConfigJson != null && new File(fileConfigJson).isFile()) {
//            isConfig = new FileInputStream(fileConfigJson);
//        } else {
//            isConfig = new ClassPathResource(CONFIG_JSON).getInputStream();
//        }
//        sparkplugNodeConfig = JacksonUtil.fromInputToObject(isConfig, SparkplugNodeConfig.class);
//        updateSparkplugNodeConfig();
//        return sparkplugNodeConfig;
    }
}
