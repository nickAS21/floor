package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;
import static org.nickas21.smart.util.StringUtils.getSignedInt;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataDachaAcBatteryBlock106 {

    private final String[] allValues = new String[53];

    private Double batteryTemperature;
    private Double batteryVoltage;
    private Integer soc;
    private Integer batteryPower;
    private Double batteryCurrent;
    private Double acVoltageL1, acVoltageL2, acVoltageL3;
    private Double acCurrentL1, acCurrentL2, acCurrentL3;
    private Integer loadPowerL1, loadPowerL2, loadPowerL3;
    private Integer totalConsumptionPower;
    private Integer totalConsumptionApparentPower;
    private Double acFrequency;

    private InverterDataDachaAcBatteryBlock106(byte[] data) {
        for (int i = 0; i < 53; i++) {
            int offset = i * 2;
            int val = getUint16(data, offset);
            allValues[i] = String.valueOf(val);

            switch (offset) {
                case 0 -> {
                    batteryTemperature = (val - 1000) * 0.1;
                    allValues[i] = String.format("%.1f", batteryTemperature);
                }
                case 2 -> {
                    batteryVoltage = val * 0.01;
                    allValues[i] = String.format("%.2f", batteryVoltage);
                }
                case 4 -> {
                    soc = val;
                    allValues[i] = String.valueOf(soc);
                }
                case 8 -> {
                    // batteryPower знаковий
                    batteryPower = getSignedInt(data, offset);
                    allValues[i] = String.valueOf(batteryPower);
                }
                case 10 -> {
                    batteryCurrent = getSignedInt(data, offset) * 0.01;
                    allValues[i] = String.format("%.2f", batteryCurrent);
                }
                case 82, 84, 86 -> {
                    double v = val * 0.1;
                    if (offset == 82) acVoltageL1 = v;
                    else if (offset == 84) acVoltageL2 = v;
                    else acVoltageL3 = v;
                    allValues[i] = String.format("%.1f", v);
                }
                case 88, 90, 92 -> {
                    double a = getSignedInt(data, offset) * 0.01;
                    if (offset == 88) acCurrentL1 = a;
                    else if (offset == 90) acCurrentL2 = a;
                    else acCurrentL3 = a;
                    allValues[i] = String.format("%.2f", a);
                }
                case 94, 96, 98 -> {
                    if (offset == 94) loadPowerL1 = val;
                    else if (offset == 96) loadPowerL2 = val;
                    else loadPowerL3 = val;
                    allValues[i] = String.valueOf(val);
                }
                case 100 -> {
                    totalConsumptionPower = val;
                    allValues[i] = String.valueOf(val);
                }
                case 102 -> {
                    totalConsumptionApparentPower = val;
                    allValues[i] = String.valueOf(val);
                }
                case 104 -> {
                    acFrequency = val * 0.01;
                    allValues[i] = String.format("%.2f", acFrequency);
                }
            }
        }
    }

    public static Optional<InverterDataDachaAcBatteryBlock106> of(byte[] data) {
        try {
            return (data != null && data.length >= 106)
                    ? Optional.of(new InverterDataDachaAcBatteryBlock106(data))
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
            // toString тепер максимально тупий і надійний
            sb.append(String.format("[%-35s][%03d]: %-8s | ", getLabel(offset), offset, allValues[i]));
            if ((i + 1) % 2 == 0) sb.append("\n    ");
        }
        return sb.toString().trim();
    }

    private String getLabel(int offset) {
        return switch (offset) {
            case 0 -> "Temperature- Battery(?)";
            case 2 -> "Battery Voltage(V)";
            case 4 -> "SoC(%)";
            case 8 -> "Battery Power(W)";
            case 10 -> "Battery current 1(A)";
            case 82 -> "AC Voltage R/U/A(V)";
            case 84 -> "AC Voltage S/V/B(V)";
            case 86 -> "AC Voltage T/W/C(V)";
            case 88 -> "AC Current R/U/A(A)";
            case 90 -> "AC Current S/V/B(A)";
            case 92 -> "AC Current T/W/C(A)";
            case 94 -> "Load Power L1(W)";
            case 96 -> "Load Power L2(W)";
            case 98 -> "Load Power L3(W)";
            case 100 -> "Total Consumption Power(W)";
            case 102 -> "Total Consumption Apparent Power(VA)";
            case 104 -> "AC Output Frequency R(Hz)";
            default -> String.format("Nothing_%03d", offset);
        };
    }
}