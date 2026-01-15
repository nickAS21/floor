package org.nickas21.smart.usr.entity;

import lombok.Data;
import org.nickas21.smart.usr.data.UsrWifiBmsInverterStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.datePatternGridStatus;

@Data
public class InvertorGolegoData90 {

    private Integer port;
    private Long createdAt = Instant.now().toEpochMilli();

    private Integer status;
    private Double acInputVoltage, acInputFrequency, batteryVoltage;
    private Integer soc, batteryChargingCurrent, batteryDischargingCurrent;
    private Double loadAcOutputVoltage, acOutputFrequency;
    private Integer loadOutputApparentPower, loadOutputActivePower, acOutputLoadPercent;
    private Double cutOffVoltage;
    private Integer mainCpuVersion;

    private Integer nominalOutputApparentPower, nominalOutputActivePower, nominalOutputVoltage, nominalAcCurrent;
    private Double ratedBatteryVoltage, comebackUtilityModeVoltage;
    private Integer nominalOutputVoltageNom;
    private Double nominalOutputFrequency;
    private Integer nominalOutputCurrent;
    private Integer maxTotalChargeCurrent, maxUtilityChargeCurrent;

    private Integer rezerv06, rezerv08, rezerv28, rezerv30, rezerv36, rezerv38;
    private Integer rezerv56, rezerv58, rezerv70, rezerv72, rezerv74, rezerv76, rezerv78, rezerv82;
    private Double rezerv60, rezerv62, rezerv64, rezerv66, rezerv68, comebackBatteryModeVoltage;

