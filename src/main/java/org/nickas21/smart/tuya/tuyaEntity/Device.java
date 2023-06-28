package org.nickas21.smart.tuya.tuyaEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.nickas21.smart.util.HttpUtil.formatter;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {
    private String name;
    private String id;
        // unit = w
    private int consumptionPower;
    private int tempSetMax;
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
    private Boolean online;
    private String product_id;
    private Boolean sub;
    private String time_zone;
    private Long update_time;
    private String uuid;
    private String owner_id;

    private String asset_id;
    private String gateway_id;
    private String bizCodeLast;

    public void setStatus (JsonNode statusArrayNode) {
        if (statusArrayNode.isArray()) {
            for (JsonNode statusNode : statusArrayNode) {
                if (statusNode.has("code")) {
                    String code = statusNode.get("code").asText();
                    DeviceStatus statusNew = treeToValue(statusNode, DeviceStatus.class);
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
            }
        }
    }

    public void setBizCode (ObjectNode bizCodeNode) {
        DeviceBizCode deviceBizCode = treeToValue(bizCodeNode, DeviceBizCode.class);
        this.setBizCodeLast(deviceBizCode.getBizCode());
        this.setUpdate_time(deviceBizCode.getTs());
        if ("nameUpdate".equals(deviceBizCode.getBizCode())) {
            deviceBizCode.setValueOld(this.getName());
            deviceBizCode.setValue(deviceBizCode.getBizData().getName());
            this.setName(deviceBizCode.getBizData().getName());
        } else if ("online".equals(deviceBizCode.getBizCode())) {
            this.setOnline(true);
            deviceBizCode.setValueOld(false);
            deviceBizCode.setValue(true);
        }else if ("offline".equals(deviceBizCode.getBizCode())) {
            this.setOnline(false);
            deviceBizCode.setValueOld(true);
            deviceBizCode.setValue(false);
        }
        log.info("Device: [{}] time: -> [{}] parameter bizCode: [{}] valueOld: [{}]  valueNew: [{}] ",
                this.getName(), formatter.format(new Date(this.getUpdate_time())), deviceBizCode.getBizCode(),
                deviceBizCode.getValueOld(), deviceBizCode.getValue());
    }

    private void setStatus(String code, DeviceStatus status) {
        this.status = this.status == null ? new HashMap<>() : this.status;
        this.status.put(code, status);
    }
}

