package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataDachaOutToHomeBlock8 {

    private final String[] allValues = new String[4];

    private Integer powerOutL1;
    private Integer powerOutL2;
    private Integer powerOutL3;
    private Integer powerOutTotal;

    private InverterDataDachaOutToHomeBlock8(byte[] data) {
        for (int i = 0; i < 4; i++) {
            int offset = i * 2;
            int val = getUint16(data, offset);
            allValues[i] = String.valueOf(val);

            switch (offset) {
                case 0 -> powerOutL1 = val;
                case 2 -> powerOutL2 = val;
                case 4 -> powerOutL3 = val;
                case 6 -> powerOutTotal = val;
            }
        }
    }

    public static Optional<InverterDataDachaOutToHomeBlock8> of(byte[] data) {
        try {
            return (data != null && data.length >= 8)
                    ? Optional.of(new InverterDataDachaOutToHomeBlock8(data))
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
            case 0 -> "Inverter Output Power L1(W)";
            case 2 -> "Inverter Output Power L2(W)";
            case 4 -> "Inverter Output Power L3(W)";
            case 6 -> "Total Inverter Output Power(W)";
            default -> String.format("Nothing_%03d", offset);
        };
    }
}