package org.nickas21.smart;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PowerValueRealTimeData {
        // Update real time data
    Long collectionTime;
        // ubit %
    double bmsSocValue;
        // unit W
    int totalSolarPower;
        // unit W
    int totalConsumptionPower;
        // unit kWh
    double totalEnergySell;
}

