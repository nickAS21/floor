package org.nickas21.smart.usr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.nickas21.smart.usr.data.ErrorLogType;
import org.nickas21.smart.usr.data.UsrTcpWifiBalanceThresholds;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.keyIdx;
import static org.nickas21.smart.usr.data.UsrTcpWiFiDecoders.keyVoltage;
import static org.nickas21.smart.usr.data.UsrTcpWiFiMessageType.C1;
import static org.nickas21.smart.usr.data.UsrTcpWifiBalanceThresholds.getBalanceStatus;
import static org.nickas21.smart.usr.data.UsrTcpWifiError.formatErrorCodeOutput;
import static org.nickas21.smart.util.JacksonUtil.newObjectNode;
import static org.nickas21.smart.util.StringUtils.intToHex;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsrTcpWifiC1Data {

    // -------- RAW DATA --------
    private int cellsCount;
    private Map<Integer, Float> cellVoltagesV = new ConcurrentHashMap<>();

    private int lifeCyclesCount;
    private int socPercent;
    private Integer errorInfoData;

    private int majorVersion;
    private int minorVersion;
    private Instant timestamp;
    private byte[] payloadBytesCur;
    private byte[] payloadBytesLastSaved;

    // -------- DERIVED DATA --------
    private Float sumCellsV;
    private JsonNode minCellV;
    private JsonNode maxCellV;
    private int deltaMv;
    private UsrTcpWifiBalanceThresholds balanceS;
    private String errorOutput;
    private String version;

    // -----------------------------
    //   MAIN UPDATE METHOD
    // -----------------------------
    public void updateC1Data(
            int cellsCount,
            Map<Integer, Float> cellVoltagesV,
            int lifeCyclesCount,
            int socPercent,
            Integer errorInfoData,
            int majorVersion,
            int minorVersion,
            Instant timestamp,
            byte[] payloadBytes
    ) {
        // ---- update raw fields ----
        this.cellsCount = cellsCount;
        this.cellVoltagesV = cellVoltagesV != null ? cellVoltagesV : Map.of();

        this.lifeCyclesCount = lifeCyclesCount;
        this.socPercent = socPercent;
        this.errorInfoData = errorInfoData;

        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.timestamp = timestamp;

        // ---- update derived fields ----
        this.version = "V%02d%02d".formatted(majorVersion, minorVersion);

        this.sumCellsV = computeSumCellsV();
        this.minCellV = computeMinCell();
        this.maxCellV = computeMaxCell();
        this.deltaMv = computeDeltaMv();
        this.balanceS = computeBalanceStatus();
        this.errorOutput = computeErrOutput();
        this.payloadBytesCur = payloadBytes;
    }

    // -----------------------------
    //   CALCULATION METHODS
    // -----------------------------
    private Float computeSumCellsV() {
        return this.cellVoltagesV.isEmpty() ? null :
                (float) this.cellVoltagesV.values().stream()
                        .mapToDouble(Float::doubleValue)
                        .sum();
    }

    private JsonNode computeMinCell() {
        if (cellVoltagesV.isEmpty()) return null;

        var minEntry = this.cellVoltagesV.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElseThrow();

        return newObjectNode()
                .put(keyIdx, minEntry.getKey())
                .put(keyVoltage, minEntry.getValue());
    }

    private JsonNode computeMaxCell() {
        if (this.cellVoltagesV.isEmpty()) return null;

        var maxEntry = this.cellVoltagesV.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        return newObjectNode()
                .put(keyIdx, maxEntry.getKey())
                .put(keyVoltage, maxEntry.getValue());
    }

    private int computeDeltaMv() {
        if (this.minCellV == null || this.maxCellV == null) return -1;

        float minV = this.minCellV.get(keyVoltage).floatValue();
        float maxV = this.maxCellV.get(keyVoltage).floatValue();

        return (int) ((maxV - minV) * 1000);
    }

    private UsrTcpWifiBalanceThresholds computeBalanceStatus() {
        if (this.deltaMv < 0) return null;
        return getBalanceStatus(this.deltaMv);
    }

    private String computeErrOutput() {
        return this.errorInfoData == null ? null : formatErrorCodeOutput(this.errorInfoData);
    }

    public String decodeC1BmsInfoPayload(String output) {
        StringBuilder out = new StringBuilder();
        out.append("\n\n--- DETAILS DECODE C1 ---\n");
        out.append(output).append("\n");
        out.append("1.1) Cells Info Table:\n");
        out.append("#  | Name             | Value\n");
        out.append("---|------------------|------------\n");
        out.append(String.format("1  | SOC:             | %d %%\n", this.socPercent));
        out.append(String.format("2  | SUM_V:           | %.2f V\n", this.sumCellsV));
        out.append(String.format("3  | Ver:             | %s\n", this.version));
        out.append(String.format("4  | Life Cycles:     | %d\n", this.lifeCyclesCount));
        out.append(String.format("5  | Cell%02d_MIN:      | %.3f V\n", this.minCellV.get(keyIdx).asInt(), this.getMinCellV().get(keyVoltage).floatValue()));
        out.append(String.format("6  | Cell%02d_MAX:      | %.3f V\n", this.maxCellV.get(keyIdx).asInt(), this.getMaxCellV().get(keyVoltage).floatValue()));
        out.append(String.format("7  | DELTA:           | %.3f V\n", this.deltaMv / 1000.0));
        out.append(String.format("8  | Balance:         | %s\n", this.balanceS));

        out.append(this.errorOutput).append("\n\n");

        out.append("1.2) Cells Table:\n#\tHEX     mV      V\n");
        for (int i = 0; i < this.cellsCount; i++) {
            Float v = this.cellVoltagesV.get(i);
            int mv = (int) (v * 1000.0);
            out.append(String.format("%02d\t%s\t%d\t%.3f V\n", i+1, Integer.toHexString(mv), mv, v));
        }
        return out.toString();
    }

    public UsrTcpWiFiPacketRecord getErrorUnbalanceForRecords(int port){
        String errorMsgUnBalanceStr = String.format("Code:   %s\n", intToHex(this.errorInfoData)) +
                String.format("Error:  %s\n", this.getBalanceS().getDescription()) +
                String.format("Cell%02d_MIN:  %.3f V\n", this.minCellV.get(keyIdx).asInt(), this.getMinCellV().get(keyVoltage).floatValue()) +
                String.format("Cell%02d_MAX:  %.3f V\n", this.maxCellV.get(keyIdx).asInt(), this.getMaxCellV().get(keyVoltage).floatValue()) +
                String.format("DELTA:       %.3f V\n", this.deltaMv / 1000.0);
        return getErrorForRecords(port, ErrorLogType.B1.name() + ":" + this.balanceS.name(), errorMsgUnBalanceStr.getBytes(StandardCharsets.UTF_8));
    }

    public UsrTcpWiFiPacketRecord getErrorOutputForRecords(int port){
        String errorMsgErrorOutputStr = String.format("Code:   %s\n", intToHex(this.errorInfoData)) +
                                        String.format("Error:  %s\n", this.getErrorOutput());
        return getErrorForRecords(port, ErrorLogType.E1.name() + ":" + intToHex(this.errorInfoData), errorMsgErrorOutputStr.getBytes(StandardCharsets.UTF_8));
    }

    private UsrTcpWiFiPacketRecord getErrorForRecords(int port, String typeError, byte[] errorMsg){
        return new UsrTcpWiFiPacketRecord(
                this.timestamp.toEpochMilli(),
                port,
                typeError,
                errorMsg.length,
                errorMsg
        );
    }

    public UsrTcpWiFiPacketRecord getInfoForRecords(int port){
        if (Arrays.equals(this.payloadBytesCur, this.payloadBytesLastSaved)) {
            return null;
        } else {
            this.payloadBytesLastSaved = this.payloadBytesCur;
            return new UsrTcpWiFiPacketRecord(
                    this.timestamp.toEpochMilli(),
                    port,
                    C1.name(),
                    this.payloadBytesLastSaved.length,
                    this.payloadBytesLastSaved);
        }
    }
}

