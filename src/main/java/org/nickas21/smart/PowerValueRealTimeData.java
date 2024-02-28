package org.nickas21.smart;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PowerValueRealTimeData {

    Long collectionTime;        // Update real time data

    int totalSolarPower;        // unit W

    double bmsSocValue;         // unit %
        // battery
    String batteryStatusValue;
    int batteryPowerValue;       // unit W
    double batteryCurrentValue;  // unit A
    double batteryVoltageValue;  // unit V
    double batterySocValue;      // unit %
    double batteryDailyCharge;   // unit kWh
    double batteryDailyDischarge;// unit kWh

    double productionTotalSolarPowerValue;             // unit kWh
    double consumptionTotalPowerValue;                 // unit kWh

    int totalConsumptionPower;  // unit W
    int totalGridPower;         // unit W
    double totalEnergySell;     // unit kWh
    double totalEnergyBuy;      // unit kWh
    double dailyEnergySell;     // unit kWh
    double dailyEnergyBuy;      // unit kWh

    String gridRelayStatus;     // Pull-in, Break
    String gridStatus;          // Purchasing energy, Grid connected, Static
}

