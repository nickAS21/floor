package org.nickas21.smart.data.dataEntityDto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoryDto {
    // --- Short Info (Header) ---
    private long timestamp;
    private String gridStatus;
    private double batterySoc;
    private String batteryStatus;
    private double batteryVol;

    // --- Full Info (Payload) ---
    private double batteryCurrent;
    private boolean gridStatusRealTimeOnLine;
    private boolean gridStatusRealTimeSwitch;
    private String batteriesFault;
    private HistoryFullInfoDto fullInfo;
}