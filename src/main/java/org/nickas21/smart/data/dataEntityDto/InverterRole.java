package org.nickas21.smart.data.dataEntityDto; // або org.nickas21.smart.data.enums

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InverterRole {
    MASTER("M"),
    SLAVE("S");

    private final String prefix;

    /**
     * Повертає роль інвертора за offset'ом від базового порту:
     * offset == 1 -> "M1"
     * offset == 2 -> "S2"
     * offset == 3 -> "S3"
     *
     * @throws IllegalArgumentException якщо offset < 1
     */
    public static String getRoleByOffset(int offset) {
        if (offset < 1) {
            throw new IllegalArgumentException("Invalid inverter offset: %d. Offset must be >= 1".formatted(offset));
        }

        if (offset == 1) {
            return MASTER.getPrefix() + offset;
        }

        return SLAVE.getPrefix() + offset;
    }
}