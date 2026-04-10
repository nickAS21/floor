package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataLoadDcBlock80 {

    private final String[] allValues = new String[40];

    private Double loadVoltageL1, loadVoltageL2, loadVoltageL3;
    private Integer loadPowerL1, loadPowerL2, loadPowerL3;
    private Integer totalConsumptionPower;
    private Integer totalConsumptionApparentPower;
    private Double loadFrequency;
    private Integer dcPowerPv1, dcPowerPv2, dcPowerPv3, dcPowerPv4;
    private Integer totalDcPowerSumPv = 0;

    public InverterDataLoadDcBlock80(byte[] data) {
        for (int i = 0; i < 40; i++) {
            int offset = i * 2;
            int val = getUint16(data, offset);
            allValues[i] = String.valueOf(val);

            switch (i) {
                case 0 -> {
                    loadVoltageL1 = val * 0.1;
                    allValues[i] = String.format("%.1f", loadVoltageL1);
                }
                case 1 -> {
                    loadVoltageL2 = val * 0.1;
                    allValues[i] = String.format("%.1f", loadVoltageL2);
                }
                case 2 -> {
                    loadVoltageL3 = val * 0.1;
                    allValues[i] = String.format("%.1f", loadVoltageL3);
                }
                case 6 -> {
                    loadPowerL1 = val;
                    allValues[i] = String.valueOf(val);
                }
                case 7 -> {
                    loadPowerL2 = val;
                    allValues[i] = String.valueOf(val);
                }
                case 8 -> {
                    loadPowerL3 = val;
                    allValues[i] = String.valueOf(val);
                }
                case 9 -> {
                    totalConsumptionPower = val;
                    allValues[i] = String.valueOf(val);
                }
                case 10 -> {
                    totalConsumptionApparentPower = val;
                    allValues[i] = String.valueOf(val);
                }
                case 11 -> {
                    loadFrequency = val * 0.01;
                    allValues[i] = String.format("%.2f", loadFrequency);
                }
                case 28 -> {
                    dcPowerPv1 = val;
                    totalDcPowerSumPv += val;
                    allValues[i] = String.valueOf(val);
                }
                case 29 -> {
                    dcPowerPv2 = val;
                    totalDcPowerSumPv += val;
                    allValues[i] = String.valueOf(val);
                }
                case 30 -> {
                    dcPowerPv3 = val;
                    totalDcPowerSumPv += val;
                    allValues[i] = String.valueOf(val);
                }
                case 31 -> {
                    dcPowerPv4 = val;
                    totalDcPowerSumPv += val;
                    allValues[i] = String.valueOf(val);
                }
            }
        }
    }

    public static Optional<InverterDataLoadDcBlock80> of(byte[] data) {
        try {
            return (data != null && data.length >= 80)
                    ? Optional.of(new InverterDataLoadDcBlock80(data))
                    : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allValues.length; i++) {
            int offset = i * 2;
            sb.append(String.format("[%-35s][%03d]: %-8s | ", getLabel(i), offset, allValues[i]));
            if ((i + 1) % 2 == 0) {
                sb.append("\n    ");
            }
        }
        sb.append(String.format("[Total DC Power Sum PV(W)          ][---]: %-8d |", totalDcPowerSumPv));
        return sb.toString().trim();
    }

    private String getLabel(int i) {
        return switch (i) {
            case 0 -> "Load Voltage L1(V)";
            case 1 -> "Load Voltage L2(V)";
            case 2 -> "Load Voltage L3(V)";
            case 6 -> "Load Power L1(W)";
            case 7 -> "Load Power L2(W)";
            case 8 -> "Load Power L3(W)";
            case 9 -> "Total Consumption Power(W)";
            case 10 -> "Total Consumption Apparent Power(VA)";
            case 11 -> "Load Frequency(Hz)";
            case 28 -> "DC Power PV1(W)";
            case 29 -> "DC Power PV2(W)";
            case 30 -> "DC Power PV3(W)";
            case 31 -> "DC Power PV4(W)";
            default -> String.format("Nothing_%03d", i * 2);
        };
    }
}