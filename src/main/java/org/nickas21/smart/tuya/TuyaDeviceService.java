package org.nickas21.smart.tuya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.service.TelegramService;
import org.nickas21.smart.data.telegram.TelegramBot;
import org.nickas21.smart.solarman.Seasons;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.tuyaEntity.Device;
import org.nickas21.smart.tuya.tuyaEntity.DeviceStatus;
import org.nickas21.smart.tuya.tuyaEntity.DeviceUpdate;
import org.nickas21.smart.tuya.tuyaEntity.Devices;
import org.nickas21.smart.usr.entity.UsrTcpWiFiBmsSummary;
import org.nickas21.smart.util.HmacSHA256Util;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.AbstractMap;
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

import static org.nickas21.smart.solarman.BatteryStatus.ALARM;
import static org.nickas21.smart.solarman.BatteryStatus.DISCHARGING;
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
import static org.nickas21.smart.util.HttpUtil.hourNightTariffStartDopDacha;
import static org.nickas21.smart.util.HttpUtil.hourNightTariffStartDopGolego;
import static org.nickas21.smart.util.HttpUtil.isDeviceInHandleMode;
import static org.nickas21.smart.util.HttpUtil.isNightTariff;
import static org.nickas21.smart.util.HttpUtil.isSwitchRelayAfterNightOff;
import static org.nickas21.smart.util.HttpUtil.minutesNightTariffStartDopDacha;
import static org.nickas21.smart.util.HttpUtil.minutesNightTariffStartDopGolego;
import static org.nickas21.smart.util.HttpUtil.offOnKey;
import static org.nickas21.smart.util.HttpUtil.tempCurrentKey;
import static org.nickas21.smart.util.HttpUtil.tempCurrentKuhnyMin;
import static org.nickas21.smart.util.HttpUtil.tempSetKey;
import static org.nickas21.smart.util.HttpUtil.toLocaleTimeString;
import static org.nickas21.smart.util.JacksonUtil.objectToJsonNode;
import static org.nickas21.smart.util.JacksonUtil.toJsonNode;
import static org.nickas21.smart.util.JacksonUtil.toResponseEntityObjectNode;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;
import static org.nickas21.smart.util.SolarmanSocUtil.SolarmanSocPercentage.PERCENTAGE_90;
import static org.nickas21.smart.util.SolarmanSocUtil.SolarmanSocPercentage.REST_FLOAT;
import static org.nickas21.smart.util.StringUtils.isBoolean;
import static org.nickas21.smart.util.StringUtils.isDecimal;

@Slf4j
@Service
@EnableConfigurationProperties({TuyaConnectionProperties.class, TuyaDeviceProperties.class})
public class TuyaDeviceService {

    public final String gridRelayDopPrefixDacha = "gridOnlineDacha";
    public final String boilerRelayDopPrefixDacha = "boilerOnlineDacha";
    public final String gridRelayDopPrefixGolego = "gridOnlineGolego";
    public final String boilerRelayDopPrefixGolego = "boilerOnlineGolego";
    public static final String deviceIdTempScaleVanna = "bfe02a3417a1a4774alyab";
    public static final String deviceIdTempScaleKuhny = "bf12ddeca9ec3aeaaboax1";
    public final String deviceId3_floor = "bf4f86fd54edc80f6aegzd";
    public final String deviceIdBadRoom = "bfa270cc48a9f36de9xi6p";
    public final String deviceIdBoylerWiFi = "bfa0c1041fa8ad83e1oeik";

    @Getter
    @Setter
    @Value("${dacha.settings.battery_critical_night_soc_charging:60}")
    private double batteryCriticalNightSocWinter; //60, 50, 40;

    @Getter
    @Setter
    @Value("${dacha.settings.devices_change_handle_control:false}")
    private boolean devicesChangeHandleControlDacha;

    @Getter
    @Setter
    @Value("${golego.settings.devices_change_handle_control:false}")
    private boolean devicesChangeHandleControlGolego;

    @Getter
    @Setter
    @Value("${dacha.settings.heater_night_auto_on_winter:false}")
    private boolean heaterNightAutoOnDachaWinter;

    @Getter
    @Setter
    @Value("${dacha.settings.heater_grid_auto_all_day:false}")
    private boolean heaterGridOnAutoAllDayDacha;

    @Getter
    @Setter
    @Value("${golego.settings.heater_grid_auto_all_day:false}")
    private boolean heaterGridOnAutoAllDayGolego;

    @Getter
    @Setter
    @Value("${app.logs_limit:100}")
    private int logsAppLimit;

    @Getter
    @Value("${app.test_bot_debugging:false}")
    private boolean testBotDebugging;

    private TuyaToken accessTuyaToken;
    @Getter
    private Devices devices;

    private String gridRelayCodeIdDacha;
    private String boilerRelayCodeIdDacha;
    private String gridRelayCodeIdHome;
    private String boilerRelayCodeIdHome;

    @Getter
    private Entry<Long, Boolean> lastUpdateTimeGridStatusInfoDacha;
    @Getter
    private Entry<Long, Boolean> lastUpdateTimeGridStatusInfoHome;

    private Entry<Long, Double> lastUpdateTimeAlarmSocDacha;
    private Entry<Long, Double> lastUpdateTimeAlarmSocGolego;

