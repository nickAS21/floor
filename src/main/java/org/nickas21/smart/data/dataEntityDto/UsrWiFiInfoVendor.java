package org.nickas21.smart.data.dataEntityDto;

import java.util.Map;

public class UsrWiFiInfoVendor {

    private static final Map<String, String> OUI_DATABASE = Map.of(
            "F4700C", "Jinan USR IOT Technology Limited",
            "9CA525", "Shandong USR IOT Technology Limited",
            "D4AD20", "Jinan USR IOT Technology Limited"
    );

    public static String getOuiVendorName(String bssid) {
        if (bssid == null || bssid.length() < 6) {
            return "UNKNOWN";
        }

        // Видаляємо всі двокрапки, тире та пробіли, беремо перші 6 символів
        String cleanMac = bssid.replaceAll("[^a-fA-F0-9]", "").toUpperCase();

        if (cleanMac.length() < 6) {
            return "UNKNOWN";
        }

        String ouiPrefix = cleanMac.substring(0, 6);
        return OUI_DATABASE.getOrDefault(ouiPrefix, "UNKNOWN");
    }
}
