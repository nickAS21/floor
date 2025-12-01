package org.nickas21.smart.usr.unit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsrTcpWifiUnit {

    // --- Метадані підключення та ідентифікації ---
    private final int port;                     // Порт, до якого підключено акумулятор (8891-8898)
    private String[] idIdents = new String[2];               // ID ідентифікатор акумулятора (19 байтів)
    private LocalDateTime startTime;            // Час першого отримання пакета (старт сесії)
    private LocalDateTime lastTime;            // Час останнього отриманого пакету

    // --- Отримані дані ---
    private UsrTcpWifiC0Data c0Data;         // Останній отриманий пакет C0 (Загальний стан)
    private UsrTcpWifiC1Data c1Data;         // Останній отриманий пакет C1 (Стан комірок)

    // Конструктор
    public UsrTcpWifiUnit(int port, String idIdent, LocalDateTime startTime) {
        this.port = port;
        this.idIdents[0] = idIdent;
        this.startTime = startTime;
        this.lastTime = startTime;
        this.c0Data = new UsrTcpWifiC0Data();
        this.c1Data = new UsrTcpWifiC1Data();
    }

    // Метод для оновлення часу (коли приходить нова посилка)
    public void updateStartTime(LocalDateTime lastTime) {
        this.lastTime = lastTime;
    }
}