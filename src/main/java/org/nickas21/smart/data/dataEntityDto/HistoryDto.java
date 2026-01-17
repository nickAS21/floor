package org.nickas21.smart.data.dataEntityDto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor // Обов'язково для Jackson
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoryDto {
    private long timestamp;
    private double batterySoc;
    private String batteryStatus;
    private double batteryVol;
    private boolean gridStatusRealTimeOnLine;
    private boolean gridStatusRealTimeSwitch;
    private Integer inverterPort;
    private String inverterPortConnectionStatus;
    private DataHomeDto dataHome;
    private List<BatteryInfoDto> batteries;

    public HistoryDto(DataHomeDto dataHome, List<BatteryInfoDto> batteries, Integer inverterPort, String inverterPortConnectionStatus) {
        this.dataHome = dataHome;
        this.batteries = batteries;

        if (dataHome != null) {
            this.timestamp = dataHome.getTimestamp();
            this.batterySoc = dataHome.getBatterySoc();
            this.batteryStatus = dataHome.getBatteryStatus();
            this.batteryVol = dataHome.getBatteryVol();
            this.gridStatusRealTimeOnLine = dataHome.isGridStatusRealTimeOnLine();
            this.gridStatusRealTimeSwitch = dataHome.isGridStatusRealTimeSwitch();
        } else {
            this.batteryStatus = "DATA_MISSING";
        }

        // Якщо timestamp не прийшов від пристрою, ставимо системний
        if (this.timestamp == 0) {
            this.timestamp = System.currentTimeMillis();
        }
        this.inverterPort = inverterPort;
        this.inverterPortConnectionStatus = inverterPortConnectionStatus;
    }
}