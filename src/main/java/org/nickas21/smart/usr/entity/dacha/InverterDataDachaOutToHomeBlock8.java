package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import java.util.Optional;
import static org.nickas21.smart.util.StringUtils.getUint16;

@Data
public class InverterDataDachaOutToHomeBlock8 {

    public static final String[] labels = {
            "Inverter Output Power L1(W)",      // 000
            "Inverter Output Power L2(W)",      // 002
            "Inverter Output Power L3(W)",      // 004
            "Total Inverter Output Power(W)"    // 006
    };

    private Integer powerOutL1;
    private Integer powerOutL2;
    private Integer powerOutL3;
    private Integer powerOutTotal;

    // Конструктор приватний, щоб змусити використовувати Optional.of()
    private InverterDataDachaOutToHomeBlock8(byte[] data) {
        this.powerOutL1 = getUint16(data, 0);
        this.powerOutL2 = getUint16(data, 2);
        this.powerOutL3 = getUint16(data, 4);
        this.powerOutTotal = getUint16(data, 6);
    }

    public static Optional<InverterDataDachaOutToHomeBlock8> of(byte[] data) {
        try {
            return (data != null) ? Optional.of(new InverterDataDachaOutToHomeBlock8(data)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Integer[] values = {powerOutL1, powerOutL2, powerOutL3, powerOutTotal};

        for (int i = 0; i < labels.length; i++) {
            int val = (values[i] != null) ? values[i] : 0;
            // Пряме форматування без проміжних рекордів для швидкості
            sb.append(String.format("[%s][%03d]:0x%04X:%-7d", labels[i], i * 2, val, val));

            if (i < labels.length - 1) {
                sb.append((i % 2 == 0) ? " | " : " | \n    ");
            } else {
                sb.append(" |");
            }
        }
        return sb.toString();
    }
}