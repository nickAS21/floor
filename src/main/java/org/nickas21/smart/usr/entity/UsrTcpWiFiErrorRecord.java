package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * {"ts":"2025-02-01T12:33:10Z","level":"WARN","code":"0x04","desc":"UNBALANCE_CELLS"}
 * {"ts":"2025-02-01T12:55:41Z","level":"ERROR","code":"0x02","desc":"UNDER_VOLTAGE_CELLS"}
 * {"ts":"2025-02-01T13:10:22Z","level":"INFO","code":"0x10","desc":"WARNING AFTER CRITICAL STATE"}
 */
public record UsrTcpWiFiErrorRecord(Instant timestamp, String code, String description) {
    @JsonCreator
    public UsrTcpWiFiErrorRecord(@JsonProperty("timestamp") Instant timestamp,
                                 @JsonProperty("code") String code,
                                 @JsonProperty("description") String description) {
        this.timestamp = timestamp;
        this.code = code;
        this.description = description;
    }
}
