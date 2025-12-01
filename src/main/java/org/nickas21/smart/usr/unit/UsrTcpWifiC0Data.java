package org.nickas21.smart.usr.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiC0Data {
    // --- Розраховані значення ---
    private String idIdent;
    private float voltageMinV;                  // [2]V: Voltage Min (V)
    private float voltageCurV;                  // [2]V: Voltage (V)
    private float currentA;                     // [2]A: Current (A) (+- int/10)
    private int soc;                            // [1]%: SOC (%)

    // --- Поля info Data та Error Data ---
    private int bmsStatus;                      // [2] BMS status (Enum, 0x07)
    private long bmsStatus1;                    // [4] BMS status1 (0x00000002)
    private long bmsStatus2;                    // [4] BMS status2 (0x00000003)
    private long errorData;                     // [4] Error info Data (3 bytes + 1 reserve)

    // Включення останнього часу отримання для контролю актуальності
    private Instant lastUpdateTime;

    public void setPayload (JsonNode payload) {
        this.idIdent = payload.get("idIdent").asText();
    }
}
