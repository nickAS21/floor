package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;
import static org.nickas21.smart.util.StringUtils.getSignedInt;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataDachaAcBatteryBlock106 {

    private final int[] allValues = new int[53]; // 106 байт / 2 = 53 регістри

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
            allValues[i] = val;

            switch (offset) {
                case 0 -> batteryTemperature = (val - 1000) * 0.1;
                case 2 -> batteryVoltage = val * 0.01;
                case 4 -> soc = val;
                case 8 -> batteryPower = val;
                case 10 -> batteryCurrent = getSignedInt(data, offset) * 0.01;
                case 82 -> acVoltageL1 = val * 0.1;
                case 84 -> acVoltageL2 = val * 0.1;
                case 86 -> acVoltageL3 = val * 0.1;
                case 88 -> acCurrentL1 = getSignedInt(data, offset) * 0.01;
                case 90 -> acCurrentL2 = getSignedInt(data, offset) * 0.01;
                case 92 -> acCurrentL3 = getSignedInt(data, offset) * 0.01;
                case 94 -> loadPowerL1 = val;
                case 96 -> loadPowerL2 = val;
                case 98 -> loadPowerL3 = val;
                case 100 -> totalConsumptionPower = val;
                case 102 -> totalConsumptionApparentPower = val;
                case 104 -> acFrequency = val * 0.01;
            }
        }
    }

    public static Optional<InverterDataDachaAcBatteryBlock106> of(byte[] data) {
        try {
            return Optional.of(new InverterDataDachaAcBatteryBlock106(data));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allValues.length; i++) {
            int offset = i * 2;
            int val = allValues[i];
            sb.append(String.format("[%-35s][%03d]:0x%04X:%-7s | ", getLabel(offset), offset, val, getFormattedValue(offset, val, allValues)));
            if ((i + 1) % 2 == 0) sb.append("\n    ");
        }
        return sb.toString();
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

    private String getFormattedValue(int offset, int val, int[] all) {
        return switch (offset) {
            case 0 -> String.format("%.1f", (val - 1000) * 0.1);
            case 2, 10, 88, 90, 92, 104 -> String.format("%.2f", (offset == 10 || offset >= 88 && offset <= 92) ? (short)val * 0.01 : val * 0.01);
            case 82, 84, 86 -> String.format("%.1f", val * 0.1);
            default -> String.valueOf(val);
        };
    }
}