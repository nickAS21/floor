package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;

import static org.nickas21.smart.util.StringUtils.getSignedInt;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataDachaBmsBlock16 {

    public static final String[] labels = {
            "ChargeVoltage(V BMS)",           // 000
            "BMS Discharge Voltage(V) BMS",   // 002
            "ChargeCurrent Limit(A) BMS",     // 004
            "DischargeCurrent Limit(A) BMS",  // 006
            "SOC(%) BMS",                     // 008
            "Voltage(V) BMS",                 // 010
            "Current(A) BMS",                 // 012
            "Temperature BMS"                 // 014
    };

    private Double chargeVoltage;
    private Double dischargeVoltage;
    private Integer chargeCurrentLimit;
    private Integer dischargeCurrentLimit;
    private Integer soc;
    private Double voltage;
    private Integer current;
    private Double temperature;

    private InverterDataDachaBmsBlock16(byte[] data) {
        this.chargeVoltage = getUint16(data, 0) * 0.01;
        this.dischargeVoltage = getUint16(data, 2) * 0.01;
        this.voltage = getUint16(data, 10) * 0.01;

        this.chargeCurrentLimit = getUint16(data, 4);
        this.dischargeCurrentLimit = getUint16(data, 6);
        this.soc = getUint16(data, 8);

        // Використовуємо оновлений метод
        this.current = getSignedInt(data, 12);

        this.temperature = (getUint16(data, 14) - 1000) * 0.1;
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
        Object[] values = {
                chargeVoltage, dischargeVoltage, chargeCurrentLimit,
                dischargeCurrentLimit, soc, voltage, current, temperature
        };

        for (int i = 0; i < labels.length; i++) {
            int offset = i * 2;
            Object val = values[i];

            // Сучасний Pattern Matching для Double
            String dec = (val instanceof Double d)
                    ? (offset == 14 ? String.format("%.1f", d) : String.format("%.2f", d))
                    : String.valueOf(val);

            sb.append(String.format("[%s][%03d]:%-7s | ", labels[i], offset, dec));

            if ((i + 1) % 2 == 0 && i != labels.length - 1) {
                sb.append("\n    ");
            }
        }
        return sb.toString();
    }
}