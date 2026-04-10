package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;

import static org.nickas21.smart.util.StringUtils.getSignedInt;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataDachaBmsBlock16 {

    private final String[] allValues = new String[8];

    private Double chargeVoltage;
    private Double dischargeVoltage;
    private Integer chargeCurrentLimit;
    private Integer dischargeCurrentLimit;
    private Integer soc;
    private Double voltage;
    private Integer current;
    private Double temperature;

    private InverterDataDachaBmsBlock16(byte[] data) {
        for (int i = 0; i < 8; i++) {
            int offset = i * 2;
            int val = getUint16(data, offset);
            allValues[i] = String.valueOf(val);

            switch (offset) {
                case 0 -> {
                    chargeVoltage = val * 0.01;
                    allValues[i] = String.format("%.2f", chargeVoltage);
                }
                case 2 -> {
                    dischargeVoltage = val * 0.01;
                    allValues[i] = String.format("%.2f", dischargeVoltage);
                }
                case 4 -> {
                    chargeCurrentLimit = val;
                    allValues[i] = String.valueOf(val);
                }
                case 6 -> {
                    dischargeCurrentLimit = val;
                    allValues[i] = String.valueOf(val);
                }
                case 8 -> {
                    soc = val;
                    allValues[i] = String.valueOf(val);
                }
                case 10 -> {
                    voltage = val * 0.01;
                    allValues[i] = String.format("%.2f", voltage);
                }
                case 12 -> {
                    current = getSignedInt(data, offset);
                    allValues[i] = String.valueOf(current);
                }
                case 14 -> {
                    temperature = (val - 1000) * 0.1;
                    allValues[i] = String.format("%.1f", temperature);
                }
            }
        }
    }

    public static Optional<InverterDataDachaBmsBlock16> of(byte[] data) {
        try {
            return (data != null && data.length >= 16)
                    ? Optional.of(new InverterDataDachaBmsBlock16(data))
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
            sb.append(String.format("[%-30s][%03d]: %-8s | ", getLabel(offset), offset, allValues[i]));
            if ((i + 1) % 2 == 0) {
                sb.append("\n    ");
            }
        }
        return sb.toString().trim();
    }

    private String getLabel(int offset) {
        return switch (offset) {
            case 0 -> "ChargeVoltage(V BMS)";
            case 2 -> "BMS Discharge Voltage(V) BMS";
            case 4 -> "ChargeCurrent Limit(A) BMS";
            case 6 -> "DischargeCurrent Limit(A) BMS";
            case 8 -> "SOC(%) BMS";
            case 10 -> "Voltage(V) BMS";
            case 12 -> "Current(A) BMS";
            case 14 -> "Temperature BMS";
            default -> String.format("Nothing_%03d", offset);
        };
    }
}