    private final Lock queueLock = new ReentrantLock();

    private final TuyaConnectionProperties connectionConfiguration;
    @Getter
    private final TuyaDeviceProperties deviceProperties;
    private final RestTemplate httpClient = new RestTemplate();
    private final WebClient authClient = WebClient.builder().build();
    private final WebClient webClient;
    private Long timeoutSecUpdateMillis;
    private boolean batteryCriticalOrHeatNightWinter = false;

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
                log.warn("Device is not null. Successful creation of a new device with id [{}] category [{}] consumptionPower [{}]",
                        device.getId(), device.getCategory(), device.getConsumptionPower());
            }
        }
        if (device != null && deviceStatus != null) {
            String nameField = deviceStatus.get(0).get("code").asText();
            boolean updateStatus = false;
            if ((device.getValueSetMaxOn() instanceof Boolean && tempCurrentKey.equals(nameField)) ||
                    (nameField != null && nameField.contains(offOnKey))) {
                Object valueOld = device.getStatus().get(nameField).getValue();
                Boolean valueNew = deviceStatus.get(0).get("value").asBoolean();
                if (valueOld == null || valueOld != valueNew) {
                    DeviceStatus devStatusSwitchNew = new DeviceStatus();
                    devStatusSwitchNew.setValue(deviceStatus.get(0).get("value"));
                    device.setStatus(nameField, devStatusSwitchNew);
                    log.info("Device (update switch by value): [{}] time: -> [{}] parameter: [{}] valueOld: [{}] valueNew: [{}] ",
                            device.getName(), toLocaleTimeString(Long.parseLong(String.valueOf(deviceStatus.get(0).get("t")))),
                            nameField, valueOld, devStatusSwitchNew.getValue());
                    updateStatus = true;
                }
            }
            if (!updateStatus) {
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
            if (device.setBizCode((ObjectNode) msg.getJson())) {
                if (device.getId().equals(this.getGridRelayCodeIdDacha()) ||
                        device.getId().equals(this.getGridRelayCodeIdGolego())) {
                    this.updateGridStateOnLineToTelegram(device.getId());
                }
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

    public void updateAllThermostat(Object tempSet) {
        String[] filters = getDeviceProperties().getCategoryForControlPowers();
        if (this.devices != null) {
            Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();
            for (Map.Entry<String, Device> entry : this.devices.getDevIds().entrySet()) {
                this.deviceUpdateCategory(entry.getValue(), filters, queueUpdate, tempSet);
            }
            queueLock.lock();
            try {
                updateThermostats(queueUpdate, false);
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

    public DeviceUpdate getDeviceUpdate(Object valueNew, Device v) {
        String fieldNameValueUpdate;
        Object valueOld;
        if (valueNew instanceof Boolean) {
            fieldNameValueUpdate = gridRelayDopPrefixDacha.equals(v.getValueSetMaxOn()) ||
                    boilerRelayDopPrefixGolego.equals(v.getValueSetMaxOn()) ? offOnKey + "_1" : offOnKey;
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
            if (deviceIdTempScaleVanna.equals(v.getId()) || deviceIdTempScaleKuhny.equals(v.getId())){
                valueNew = Objects.equals(valueNew, deviceProperties.getTempSetMin()) ? Integer.valueOf((Integer) valueNew * 10) : v.getValueSetMaxOn() != null ? (Integer)v.getValueSetMaxOn() * 10 : null;
                valueOld = v.getStatusValue(fieldNameValueUpdate, deviceProperties.getTempSetMin() * 10);
            } else {
                valueNew = Objects.equals(valueNew, deviceProperties.getTempSetMin()) ? valueNew : v.getValueSetMaxOn();
                valueOld = v.getStatusValue(fieldNameValueUpdate, deviceProperties.getTempSetMin());
            }

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
            updateThermostats(queueUpdate, false);
        } finally {
            queueLock.unlock();
        }
    }

    public void updateThermostats(Map<Device, DeviceUpdate> queueUpdate, boolean isUpdateAlways) {
        int size = queueUpdate.size();
        if (size > 0) {
            // Schedule the task to run at fixed intervals
            log.info("Start updateThermostats time size: [{}]", size);
            if (timeoutSecUpdateMillis == null) {
                this.setTimeoutSecUpdateMillis(this.solarmanStationsService.getSolarmanStation().getTimeoutSec() / 2);
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
            updateThermostats(queueUpdate, false);
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
            updateThermostats(queueUpdate, false);
        } finally {
            queueLock.unlock();
        }
    }

    private LinkedHashMap<String, Device> getDevicesTempSort(boolean order, String... filters) {
        HashMap<String, Integer> devicesPowerNotSort = new HashMap<>();
        LinkedHashMap<String, Device> sortedMap = new LinkedHashMap<>();
        Object key = null;
        Object value = null;
        try {
            for (Map.Entry<String, Device> entry : this.devices.getDevIds().entrySet()) {
                try {
                    key = entry.getKey();
                    value = entry.getValue();
                    for (String f : filters) {
                        if (entry.getValue().getCategory().equals(f)) {
                            Object statusValue = entry.getValue().getStatusValue(tempCurrentKey, deviceProperties.getTempSetMin());
                            Integer statusValueInt = statusValue instanceof com.fasterxml.jackson.databind.node.IntNode ?
                                    ((com.fasterxml.jackson.databind.node.IntNode) statusValue).intValue() : (Integer) statusValue;
                            devicesPowerNotSort.put(entry.getKey(), statusValueInt);
                        }
                    }
                } catch (Exception innerException) {
                    log.error("getDevicesTempSort: Error processing entry: key [{}], value [{}]", key, value, innerException);
                }
            }
        } catch (Exception e) {
            log.error("getDevicesTempSort: key [{}], value [{}] ", key, value, e);
        }

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
        HttpHeaders httpHeaders = createHeaders(ts);
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
        HttpHeaders httpHeaders = createHeaders(ts);
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
                        Object valueInDeviceCur = device.getStatus() != null && nameParam != null && device.getStatus().get(nameParam) != null ?
                                device.getStatus().get(nameParam).getValue() : null;
                        Object valNumber = commands.get(0).get("value").getNodeType().name().equals("NUMBER") ? commands.get(0).get("value").asInt() :
                                commands.get(0).get("value").getNodeType().name().equals("BOOLEAN") ? commands.get(0).get("value").asBoolean() : commands.get(0).get("value");
                        if (valNumber != null && !valNumber.equals(valueInDeviceCur)) {
                            DeviceStatus statusNew = new DeviceStatus();
                            statusNew.setValue(valNumber);
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
                Object[] devParams = new Object[2];
                devParams[0] = Integer.parseInt(devId[1]);
                if (devId.length == 3) {
                    devParams[1] = isBoolean(devId[2]);
                    devParams[1] = devParams[1] != null ? devParams[1] :
                            isDecimal(devId[2]) ? Integer.parseInt(devId[2]) : devId[2];
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
            for (Entry e : devices.getDevIds().entrySet()) {
                log.info("name: [{}] id: [{}] ", ((Device) e.getValue()).getName(), e.getKey());
            }
            try {
                this.updateAllThermostatToMin("start");
            } catch (Exception e) {
                log.error("Update  Devices after start is failed. [{}]", e.getMessage());
            }
        } else {
            log.error("Init tuya Devices failed All from [{}]", this.connectionConfiguration.getDeviceIds().length);
        }
    }

    private Device initDeviceTuya(String deviceId, Object... devParams) throws Exception {
        Device device = null;
        if (!isDeviceInHandleMode(deviceId)) {
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
                        devices.getDevIds().get(deviceId).setConsumptionPower((Integer) devParams[0]);
                        devices.getDevIds().get(deviceId).setValueSetMaxOn(devParams[1]);
                    } else if ("wk".equals(device.getCategory())) {
                        devices.getDevIds().get(deviceId).setConsumptionPower(2000);
                    }
                } else {
                    log.error("Init tuya Device with Id [{}}] failed... ", deviceId);
                }
            } else {
                log.warn("Device with id [{}] is not available", deviceId);
            }
        } else {
            log.error("Device with id [{}] is not available. Device is off or handle mode or bad or delete", deviceId);
        }
        return device;
    }

    //    https://openapi.tuyaeu.com/v1.0/iot-03/devices/bfa715581477683002qb4l/freeze-state
    private ResponseEntity<ObjectNode> sendRequest(RequestEntity<Object> requestEntity) throws Exception {
        try {
            ResponseEntity<String> responseEntityStr =
                    httpClient.exchange(
                            requestEntity.getUrl(),
                            Objects.requireNonNull(requestEntity.getMethod()),
                            requestEntity,
                            String.class
                    );

            ResponseEntity<ObjectNode> responseEntity = toResponseEntityObjectNode(responseEntityStr);
            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                throw new RuntimeException(String.format("No response for device command request! Reason code from Tuya Cloud: %s", responseEntity.getStatusCode()));
            } else {
                JsonNode body = toJsonNode(responseEntityStr.getBody());
                if (body != null && body.get("success").asBoolean()) {
                    return responseEntity;
                } else {
                    assert responseEntity.getBody() != null;
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
        return this.accessTuyaToken != null && this.accessTuyaToken.getExpireTimeFinish() > System.currentTimeMillis();
    }

    private boolean hasRefreshAccessToken() {
        Long deltaRefresh = this.solarmanStationsService.getSolarmanStation().getTimeoutSec() * 1000;
        return (System.currentTimeMillis() - this.accessTuyaToken.getT()) > (this.accessTuyaToken.getExpireTimeMilli() - deltaRefresh);
    }

    public void updateGridStateOnLineToTelegram() {
        this.updateGridStateOnLineToTelegram(this.getGridRelayCodeIdDacha());
        this.updateGridStateOnLineToTelegram(this.getGridRelayCodeIdGolego());
    }

    public void updateGridStateOnLineToTelegram(String gridRelayCodeId) {
        if (!this.testBotDebugging) {
            if (this.getGridRelayCodeIdDacha() != null) {
                Entry<Long, Boolean> gridStateOnLine = this.devices.getDevIds().get(gridRelayCodeId).currentStateOnLine();
                if (gridStateOnLine != null) {
                    TelegramBot bot;
                    String msg = null;
                    Entry<Long, Boolean> lastUpdateTimeGridStatusInfo;
                    if (gridRelayCodeId.equals(this.getGridRelayCodeIdDacha())) {
                        bot = telegramService.getTelegramBotDacha();
                        lastUpdateTimeGridStatusInfo = this.lastUpdateTimeGridStatusInfoDacha;
                    } else {
                        bot = telegramService.getTelegramBotHome();
                        lastUpdateTimeGridStatusInfo = this.lastUpdateTimeGridStatusInfoHome;
                    }
                    if (lastUpdateTimeGridStatusInfo == null) {
                        // first message
                        msg = telegramService.sendFirstMsgGridStatusToTelegram(bot, gridStateOnLine);
                    } else {
                        if (gridStateOnLine.getValue() != lastUpdateTimeGridStatusInfo.getValue()) {
                            // next  message
                            msg = telegramService.sendMsgGridStatusToTelegram(bot, lastUpdateTimeGridStatusInfo.getKey(), gridStateOnLine);
                        } else {
                            log.info("Telegram[{}] is not sending messages... because the state has not changed.", bot.getHouseName());
                        }
                    }
                    if (msg != null) {
                        if (gridRelayCodeId.equals(this.getGridRelayCodeIdDacha())) {
                            this.lastUpdateTimeGridStatusInfoDacha = gridStateOnLine;
                        } else {
                            this.lastUpdateTimeGridStatusInfoHome = gridStateOnLine;
                        }
                        log.info("\nTelegram[{}] send msg: \n{}", bot.getHouseName(), msg);
                    }
                }
            }
        }
    }

    public void updateMessageAlarmToTelegram(String msg) {
        if (!this.testBotDebugging) {
            String message = msg == null ? "Перезавантаження програми. Початок відстеження Alarm message." : msg;  // first
            TelegramBot bot = telegramService.getTelegramBotAlarm();
            if  (bot != null) {
                telegramService.sendNotification(bot, message);
//                msg = msg == null ? "Restarting the program. Start tracking Alarm message." : msg;
                log.info("Telegram: [{}] send msg: {}", bot.getHouseName(), msg);
            } else {
                log.error("Telegram: bot [null], no send msg: {}", msg);
            }
        }
    }

    public void sendDachaGolegoBatteryChargeRemaining(double batVolNew, double batCurNew, double bmsVolNew, double bmsCurNew, double bmsTempNew, double batterySocNew,
                                                      double  batteryPowerNew, String batteryStatusNew, UsrTcpWiFiBmsSummary usrBmsSummary) {
        double batteryChargeRemainingDacha = batterySocNew;
        double batteryChargeRemainingGolego = usrBmsSummary == null ? 0 : usrBmsSummary.socPercent();
        Entry<Long, Double> lastUpdateTimeAlarmDacha = new AbstractMap.SimpleEntry<>(Instant.now().toEpochMilli(), batteryChargeRemainingDacha);
        Entry<Long, Double> lastUpdateTimeAlarmGolego = usrBmsSummary == null ? null : new AbstractMap.SimpleEntry<>(usrBmsSummary.timestamp().toEpochMilli(), batteryChargeRemainingGolego);

        // If null - first
        // Not equals and (equals 100% or <= 90%)
        if (
                shouldTriggerDachaSocAlarm(this.lastUpdateTimeAlarmSocDacha, batteryChargeRemainingDacha) ||
                shouldTriggerGolegoSocAlarm(this.lastUpdateTimeAlarmSocGolego, batteryChargeRemainingGolego, usrBmsSummary)
        ) {
            String msg = "INFO, ";
            if (batteryChargeRemainingDacha <= ALARM.getSoc()) {
                msg = "ERROR, ";
            } else if (batteryChargeRemainingDacha <= DISCHARGING.getSoc()) {
                msg = "WARNING, ";
            }
            // if battery == USER
//            String msgSoc = msg + "Battery Remaining at the Country House: [" + batteryChargeRemaining + " %]/(on inverter [" + batterySocFromSolarman + " %]).";
            double bmsPower = Math.round((bmsVolNew * bmsCurNew) * 100.0) / 100.0;

            String  msgSoc = msg + "Info: Battery on the Country House:\n" +
                    "- SOC: [" + batteryChargeRemainingDacha + " %];\n" +
                    "- BatteryStatus: [" + batteryStatusNew + "];\n" +
                    "- BmsVoltage: [" + bmsVolNew + " V];\n" +
                    "- BmsPower: [" + bmsPower + " W];\n" +
                    "- BmsCurrent: [" + bmsCurNew + " A];\n" +
                    "- BmsTemperature: [" + bmsTempNew + "  ℃];\n" +
                    "- Powers: [" + batteryPowerNew + " W];\n";
            if (usrBmsSummary == null) {
                msgSoc = msgSoc + "- Voltages: [" + batVolNew + " V];\n" +
                        "- Currents: [" + batCurNew + " A].";
            } else {
                msgSoc = msgSoc + "Info: Usr Battery on Golego:\n" +
                        "- SOC: [" + usrBmsSummary.socPercent() + " %];\n"
                        + usrBmsSummary.bmsSummary();
            }
            this.updateMessageAlarmToTelegram(msgSoc);
            this.lastUpdateTimeAlarmSocDacha = lastUpdateTimeAlarmDacha;
            this.lastUpdateTimeAlarmSocGolego = lastUpdateTimeAlarmGolego;
        }
        if (usrBmsSummary != null && usrBmsSummary.bmsErrors() != null) {
            String  msgError = "Errors: Usr Battery on the Golego:\n" + usrBmsSummary.bmsErrors();
            this.updateMessageAlarmToTelegram(msgError);
        }
    }

    private boolean shouldTriggerDachaSocAlarm(Entry<Long, Double> lastValue, double newSoc) {

        // first update
        if (lastValue == null) {
            return true;
        }

        // SOC changed
        boolean changed = !lastValue.getValue().equals(newSoc);

        // SOC == 100% OR <= 90%
        boolean inRange = (newSoc == REST_FLOAT.getPercentage() ||
                newSoc <= PERCENTAGE_90.getPercentage());

        return changed && inRange;
    }

    private boolean shouldTriggerGolegoSocAlarm(Entry<Long, Double> lastValue,
                                                double newSoc,
                                                UsrTcpWiFiBmsSummary usrBmsSummary) {

        // no BMS summary → cannot check
        if (usrBmsSummary == null) {
            return false;
        }
        // first update
        if (lastValue == null) {
            return true;
        }
        // SOC changed
        boolean changed = !lastValue.getValue().equals(newSoc);
        // SOC == 100% OR <= 90%
        boolean inRange = (newSoc == REST_FLOAT.getPercentage() ||
                newSoc <= PERCENTAGE_90.getPercentage());

        return changed && inRange;
    }



    public String getGridRelayCodeIdDacha() {
        if (this.gridRelayCodeIdDacha == null) {
            this.gridRelayCodeIdDacha = this.getGridRelayCode(this.gridRelayDopPrefixDacha);
        }
        return this.gridRelayCodeIdDacha;
    }

    public String getBoilerRelayCodeIdDacha() {
        if (this.boilerRelayCodeIdDacha == null) {
            this.boilerRelayCodeIdDacha = this.getGridRelayCode(this.boilerRelayDopPrefixDacha);
        }
        return this.boilerRelayCodeIdHome;
    }

    public String getGridRelayCodeIdGolego() {
        if (this.gridRelayCodeIdHome == null) {
            this.gridRelayCodeIdHome = this.getGridRelayCode(this.gridRelayDopPrefixGolego);
        }
        return this.gridRelayCodeIdHome;
    }


    public String getBoilerRelayCodeIdGolego() {
        if (this.boilerRelayCodeIdHome == null) {
            this.boilerRelayCodeIdHome = this.getGridRelayCode(this.boilerRelayDopPrefixGolego);
        }
        return this.boilerRelayCodeIdHome;
    }

    public Boolean getGridRelayCodeDachaStateOnLine() {
        if (this.getGridRelayCodeIdDacha() != null) {
            Device gridDevice = this.devices.getDevIds().get(this.getGridRelayCodeIdDacha());
            if (gridDevice != null) {
                return gridDevice.getCurrentStateOnLineValue();
            }
        }
        return null;
    }

    public Boolean getGridRelayCodeGolegoStateOnLine() {
        if (this.getGridRelayCodeIdGolego() != null) {
            Device gridDevice = this.devices.getDevIds().get(this.getGridRelayCodeIdGolego());
            if (gridDevice != null) {
                return gridDevice.getCurrentStateOnLineValue();
            }
        }
        return null;
    }

    public Boolean getGridRelayCodeGolegoStateSwitch() {
        if (this.getGridRelayCodeIdGolego() != null) {
            Device gridDevice = this.devices.getDevIds().get(this.getGridRelayCodeIdGolego());
            String keySwitchGolego = offOnKey;
            if (gridDevice != null && gridDevice.getStatus() != null && gridDevice.getStatus().containsKey(keySwitchGolego)) {
                Object statusValueGolego = gridDevice.getStatus().get(keySwitchGolego).getValue();
                if (statusValueGolego instanceof Boolean){
                    return (Boolean) statusValueGolego;
                } else {
                    return ((BooleanNode)(statusValueGolego)).asBoolean();
                }
            }
        }
        return null;
    }
    public Boolean getGridRelayCodeDachaStateSwitch() {
        if (this.getGridRelayCodeIdDacha() != null) {
            Device gridDevice = this.devices.getDevIds().get(this.getGridRelayCodeIdDacha());
            String keySwitchDacha = offOnKey + "_1";
            if (gridDevice != null && gridDevice.getStatus() != null && gridDevice.getStatus().containsKey(keySwitchDacha)) {
                Object statusValueDacha = gridDevice.getStatus().get(keySwitchDacha).getValue();
                if (statusValueDacha instanceof Boolean){
                    return (Boolean) statusValueDacha;
                } else {
                    return ((BooleanNode)(statusValueDacha)).asBoolean();
                }
            }
        }
        return null;
    }

    public void setTimeoutSecUpdateMillis(Long timeoutSecUpdate) {
        this.timeoutSecUpdateMillis = timeoutSecUpdate * 1000;
    }

    /**
     * All
     * - Alarm == soc 30% Grid - on BoylerGolego - off
     * - off:  from  6:50 to 8:00
     * - on/off => handle (any) +
     * Auto
     * - Golego
     * -- night: from 23:00 to 6:50
     *     on:
     * -- day: from 8:00 to 23
     *    off:
     * - Dacha
     * -- night ==  from  23:00 + dop to 6:50
     *      on:  batteryCriticalNightSocWinter = 60, 50, 40; and Winter:
     *      off: // summer:
     * -- day == from 8:00 to (23 + dop)
     *     off
     */
    public void updateOnOfSwitchRelay(double batterySocFromSolarman, double batterySocFromUsr, double totalGridPower) {
        this.updateSwitchReleayDachaAndThermostatFirstFloor(batterySocFromSolarman, totalGridPower);
        this.updateOnOffSwitchRelayGolego(this.getGridRelayCodeIdGolego(), batterySocFromUsr);
        this.updateOnOffSwitchRelayGolego(this.getBoilerRelayCodeIdGolego(), batterySocFromUsr);
    }

    /**
     * Auto -> night
     * Always -> only if Relay Golego online
     */
    public void updateOnOffSwitchRelayGolego(String gridRelayCodeId, double batterySocFromUsr) {
        Device device = this.devices.getDevIds().get(gridRelayCodeId);
        if (device == null) {
            log.error("Device Relay Golego switch is null... , is offline... and is not update");
        } else if (device.currentStateOnLine().getValue()) {
            boolean paramOnOff = false; // isSwitchRelayAfterNightOff()
            boolean nightTariff = isNightTariff(hourNightTariffStartDopGolego, minutesNightTariffStartDopGolego);
            if (batterySocFromUsr >= 0 && batterySocFromUsr < ALARM.getSoc()) {
                paramOnOff = this.getGridRelayCodeIdGolego().equals(gridRelayCodeId);
            } else if (nightTariff) {  // night: from 23:00 to 6:50
                paramOnOff = true;
            }
            if (this.heaterGridOnAutoAllDayGolego) {
                paramOnOff = this.getGridRelayCodeGolegoStateOnLine();
            }
            Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();
            DeviceUpdate deviceUpdate = getDeviceUpdate(paramOnOff, device);

//            if (!isSwitchRelayAfterNightOff() && this.devicesChangeHandleControlGolego) {   // handle and not -> 7-8 (AfterNight)
            if (this.devicesChangeHandleControlGolego) {   // handle and not -> 7-8 (AfterNight)
                deviceUpdate.setValueNew(deviceUpdate.getValueOld());
            }

            if (deviceUpdate.isUpdate()) {
                log.info("Relay switch [{}] updated to [{}], night tariff: [{}].", device.getName(), paramOnOff ? "on" : "off", nightTariff);
                queueUpdate.put(device, deviceUpdate);
            } else {
                log.info("Relay switch: [{}] not Update. [{}] changeValue [{}] currentValue [{}]",
                        device.getName(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), deviceUpdate.getValueOld());
            }
            queueLock.lock();
            try {
                updateThermostats(queueUpdate, false);
            } finally {
                queueLock.unlock();
            }

        } else {
            log.warn("Device relay switch [{}] cannot be updated, is offline...", device.getName());
        }
    }

    public void updateSwitchRelayDacha(Device device, boolean paramOnOff) {
        Map<Device, DeviceUpdate> queueUpdate = new ConcurrentHashMap<>();

        if (this.heaterGridOnAutoAllDayDacha) {
            paramOnOff = this.getGridRelayCodeDachaStateOnLine();
        }

        DeviceUpdate deviceUpdate = getDeviceUpdate(paramOnOff, device);
        if (!isSwitchRelayAfterNightOff() && this.devicesChangeHandleControlDacha) {
            deviceUpdate.setValueNew(deviceUpdate.getValueOld());
        }
        if (deviceUpdate.isUpdate()) {
            log.info("Grid Relay Dacha switch: [{}] updated to [{}], night tariff: [{}].", device.getName(), paramOnOff ? "on" : "off",  isNightTariff(hourNightTariffStartDopDacha, minutesNightTariffStartDopDacha));
            queueUpdate.put(device, deviceUpdate);
        } else {
            log.info("Grid Relay Dacha switch: [{}] not Update. [{}] changeValue [{}] currentValue [{}]",
                    device.getName(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), deviceUpdate.getValueOld());
        }
        queueLock.lock();
        try {
            updateThermostats(queueUpdate, false);
        } finally {
            queueLock.unlock();
        }
    }

    private String getGridRelayCode(String gridRelayDopPrefix) {
        if (this.devices != null && this.devices.getDevIds() != null) {
            for (Entry<String, Device> deviceId : this.devices.getDevIds().entrySet()) {
                if (gridRelayDopPrefix.equals(deviceId.getValue().getValueSetMaxOn())) {
                    return deviceId.getKey();
                }
            }
        }
        return null;
    }

    /**
     *  If the temperatureIn Kuhny <= 2  -> on Always
     */
    public void updateSwitchReleayDachaAndThermostatFirstFloor(double batterySocFromSolarman, double totalGridPower) {
        log.info("updateGridRelayDachaSwitchOffOnFirstFloor start: heaterGridOnAutoAllDayDacha [{}], devicesChangeHandleControlDacha [{}], heaterNightAutoOnDachaWinter [{}], SeasonsID [{}], batteryCriticalNightSocWinter [{}]",
                this.heaterGridOnAutoAllDayDacha, this.devicesChangeHandleControlDacha, this.heaterNightAutoOnDachaWinter,
                this.solarmanStationsService.getSolarmanStation().getSeasonsId(), this.batteryCriticalNightSocWinter);
        log.info("this.devices != null [{}], this.devices.getDevIds() != mull [{}], this.devices.getDevIds().get(deviceIdTempScaleKuhny) [{}]",
                this.devices != null, this.devices.getDevIds() != null, this.devices.getDevIds().get(deviceIdTempScaleKuhny) != null);

        if (this.devices != null && this.devices.getDevIds().get(deviceIdTempScaleKuhny) != null) {
            Integer tempCurKuhny = (Integer) this.devices.getDevIds().get(deviceIdTempScaleKuhny).getStatus().get(tempCurrentKey).getValue();
            if (tempCurKuhny != null) {
                Integer thermostatValueNew = this.getDeviceProperties().getTempSetMin();
                boolean thermostatSwitchOffOnNew = false;
                boolean gridRelayDachaStateOnLine = this.getGridRelayCodeDachaStateOnLine();
                boolean gridRelayDachaSwitchOffOnOLd = this.getGridRelayCodeDachaStateSwitch();
                boolean gridRelayDachaSwitchOffOnNew = gridRelayDachaSwitchOffOnOLd;
                boolean isUpdateSwitchThermostat = false;
                String msg = null;
                tempCurKuhny = tempCurKuhny / 10;
                if (tempCurKuhny <= tempCurrentKuhnyMin) { // tempMin
                    isUpdateSwitchThermostat = true;
                    gridRelayDachaSwitchOffOnNew = true;
                    thermostatSwitchOffOnNew = true;
                    thermostatValueNew = this.getDeviceProperties().getTempSetMin();
                    msg = "Critical temperature at the Country House: [" + tempCurKuhny + "].";
                } else if (batterySocFromSolarman <= ALARM.getSoc()){
                    gridRelayDachaSwitchOffOnNew = true;
                    msg = "Critical SOC at the Country House: [" + batterySocFromSolarman + "] %.";
                } else if (this.solarmanStationsService.getSolarmanStation().getSeasonsId() == Seasons.SUMMER.getSeasonsId()) {
                    gridRelayDachaSwitchOffOnNew = false;
                } else {
                    if (isSwitchRelayAfterNightOff()) {
                        isUpdateSwitchThermostat = true;
                        gridRelayDachaSwitchOffOnNew = false;
                        thermostatValueNew = this.getDeviceProperties().getTempSetMin();
                    } else {
                        if (this.heaterGridOnAutoAllDayDacha) {
                            if (gridRelayDachaStateOnLine) {
                                isUpdateSwitchThermostat = true;
                                gridRelayDachaSwitchOffOnNew = true;
                                if(totalGridPower <= 0){
                                    thermostatValueNew = this.getDeviceProperties().getTempSetMin();
                                } else {
                                    thermostatSwitchOffOnNew = true;
                                    thermostatValueNew = this.getDeviceProperties().getTempSetMax();
                                }
                            } else {
                                isUpdateSwitchThermostat = true;
                                thermostatValueNew = this.getDeviceProperties().getTempSetMin();
                            }
                        } else if (this.heaterNightAutoOnDachaWinter) {// auto night in winter  if grid - on
                            thermostatValueNew = this.updateHeaterFirstWinterAuto(gridRelayDachaStateOnLine, batterySocFromSolarman);
                            isUpdateSwitchThermostat = true;
                            if (this.getDeviceProperties().getTempSetMin().equals(thermostatValueNew)) {
                                gridRelayDachaSwitchOffOnNew = false;
                            } else {
                                gridRelayDachaSwitchOffOnNew = true;
                                thermostatSwitchOffOnNew = true;
                            }
                            if (totalGridPower <= 0){
                                thermostatSwitchOffOnNew = false;
                                thermostatValueNew = this.getDeviceProperties().getTempSetMin();
                            }
                        } else if (!this.devicesChangeHandleControlDacha) {
                            Integer thermostatValueForGridSwitch = this.updateHeaterFirstWinterAuto(gridRelayDachaStateOnLine, batterySocFromSolarman);
                            if (this.getDeviceProperties().getTempSetMin().equals(thermostatValueForGridSwitch)) {
                                gridRelayDachaSwitchOffOnNew = false;
                            } else {
                                // is not handler + winter +  night + batteryCriticalNightSocWinter
                                gridRelayDachaSwitchOffOnNew = true;
                            }
                        }
                    }
                }

                if (msg != null) {
                    this.updateMessageAlarmToTelegram(msg);
                }

                if (gridRelayDachaSwitchOffOnOLd != gridRelayDachaSwitchOffOnNew) {
                    Device device = this.devices.getDevIds().get(this.getGridRelayCodeIdDacha());
                    this.updateSwitchRelayDacha(device, gridRelayDachaSwitchOffOnNew);
                    log.info("updateGridRelayDachaSwitchOffOnFirstFloor: [{}] msgError: [{}]", gridRelayDachaSwitchOffOnNew, msg == null ? "empty" : msg);
                }

                if (isUpdateSwitchThermostat) {
                    // update temp switch
                    String[] filters = getDeviceProperties().getCategoryForControlPowers();
                    Map<Device, DeviceUpdate> queueUpdateSwitch = new ConcurrentHashMap<>();
                    for (Map.Entry<String, Device> entry : this.devices.getDevIds().entrySet()) {
                        if (!deviceId3_floor.equals(entry.getKey()) &&
                                !deviceIdBadRoom.equals(entry.getKey()) &&
                                !deviceIdBoylerWiFi.equals(entry.getKey())) {
                            this.deviceUpdateCategory(entry.getValue(), filters, queueUpdateSwitch, thermostatSwitchOffOnNew); // on/off term
                        }
                    }
                    queueLock.lock();
                    try {
                        log.info("updateSwitchThermostatFirstFloor: switch: [{}] msgError: [{}]", thermostatSwitchOffOnNew, msg == null ? "empty" : msg);
                        updateThermostats(queueUpdateSwitch, false);
                    } finally {
                        queueLock.unlock();
                    }
                    // update temp Value
                    Map<Device, DeviceUpdate> queueUpdateValue = new ConcurrentHashMap<>();
                    for (Map.Entry<String, Device> entry : this.devices.getDevIds().entrySet()) {
                        if (!deviceId3_floor.equals(entry.getKey()) &&
                                !deviceIdBadRoom.equals(entry.getKey()) &&
                                !deviceIdBoylerWiFi.equals(entry.getKey())) {
                            this.deviceUpdateCategory(entry.getValue(), filters, queueUpdateValue, thermostatValueNew); // on/off term
                        }
                    }
                    queueLock.lock();
                    try {
                        log.info("updatValueThermostatFirstFloor: value: [{}]  msgError: {}", thermostatValueNew, msg == null ? "empty" : msg);
                        updateThermostats(queueUpdateValue, false);
                    } finally {
                        queueLock.unlock();
                    }

                }
            } else {
                log.error("UpdateSwitchThermostatTemp. Device Kuhny is null, Devices not Update.");
            }
        } else {
            log.error("UpdateSwitchThermostatTemp. Devices is null, Devices not Update.");
        }
    }

    private Integer updateHeaterFirstWinterAuto(boolean switchOnLine, double batterySocCur) {

//        if (isNightTariff(hourNightTariffStartDopDacha, minutesNightTariffStartDopDacha) &&
//            this.solarmanStationsService.getSolarmanStation().getSeasonsId() == Seasons.WINTER.getSeasonsId() &&
//                switchOnLine && this.batteryCriticalNightSocWinter > batterySocCur) {
//                    return this.getDeviceProperties().getTempSetMax();
//        }
        if ( batteryCriticalOrHeatNightSwitchRelayDachaOnOffWinter(batterySocCur) && switchOnLine) {
                    return this.getDeviceProperties().getTempSetMax();
        }
        return this.getDeviceProperties().getTempSetMin();
    }

    private void deviceUpdateCategory(Device device, String[] filters, Map<Device, DeviceUpdate> queueUpdate, Object valueNew) {
        for (String f : filters) {
            if (f.equals(device.getCategory())) {
                DeviceUpdate deviceUpdate = getDeviceUpdate(valueNew, device);
                if (deviceUpdate.isUpdate()) {
                    queueUpdate.put(device, deviceUpdate);
                } else {
                    log.info("UpdateCategory: Device: [{}] not Update. [{}] changeValue [{}] currentValue [{}]",
                            device.getName(), deviceUpdate.getFieldNameValueUpdate(), deviceUpdate.getValueNew(), deviceUpdate.getValueOld());
                }
            }
        }
    }


    /**
     * battery is charge/discharge  if night and Winter
     * працює тільки взимку + нічний тариф + тільки для dacha
     * якщо SOC батареї падає нижче критичного, фіксує стан batteryCriticalOrHeatNightWinter = true
     * після цього повертає true завжди, доки не закінчиться ніч або зима
     *
     * paramOnOff = true/false if: is NightTariff && this.getHourChargeBattery() in NightTariff && Winter
     * -- HourChargeBattery < 7 ... =>  HourChargeBattery >= 1:30
     * --if batterySocFromSolarman <= batteryCriticalNightSocWinter → critical night mode ON = true => 60%/50%/40%
     */
    public boolean batteryCriticalOrHeatNightSwitchRelayDachaOnOffWinter(double batterySocFromSolarman) {
        boolean isWinter = this.solarmanStationsService.getSolarmanStation().getSeasonsId() == Seasons.WINTER.getSeasonsId();

        if (!isWinter || !isNightTariff(hourNightTariffStartDopDacha, minutesNightTariffStartDopDacha)) { // summer or day
            this.batteryCriticalOrHeatNightWinter = false;
            return false;
        }

        if (this.batteryCriticalOrHeatNightWinter) {
            return true;
        }

        if (batterySocFromSolarman <= this.batteryCriticalNightSocWinter) {
            this.batteryCriticalOrHeatNightWinter = true;
            return true;
        }

        return false;
    }
}

