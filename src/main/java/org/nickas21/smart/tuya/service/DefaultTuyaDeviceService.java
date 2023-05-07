package org.nickas21.smart.tuya.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.nickas21.smart.SmartSolarmanTuyaService;
import org.nickas21.smart.tuya.tuyaEntity.DeviceStatus;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.tuyaEntity.Device;
import org.nickas21.smart.tuya.tuyaEntity.Devices;
import org.nickas21.smart.util.HmacSHA256Util;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

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
import static org.nickas21.smart.util.HttpUtil.tempCurrentKey;
import static org.nickas21.smart.util.HttpUtil.tempSetKey;
import static org.nickas21.smart.util.HttpUtil.tuyaTokenInvalid;
import static org.nickas21.smart.util.HttpUtil.tuyaTokenInvalidCode;
import static org.nickas21.smart.util.JacksonUtil.objectToJsonNode;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;
import static org.nickas21.smart.util.HttpUtil.formatter;

@Slf4j
@Service
public class DefaultTuyaDeviceService implements TuyaDeviceService {

    private TuyaToken accessTuyaToken;
    private Devices devices;

    private ExecutorService executor;
    private TuyaMessageDataSource connectionConfiguration;
    private final RestTemplate httpClient = new RestTemplate();

    @Autowired
    SmartSolarmanTuyaService smartSolarmanTuyaService;

    @Override
    public void init() {
        accessTuyaToken = getTuyaToken();
        devices = new Devices();
        if (accessTuyaToken != null) {
            sendInitRequest();
        }
        log.info("init successful: [{}] devices", devices.getDevIds().size());
        // Test Sun Uzel
        String deviceIdTest = "bfa715581477683002qb4l";
        String deviceIdsTest = "bf11fce4b500291373jnn2:1800,bfa715581477683002qb4l:1800,bfc99c5e1b444322eaaqgu:1800";
//        updateAllTermostat (18);
        smartSolarmanTuyaService.solarmanRealTimeDataStart();
    }

    public void setConnectionConfiguration(TuyaMessageDataSource connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
    }

    @PreDestroy
    public void destroy() {
        try {
            this.updateAllThermostat(this.getConnectionConfiguration().getTempSetMin(),
                    this.getConnectionConfiguration().getCategoryForControlPowers());
        } catch (Exception e) {
            log.error("Destroy error ", e);
        }
    }

    @Override
    public void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    @SneakyThrows
    public void devicesFromUpDateStatusValue(TuyaConnectionMsg msg) {
//        log.info("TuyaConnectionMsg: [{}]",  msg);
//        updateAllTermostat (5);
//        String deviceIdTest = "bf11fce4b500291373jnn2";
//        sendPostRequestCommand(deviceIdTest, "temp_set", 5); // temp_current
        String deviceId = msg.getJson().get("devId").asText();
        JsonNode deviceStatus = msg.getJson().get("status");
        Device device = deviceStatus != null ? this.devices.getDevIds().get(deviceId) : null;
        if (device != null) {
            device.setStatus(deviceStatus);
            if (Arrays.stream(this.getConnectionConfiguration().getCategoryForControlPowers()).anyMatch(device.getCategory()::equals)) {
                String nameField = deviceStatus.get(0).get("code").asText();
                DeviceStatus devStatus = device.getStatus().get(nameField);
                log.info("Device: [{}] parameter: [{}] time: [{}] valueOld: [{}] valueNew: [{}] ", device.getName(), nameField,
                        formatter.format(new Date(Long.valueOf(String.valueOf(deviceStatus.get(0).get("t"))))),
                        devStatus.getValueOld(), devStatus.getValue());
            }

        } else {
            log.error("Device or status is null, [{}]", msg);
        }
    }

