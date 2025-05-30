package org.nickas21.smart;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PowerValueRealTimeData {

    Long collectionTime;        // Update real time data

    double totalSolarPower;        // unit W

    double bmsSocValue;         // unit %
        // battery
    String batteryStatusValue;
    double batteryPowerValue;       // unit W
    double batteryCurrentValue;  // unit A
    double batteryVoltageValue;  // unit V
    double bmsVoltageValue;  // unit V
    double bmsCurrentValue;  // unit A
    double batterySocValue;      // unit %
    double batteryDailyCharge;   // unit kWh
    double batteryDailyDischarge;// unit kWh

    double productionTotalSolarPowerValue;             // unit kWh
    double consumptionTotalPowerValue;                 // unit kWh

    double totalConsumptionPower;  // unit W
    double totalGridPower;         // unit W
    double totalEnergySell;     // unit kWh
    double totalEnergyBuy;      // unit kWh
    double dailyEnergySell;     // unit kWh
    double dailyEnergyBuy;      // unit kWh

    String gridStatusRelay;     // Pull-in, Break
    String gridStatusSolarman;      // Purchasing energy, Grid connected, Static
}

