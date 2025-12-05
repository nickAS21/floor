package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.lenErrorInfoData;
import static org.nickas21.smart.usr.data.UsrTcpWifiError.formatErrorCodeOutput;
import static org.nickas21.smart.usr.data.UsrTcpWifiStatus.fromCode;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiC0Data {

    private double voltageMinV;
    private double voltageCurV;
    private double currentCurA;
    private int socPercent;

    private int bmsStatus;
    private String bmsStatusStr;
    private long bmsStatus1;
    private long bmsStatus2;
    private int errorInfoData;
    private String errorOutput;

    private Instant timestamp;

    // -----------------------------
    //   MAIN UPDATE METHOD
    // -----------------------------
    public void updateC0Data(double voltageMinV, double voltageCurV, double currentCurA, int socPercent, int bmsStatus,
                             long bmsStatus1, long bmsStatus2, int errorInfoData, Instant timestamp) {
        this.voltageMinV = voltageMinV;
        this.voltageCurV = voltageCurV;
        this.currentCurA = currentCurA;
        this.socPercent = socPercent;
        this.bmsStatus = bmsStatus;
        this.bmsStatusStr = fromCode(bmsStatus).getStatus();
        this.bmsStatus1 = bmsStatus1;
        this.bmsStatus2 = bmsStatus2;
        this.errorInfoData = errorInfoData;
        this.errorOutput = formatErrorCodeOutput(this.errorInfoData);
        this.timestamp = timestamp;
    }

    public String decodeC0BmsInfoPayload(String output) {
        try {
            StringBuilder out = new StringBuilder();
            out.append("\n" + output + "\n");
            out.append("#  | Name             | Value\n");
            out.append("---|------------------|------------\n");
            out.append(String.format("1  | Voltage Min (V)  | %.2f V\n", this.getVoltageMinV()));
            out.append(String.format("2  | Voltage (V)      | %.2f V\n", this.getVoltageCurV()));
            out.append(String.format("3  | Current (A)      | %.2f A\n", this.getCurrentCurA()));
            out.append(String.format("4  | SOC (%%)          | %d %%\n", this.getSocPercent()));
            out.append(String.format("5  | BMS status       | %s\n", this.getBmsStatusStr()));
            out.append(String.format("6  | Error info Data  | 0x%s (%dB)\n", Integer.toHexString(this.getErrorInfoData()).toUpperCase(), lenErrorInfoData));
            out.append("------------------------------------------------\n");
            out.append(this.errorOutput).append("\n");
            out.append("------------------------------------------------\n");
            return out.toString();

        } catch (Exception e) {
            log.error("CRITICAL DECODE ERROR C0", e);
            return "\n--- CRITICAL DECODE ERROR C0 ---\n" + e.getMessage() + "\n";
        }
    }
}
