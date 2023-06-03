package org.nickas21.smart.tuya.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.nickas21.smart.tuya.tuyaEntity.DeviceStatus;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.tuyaEntity.Device;
import org.nickas21.smart.tuya.tuyaEntity.Devices;
import org.nickas21.smart.util.HmacSHA256Util;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.nickas21.smart.tuya.constant.TuyaApi.CODE;
import static org.nickas21.smart.tuya.constant.TuyaApi.COMMANDS;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_DEVICES_ID_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_DEVICE_STATUS_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_REFRESH_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.POST_DEVICE_COMMANDS_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.TOKEN_GRANT_TYPE;
import static org.nickas21.smart.tuya.constant.TuyaApi.VALUE;
import static org.nickas21.smart.util.HttpUtil.creatHttpPathWithQueries;
import static org.nickas21.smart.util.HttpUtil.getBodyHash;
import static org.nickas21.smart.util.HttpUtil.tempCurrentKey;
import static org.nickas21.smart.util.HttpUtil.tempSetKey;
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

    @Override
    public void init(ExecutorService submitExecutor) throws Exception {
        this.executor = submitExecutor;
        this.devices = new Devices();
        try {
            if (getTuyaToken() != null) {
                sendInitRequest();
                log.info("Init tuya successful: [{}] devices", devices.getDevIds().size());
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Init tuya error. Tuya token required, not null.", e);
            throw new Exception(e);
        }
    }

    public void setConnectionConfiguration(TuyaMessageDataSource connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
    }

    public void destroy() throws Exception {
        if (this.getConnectionConfiguration() != null) {
            this.updateAllThermostat(this.getConnectionConfiguration().getTempSetMin(),
                    this.getConnectionConfiguration().getCategoryForControlPowers());
        }
    }

    public void devicesFromUpDateStatusValue(TuyaConnectionMsg msg) throws Exception {
        String deviceId = msg.getJson().get("devId").asText();
        JsonNode deviceStatus = msg.getJson().get("status");
        JsonNode bizCode = msg.getJson().get("bizCode");
        Device device = this.devices.getDevIds().get(deviceId);
        if (device == null) {
            device = initDeviceTuya(deviceId);
            if (device == null) {
                log.warn("Device is null. Failed to create new device ... [{}]", msg);
            } else {
                log.warn("Device is null. Successful creation of a new device with id [{}] category [{}] consumptionPower [{}]",
                        device.getId(), device.getCategory(), device.getConsumptionPower());
            }
        }
        if (device != null && deviceStatus != null) {
            device.setStatus(deviceStatus);
            String nameField = deviceStatus.get(0).get("code").asText();
            DeviceStatus devStatus = device.getStatus().get(nameField);
            if (device.getCategory() != null && Arrays.stream(this.getConnectionConfiguration().getCategoryForControlPowers()).anyMatch(device.getCategory()::equals)) {
                log.info("Device: [{}] time: -> [{}] parameter: [{}] valueOld: [{}] valueNew: [{}] ",
                        device.getName(), formatter.format(new Date(Long.valueOf(String.valueOf(deviceStatus.get(0).get("t"))))),
                        nameField, devStatus.getValueOld(), devStatus.getValue());
            }
        }
        if (bizCode != null) {
            device.setBizCode((ObjectNode) msg.getJson());
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
        sendPostRequest(path, commandsNode, deviceName);
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
    public void updateAllThermostat(Integer tempSet, String... filters) throws Exception {
        for (Map.Entry<String, Device> entry : this.devices.getDevIds().entrySet()) {
            String k = entry.getKey();
            Device v = entry.getValue();
            for (String f : filters) {
                if (f.equals(v.getCategory())) {
                    if (v.getStatus().get(tempSetKey).getValue() != tempSet) {
                        sendPostRequestCommand(k, tempSetKey, tempSet, v.getName());
                    } else {
                        String stateBmsSoc = tempSet == this.getConnectionConfiguration().getTempSetMax() ? "Max bmsSoc" : "Min bmsSoc";
                        log.info("Device: [{}] not Update. [{}] [{}] changeValue [{}] currentValue [{}]",
                                v.getName(), stateBmsSoc, tempSetKey, tempSet, v.getStatus().get(tempSetKey).getValue());
                    }
                }
            }
        }
    }

    @Override
    public void updateThermostatBatteryCharge(int deltaPower, String... filters) throws Exception {
        AtomicReference<Integer> atomicDeltaPower = new AtomicReference<>(deltaPower);
        LinkedHashMap<String, Device> devicesTempSort = getDevicesTempSort(true, filters);
        Integer tempSet = this.getConnectionConfiguration().getTempSetMax();
        for (Map.Entry<String, Device> entry : devicesTempSort.entrySet()) {
            String k = entry.getKey();
            Device v = entry.getValue();
            if ((atomicDeltaPower.get() - v.getConsumptionPower()) > 0) {
                if (v.getStatus().get(tempSetKey).getValue() != tempSet) {
                    sendPostRequestCommand(k, tempSetKey, tempSet, this.devices.getDevIds().get(k).getName());
                    atomicDeltaPower.getAndUpdate(value -> value - v.getConsumptionPower());
                } else {
                    log.info("Device: [{}] not Update. Charge left power [{}], [{}] changeValue [{}] lastValue [{}]",
                            v.getName(), atomicDeltaPower.get(), tempSetKey, tempSet, v.getStatus().get(tempSetKey).getValue());
                }
            } else {
                log.info("Device: [{}] not Update. Charge left power [{}] consumptionPower [{}], tempSetKey changeValue [{}] currentValue [{}]",
                        v.getName(), atomicDeltaPower.get(), v.getConsumptionPower(), tempSet, v.getStatus().get(tempSetKey).getValue());
            }
        }
    }

    @Override
    public void updateThermostatBatteryDischarge(int deltaPower, String... filters) throws Exception {
        AtomicReference<Integer> atomicDeltaPower = new AtomicReference<>(deltaPower);
        LinkedHashMap<String, Device> devicesTempSort = getDevicesTempSort(false, filters);
        Integer tempSet = this.getConnectionConfiguration().getTempSetMin();
        for (Map.Entry<String, Device> entry : devicesTempSort.entrySet()) {
            String k = entry.getKey();
            Device v = entry.getValue();
            if (atomicDeltaPower.get() < 0) {
                if (v.getStatus().get(tempSetKey).getValue() != tempSet) {
                    sendPostRequestCommand(k, tempSetKey, this.getConnectionConfiguration().getTempSetMin(), v.getName());
                    atomicDeltaPower.getAndUpdate(value -> value + v.getConsumptionPower());
                } else {
                    log.info("Device: [{}] not Update. Discharge left power [{}], [{}] changeValue [{}] lastValue [{}]",
                            v.getName(), atomicDeltaPower.get(), tempSetKey, tempSet, v.getStatus().get(tempSetKey).getValue());
                }
            } else {
                log.info("Device: [{}] not Update. Discharge left power [{}] consumptionPower [{}], tempSetKey changeValue [{}] currentValue [{}]",
                        v.getName(), atomicDeltaPower.get(), v.getConsumptionPower(), tempSet, v.getStatus().get(tempSetKey).getValue());
            }
        }
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

    private TuyaToken createTuyaToken() throws ExecutionException, InterruptedException {
        Future<TuyaToken> future = executor.submit(() -> {
            try {
                return createGetTuyaToken();
            } catch (Exception e) {
                log.error("Create token error", e);
                return null;
            }
        });
        TuyaToken createToken = future.get();
        if (Objects.isNull(createToken)) {
            log.error("Create token required, not null.");
        }
        return createToken;
    }

    private TuyaToken createGetTuyaToken() throws Exception {
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

    private TuyaToken refreshTuyaToken(CountDownLatch c) throws Exception {
        try {
            String path = String.format(GET_TUYA_REFRESH_TOKEN_URL_PATH, this.accessTuyaToken.getRefreshToken());
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
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            c.countDown();
        }
    }

    private RequestEntity<Object> createGetTuyaRequest(String path, boolean isGetToken) throws Exception {
        HttpMethod httpMethod = HttpMethod.GET;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createTuyaHeaders(ts);
        if (!isGetToken) httpHeaders.add("access_token", getTuyaToken().getAccessToken());
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
        httpHeaders.add("access_token", getTuyaToken().getAccessToken());
        String strToSign = this.connectionConfiguration.getAk() + getTuyaToken().getAccessToken() +
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

    private TuyaToken getTuyaToken() throws Exception {
        if (this.accessTuyaToken == null) {
            this.accessTuyaToken = this.createTuyaToken();
        } else if (!hasValidAccessToken()) {
            CountDownLatch cdl = new CountDownLatch(1);
            log.info("Refresh Tuya token: expireAt [{}] currentDate [{}]", formatter.format(new Date(this.accessTuyaToken.getExpireAt() + 20_000)), formatter.format(new Date()));
            this.accessTuyaToken = this.refreshTuyaToken(cdl);
            cdl.await();
        }
        return this.accessTuyaToken;
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
                initDeviceTuya (deviceId, consumptionPower);
            } catch (Exception e) {
                log.error("Failed init device with id [{}] [{}]", deviceIdWithPower, e.getMessage());
            }
        }
    }

    private Device initDeviceTuya(String deviceId, int... consumptionPower) throws Exception {
        Device device = null;
        String path = String.format(GET_DEVICES_ID_URL_PATH, deviceId);
        RequestEntity<Object> requestEntity = createGetTuyaRequest(path, false);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            device = treeToValue(result, Device.class);
            devices.getDevIds().put(deviceId, device);
            path = String.format(GET_DEVICE_STATUS_URL_PATH, deviceId);
            requestEntity = createGetTuyaRequest(path, false);
            responseEntity = sendRequest(requestEntity);
            if (responseEntity != null) {
                result = responseEntity.getBody().get("result");
                devices.getDevIds().get(deviceId).setStatus(result);
                if (consumptionPower.length > 0) {
                    devices.getDevIds().get(deviceId).setConsumptionPower(consumptionPower[0]);
                } else if ("wk".equals(device.getCategory())) {
                    devices.getDevIds().get(deviceId).setConsumptionPower(2000);
                }
            }
        } else {
            log.warn("Device with id [{}] is not available", deviceId);
        }
        return device;
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
                    }
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Method: [{}], url: [{}], body: [{}]. ", requestEntity.getMethod(), requestEntity.getUrl(), requestEntity.getBody(), e);
            throw new Exception(e.getMessage());
        }
    }

    private boolean hasValidAccessToken() {
        return this.accessTuyaToken.getExpireAt() + 20_000 > System.currentTimeMillis();
    }
}
