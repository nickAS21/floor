package org.nickas21.smart.usr.data.fault;

import lombok.Getter;

import java.util.Arrays;

/**
 * 8. Troubleshooting
 * 1. Every fault is presented by a fault code. If the battery fault light is on, please
 * check the Fault code in Homepage.
 * 2. If the battery fault light on, pls check the Troubleshooting number in Homepage
 * in your AOBOET APP, if the code is 0x1***, this problem would be recovered
 * by itself. But if the code is 0x2*** or 0x3***, please contact the AOBOET after
 * service hot line or your distributor for help.
 * 3. If the information of battery cannot be seen in the monitoring system, check the
 * battery status first. If the battery status is OFF, please turn the battery on, and
 * then check the WLAN is accessible for battery.
 * 4. If Register the battery failure, please check the network of mobile phone nearby
 * the battery installation site available and stable.
 * Fault Code
 * Table 8-1 Fault Code of Battery
 * Detail fault message
 * 0x1001Battery under voltage warning
 * 0x1002Battery over voltage warning
 * 0x1003Battery under temperature warning
 * 0x1004Battery over temperature warning
 * 0x1005Battery charge over current warning
 * 0x1006Battery discharge over current warning
 * 0x1007Cell over discharge warning
 * 0x1008Cell over charge warning
 * 0x1009Battery charge with over temperature warning
 * 0x1010Battery discharge with over temperature warning
 * 0x1011Battery charge with under temperature warning
 * 0x1012Battery discharge with under temperature warning
 * 0x2001Battery under voltage protect
 * 0x2002Battery and cell over discharge protect
 * 0x2003Battery over charge protect
 * 0x2004Battery over voltage and cell over charge protect
 * 0x2005Battery under temperature protect
 * 0x2006Battery over temperature protect
 * 0x2007Battery charge over current protect
 * 0x2008Battery discharge over current protect
 * 0x2009Cell over discharge protect
 * 0x2010Cell over charge protect
 * 0x3000Communication broken between master and slave Battery
 * 0x3001Address select fault
 */
@Getter
public enum UsrTcpWifiFault {

    // -------- WARNINGS (0x1xxx) --------
    BATTERY_UNDER_VOLTAGE_WARNING(0x1001, UsrTcpWifiFaultCategory.WARNING,
            "Battery under voltage warning"),
    BATTERY_OVER_VOLTAGE_WARNING(0x1002, UsrTcpWifiFaultCategory.WARNING,
            "Battery over voltage warning"),
    BATTERY_UNDER_TEMPERATURE_WARNING(0x1003, UsrTcpWifiFaultCategory.WARNING,
            "Battery under temperature warning"),
    BATTERY_OVER_TEMPERATURE_WARNING(0x1004, UsrTcpWifiFaultCategory.WARNING,
            "Battery over temperature warning"),
    BATTERY_CHARGE_OVER_CURRENT_WARNING(0x1005, UsrTcpWifiFaultCategory.WARNING,
            "Battery charge over current warning"),
    BATTERY_DISCHARGE_OVER_CURRENT_WARNING(0x1006, UsrTcpWifiFaultCategory.WARNING,
            "Battery discharge over current warning"),
    CELL_OVER_DISCHARGE_WARNING(0x1007, UsrTcpWifiFaultCategory.WARNING,
            "Cell over discharge warning"),
    CELL_OVER_CHARGE_WARNING(0x1008, UsrTcpWifiFaultCategory.WARNING,
            "Cell over charge warning"),

    // -------- PROTECTION (0x2xxx) --------
    BATTERY_UNDER_VOLTAGE_PROTECT(0x2001, UsrTcpWifiFaultCategory.PROTECTION,
            "Battery under voltage protect"),
    BATTERY_OVER_DISCHARGE_PROTECT(0x2002, UsrTcpWifiFaultCategory.PROTECTION,
            "Battery and cell over discharge protect"),
    BATTERY_OVER_CHARGE_PROTECT(0x2003, UsrTcpWifiFaultCategory.PROTECTION,
            "Battery over charge protect"),

