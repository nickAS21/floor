package org.nickas21.smart.tuya.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.nickas21.smart.solarman.service.SolarmanStationsService;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.response.Device;
import org.nickas21.smart.tuya.response.Devices;
import org.nickas21.smart.tuya.source.ApiTuyaDataSource;
import org.nickas21.smart.util.HmacSHA256Util;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.nickas21.smart.tuya.constant.TuyaApi.CODE;
import static org.nickas21.smart.tuya.constant.TuyaApi.COMMANDS;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_DEVICES_ID_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_REFRESH_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_DEVICE_STATUS_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.POST_DEVICE_COMMANDS_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.TOKEN_GRANT_TYPE;
import static org.nickas21.smart.tuya.constant.TuyaApi.VALUE;
import static org.nickas21.smart.util.HttpUtil.creatHttpPathWithQueries;
import static org.nickas21.smart.util.HttpUtil.getBodyHash;
import static org.nickas21.smart.util.HttpUtil.sendRequest;
import static org.nickas21.smart.util.JacksonUtil.objectToJsonNode;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;

@Slf4j
@Service
public class DefaultTuyaDeviceService implements TuyaDeviceService {

    private TuyaToken accessTuyaToken;
    private Devices devices;

    private ExecutorService executor;
    private TuyaMessageDataSource connectionConfiguration;

    @Autowired
    private ApiTuyaDataSource dataSource;

    @Autowired
    private SolarmanStationsService solarmanStationsService;


    public DefaultTuyaDeviceService(ApiTuyaDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void init() {
        double bmsSoc = 0;
        accessTuyaToken = getTuyaToken();
        devices = new Devices();
        if (accessTuyaToken != null) {
            sendInitRequest();
            bmsSoc = solarmanStationsService.getRealTimeDataStart();
        }
        log.info("init successful: [{}] devices", devices.getDevIds().size());
        // Test Sun Uzel
        String deviceIdTest = "bfa715581477683002qb4l";
        String deviceIdsTest = "bf11fce4b500291373jnn2,bfa715581477683002qb4l,bfc99c5e1b444322eaaqgu";
//        updateAllTermostat (18);
    }

    public void setConnectionConfiguration(TuyaMessageDataSource connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
    }

    @Override
    public void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    @SneakyThrows
    public void devicesFromUpDateStatusValue(TuyaConnectionMsg msg) {
        log.info("TuyaConnectionMsg: [{}]",  msg);
//        updateAllTermostat (5);
//        String deviceIdTest = "bf11fce4b500291373jnn2";
//        sendPostRequestCommand(deviceIdTest, "temp_set", 5);
        String deviceId = msg.getJson().get("devId").asText();
        JsonNode deviceStatus = msg.getJson().get("status");
        Device device = this.devices.getDevIds().get(deviceId);
        if ( device != null) {
            device.setStatus(deviceStatus);
        } else {
            log.error("dev is null");
        }
    }

    /**
     * devicesToUpDateStatusValue
     */
    @SneakyThrows
    public void sendPostRequestCommand(String deviceId, String code, Object value) {
        ObjectNode commandsNode = JacksonUtil.newObjectNode();
        ArrayNode arrayNode = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        ObjectNode data = JacksonUtil.newObjectNode();
        JsonNode valueNode = objectToJsonNode(value);
        JsonNode codeNode = objectToJsonNode(code);
        data.set(CODE, codeNode);
        data.set(VALUE, valueNode);
        arrayNode.add(data);
        commandsNode.set(COMMANDS, arrayNode);
        String path = String.format(POST_DEVICE_COMMANDS_URL_PATH, deviceId);
        sendPostRequest(path, commandsNode);
    }

    @SneakyThrows
    public void sendPostRequestDevice(String deviceId, String pathConst, String code, Object value) {
        ObjectNode data = JacksonUtil.newObjectNode();
        JsonNode valueNode = objectToJsonNode(value);
        data.set(code, valueNode);
        String path = String.format(pathConst, deviceId);
        sendPostRequest(path, data);

    }

    @SneakyThrows
    public void sendGetRequestLogs(String deviceId, Long start_time, Long end_time, Integer size) {
//    public void sendGetRequestLogs(String deviceId) {
//        Long end_time = 1682328116777L;
//        Long start_time = 1682344439506L;
////        String path = String.format(GET_LOGS_URL_PATH, deviceId, start_time, end_time);
//        String path = String.format(GET_LOGS_URL_PATH, deviceId);
//        Map<String, Object> queries = new HashMap<>();
//
//        queries.put("start_time", start_time);
//        queries.put("end_time", end_time);
//        queries.put("last_row_key", "");
//        queries.put("event_types", "publish");
//        queries.put("size", 20);
//        path = creatPathWithQueries(path, queries);
//        RequestEntity<Object> requestEntity = createGetRequest(path, false);
//        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity, HttpMethod.GET);
//        if (responseEntity != null) {
//            JsonNode result = responseEntity.getBody().get("result");
//            log.warn("result: [{}]", result);
//        }

//        sendGetRequest(path);
    }

    public void updateAllTermostat (Integer temp_set) {
        this.devices.getDevIds().forEach((k,v) -> {
            if(v.getCategory().equals("wk")) {
                sendPostRequestCommand(k, "temp_set", temp_set);
            }
        });
    }

    @SneakyThrows
    private TuyaToken createTuyaToken() {
        Future<TuyaToken> future = executor.submit(() -> {
            try {
                return createGetTuyaToken();
            } catch (Exception e) {
                log.error("create token error", e);
                return null;
            } finally {
            }
        });
        TuyaToken createToken = future.get();
        if (Objects.isNull(createToken)) {
            log.error("Create token required, not null.");
        }
        return createToken;
    }

    @SneakyThrows
    private TuyaToken createGetTuyaToken() {
        Map<String, Object> queries = new HashMap<>();
        queries.put("grant_type", TOKEN_GRANT_TYPE);
        String path = creatHttpPathWithQueries(GET_TUYA_TOKEN_URL_PATH, queries);
        RequestEntity<Object> requestEntity = createGetTuyaRequest(path, true);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            Long expireAt = responseEntity.getBody().get("t").asLong() + result.get("expire_time").asLong() * 1000;
            return TuyaToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .uid(result.get("uid").asText())
                    .expireAt(expireAt)
                    .build();
        }
        return null;
    }

