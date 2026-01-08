package org.nickas21.smart.usr.entity;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.datePatternGridStatus;

@Data
public class InvertorGolegoData32 {
    private Integer port;
    private Long createdAt = Instant.now().toEpochMilli();

    // Дані згідно з мапінгом
    private Double batteryVoltage1;        // [00:01]
    private Double batteryVoltage2;        // [02:03]
    private Double ratedBatteryVoltage;    // [04:05]
    private Double batteryVoltage3;        // [06:07]
    private Double currentCollectionFrequency;        // [08:09]
    private Integer batteryEqualizeTimeout; // [10:11]
    private Integer batteryEqualizeInterval; // [12:13]

    // Резерви
    private Integer rezerv14, rezerv16, rezerv18, rezerv20, rezerv22, rezerv24, rezerv26, standardCollectionFrequency, rezerv30;

    private String[] hexMap = new String[0];

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePatternGridStatus);
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault());
        String timeStr = ldt.format(formatter);


        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== INVERTER DECODE 32 (Port: %d) | %s ===\n", port, timeStr));

        sb.append(formatLine(0, hexMap[0], batteryVoltage1, "V (Напруга АКБ)"));
        sb.append(formatLine(2, hexMap[1], batteryVoltage2, "V (Напруга АКБ)"));
        sb.append(formatLine(4, hexMap[2], ratedBatteryVoltage, "V Rated Battery Voltage"));
        sb.append(formatLine(6, hexMap[3], batteryVoltage3, "V (Напруга АКБ)"));
        sb.append(formatLine(8, hexMap[4], currentCollectionFrequency, "min (Current Collection Frequency"));
        sb.append(formatLine(10, hexMap[5], batteryEqualizeTimeout, "min (Battery Equalyze Timeout)"));
        sb.append(formatLine(12, hexMap[6], batteryEqualizeInterval, "day (Battery Equalyze Intervsal)"));
        sb.append(formatLine(14, hexMap[7], rezerv14, "Rezerv_14"));
        sb.append(formatLine(16, hexMap[8], rezerv16, "Rezerv_16"));
        sb.append(formatLine(18, hexMap[9], rezerv18, "Rezerv_18"));
        sb.append(formatLine(20, hexMap[10], rezerv20, "Rezerv_20"));
        sb.append(formatLine(22, hexMap[11], rezerv22, "Rezerv_22"));
        sb.append(formatLine(24, hexMap[12], rezerv24, "Rezerv_24"));
        sb.append(formatLine(26, hexMap[13], rezerv26, "Rezerv_26"));
        sb.append(formatLine(28, hexMap[14], standardCollectionFrequency, "min (Standard Collection Frequency)"));
        sb.append(formatLine(30, hexMap[15], rezerv30, "Rezerv_30"));

        sb.append("==============================================");
        return sb.toString();
    }

    private String formatLine(int idx, String hex, Object val, String label) {
        if (val == null) return "";
        // Множник 10 тільки для напруг (0-7 індекси), для інших 1
        int multiplier = (idx <= 7) ? 10 : 1;
        long rawVal = (val instanceof Double) ? Math.round((Double)val * multiplier) : ((Number)val).longValue();
        String fVal = (val instanceof Double) ? String.format("%.1f", val) : val.toString();

        return String.format("[%d:%d] %s: %d -> %s %s\n",
                idx, idx + 1, (hex != null ? hex : "00 00"), rawVal, fVal, label);
    }
}