    /**
     * devicesToUpDateStatusValue
     */
    public void sendPostRequestCommand(String deviceId, String code, Object value, String... deviceName) throws Exception {
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
        try {
            sendPostRequest(path, commandsNode, deviceName);
        } catch (Exception e) {
            if (tuyaTokenInvalid.equals(e.getMessage())) {
                this.accessTuyaToken = refreshTuyaToken();
                if (this.accessTuyaToken != null) {
                    try {
                        sendPostRequest(path, commandsNode, deviceName);
                    } catch (Exception exception) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            } else {
                throw new RuntimeException(e.getMessage());
            }
        }
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

    @Override
    public void updateAllThermostat(Integer temp_set, String... filters) throws Exception {
        this.devices.getDevIds().forEach((k, v) -> {
            for (String f : filters) {
                if (v.getCategory().equals(f)) {
                    if (v.getStatus().get(tempSetKey).getValue() != temp_set) {
                        try {
                            sendPostRequestCommand(k, tempSetKey, temp_set, v.getName());
                        } catch (Exception e) {
                            try {
                                throw new Exception(e);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                    } else {
                        log.info("Device: [{}], updateAllTermostat, tempSetKey newValue [{}] equals oldValue", v.getName(), temp_set);
                    }
                }
            }
        });
    }

    @Override
    public void updateThermostatBatteryCharge(int deltaPower, String... filters) throws Exception {
        AtomicReference<Integer> atomicDeltaPower = new AtomicReference<>(deltaPower);
        LinkedHashMap<String, Device> devicesTempSort = getDevicesTempSort(true, filters);
        Integer temp_set = this.getConnectionConfiguration().getTempSetMax();
        devicesTempSort.forEach((k, v) -> {
            if (atomicDeltaPower.get() >= (v.getConsumptionPower() * 0.75)) {
                try {
                    if (v.getStatus().get(tempSetKey).getValue() != temp_set) {
                        sendPostRequestCommand(k, tempSetKey, temp_set, this.devices.getDevIds().get(k).getName());
                        atomicDeltaPower.getAndUpdate(value -> value - v.getConsumptionPower());
                    } else {
                        log.info("Device: [{}], updateAllTermostat, tempSetKey newValue [{}] equals oldValue", v.getName(), temp_set);
                    }
                } catch (Exception e) {
                    try {
                        throw new Exception(e);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void updateThermostatBatteryDischarge(int deltaPower, String... filters) throws Exception {
        AtomicReference<Integer> atomicDeltaPower = new AtomicReference<>(deltaPower);
        LinkedHashMap<String, Device> devicesTempSort = getDevicesTempSort(false, filters);
        Integer temp_set = this.getConnectionConfiguration().getTempSetMin();
        devicesTempSort.forEach((k, v) -> {
            if (Arrays.stream(this.getConnectionConfiguration().getCategoryForControlPowers()).anyMatch(v.getCategory()::equals)
                    && (atomicDeltaPower.get() - v.getConsumptionPower()) < 0) {

                if (atomicDeltaPower.get() < 0 && v.getStatus().get(tempSetKey).getValue() != temp_set) {
                    try {
                        sendPostRequestCommand(k, tempSetKey, this.getConnectionConfiguration().getTempSetMin(), v.getName());
                        atomicDeltaPower.getAndUpdate(value -> value + v.getConsumptionPower());
                    } catch (Exception e) {
                        log.error("", e);
                    }
                } else {
                    log.info("Device: [{}], updateThermostatBatteryDischarge, tempSetKey newValue [{}] equals oldValue", v.getName(), temp_set);
                }
            }
        });
    }

    private LinkedHashMap<String, Device> getDevicesTempSort(boolean order, String... filters) {
        HashMap<String, Integer> devicesPowerNotSort = new HashMap<>();
        LinkedHashMap<String, Device> sortedMap = new LinkedHashMap<>();
        this.devices.getDevIds().forEach((k, v) -> {
            for (String f : filters) {
                if (v.getCategory().equals(f)) {
                    devicesPowerNotSort.put(k, (Integer) v.getStatus().get(tempCurrentKey).getValue());
                }
            }
        });
        List<Map.Entry<String, Integer>> list = new ArrayList<>(devicesPowerNotSort.entrySet());
        if (order) {
            list.sort(Map.Entry.comparingByValue());
        } else {
            list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        }
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), this.devices.getDevIds().get(entry.getKey()));
        }
        return sortedMap;
    }

    @Override
    public TuyaMessageDataSource getConnectionConfiguration() {
        return this.connectionConfiguration;
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

    public TuyaToken refreshTuyaToken() throws Exception {
        return refreshGetToken();

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
        httpHeaders.add("nonce", "");
        httpHeaders.add("Content-Type", "application/json");
        return httpHeaders;
    }

    private TuyaToken getTuyaToken() {
        if (this.accessTuyaToken != null) {
            if (!hasValidAccessToken()) {
                try {
                    this.accessTuyaToken = refreshGetToken();
                } catch (Exception e) {
                    log.error("Refresh Tuya Token: [{}]", e.getMessage());
                }
            }
        } else {
            this.accessTuyaToken = createTuyaToken();
        }
        return this.accessTuyaToken;
    }

    private boolean hasValidAccessToken() {
        return this.accessTuyaToken.getExpireAt() + 20_000 > System.currentTimeMillis();
    }

    private String stringToSign(String path, String bodyHash, HttpMethod httpMethod) throws Exception {
        List<String> lines = new ArrayList<>(16);
        lines.add(httpMethod.name());
        lines.add(bodyHash);
        lines.add("");
        lines.add(path);
        return String.join("\n", lines);
    }

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

    private void sendPostRequest(String path, ObjectNode commandsNode, String... deviceName) throws Exception {
        RequestEntity<Object> requestEntity = createRequestWithBody(path, commandsNode, HttpMethod.POST);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            JsonNode success = responseEntity.getBody().get("success");
            if (deviceName.length > 0) {
                log.info("Device: [{}] POST result [{}], body [{}]", deviceName[0], result.booleanValue() & success.booleanValue(), requestEntity.getBody().toString());
            } else {
                log.info("POST result [{}], body [{}]", result.booleanValue() & success.booleanValue(), requestEntity.getBody().toString());
            }
        }
    }

    private void sendInitRequest() {
        for (String deviceIdWithPower : this.connectionConfiguration.getDeviceIds()) {
            try {
                String[] devId = deviceIdWithPower.split(":");
                String deviceId = devId[0];
                int consumptionPower = Integer.parseInt(devId[1]);
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
                        devices.getDevIds().get(deviceId).setConsumptionPower(consumptionPower);
                    }
                } else {
                    log.error("Failed init device with id [{}]", deviceId);
                }
            } catch (Exception e) {
                log.error("Failed init device with id [{}] [{}]", deviceIdWithPower, e.getMessage());
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

    //    https://openapi.tuyaeu.com/v1.0/iot-03/devices/bfa715581477683002qb4l/freeze-state
    private ResponseEntity<ObjectNode> sendRequest(RequestEntity<Object> requestEntity) throws Exception {
        try {
            ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(), requestEntity.getMethod(), requestEntity, ObjectNode.class);
            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                throw new RuntimeException(String.format("No response for device command request! Reason code from Tuya Cloud: %s", responseEntity.getStatusCode().toString()));
            } else {
                if (Objects.requireNonNull(responseEntity.getBody()).get("success").asBoolean()) {
                    return responseEntity;
                } else {
                   if (responseEntity.getBody().has("code") && responseEntity.getBody().has("msg")) {
                    log.error("code: [{}], msg: [{}]", responseEntity.getBody().get("code").asInt(), responseEntity.getBody().get("msg").asText());
                    if (responseEntity.getBody().get("code").asInt() == tuyaTokenInvalidCode) {
                        log.error ("code1010: [{}],  token : [{}]", responseEntity.getBody().get("code").asInt(), this.accessTuyaToken);
                    throw new Exception(tuyaTokenInvalid);
                    }
                   }
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Method: [{}], url: [{}], body: [{}]. ", requestEntity.getMethod(), requestEntity.getUrl(), requestEntity.getBody(), e);
            throw new Exception(e.getMessage());
        }
    }
}
