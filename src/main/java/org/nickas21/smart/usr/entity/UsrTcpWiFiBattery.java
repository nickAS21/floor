package org.nickas21.smart.usr.entity;

import lombok.Data;
import org.nickas21.smart.usr.data.UsrTcpWiFiMessageType;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class UsrTcpWiFiBattery {

    private final Map<UsrTcpWiFiMessageType, String> idIdents = new ConcurrentHashMap<>();// ID ідентифікатор акумулятора (19 байтів)

    // --- Метадані підключення та ідентифікації ---
    private final int port;                     // Порт, до якого підключено акумулятор (8891-8898)

    private Instant startTime;            // Час першого отримання пакета (старт сесії)
    private Instant lastTime;            // Час останнього отриманого пакету

    // --- Отримані дані ---
    private UsrTcpWifiC0Data c0Data;         // Поточний пакет C0 (Загальний стан)
    private UsrTcpWifiC1Data c1Data;         // Поточний отриманий пакет C1 (Стан комірок)
    private UsrTcpWiFiErrorRecord errRecord; // Поточна остання помилка

    public UsrTcpWiFiBattery(int port) {
        this.port = port;
        this.startTime = Instant.now();
        this.lastTime = startTime;
        this.c0Data = new UsrTcpWifiC0Data();
        this.c1Data = new UsrTcpWifiC1Data();
    }

    public void updateC0(UsrTcpWifiC0Data data, Instant lastTime) {
        this.c0Data = data;
        this.lastTime = lastTime;
    }

    public void updateC1(UsrTcpWifiC1Data data, Instant lastTime) {
        this.c1Data = data;
        this.lastTime = lastTime;
    }

    // Метод для оновлення часу (коли приходить нова посилка)
    public void updateStartTime(Instant lastTime) {
        this.lastTime = lastTime;
    }

    public boolean isOnline() {
        return this.lastTime != null && this.lastTime.isAfter(Instant.now().minusSeconds(60));
    }

}