    @SneakyThrows
    private TuyaToken refreshTuyaToken() {
        Future<TuyaToken> future = executor.submit(() -> {
            try {
                return refreshGetToken();
            } catch (Exception e) {
                log.error("Refresh tuya token error", e);
                return null;
            } finally {
            }
        });
        TuyaToken refreshedToken = future.get();
        if (Objects.isNull(refreshedToken)) {
            log.error("Refreshed tuya token required, not null.");
        }
        return refreshedToken;
    }

    private RequestEntity<Object> createGetTuyaRequest(String path, boolean isGetToken) throws Exception {
        HttpMethod httpMethod = HttpMethod.GET;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createTuyaHeaders(ts);
        if (!isGetToken) httpHeaders.add("access_token", accessTuyaToken.getAccessToken());
        String strToSign = isGetToken ? this.connectionConfiguration.getAk() + ts + stringToSign(path, getBodyHash(null), httpMethod) :
                this.connectionConfiguration.getAk() + accessTuyaToken.getAccessToken() + ts + stringToSign(path, getBodyHash(null), httpMethod);
        String signedStr = sign(strToSign, this.connectionConfiguration.getSk());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(this.connectionConfiguration.getRegion().getApiUrl() + path);
        return new RequestEntity<>(httpHeaders, httpMethod, uri);
    }

    private RequestEntity<Object> createRequestWithBody(String path, ObjectNode body, HttpMethod httpMethod) throws Exception {
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createTuyaHeaders(ts);
        httpHeaders.add("access_token", accessTuyaToken.getAccessToken());
        String strToSign = this.connectionConfiguration.getAk() + accessTuyaToken.getAccessToken() +
                ts + stringToSign(path, getBodyHash(body.toString()), httpMethod);
        String signedStr = sign(strToSign, this.connectionConfiguration.getSk());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(this.connectionConfiguration.getRegion().getApiUrl() + path);
        return new RequestEntity<>(body.toString(), httpHeaders, httpMethod, uri);
    }

