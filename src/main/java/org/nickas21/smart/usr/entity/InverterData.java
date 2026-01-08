package org.nickas21.smart.usr.entity;

import lombok.Data;

import java.time.Instant;

@Data
public class InverterData {
    private final int port;                     // Порт, до якого підключено акумулятор (8900)

    private Instant startTime;            // Час першого отримання пакета (старт сесії)
    private Instant lastTime;
    private InvertorGolegoData90 invertorGolegoData90;
    private InvertorGolegoData32 invertorGolegoData32;

    public InverterData(int port, InvertorGolegoData32 invertorGolegoData32, InvertorGolegoData90 invertorGolegoData90) {
        this.port = port;
        this.startTime = Instant.now();
        this.lastTime = startTime;
        this.invertorGolegoData90 = invertorGolegoData90;
        this.invertorGolegoData32 = invertorGolegoData32;
    }

    public void inverterDataUpdate (InvertorGolegoData90 invertorGolegoData90) {
        this.lastTime = Instant.now();
        this.invertorGolegoData90 = invertorGolegoData90;
    }

    public void inverterDataUpdate (InvertorGolegoData32 invertorGolegoData32) {
        this.lastTime = Instant.now();
        this.invertorGolegoData32 = invertorGolegoData32;
    }
}
