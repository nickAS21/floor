package org.nickas21.smart.usr.unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Enum for managing and mapping error codes received from the BMS.
 * Maps error bitmasks (Hex Integers) to human-readable descriptions.
 */
public enum UsrTcpWifiError {

    // --- BYTE 1 FAULTS (Critical/General: Bits 31-24) ---
    CRITICAL(0x20, "CRITICAL_FAULT", 1),
    WARNING(0x10, "WARNING: STATE AFTER CRITICAL FAULT", 1),

    // --- BYTE 2 FAULTS (Specific: Bits 23-16) ---
    UNIT_LOW_CHARGE(0x08, "CRITICALLY_LOW_CHARGE (<18%)", 2),
    CELLS_UNBALANCE(0x04, "CELL_UNBALANCE", 2),
    CELLS_UNDER_VOLTAGE(0x02, "UNDER_VOLTAGE_CELL", 2),
    CELLS_OVER_VOLTAGE(0x01, "OVER_VOLTAGE_CELL", 2);


    private final int code;
    private final String description;
    private final int byteType; // 1 or 2


    // Constructor
    UsrTcpWifiError(int code, String description, int byteType) {
        this.code = code;
        this.description = description;
        this.byteType = byteType;
    }

    /**
     * Finds the enum member that matches the exact code value.
     * NOTE: This method is O(N) complexity as it iterates through all members.
     */
    public static UsrTcpWifiError fromCode(int code) {
        for (UsrTcpWifiError to : UsrTcpWifiError.values()) {
            if (to.code == code) {
                return to;
            }
        }
        return null;
    }

    public static List<String> decodeErrorFlags(int errorCodeInt) {
        List<String> messages = new ArrayList<>();

        // Extract BYTE1 (Critical/General) from bits 31-24 (4th byte)
        int byte1 = (errorCodeInt >> 24) & 0xFF;

        // Extract BYTE2 (Specific) from bits 23-16 (3rd byte)
        int byte2 = (errorCodeInt >> 16) & 0xFF;

        // Iterate through ALL enum members to check the bitmasks
        for (UsrTcpWifiError error : UsrTcpWifiError.values()) {

            int targetByte = error.getByteType() == 1 ? byte1 : byte2;

            // Check if the corresponding byte (byte1 or byte2) has the fault bit set
            if ((targetByte & error.getCode()) != 0) {
                // Check that the fault belongs to the currently processed byte
                if (error.getByteType() == 1 && error.getCode() == (byte1 & error.getCode())) {
                    messages.add(error.getDescription() + " (0x" + Integer.toHexString(error.getCode()).toUpperCase() + ")");
                } else if (error.getByteType() == 2 && error.getCode() == (byte2 & error.getCode())) {
                    messages.add(error.getDescription() + " (0x" + Integer.toHexString(error.getCode()).toUpperCase() + ")");
                }
            }
        }

        // Check for UNKNOWN codes in the lowest 2 bytes (bits 15-0)
        if (messages.isEmpty() && (errorCodeInt & 0xFFFF) != 0) {
            messages.add("UNKNOWN_ERROR_CODE (0x" + Integer.toHexString(errorCodeInt).toUpperCase() + ")");
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


    public int getCode() {
        return code;
    }

    public int getByteType() {
        return byteType;
    }

    public String getDescription() {
        return description;
    }
}