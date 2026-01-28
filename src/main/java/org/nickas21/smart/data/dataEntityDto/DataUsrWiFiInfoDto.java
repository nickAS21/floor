package org.nickas21.smart.data.dataEntityDto;

import lombok.Data;
import org.nickas21.smart.util.LocationType;

@Data
public class DataUsrWiFiInfoDto {
    int id;                         // Number -> Обчислюється: netIpA - 18890 = id
    LocationType locationType;      // DACH/GOLEGO
    String bssidMac;                // 9c:a5:25:fe:4b:20", Беремо з ??
    String ssidWifiBms;             // "USR-WIFI232-B2_...", //  з констант або fast.html
    String netIpA;                  // Network A Setting -> IP сервера, куди BMS підключається у STA-режимі як клієнт (ноут / AWS)
    int netAPort;                   // Network A Setting Port STA mode Client  -> Обчислюється: 18890 + id
    String netIpB;                  // Socket B: =>  IP BMS у STA mode
    int netBPort;                   // Socket B: =>  Port BMS у STA mode -> Обчислюється: 8890 + id
    String oui;                     // Chip manufacturer - > Обчислюється за bssid
}
