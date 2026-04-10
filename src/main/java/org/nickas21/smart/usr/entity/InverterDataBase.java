package org.nickas21.smart.usr.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(of = "port")
public abstract class InverterDataBase {

    protected final int port;

    protected Instant startTime;
    protected Instant lastTime;

    public InverterDataBase(int port) {
        this.port = port;
        this.startTime = Instant.now();
        this.lastTime = this.startTime;
    }

    protected void updateTime() {
        this.lastTime = Instant.now();
    }
}
