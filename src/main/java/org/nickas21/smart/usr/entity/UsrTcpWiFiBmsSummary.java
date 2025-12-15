package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UsrTcpWiFiBmsSummary(Instant timestamp, int socPercent, String bmsErrors, String bmsSummary) {
    @JsonCreator
    public UsrTcpWiFiBmsSummary(@JsonProperty("timestamp") Instant timestamp,
                                 @JsonProperty("socPercent") int socPercent,
                                 @JsonProperty("bmsErrors") String bmsErrors,
                                 @JsonProperty("bmsSummary") String bmsSummary) {
        this.timestamp = timestamp;
        this.socPercent = socPercent;
        this.bmsErrors = bmsErrors;
        this.bmsSummary = bmsSummary;
    }
}
