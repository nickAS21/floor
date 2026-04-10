package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataDachaDailyTotalBlock118 {

    private final String[] allValues = new String[59]; // 118 байт / 2 = 59 регістрів

    private Double dailyChargingEnergy;
    private Double dailyDischargingEnergy;
    private Double totalDischargingEnergy;
    private Double totalEnergySell;
    private Double inverterTemperature;

    private InverterDataDachaDailyTotalBlock118(byte[] data) {
        for (int i = 0; i < 59; i++) {
            int offset = i * 2;
            int val = getUint16(data, offset);
            allValues[i] = String.valueOf(val);

            switch (offset) {
                case 28 -> {
                    dailyChargingEnergy = val * 0.1;
                    allValues[i] = String.format("%.1f", dailyChargingEnergy);
                }
                case 30 -> {
                    dailyDischargingEnergy = val * 0.1;
                    allValues[i] = String.format("%.1f", dailyDischargingEnergy);
                }
                case 36 -> {
                    totalDischargingEnergy = val * 0.1;
                    allValues[i] = String.format("%.1f", totalDischargingEnergy);
                }
                case 48 -> {
                    totalEnergySell = val * 0.1;
                    allValues[i] = String.format("%.1f", totalEnergySell);
                }
                case 82 -> {
                    inverterTemperature = (val - 1000) * 0.1;
                    allValues[i] = String.format("%.1f", inverterTemperature);
                }
            }
        }
    }

    public static Optional<InverterDataDachaDailyTotalBlock118> of(byte[] data) {
        try {
            return (data != null && data.length >= 118)
                    ? Optional.of(new InverterDataDachaDailyTotalBlock118(data))
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
            sb.append(String.format("[%-35s][%03d]: %-8s | ", getLabel(offset), offset, allValues[i]));
            if ((i + 1) % 2 == 0) {
                sb.append("\n    ");
            }
        }
        return sb.toString().trim();
    }

    private String getLabel(int offset) {
        return switch (offset) {
            case 28 -> "Daily Charging Energy(kWh)";
            case 30 -> "Daily Discharging Energy(kWh)";
            case 36 -> "Total Discharging Energy(kWh)";
            case 48 -> "Total Energy Sell(kWh)";
            case 82 -> "Temperature - Inverter(?)";
            default -> String.format("Nothing_%03d", offset);
        };
    }
}