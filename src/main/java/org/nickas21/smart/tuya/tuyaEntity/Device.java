package org.nickas21.smart.tuya.tuyaEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.util.HttpUtil.toLocaleTimeString;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {
    private String name;
    private String id;
        // unit = w
    private int consumptionPower;
    private Object valueSetMaxOn;
    private Map<String, DeviceStatus> status;
    private Long active_time;
        // cz,          wsdcg,                              wk
    private String category;
        // Socket,      Temperature and Humidity Sensor,    Thermostat
    private String category_name;
        // Smart Plug   Temperature and humidity alarm,     WiFi thermostat
    private String product_name;
    // 21BL-JK,         AS90,                               DLT-001
    private String model;
    private String lon;
    private String lat;
    private Long create_time;
    private String icon;

    private String ip;

    private String local_key;
    private Map <Long, Boolean> onLine = new ConcurrentHashMap<>();
    private String product_id;
    private Boolean sub;
    private String time_zone;
    private Long update_time;
    private String uuid;
    private String owner_id;

    private String asset_id;
    private String gateway_id;
    private String bizCodeLast;

    public void setStatus (JsonNode statusNodes) {
        if (statusNodes.isArray()) {
            for (JsonNode statusNode : statusNodes) {
                if (statusNode.has("code")) {
                    String code = statusNode.get("code").asText();
                    DeviceStatus statusNew = treeToValue(statusNode, DeviceStatus.class);
                    setStatus (statusNew, code);
                }
            }
        }
    }

    public void setStatusOnline (JsonNode statusNodes) {
        if (statusNodes.has("active_time") && statusNodes.has("online")) {
            this.getOnLine().put(Instant.now().toEpochMilli(), statusNodes.get("online").asBoolean());
        }
    }

    public void setStatus (DeviceStatus statusNew, String code) {
        DeviceStatus statusOld = this.status != null ? this.status.get(code) : null;
        if (statusNew != null) {
            statusNew.setEventTime(System.currentTimeMillis());
            statusNew.setName(code);
            if (statusOld != null) {
                statusNew.setValueOld(statusOld.getValue());
                statusNew.setName(code);
            }
            setStatus(code, statusNew);
            log.trace("Init: devId [{}] devName [{}], status: [{}] -> old=[{}], new=[{}]",
                    this.id, this.name, code, statusNew.getValueOld(), statusNew.getValue());
        }
    }

    public boolean setBizCode (ObjectNode bizCodeNode) {
        DeviceBizCode deviceBizCode = treeToValue(bizCodeNode, DeviceBizCode.class);
        boolean updateGridOnline = false;
        this.setBizCodeLast(deviceBizCode.getBizCode());
        this.setUpdate_time(deviceBizCode.getTs());
        if ("nameUpdate".equals(deviceBizCode.getBizCode())) {
            deviceBizCode.setValueOld(this.getName());
            deviceBizCode.setValue(deviceBizCode.getBizData().getName());
            this.setName(deviceBizCode.getBizData().getName());
        } else if ("online".equals(deviceBizCode.getBizCode())) {
            this.onLine.put(deviceBizCode.getTs(), true);
            deviceBizCode.setValueOld(false);
            deviceBizCode.setValue(true);
            updateGridOnline = true;
        }else if ("offline".equals(deviceBizCode.getBizCode())) {
            this.onLine.put(deviceBizCode.getTs(), false);
            deviceBizCode.setValueOld(true);
            deviceBizCode.setValue(false);
            updateGridOnline = true;
        }
        log.info("Device: [{}] time: -> [{}] parameter bizCode: [{}] valueOld: [{}]  valueNew: [{}] ",
                this.getName(), toLocaleTimeString(this.getUpdate_time()), deviceBizCode.getBizCode(),
                deviceBizCode.getValueOld(), deviceBizCode.getValue());
        return updateGridOnline;
    }
    public Object getStatusValue (String key, Object valueDef){
        return this.status == null || this.status.get(key) == null ? valueDef : this.status.get(key).getValue();
    }
    public Object getStatusValue (String key){
        return this.status == null || this.status.get(key) == null ? null : this.status.get(key).getValue();
    }

    private void setStatus(String code, DeviceStatus status) {
        if (this.status == null) {
            this.status = new HashMap<>();
        }
        this.status.put(code, status);
    }

    public Entry<Long, Boolean> currentStateOnLine() {
        Optional<Entry<Long, Boolean>> maxEntry = this.getOnLine().entrySet()
                .stream()
                .max(Entry.comparingByKey());
        return maxEntry.orElse(null);
    }

    public Boolean getCurrentStateOnLineValue() {
        Entry<Long, Boolean> stateOnLine = this.currentStateOnLine();
        return stateOnLine != null ? stateOnLine.getValue() : null;
    }
}