    // -------- COMMUNICATION (0x3xxx) --------
    COMMUNICATION_BROKEN(0x3000, UsrTcpWifiFaultCategory.COMMUNICATION,
            "Communication broken between master and slave Battery"),
    ADDRESS_SELECT_FAULT(0x3001, UsrTcpWifiFaultCategory.COMMUNICATION,
            "Address select fault");

    private final int code;
    private final UsrTcpWifiFaultCategory category;
    private final String description;

    UsrTcpWifiFault(int code, UsrTcpWifiFaultCategory category, String description) {
        this.code = code;
        this.category = category;
        this.description = description;
    }

    public static UsrTcpWifiFault fromCode(int code) {
        return Arrays.stream(values())
                .filter(f -> f.code == code)
                .findFirst()
                .orElse(null);
    }


//    // --- BYTE 1 (бiти 15–8) ---
//    CRITICAL(0x20, 1, "CRITICAL_FAULT"),
//    WARNING(0x10, 1, "WARNING: STATE AFTER CRITICAL FAULT"),
//
//    // --- BYTE 0 (бiти 7–0) ---
//    UNIT_LOW_CHARGE(0x08, 0, "CRITICALLY_LOW_CHARGE (<18%)"),
//    CELLS_UNBALANCE(0x04, 0, "UNBALANCE_CELLS"),
//    CELLS_UNDER_VOLTAGE(0x02, 0, "UNDER_VOLTAGE_CELLS"),
//    CELLS_OVER_VOLTAGE(0x01, 0, "OVER_VOLTAGE_CELLS");
//
//    private final int mask;
//    private final int byteIndex;  // 0 = low byte, 1 = high byte
//    private final String description;
//
//    UsrTcpWifiFault(int mask, int byteIndex, String description) {
//        this.mask = mask;
//        this.byteIndex = byteIndex;
//        this.description = description;
//    }
//
//    public int getMask() {
//        return mask;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//
//
//
//
//    public static List<String> parseDescriptions(int value) {
//        return Arrays.stream(values())
//                .filter(f -> f.isSet(value))
//                .map(f -> f.description + " (0x" + Integer.toHexString(f.mask).toUpperCase() + ")")
//                .toList();
//    }
//
//    public static List<String> decodeErrorFlags(int errorValue) {
//        List<String> messages = parseDescriptions(errorValue);
//
//        // Якщо не знайдено жодного, але errorValue != 0 → UNKNOWN
//        if (messages.isEmpty() && errorValue != 0) {
//            messages = new ArrayList<>();
//            messages.add(
//                    "UNKNOWN_ERROR_CODE (0x" + Integer.toHexString(errorValue).toUpperCase() + ")"
//            );
//        }
//
//        return messages;
//    }

    public static String formatErrorCodeOutput(Integer errorInfoData) {

        if (errorInfoData == null || errorInfoData == 0) {
            return "OK (No faults)";
        }

        UsrTcpWifiFault fault = UsrTcpWifiFault.fromCode(errorInfoData);
        UsrTcpWifiFaultCategory category = UsrTcpWifiFaultCategory.fromCode(errorInfoData);

        if (fault == null) {
            return String.format(
                    "UNKNOWN_FAULT: 0x%04X; Category: %s",
                    errorInfoData,
                    category.getDescription()
            );
        }

        return String.format(
                "Fault_Code_HEX: 0x%04X; Category: %s; Description: %s",
                fault.getCode(),
                fault.getCategory().getDescription(),
                fault.getDescription()
        );

//        String details;
//
//        if (errorValue == 0) {
//            details = "Byte_Valid";
//        } else {
//            List<String> decoded = decodeErrorFlags(errorValue);
//
//            StringBuilder sb = new StringBuilder();
//            sb.append("Error_Code_HEX: ").append(Integer.toHexString(errorValue).toUpperCase());
//            sb.append("; Decoded_Errors: ").append(String.join(", ", decoded));
//
//            details = "Byte_Invalid (Details: " + sb + ")";
//        }
//
//        return String.format("Error_Code: %s", details);
    }
}
