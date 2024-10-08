package org.nickas21.smart.tuya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.service.TelegramService;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.tuyaEntity.Device;
import org.nickas21.smart.tuya.tuyaEntity.DeviceStatus;
import org.nickas21.smart.tuya.tuyaEntity.DeviceUpdate;
import org.nickas21.smart.tuya.tuyaEntity.Devices;
import org.nickas21.smart.util.HmacSHA256Util;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.nickas21.smart.tuya.constant.TuyaApi.CODE;
import static org.nickas21.smart.tuya.constant.TuyaApi.COMMANDS;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_DEVICES_ID_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_DEVICE_STATUS_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_REFRESH_TOKEN_ERROR_1010;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_REFRESH_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.POST_DEVICE_COMMANDS_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.TOKEN_GRANT_TYPE;
import static org.nickas21.smart.tuya.constant.TuyaApi.VALUE;
import static org.nickas21.smart.util.HttpUtil.creatHttpPathWithQueries;
import static org.nickas21.smart.util.HttpUtil.getBodyHash;
import static org.nickas21.smart.util.HttpUtil.offOnKey;
import static org.nickas21.smart.util.HttpUtil.tempCurrentKey;
import static org.nickas21.smart.util.HttpUtil.tempSetKey;
import static org.nickas21.smart.util.HttpUtil.toLocaleTimeString;
import static org.nickas21.smart.util.JacksonUtil.objectToJsonNode;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;
import static org.nickas21.smart.util.JacksonUtil.toJsonNode;

@Slf4j
@Service
@EnableConfigurationProperties({TuyaConnectionProperties.class, TuyaDeviceProperties.class})
public class TuyaDeviceService {

    public final String onLine = "online";
    public final String nameFieldTempCurrent = "temp_current";
    public final String nameFieldSwitch = "switch";

    private TuyaToken accessTuyaToken;
    public Devices devices;

    private String gridRelayCodeId;

    private Entry<Long, Boolean> lastUpdateTimeGridStatusInfo;

    private final Lock queueLock = new ReentrantLock();

    private final TuyaConnectionProperties connectionConfiguration;
    private final TuyaDeviceProperties deviceProperties;
    private final RestTemplate httpClient = new RestTemplate();
    private final WebClient authClient = WebClient.builder().build();
    private final WebClient webClient;
    private Long timeoutSecUpdateMillis;

    @Autowired
    SolarmanStationsService solarmanStationsService;
    @Autowired
    private TelegramService telegramService;

    public TuyaDeviceService(TuyaConnectionProperties connectionConfiguration, TuyaDeviceProperties deviceProperties) {
        this.connectionConfiguration = connectionConfiguration;
        this.deviceProperties = deviceProperties;
        this.webClient = WebClient.builder()
                .baseUrl(connectionConfiguration.getRegion().getApiUrl())
                .filter(ExchangeFilterFunction.ofRequestProcessor(
                        (ClientRequest request) -> Mono.just(ClientRequest.from(request)
                                .headers(httpHeaders -> httpHeaders.setBearerAuth(getTuyaToken().getAccessToken()))
                                .build())))
                .build();
    }

    public void init() {
        this.devices = new Devices(null);
        if (getTuyaToken() != null) {
            sendInitRequest();
        } else {
            log.error("Init tuya error. Tuya token required, not null.");
            throw new RuntimeException("Failed to get token for TUYA");
        }
    }

