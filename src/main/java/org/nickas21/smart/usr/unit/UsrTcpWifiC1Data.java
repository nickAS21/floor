package org.nickas21.smart.usr.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiC1Data {
    private int cellsCount;                     // [1] cells_cnt
    private List<Float> cellVoltagesV;          // [32] cells_v (Список напруг кожної комірки)
    private int lifeCyclesCount;                // [2] life_cycles_cnt (Життєві цикли)
    private int socPercent;                     // [1] soc (Повторне значення SOC)
    private long errorData;                     // [3] Error info Data
    //  private int version;                    // [2] ver (Наприклад, 0C0C)
    private int majorVersion;
    private int minorVersion;


    // Включення останнього часу отримання для контролю актуальності
    private Instant lastUpdateTime;

    public String getVersion() {
        return String.format("\nVer: V%02d%02d\n", this.majorVersion, this.minorVersion);
    }
}
