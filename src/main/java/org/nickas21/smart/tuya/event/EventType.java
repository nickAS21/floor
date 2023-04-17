package org.nickas21.smart.tuya.event;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: jinyun.zhou@tuya.com
 **/
public enum EventType {
    STATUS_REPORT(4, "statusReport", "Data Reporting"),

    ONLINE(20, "online", "Device is Online"),
    OFFLINE(20, "offline", "Device is Offline"),
    NAME_UPDATE(20, "nameUpdate", "Rename the Device"),
    DP_NAME_UPDATE(20, "dpNameUpdate", "Modify device function point name"),
    DELETE(20, "delete", "Unbind the device (delete the device)"),
    BIND_USER(20, "bindUser", "Device Binding"),
    UPGRADE_STATUS(20, "upgradeStatus", "Device Upgrade Status"),
    SHARE(20, "share", "Device sharing"),
    DEVICE_SIGNAL(20, "deviceSignal", "Device Semaphore"),
    DEVICE_DP_COMMAND(20, "deviceDpCommand", "Device Control"),

    USER_REGISTER(21, "userRegister", "User Register"),
    USER_UPDATE(21, "userUpdate", "User Update"),
    USER_DELETE(21, "userDelete", "User Logout"),

    AUTOMATION_EXTERNAL_ACTION(22, "automationExternalAction", "Automation External Action"),

    SCENE_EXECUTE(25, "sceneExecute", "Scene execution"),

    ROOM_NAME_UPDATE(34, "roomNameUpdate", "Room Renamed"),
    ROOM_SORT(34, "roomSort", "Room Sort"),

    HOME_CREATE(35, "homeCreate", "Home Create"),
    HOME_UPDATE(35, "homeUpdate", "Home Update"),
    HOME_DELETE(35, "homeDelete", "Home Delete"),
    ROOM_CREATE(35, "roomCreate", "Room Create"),
    ROOM_DELETE(35, "roomDelete", "Room Delete"),

    // extend other type message here
    UNKNOWN_MESSAGE(null, "unknownMessage", "Unknown Message"),
    ;

    private Integer protocol;
    private final String type;
    private String description;

    private static final Map<String, EventType> CACHE;

    static {
        CACHE = Arrays.stream(EventType.values()).collect(Collectors.toMap(
            item -> (item.getProtocol() + item.getType()), Function.identity()));
    }

    EventType(Integer protocol, String type, String description) {
        this.type = type;
        this.protocol = protocol;
        this.description = description;
    }

    public static EventType of(Integer protocol, String bizCode) {
        //数据上报特有逻辑
        if (protocol == 4 && bizCode == null) {
            bizCode = STATUS_REPORT.type;
        }
        return CACHE.getOrDefault(protocol + bizCode, UNKNOWN_MESSAGE);
    }

    public Integer getProtocol() {
        return protocol;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}