    public TuyaDeviceProperties getDeviceProperties() {
        return deviceProperties;
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
            String nameField = deviceStatus.get(0).get("code").asText();
            boolean updateStatus = false;

            if (device.getValueSetMaxOn() instanceof Boolean && nameFieldTempCurrent.equals(nameField)) {
                Boolean valueCur = (Boolean) device.getStatus().get(nameFieldSwitch).getValue();
                if (!valueCur) {
                    DeviceStatus devStatusSwitchNew = new DeviceStatus();
                    devStatusSwitchNew.setValue(true);
                    device.setStatus(nameFieldSwitch, devStatusSwitchNew);
                    log.info("Device (update switch by value): [{}] time: -> [{}] parameter: [{}] valueOld: [{}] valueNew: [{}] ",
                            device.getName(), toLocaleTimeString(Long.parseLong(String.valueOf(deviceStatus.get(0).get("t")))),
                            nameFieldSwitch, valueCur, devStatusSwitchNew.getValue());
                    updateStatus = true;
                }
            }
            if (!updateStatus){
                device.setStatus(deviceStatus);
            }
            if (device.getCategory() != null && Arrays.asList(deviceProperties.getCategoryForControlPowers()).contains(device.getCategory())) {
                DeviceStatus devStatus = device.getStatus().get(nameField);
                log.info("Device: [{}] time: -> [{}] parameter: [{}] valueOld: [{}] valueNew: [{}] ",
                        device.getName(), toLocaleTimeString(Long.parseLong(String.valueOf(deviceStatus.get(0).get("t")))),
                        nameField, devStatus.getValueOld(), devStatus.getValue());
            }
        }
        if (bizCode != null && device != null) {
            if (device.setBizCode((ObjectNode) msg.getJson()) && device.getId().equals(this.getGridRelayCodeId())) {
                updateGridStateOnLine();
            }
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

    public boolean updateAllThermostatToMin(String msgInfo) {
        String[] filters = getDeviceProperties().getCategoryForControlPowers();
        if (this.devices != null) {
            queueLock.lock();
            int cntUpdate = 0;
            try {
                log.info("Start update Devices [{}].", this.devices.getDevIds().size());

                Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();
                for (Map.Entry<String, Device> entry : this.devices.getDevIds().entrySet()) {
                    Device v = entry.getValue();
                    for (String f : filters) {
                        if (f.equals(v.getCategory())) {
                            DeviceUpdate deviceUpdate = getDeviceUpdate(this.getDeviceProperties().getTempSetMin(), v);
                            queueUpdate.put(v, deviceUpdate);
                            cntUpdate++;
                        }
                    }
                }
                updateThermostats(queueUpdate, true);
            } finally {
                queueLock.unlock();
                log.info("Finish updating Devices [{}] from [{}] after [{}]", cntUpdate, this.devices.getDevIds().size(), msgInfo);
            }
            return true;
        } else {
            log.error("Devices is null, Devices not Update after start.");
            return false;
        }
    }

    public void updateAllThermostat(Object tempSet){
        String[] filters = getDeviceProperties().getCategoryForControlPowers();
        if (this.devices != null) {
            Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();
            for (Map.Entry<String, Device> entry : this.devices.getDevIds().entrySet()) {
                Device device = entry.getValue();
                for (String f : filters) {
                    if (f.equals(device.getCategory())) {
                        DeviceUpdate deviceUpdate = getDeviceUpdate(tempSet, device);
                        if (deviceUpdate.isUpdate()) {
                            queueUpdate.put(device, deviceUpdate);
                        } else {
                            log.info("Device: [{}] not Update. [{}] changeValue [{}] currentValue [{}]",
                                    device.getName(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), deviceUpdate.getValueOld());
                        }
                    }
                }
            }
            queueLock.lock();
            try {
                updateThermostats(queueUpdate,false);
            } finally {
                queueLock.unlock();
            }
        } else {
            log.error("Devices is null, Devices not Update.");
        }
    }

    public void updateAllDevicePreDestroy() {
        if (this.devices != null) {
            updateThermostatBatteryDischargePreDestroy(getDeviceProperties().getCategoryForControlPowers());
        } else {
            log.error("Devices is null, Devices not Update.");
        }
    }

    private DeviceUpdate getDeviceUpdate(Object valueNew, Device v) {
        String fieldNameValueUpdate;
        Object valueOld;
        if (valueNew instanceof Boolean) {
            fieldNameValueUpdate = offOnKey;
            valueOld = v.getStatusValue(fieldNameValueUpdate, false);
        } else if (v.getValueSetMaxOn() != null && v.getValueSetMaxOn() instanceof Boolean) {
            fieldNameValueUpdate = offOnKey;
            valueOld = v.getStatusValue(fieldNameValueUpdate, false);
            if (valueNew == this.getDeviceProperties().getTempSetMin()) {
                valueNew = false;
            } else {
                valueNew = v.getValueSetMaxOn();
            }
        } else {
            fieldNameValueUpdate = tempSetKey;
            valueNew = Objects.equals(valueNew, deviceProperties.getTempSetMin()) ? valueNew : v.getValueSetMaxOn();
            valueOld = v.getStatusValue(fieldNameValueUpdate, deviceProperties.getTempSetMin());
        }
        return new DeviceUpdate(fieldNameValueUpdate, valueNew, valueOld);
    }

    public void updateThermostatBatteryCharge(int deltaPower, String... filters) {
        AtomicReference<Integer> atomicDeltaPower = new AtomicReference<>(deltaPower);
        LinkedHashMap<String, Device> devicesTempSort = getDevicesTempSort(true, filters);
        Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();
        for (Map.Entry<String, Device> entry : devicesTempSort.entrySet()) {
            Device v = entry.getValue();
            Object valueNew = v.getValueSetMaxOn();
            DeviceUpdate deviceUpdate = getDeviceUpdate(valueNew, v);
            Object valueOld = v.getStatusValue(deviceUpdate.getFieldNameValueUpdate());
            if (atomicDeltaPower.get() - v.getConsumptionPower() > 0) {
                if (deviceUpdate.isUpdate()) {
                    queueUpdate.put(v, deviceUpdate);
                    log.info("Device: [{}] Add to Queue. Charge left power [{}] - [{}] = [{}], [{}] changeValue [{}] lastValue [{}]",
                            v.getName(),
                            atomicDeltaPower.get(), v.getConsumptionPower(), atomicDeltaPower.get() - v.getConsumptionPower(),
                            deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(),
                            valueOld);
                    atomicDeltaPower.getAndUpdate(value -> value - v.getConsumptionPower());
                } else {
                    log.info("Device: [{}] not Update. Charge left power [{}], [{}] changeValue [{}] lastValue [{}]",
                            v.getName(), atomicDeltaPower.get(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), valueOld);
                }
            } else {
                log.info("Device: [{}] not Update. Charge left power [{}] consumptionPower [{}], tempSetKey changeValue [{}] currentValue [{}]",
                        v.getName(), atomicDeltaPower.get(), v.getConsumptionPower(), deviceUpdate.getValueNew(), valueOld);
            }
        }
        queueLock.lock();
        try {
            updateThermostats(queueUpdate,false);
        } finally {
            queueLock.unlock();
        }
    }

    public void updateThermostats( Map<Device, DeviceUpdate> queueUpdate, boolean isUpdateAlways) {
        int size = queueUpdate.size();
        if (size > 0) {
            // Schedule the task to run at fixed intervals
            log.warn("Start updateThermostats time size: [{}]", size);
            if (timeoutSecUpdateMillis == null) {
               this.setTimeoutSecUpdateMillis(solarmanStationsService.getSolarmanStation().getTimeoutSec()/2);
            }
            int intervalMillis = (timeoutSecUpdateMillis / size) / 4 < 30000 ? (int) (timeoutSecUpdateMillis / size) / 4 : 30000;
            AtomicInteger atomicTaskCnt = new AtomicInteger(0);
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    queueUpdate.forEach((device, deviceUpdate) -> {
                        try {
                            Object changeValue = deviceUpdate.getValueNew();
                            Object lastValue = device.getStatusValue(deviceUpdate.getFieldNameValueUpdate());
                            String update = "";
                            if (!changeValue.equals(lastValue) || isUpdateAlways) {
                                sendPostRequestCommand(device.getId(), deviceUpdate.getFieldNameValueUpdate(), changeValue, device.getName());
                                atomicTaskCnt.incrementAndGet();
                            } else {
                                update = "Not";
                            }
                            log.info("Timer: Device: [{}] " + update + " Update. Parameter [{}] lastValue [{}] changeValue [{}] ",
                                    device.getName(), deviceUpdate.getFieldNameValueUpdate(), lastValue, changeValue);
                        } catch (Exception e) {
                            log.error("Timer: Device: [{}] Update. [{}], intervalMillis: [{}], timeoutSecUpdateMillis [{}], size [{}]",
                                    device.getName(), e.getMessage(), intervalMillis, timeoutSecUpdateMillis, size);
                        }
                    });
                    // Stop the timer when iteration is complete
                  timer.cancel();
                    log.info("Finish run timer: [{}] from [{}]", atomicTaskCnt.get(), size);
                }
            };
            timer.scheduleAtFixedRate(task, 0, intervalMillis);
        } else {
            log.info("No updates to process.");
        }
    }

    public void updateThermostatBatteryDischarge(int deltaPower, String... filters) {
        AtomicReference<Integer> atomicDeltaPower = new AtomicReference<>(deltaPower);
        LinkedHashMap<String, Device> devicesTempSort = getDevicesTempSort(false, filters);
        Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();
        for (Map.Entry<String, Device> entry : devicesTempSort.entrySet()) {
            Device device = entry.getValue();
            Object valueNew = deviceProperties.getTempSetMin();
            DeviceUpdate deviceUpdate = getDeviceUpdate(valueNew, device);
            if (atomicDeltaPower.get() < 0) {
                if (deviceUpdate.isUpdate()) {
                    queueUpdate.put(device, deviceUpdate);
                    log.info("Device: [{}] Add to Queue. Discharge left power [{}] - [{}] = [{}], [{}] changeValue [{}] lastValue [{}]",
                            device.getName(),
                            atomicDeltaPower.get(), device.getConsumptionPower(), atomicDeltaPower.get() + device.getConsumptionPower(),
                            deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(),
                            device.getStatusValue(deviceUpdate.getFieldNameValueUpdate()));
                    atomicDeltaPower.getAndUpdate(value -> value + device.getConsumptionPower());
                } else {
                    log.info("Device: [{}] not Update. Discharge left power [{}], [{}] changeValue [{}] lastValue [{}]",
                            device.getName(), atomicDeltaPower.get(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), device.getStatusValue(deviceUpdate.getFieldNameValueUpdate()));
                }
            } else {
                log.info("Device: [{}] not Update. Discharge left power [{}], [{}] changeValue [{}] lastValue [{}]",
                        device.getName(), atomicDeltaPower.get(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), device.getStatusValue(deviceUpdate.getFieldNameValueUpdate()));
            }
        }
        queueLock.lock();
        try {
            updateThermostats(queueUpdate,false);
        } finally {
            queueLock.unlock();
        }
    }

    public void updateThermostatBatteryDischargePreDestroy(String... filters) {
        LinkedHashMap<String, Device> devicesTempSort = getDevicesTempSort(false, filters);
        Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();
        for (Map.Entry<String, Device> entry : devicesTempSort.entrySet()) {
            Device device = entry.getValue();
            Object valueNew = deviceProperties.getTempSetMin();
            if (device.getValueSetMaxOn() != null && device.getValueSetMaxOn() instanceof Boolean) {
                valueNew = false;
            }
            DeviceUpdate deviceUpdate = getDeviceUpdate(valueNew, device);
            log.info("Device: [{}] Update (PreDestroy). [{}] changeValue [{}] lastValue [{}]",
                    device.getName(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), device.getStatusValue(deviceUpdate.getFieldNameValueUpdate()));
            queueUpdate.put(device, deviceUpdate);
        }
        queueLock.lock();
        try {
            updateThermostats(queueUpdate,false);
        } finally {
            queueLock.unlock();
        }
    }

    private LinkedHashMap<String, Device> getDevicesTempSort(boolean order, String... filters) {
        HashMap<String, Integer> devicesPowerNotSort = new HashMap<>();
        LinkedHashMap<String, Device> sortedMap = new LinkedHashMap<>();
        this.devices.getDevIds().forEach((k, v) -> {
            for (String f : filters) {
                if (v.getCategory().equals(f)) {
                    devicesPowerNotSort.put(k, (Integer) v.getStatusValue(tempCurrentKey, deviceProperties.getTempSetMin()));
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

    @SneakyThrows
    private TuyaToken getTuyaToken() {
        if (hasValidAccessToken()) {
            if (hasRefreshAccessToken()) {
                this.accessTuyaToken = refreshTuyaToken();
                String tokenStart = this.accessTuyaToken != null && this.accessTuyaToken.getT() != null ? toLocaleTimeString(this.accessTuyaToken.getT()) : null;
                String expireTimeFinish = this.accessTuyaToken != null && this.accessTuyaToken.getExpireTimeFinish() != null ? toLocaleTimeString(this.accessTuyaToken.getExpireTimeFinish()) : null;
                log.info("Refresh Tuya token: start [{}] expireTimeFinish [{}}]", tokenStart, expireTimeFinish);
            }
        } else {
            this.accessTuyaToken = this.createTuyaToken();
            log.info("Create Tuya token: start [{}] expireTimeFinish [{}}]", toLocaleTimeString(this.accessTuyaToken.getT()), toLocaleTimeString(this.accessTuyaToken.getExpireTimeFinish()));
        }
        return this.accessTuyaToken;
    }


    public TuyaConnectionProperties getConnectionConfiguration() {
        return this.connectionConfiguration;
    }

    private TuyaToken createTuyaToken() throws Exception {
        Map<String, Object> queries = new HashMap<>();
        queries.put("grant_type", TOKEN_GRANT_TYPE);
        String path = creatHttpPathWithQueries(GET_TUYA_TOKEN_URL_PATH, queries);
        var createToken = createGetTuyaToken(path);
        if (Objects.isNull(createToken)) {
            log.error("Create token required, not null.");
        }
        return createToken;
    }

    private TuyaToken createGetTuyaToken(String path) throws Exception {
        RequestEntity<Object> requestEntity = createGetTuyaRequest(path, true);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (validateResponse(responseEntity) && responseEntity.getBody() != null && responseEntity.getBody().has("result")) {
            JsonNode result = responseEntity.getBody().get("result");
            Long t = responseEntity.getBody().get("t").asLong();
            String tid = responseEntity.getBody().get("tid").asText();
            return getExpireTuyaToken(result, t, tid);
        }
        return null;
    }

    private TuyaToken getExpireTuyaToken(JsonNode result, Long t, String tid) {
        return TuyaToken.builder()
                .accessToken(result.get("access_token").asText())
                .refreshToken(result.get("refresh_token").asText())
                .uid(result.get("uid").asText())
                .tid(tid)
                .expireTime(result.get("expire_time").asLong())
                .t(t)
                .build();
    }

    private boolean validateResponse(ResponseEntity<ObjectNode> responseEntity) {
        return responseEntity != null
                && HttpStatus.OK.equals(responseEntity.getStatusCode())
                && responseEntity.getBody() != null;
    }

    private TuyaToken refreshTuyaToken() throws Exception {
        String path = String.format(GET_TUYA_REFRESH_TOKEN_URL_PATH, this.accessTuyaToken.getRefreshToken());
        String ts = String.valueOf(System.currentTimeMillis());
        String signature = sign(this.connectionConfiguration.getAk() + ts +
                stringToSign(path, getBodyHash(null), HttpMethod.GET), this.connectionConfiguration.getSk());
        ResponseSpec responseSpec = authClient.get()
                .uri(connectionConfiguration.getRegion().getApiUrl(),
                        uriBuilder -> uriBuilder
                                .path(path)
                                .build())
                .headers(httpHeaders -> pupulateHeaders(httpHeaders, ts, signature))
                .retrieve();
        ObjectNode responseEntityNode = responseSpec.bodyToMono(ObjectNode.class).block();
        if (responseEntityNode != null && responseEntityNode.has("success") && responseEntityNode.get("success").asBoolean()) {
            JsonNode result = responseEntityNode.get("result");
            Long t = responseEntityNode.get("t").asLong();
            String tid = responseEntityNode.get("tid").asText();
            return getExpireTuyaToken(result, t, tid);
        } else {
            if (responseEntityNode != null && responseEntityNode.get("code").asInt() == GET_TUYA_REFRESH_TOKEN_ERROR_1010) {
                log.error("{}", responseEntityNode.get("msg").asText());
                return createTuyaToken();
            }
            return null;
        }
    }

    private RequestEntity<Object> createGetTuyaRequest(String path, boolean isGetToken) {
        HttpMethod httpMethod = HttpMethod.GET;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createHeaders(ts);
        if (!isGetToken) httpHeaders.add("access_token", getTuyaToken().getAccessToken());
        String strToSign = isGetToken ? this.connectionConfiguration.getAk() + ts + stringToSign(path, getBodyHash(null), httpMethod) :
                this.connectionConfiguration.getAk() + accessTuyaToken.getAccessToken() + ts + stringToSign(path, getBodyHash(null), httpMethod);
        String signedStr = sign(strToSign, this.connectionConfiguration.getSk());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(this.connectionConfiguration.getRegion().getApiUrl() + path);
        return new RequestEntity<>(httpHeaders, httpMethod, uri);
    }

    private RequestEntity<Object> createRequestWithBody(String path, ObjectNode body) {
        HttpMethod httpMethod = HttpMethod.POST;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createHeaders(ts);
        httpHeaders.add("access_token", getTuyaToken().getAccessToken());
        String strToSign = this.connectionConfiguration.getAk() + getTuyaToken().getAccessToken() +
                ts + stringToSign(path, getBodyHash(body.toString()), httpMethod);
        String signedStr = sign(strToSign, this.connectionConfiguration.getSk());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(this.connectionConfiguration.getRegion().getApiUrl() + path);
        return new RequestEntity<>(body.toString(), httpHeaders, httpMethod, uri);
    }

    private HttpHeaders createHeaders(String ts) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("client_id", this.connectionConfiguration.getAk());
        httpHeaders.add("t", ts);
        httpHeaders.add("sign_method", "HMAC-SHA256");
        httpHeaders.add("nonce", "");
        httpHeaders.add("Content-Type", "application/json");
        return httpHeaders;
    }

    private void pupulateHeaders(HttpHeaders headers, String ts, String signature) {
        headers.add("client_id", this.connectionConfiguration.getAk());
        headers.add("t", ts);
        headers.add("sign_method", "HMAC-SHA256");
        headers.add("nonce", "");
        headers.add("sign", signature);
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private String stringToSign(String path, String bodyHash, HttpMethod httpMethod) {
        List<String> lines = new ArrayList<>(16);
        lines.add(httpMethod.name());
        lines.add(bodyHash);
        lines.add("");
        lines.add(path);
        return String.join("\n", lines);
    }

    @SneakyThrows
    private String sign(String content, String secret) {
        byte[] rawHmac = HmacSHA256Util.sign(content, secret.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().withUpperCase().formatHex(rawHmac);
    }

    private void sendPostRequest(String path, ObjectNode commandsNode, String... deviceName) throws Exception {
        RequestEntity<Object> requestEntity = createRequestWithBody(path, commandsNode);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null && responseEntity.getBody() != null) {
            JsonNode result = responseEntity.getBody().get("result");
            if (responseEntity.getBody().has("success")) {
                JsonNode success = responseEntity.getBody().get("success");
                if (deviceName.length > 0) {
                    log.info("Device: [{}] POST result [{}], body [{}]", deviceName[0], result.booleanValue() & success.booleanValue(), requestEntity.getBody());
                    Device device = devices.getDeviceByName(deviceName[0]);
                    if (device != null) {
                        ArrayNode commands = (ArrayNode) toJsonNode(commandsNode.get("commands").toString());
                        String nameParam = commands.get(0).get("code").asText();
                        Object valueInDeviceCur = device.getStatus() != null && nameParam!= null && device.getStatus().get(nameParam) != null ?
                                device.getStatus().get(nameParam).getValue() : null;
                        Object val = commands.get(0).get("value").getNodeType().name().equals("NUMBER") ? commands.get(0).get("value").asInt() :
                                commands.get(0).get("value").getNodeType().name().equals("BOOLEAN") ? commands.get(0).get("value").asBoolean() : commands.get(0).get("value");
                        if (val != null && !val.equals(valueInDeviceCur)) {
                            DeviceStatus statusNew = new DeviceStatus();
                            statusNew.setValue(val);
                            device.setStatus(commands.get(0).get("code").asText(), statusNew);
                        }
                        DeviceStatus statusCur = device.getStatus().get(nameParam);
                        log.info("Device (sendPostRequest): [{}] time: -> [{}] parameter: [{}] valueOld: [{}] valueNew: [{}] ",
                                device.getName(), toLocaleTimeString(statusCur.getEventTime()),
                                nameParam, statusCur.getValueOld(), statusCur.getValue());
                    }
                } else {
                    log.info("POST result [{}], body [{}]", result.booleanValue() & success.booleanValue(), requestEntity.getBody());
                }
            } else {
                log.error("POST result [{}], body [{}] failed", result.booleanValue(), requestEntity.getBody());
            }
        }
    }

    private void sendInitRequest() {
        for (String deviceIdWithPower : this.connectionConfiguration.getDeviceIds()) {
            try {
                String[] devId = deviceIdWithPower.split(":");
                String deviceId = devId[0];
                int[] devParams = new int[2];
                devParams[0] = Integer.parseInt(devId[1]);
                if (devId.length == 3) {
                    if ("false".equals(devId[2]) || "true".equals(devId[2])) {
                        devParams[1] = "false".equals(devId[2]) ? 0 : -1;
                    } else if (onLine.equals(devId[2])) {
                        devParams[1] = -2;
                    } else {
                        devParams[1] = Integer.parseInt(devId[2]);
                    }
                } else {
                    devParams[1] = deviceProperties.getTempSetMax();
                }
                initDeviceTuya(deviceId, devParams);
            } catch (Exception e) {
                log.error("Failed init device with id [{}] [{}]", deviceIdWithPower, e.getMessage());
            }
        }
        if (devices != null) {
            log.info("Init tuya Devices successful: [{}], from [{}]", devices.getDevIds().size(), this.connectionConfiguration.getDeviceIds().length);
            try {
                this.updateAllThermostatToMin("start");
            } catch (Exception e) {
                log.error("Update  Devices after start is failed. [{}]", e.getMessage());
            }
        } else {
            log.error("Init tuya Devices failed All from [{}]", this.connectionConfiguration.getDeviceIds().length);
        }
    }

    private Device initDeviceTuya(String deviceId, int... devParams) throws Exception {
        Device device = null;
        String path = String.format(GET_DEVICES_ID_URL_PATH, deviceId);
        RequestEntity<Object> requestEntity = createGetTuyaRequest(path, false);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null && responseEntity.getBody() != null) {
            JsonNode result = responseEntity.getBody().get("result");
            device = treeToValue(result, Device.class);
            device.setStatusOnline(result);
            devices.getDevIds().put(deviceId, device);
            path = String.format(GET_DEVICE_STATUS_URL_PATH, deviceId);
            requestEntity = createGetTuyaRequest(path, false);
            responseEntity = sendRequest(requestEntity);
            if (responseEntity != null) {
                if (responseEntity.getBody() != null && responseEntity.getBody().has("result")) {
                    result = responseEntity.getBody().get("result");
                    devices.getDevIds().get(deviceId).setStatus(result);
                }
                if (devParams.length > 0) {
                    devices.getDevIds().get(deviceId).setConsumptionPower(devParams[0]);
                    if (devParams[1] == -2) {
                        devices.getDevIds().get(deviceId).setValueSetMaxOn(onLine);
                    } else if (devParams[1] <= 0 && devParams[1] > -2) {
                        devices.getDevIds().get(deviceId).setValueSetMaxOn(devParams[1] != 0);
                    } else {
                        devices.getDevIds().get(deviceId).setValueSetMaxOn(devParams[1]);
                    }

                } else if ("wk".equals(device.getCategory())) {
                    devices.getDevIds().get(deviceId).setConsumptionPower(2000);
                }
            } else {
                log.error("Init tuya Device with Id [{}}] failed... ", deviceId);
            }
        } else {
            log.warn("Device with id [{}] is not available", deviceId);
        }
        return device;
    }

    //    https://openapi.tuyaeu.com/v1.0/iot-03/devices/bfa715581477683002qb4l/freeze-state
    private ResponseEntity<ObjectNode> sendRequest(RequestEntity<Object> requestEntity) throws Exception {
        try {
            ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(), Objects.requireNonNull(requestEntity.getMethod()), requestEntity, ObjectNode.class);
            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                throw new RuntimeException(String.format("No response for device command request! Reason code from Tuya Cloud: %s", responseEntity.getStatusCode()));
            } else {
                if (Objects.requireNonNull(responseEntity.getBody()).get("success").asBoolean()) {
                    return responseEntity;
                } else {
                    if (responseEntity.getBody().has("code") && responseEntity.getBody().has("msg")) {
                        log.error("code: [{}], msg: [{}]", responseEntity.getBody().get("code").asInt(), responseEntity.getBody().get("msg").asText());
                        if (responseEntity.getBody().get("code").asInt() == 28841002) {
                            System.exit(0);
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

    private boolean hasValidAccessToken() {
        return this.accessTuyaToken != null && this.accessTuyaToken.getExpireTimeFinish() > System.currentTimeMillis();
    }

    private boolean hasRefreshAccessToken() {
        Long deltaRefresh = solarmanStationsService.getSolarmanStation().getTimeoutSec() * 1000;
        return (System.currentTimeMillis() - this.accessTuyaToken.getT()) > (this.accessTuyaToken.getExpireTimeMilli() - deltaRefresh);
    }

    public void updateGridStateOnLine() {
        if (this.getGridRelayCodeId() != null) {
            Entry<Long, Boolean> gridStateOnLine = this.devices.getDevIds().get(this.getGridRelayCodeId()).currentStateOnLine();
            if (gridStateOnLine != null) {
                String msg = null;
                if (this.lastUpdateTimeGridStatusInfo == null) {
                    // first message
                    msg = telegramService.sendFirstMsgToTelegram(gridStateOnLine);
                } else {
                    // next  message
                    if (gridStateOnLine.getValue() != this.lastUpdateTimeGridStatusInfo.getValue()) {
                        msg = telegramService.sendMsgToTelegram(this.lastUpdateTimeGridStatusInfo.getKey(), gridStateOnLine);
                    }
                }
                if (msg != null) {
                    this.lastUpdateTimeGridStatusInfo = gridStateOnLine;
                    log.info("\nTelegram send msg: \n{}", msg);
                }
            }
        }
    }

    public String getGridRelayCodeId() {
        if (this.gridRelayCodeId == null) {
            this.gridRelayCodeId = this.getGridRelayCode();
        }
        return this.gridRelayCodeId;
    }

    public Boolean getGridRelayCodeStateOnLine(){
        if (getGridRelayCodeId() != null) {
            Device gridDevice = this.devices.getDevIds().get(getGridRelayCodeId());
            if (gridDevice != null) {
                return gridDevice.getCurrentStateOnLineValue();
            }
        }
        return null;
    }

    public void setTimeoutSecUpdateMillis(Long timeoutSecUpdate) {
        this.timeoutSecUpdateMillis = timeoutSecUpdate * 1000;
    }


    private String getGridRelayCode(){
        if (this.devices != null && this.devices.getDevIds() != null) {
            for (Entry<String, Device> deviceId: this.devices.getDevIds().entrySet()) {
                if(this.onLine.equals(deviceId.getValue().getValueSetMaxOn())){
                    return deviceId.getKey();
                }
            }
        }
        return null;
    }
}