    private String[] hexMap = new String[0];


    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePatternGridStatus);
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault());
        String timeStr = ldt.format(formatter);


        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== INVERTER DECODE 32 (Port: %d) | %s ===\n", port, timeStr));

        sb.append(formatLine(0, hexMap[0], status, "Статус (" + getStatus() + ")"));
        sb.append(formatLine(2, hexMap[1], acInputVoltage, "Ac Input Voltage"));
        sb.append(formatLine(4, hexMap[2], acInputFrequency, "Hz AC Input Frequency"));
        sb.append(formatLine(6, hexMap[3], rezerv06, "Rezerv_06"));
        sb.append(formatLine(8, hexMap[4], rezerv08, "Rezerv_08"));
        sb.append(formatLine(10, hexMap[5], batteryVoltage, "V (Напруга АКБ)"));
        sb.append(formatLine(12, hexMap[6], soc, "Soc %"));
        sb.append(formatLine(14, hexMap[7], batteryChargingCurrent, "A Battery -> Charging Current "));
        sb.append(formatLine(16, hexMap[8], batteryDischargingCurrent, "A Battery -> Discharging Current "));
        sb.append(formatLine(18, hexMap[9], loadAcOutputVoltage, "AC Load -> Output Voltage V"));
        sb.append(formatLine(20, hexMap[10], acOutputFrequency, "AC Output Frequency Hz"));
        sb.append(formatLine(22, hexMap[11], loadOutputApparentPower, "VA Load -> OutPut Apparent Power"));
        sb.append(formatLine(24, hexMap[12], loadOutputActivePower, "W Load -> OutPut Active Power"));
        sb.append(formatLine(26, hexMap[13], acOutputLoadPercent, "% AC Out Put Load"));
        sb.append(formatLine(28, hexMap[14], rezerv28, "Rezerv_28"));
        sb.append(formatLine(30, hexMap[15], rezerv30, "Rezerv_30"));
        sb.append(formatLine(32, hexMap[16], cutOffVoltage, "Rezerv_32 (Cut-off V)"));
        sb.append(formatLine(34, hexMap[17], mainCpuVersion, "Main CPU Version"));
        sb.append(formatLine(36, hexMap[18], rezerv36, "Rezerv_36"));
        sb.append(formatLine(38, hexMap[19], rezerv38, "Rezerv_38"));
        sb.append(formatLine(40, hexMap[20], nominalOutputApparentPower, "VA Nominal Output Apparent Power"));
        sb.append(formatLine(42, hexMap[21], nominalOutputActivePower, "W Nominal Output Active Power"));
        sb.append(formatLine(44, hexMap[22], nominalOutputVoltage, "V Nominal Output Voltage"));
        sb.append(formatLine(46, hexMap[23], nominalAcCurrent, "A Nominal AC current"));
        sb.append(formatLine(48, hexMap[24], ratedBatteryVoltage, "V Rated Battery Voltage"));
        sb.append(formatLine(50, hexMap[25], nominalOutputVoltageNom, "V Nominal OutPut Voltage"));
        sb.append(formatLine(52, hexMap[26], nominalOutputFrequency, "Hz Nominal OutPut Frequency"));
        sb.append(formatLine(54, hexMap[27], nominalOutputCurrent, "A Nominal OutPut Current"));
        sb.append(formatLine(56, hexMap[28], rezerv56, "Rezerv_56"));
        sb.append(formatLine(58, hexMap[29], rezerv58, "Rezerv_58"));
        sb.append(formatLine(60, hexMap[30], rezerv60, "V Rezerv_60"));
        sb.append(formatLine(62, hexMap[31], rezerv62, "V Rezerv_62"));
        sb.append(formatLine(64, hexMap[32], rezerv64, "V Rezerv_64"));
        sb.append(formatLine(66, hexMap[33], rezerv66, "V Rezerv_66"));
        sb.append(formatLine(68, hexMap[34], rezerv68, "V Rezerv_68"));
        sb.append(formatLine(70, hexMap[35], rezerv70, "Rezerv_70"));
        sb.append(formatLine(72, hexMap[36], rezerv72, "Rezerv_72"));
        sb.append(formatLine(74, hexMap[37], rezerv74, "Rezerv_74"));
        sb.append(formatLine(76, hexMap[38], rezerv76, "Rezerv_76"));
        sb.append(formatLine(78, hexMap[39], rezerv78, "Rezerv_78"));
        sb.append(formatLine(80, hexMap[40], maxTotalChargeCurrent, "A Max Total Charge Current"));
        sb.append(formatLine(82, hexMap[41], rezerv82, "Rezerv_82"));
        sb.append(formatLine(84, hexMap[42], maxUtilityChargeCurrent, "A Max Utility Charge Current"));
        sb.append(formatLine(86, hexMap[43], comebackUtilityModeVoltage, "V Comeback Utility Mode Voltage Point Under (SBU Priority)"));
        sb.append(formatLine(88, hexMap[44], comebackBatteryModeVoltage, "V Comeback Battery Mode Voltage Point Under (SBU Priority)"));

        sb.append("==============================================");
        return sb.toString();
    }

    private String formatLine(int idx, String hex, Object val, String label) {
        if (val == null) return "";

        // Розрахунок rawVal та fVal залишається без змін
        long rawVal = (val instanceof Double) ? Math.round((Double)val * (idx >= 60 && idx <= 66 ? 100 : 10)) : ((Number)val).longValue();
        String fVal = (val instanceof Double) ? String.format(idx >= 60 && idx <= 66 ? "%.2f" : "%.1f", val) : val.toString();

        // ЗАХИСТ: перевіряємо, чи hex не null, і чи індекс не виходить за межі масиву
        String hexStr = (hexMap != null && idx/2 < hexMap.length && hexMap[idx/2] != null) ? hexMap[idx/2] : "00 00";

        return String.format("[%d:%d] %s: %d -> %s %s\n", idx, idx + 1, hexStr, rawVal, fVal, label);
    }

    public String getStatus() {
        return UsrWifiBmsInverterStatus.fromCode(this.status).getStatus();
    }

    public Integer getBatteryCurrent() {
        return UsrWifiBmsInverterStatus.DISCHARGING.getCode().equals(this.status) ? -this.batteryDischargingCurrent : this.batteryChargingCurrent;
    }
}