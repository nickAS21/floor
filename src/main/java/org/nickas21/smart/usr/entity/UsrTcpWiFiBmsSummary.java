package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UsrTcpWiFiBmsSummary(Instant timestamp, int socPercent, String bmsSummary) {
    @JsonCreator
    public UsrTcpWiFiBmsSummary(@JsonProperty("timestamp") Instant timestamp,
                                 @JsonProperty("socPercent") int socPercent,
                                 @JsonProperty("bmsSummary") String bmsSummary) {
        this.timestamp = timestamp;
        this.socPercent = socPercent;
        this.bmsSummary = bmsSummary;
    }
}
