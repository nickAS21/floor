package org.nickas21.smart.usr.entity.golego;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nickas21.smart.usr.data.UsrTcpWiFiMessageType;
import org.nickas21.smart.usr.entity.BatteryDataBase;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecordError;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@EqualsAndHashCode(callSuper = true)
public class BatteryDataUsrTcpWiFi extends BatteryDataBase {

    private final Map<UsrTcpWiFiMessageType, String> idIdents = new ConcurrentHashMap<>();// ID ідентифікатор акумулятора (19 байтів)

    // --- Отримані дані ---
    private UsrTcpWifiC0Data c0Data;         // Поточний пакет C0 (Загальний стан)
    private UsrTcpWifiC1Data c1Data;         // Поточний отриманий пакет C1 (Стан комірок)
    private UsrTcpWiFiPacketRecordError errRecordE1;
    private UsrTcpWiFiPacketRecordError errRecordB1;

    public BatteryDataUsrTcpWiFi(int port) {
        super(port);
        this.c0Data = new UsrTcpWifiC0Data();
        this.c1Data = new UsrTcpWifiC1Data();
    }
}
