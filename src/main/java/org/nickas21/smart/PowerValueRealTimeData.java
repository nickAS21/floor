package org.nickas21.smart;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PowerValueRealTimeData {

    Long collectionTime;        // Update real time data

    double totalSolarPower;        // unit W
    String inverterProtocolVersionValue;
    String inverterMAINValue;
    String inverterHMIValue;
    double inverterTempValue;   // ℃

        // battery
    double bmsSocValue;             // unit %
    double bmsTempValue;            // ℃
    String batteryStatusValue;
    double batteryPowerValue;       // unit W
    double batteryCurrentValue;     // unit A
    double batteryVoltageValue;     // unit V
    double bmsVoltageValue;         // unit V
    double bmsCurrentValue;         // unit A
    double batterySocValue;         // unit %
    double dailyBatteryCharge;      // unit kWh
    double dailyBatteryDischarge;   // unit kWh

    double totalProductionSolarPower;             // unit W
    double totalHomePower;   // unit W
    double totalGridPower;          // unit W

    double totalEnergySell;         // unit kWh
    double totalEnergyBuy;          // unit kWh
    double dailyEnergySell;         // unit kWh
    double dailyEnergyBuy;          // unit kWh
    double dailyHomeConsumptionPower;                 // unit kWh
    double dailyProductionSolarPower;                 // unit kWh

    String gridStatusRelay;         // Pull-in, Break
    String gridStatusSolarman;      // Purchasing energy, Grid connected, Static
}

