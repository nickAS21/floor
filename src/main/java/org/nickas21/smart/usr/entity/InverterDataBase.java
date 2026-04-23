package org.nickas21.smart.usr.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.ZoneId;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.updateTimeStampToUtc;

@Data
@EqualsAndHashCode(of = "port")
public abstract class InverterDataBase {

    protected final int port;
    protected final ZoneId zoneId;

    protected Instant startTime;
    protected Instant lastTime;

    public InverterDataBase(int port, ZoneId zoneId) {
        this.port = port;
        this.zoneId = zoneId;
        Instant now = Instant.now();
        long offsetMs = updateTimeStampToUtc(now.toEpochMilli()/1000L, zoneId);
        this.startTime = now.plusMillis(offsetMs);;
        this.lastTime = this.startTime;
    }

    protected void updateTime() {
        Instant now = Instant.now();
        // РАХУЄМО ОФСЕТ ТУТ ТАК САМО, ЯК У КОНСТРУКТОРІ
        long offsetMs = updateTimeStampToUtc(now.toEpochMilli()/1000L, this.zoneId);
        this.lastTime = now.plusMillis(offsetMs); // Тепер тут теж БУДЕ 12:00, а не 09:00
    }
}
