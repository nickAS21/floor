package org.nickas21.smart.usr.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum UsrTcpWifiError {

    // --- BYTE 1 (бiти 15–8) ---
    CRITICAL(0x20, 1, "CRITICAL_FAULT"),
    WARNING(0x10, 1, "WARNING: STATE AFTER CRITICAL FAULT"),

    // --- BYTE 0 (бiти 7–0) ---
    UNIT_LOW_CHARGE(0x08, 0, "CRITICALLY_LOW_CHARGE (<18%)"),
    CELLS_UNBALANCE(0x04, 0, "UNBALANCE_CELLS"),
    CELLS_UNDER_VOLTAGE(0x02, 0, "UNDER_VOLTAGE_CELLS"),
    CELLS_OVER_VOLTAGE(0x01, 0, "OVER_VOLTAGE_CELLS");

    private final int mask;
    private final int byteIndex;  // 0 = low byte, 1 = high byte
    private final String description;

    UsrTcpWifiError(int mask, int byteIndex, String description) {
        this.mask = mask;
        this.byteIndex = byteIndex;
        this.description = description;
    }

    public int getMask() {
        return mask;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSet(int value) {
        int byteValue = (value >> (byteIndex * 8)) & 0xFF;
        return (byteValue & mask) != 0;
    }

    public static List<UsrTcpWifiError> parse(int value) {
        return Arrays.stream(values())
                .filter(f -> f.isSet(value))
                .toList();
    }

    public static List<String> parseDescriptions(int value) {
        return Arrays.stream(values())
                .filter(f -> f.isSet(value))
                .map(f -> f.description + " (0x" + Integer.toHexString(f.mask).toUpperCase() + ")")
                .toList();
    }

    public static List<String> decodeErrorFlags(int errorValue) {
        List<String> messages = parseDescriptions(errorValue);

        // Якщо не знайдено жодного, але errorValue != 0 → UNKNOWN
        if (messages.isEmpty() && errorValue != 0) {
            messages = new ArrayList<>();
            messages.add(
                    "UNKNOWN_ERROR_CODE (0x" + Integer.toHexString(errorValue).toUpperCase() + ")"
            );
        }

        return messages;
    }

    public static String formatErrorCodeOutput(int errorValue) {
        String details;

        if (errorValue == 0) {
            details = "Byte_Valid";
        } else {
            List<String> decoded = decodeErrorFlags(errorValue);

            StringBuilder sb = new StringBuilder();
            sb.append("Error_Code_HEX: ").append(Integer.toHexString(errorValue).toUpperCase());
            sb.append("; Decoded_Errors: ").append(String.join(", ", decoded));

            details = "Byte_Invalid (Details: " + sb + ")";
        }

        return String.format("Error_Code: %s", details);
    }
}
