package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecord;

import java.time.Instant;
import java.util.Arrays;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.lenErrorInfoData;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C0;
import static org.nickas21.smart.usr.data.fault.UsrTcpWifiFault.formatErrorCodeOutput;
import static org.nickas21.smart.usr.data.UsrTcpWifiStatus.fromCode;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiC0Data {

    private double voltageMinV;
    private double voltageCurV;
    private double currentCurA;
    private double socPercent;

    private int bmsStatus;
    private String bmsStatusStr;
    private long bmsStatus1;
    private long bmsStatus2;
    private Integer errorInfoData;
    private String errorOutput;
    private Instant timestamp;
    private byte[] payloadBytesCur;
    private byte[] payloadBytesLastSaved;

    // -----------------------------
    //   MAIN UPDATE METHOD
    // -----------------------------
    public void updateC0Data(double voltageMinV, double voltageCurV, double currentCurA, double socPercent, int bmsStatus,
                             long bmsStatus1, long bmsStatus2, int errorInfoData, Instant timestamp, byte[] payloadBytes) {
        this.voltageMinV = voltageMinV;
        this.voltageCurV = voltageCurV;
        this.currentCurA = currentCurA;
        this.socPercent = socPercent;
        this.bmsStatus = bmsStatus;
        this.bmsStatusStr = fromCode(bmsStatus).getStatus();
        this.bmsStatus1 = bmsStatus1;
        this.bmsStatus2 = bmsStatus2;
        this.errorInfoData = errorInfoData; // 0x1007 (hex) = 4103 (dec)  0x1008 (hex) = 4104 (dec) 0x2007 (hex) = 8199(dec) 0x2008 (hex) = 8200(dec)
        this.errorOutput = computeErrOutput();
        this.timestamp = timestamp;
        this.payloadBytesCur = payloadBytes;
    }

    private String computeErrOutput() {
        return this.errorInfoData == null ? null : formatErrorCodeOutput(this.errorInfoData);
    }

    public String decodeC0BmsInfoPayload(String output) {
        try {
            return "\n\n--- DETAILS DECODE C0 ---\n" +
                    output + "\n" +
                    "#  | Name             | Value\n" +
                    "---|------------------|------------\n" +
                    String.format("1  | SOC (%%)          | %.2f %%\n", this.getSocPercent()) +
                    String.format("2  | Voltage (V)      | %.2f V\n", this.getVoltageCurV()) +
                    String.format("3  | Current (A)      | %.2f A\n", this.getCurrentCurA()) +
                    String.format("4  | Voltage Min (V)  | %.2f V\n", this.getVoltageMinV()) +
                    String.format("5  | BMS status       | %s\n", this.getBmsStatusStr()) +
                    String.format("6  | Error info Data  | 0x%s (%dB)\n", Integer.toHexString(this.getErrorInfoData()).toUpperCase(), lenErrorInfoData) +
                    "------------------------------------------------\n" +
                    this.errorOutput + "\n" +
                    "------------------------------------------------\n";

        } catch (Exception e) {
            log.error("CRITICAL DECODE ERROR C0", e);
            return "\n--- CRITICAL DECODE ERROR C0 ---\n" + e.getMessage() + "\n";
        }
    }

    public UsrTcpWiFiPacketRecord getInfoForRecords(int port){
        if (Arrays.equals(this.payloadBytesCur, this.payloadBytesLastSaved)) {
            return null;
        } else {
            this.payloadBytesLastSaved = this.payloadBytesCur;
            return new UsrTcpWiFiPacketRecord(
                    this.timestamp.toEpochMilli(),
                    port,
                    C0.name(),
                    this.payloadBytesCur.length,
                    this.payloadBytesCur);
        }
    }
}
