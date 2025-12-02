package org.nickas21.smart.usr.unit;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enum for managing and mapping error codes received from the BMS.
 * Maps error bitmasks (Hex Integers) to human-readable descriptions.
 */
@Getter
public enum UsrTcpWifiError {


    // --- BYTE 1 (біти 15–8) ---
    CRITICAL(0x20, "CRITICAL_FAULT", 1),
    WARNING(0x10, "WARNING: STATE AFTER CRITICAL FAULT", 1),

    // --- BYTE 0 (біти 7–0) ---
    UNIT_LOW_CHARGE(0x08, "CRITICALLY_LOW_CHARGE (<18%)", 0),
    CELLS_UNBALANCE(0x04, "UNBALANCE_CELLS", 0),
    CELLS_UNDER_VOLTAGE(0x02, "UNDER_VOLTAGE_CELLS", 0),
    CELLS_OVER_VOLTAGE(0x01, "OVER_VOLTAGE_CELLS", 0);

    private final int mask;
    private final int byteIndex;
    private final String description;

    UsrTcpWifiError(int mask, String description, int byteIndex) {
        this.mask = mask;
        this.description = description;
        this.byteIndex = byteIndex;
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
                .map(f -> f.getDescription()
                        + " (0x"
                        + Integer.toHexString(f.getMask()).toUpperCase()
                        + ")")
                .toList();
    }

    public static List<String> decodeErrorFlags(int errorValue) {
        List<String> messages =  Arrays.stream(UsrTcpWifiError.values())
                .filter(f -> f.isSet(errorValue))
                .map(f -> f.getDescription()
                        + " (0x"
                        + Integer.toHexString(f.getMask()).toUpperCase()
                        + ")")
                .toList();

        // Check for UNKNOWN codes in the lowest 2 bytes (bits 15-0)
        if (messages.isEmpty() && errorValue != 0) {
            messages = new ArrayList<>();
            messages.add("UNKNOWN_ERROR_CODE (0x" + Integer.toHexString(errorValue).toUpperCase() + ")");
        }

        return messages;
    }

    public static String formatErrorCodeOutput(int errorValue, int number) {
        String details;
        if (errorValue == 0) {
            details = "Byte_Valid";
        } else {
            List<String> decoded = decodeErrorFlags(errorValue);
            StringBuilder sb = new StringBuilder();
            sb.append("Error_Code_HEX: ").append(Integer.toHexString(errorValue).toUpperCase());
            sb.append("; Decoded_Errors: ").append(String.join(", ", decoded));
            details = "Byte_Invalid (Details: " + sb.toString() + ")";
        }
        return String.format("%d  | Error_Code:      | %s", number, details);
    }

}