    private HttpHeaders createTuyaHeaders(String ts) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("client_id", this.connectionConfiguration.getAk());
        httpHeaders.add("t", ts);
        httpHeaders.add("sign_method", "HMAC-SHA256");
        httpHeaders.add("nonhasValidAccessToken()ce", "");
        httpHeaders.add("Content-Type", "application/json");
        return httpHeaders;
    }

    private TuyaToken getTuyaToken() {
        if (accessTuyaToken != null) {
            if (!hasValidAccessToken()) {
                accessTuyaToken = refreshTuyaToken();
            }
        } else {
            accessTuyaToken = createTuyaToken();
        }
        return accessTuyaToken;
    }

    private boolean hasValidAccessToken() {
        return accessTuyaToken.getExpireAt() + 20_000 > System.currentTimeMillis();
    }

    private String stringToSign(String path, String bodyHash, HttpMethod httpMethod) throws Exception {
        List<String> lines = new ArrayList<>(16);
        lines.add(httpMethod.name());
        lines.add(bodyHash);
        lines.add("");
        lines.add(path);
        return String.join("\n", lines);
    }
//
//    private String getBodyHash(String body) throws Exception {
//        if (StringUtils.isBlank(body)) {
//            return EMPTY_HASH;
//        } else {
//            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
//            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
//            return Hex.encodeHexString(messageDigest.digest());
//        }
//    }

    private String sign(String content, String secret) throws Exception {
        byte[] rawHmac = HmacSHA256Util.sign(content, secret.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(rawHmac).toUpperCase();
    }

    private TuyaToken refreshGetToken() throws Exception {
        String path = String.format(GET_TUYA_REFRESH_TOKEN_URL_PATH, accessTuyaToken.getRefreshToken());
        RequestEntity<Object> requestEntity = createGetTuyaRequest(path, true);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            Long expireAt = responseEntity.getBody().get("t").asLong() + result.get("expire_time").asLong() * 1000;
            return TuyaToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .uid(result.get("uid").asText())
                    .expireAt(expireAt)
                    .build();
        }
        return null;
    }

    private void sendPostRequest(String path, ObjectNode commandsNode) throws Exception {
        RequestEntity<Object> requestEntity = createRequestWithBody(path, commandsNode, HttpMethod.POST);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            log.info("result POST: [{}]", result);
        }
    }

    private void sendPutRequest(String path, ObjectNode commandsNode) throws Exception {
        RequestEntity<Object> requestEntity = createRequestWithBody(path, commandsNode, HttpMethod.PUT);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            log.info("result PUT: [{}]", result);
        }
    }

    private void sendInitRequest() {
        for (String deviceId : this.connectionConfiguration.getDeviceIds()) {
            try {
                String path = String.format(GET_DEVICES_ID_URL_PATH, deviceId);
                RequestEntity<Object> requestEntity = createGetTuyaRequest(path, false);
                ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
                if (responseEntity != null) {
                    JsonNode result = responseEntity.getBody().get("result");
                    Device device = treeToValue(result, Device.class);
                    devices.getDevIds().put(deviceId, device);
                    path = String.format(GET_DEVICE_STATUS_URL_PATH, deviceId);
                    requestEntity = createGetTuyaRequest(path, false);
                    responseEntity = sendRequest(requestEntity);
                    if (responseEntity != null) {
                        result = responseEntity.getBody().get("result");
                        devices.getDevIds().get(deviceId).setStatus(result);
                    }
                } else {
                    log.error("Failed init device with id [{}]", deviceId);
                }
            } catch (Exception e) {
                log.error("Failed init device with id [{}] [{}]", deviceId, e.getMessage());
            }
        }
    }


    @SneakyThrows
    private void sendGetRequest(String path) {
        RequestEntity<Object> requestEntity = createGetTuyaRequest(path, false);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            log.warn("result: [{}]", result);
        }
    }


}
