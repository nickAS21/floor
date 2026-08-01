package org.nickas21.smart;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PowerValueRealTimeData {

    Long collectionTime;        // Update real time data

    double totalSolarPower;        // unit W
    String inverterProtocolVersionValue;
    String inverterM1ParallelInformationValue;
    double inverterM1DcVoltagePV1Value; // value = 479.70 -> unit = V
    double inverterM1DcVoltagePV2Value;
    double inverterM1DcCurrentPV1Value;  // value = 0.20 -> unit = A
    double inverterM1DcCurrentPV2Value;
    double inverterM1DcPowerPV1Value;    //  value = 108 -> unit = W
    double inverterM1DcPowerPV2Value;
    String inverterS2ParallelInformationValue;
    double inverterS2DcVoltagePV1Value;
    double inverterS2DcVoltagePV2Value;
    double inverterS2DcCurrentPV1Value;
    double inverterS2DcCurrentPV2Value;
    double inverterS2DcPowerPV1Value;
    double inverterS2DcPowerPV2Value;
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
    double gridVoltageL1;          // unit V
    double gridVoltageL2;          // unit V
    double gridVoltageL3;          // unit V

    double totalEnergySell;         // unit kWh
    double totalEnergyBuy;          // unit kWh
    double dailyEnergySell;         // unit kWh
    double dailyEnergyBuy;          // unit kWh
    double dailyHomeConsumptionPower;                 // unit kWh
    double dailyProductionSolarPower;                 // unit kWh

    String gridStatusRelay;         // Pull-in, Break
    String gridStatusSolarman;      // Purchasing energy, Grid connected, Static
}

