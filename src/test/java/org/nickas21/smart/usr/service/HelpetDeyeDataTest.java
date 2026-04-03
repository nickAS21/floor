package org.nickas21.smart.usr.service;

import java.util.List;

public class HelpetDeyeDataTest {

    private HelpetDeyeDataTest() {
    }

    // --- SMALL RESPONSES ---
    /**
     * 01 addr
     * 03 func
     * 02 bytes
     * 0005 → значення = 5
     */
    public static final String REG_0005 = "01030200057847";
    /**
     * 0
     */
    public static final String REG_0000 = "0103020000B844";
    /**
     * 1
     */
    public static final String REG_0001 = "01030200017984";
    /**
     * -1
     */
    public static final String REG_FFFF = "010302FFFFB9F4";

    // Пакет №5: REG_0258 (Battery Capacity)
    /**
     * Заголовок: 010302 (ID: 1, Func: 3 (Get), Len: 2)
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | Offset | Поле Modbus          | Поле CSV (12:07:37)  | Raw Hex | Значення| Реальне значення      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | 00-01  | Battery Capacity     | Battery Capacity(Ah) | 0258    | 600     | 600 Ah |
     * | 02-03  | CRC16 Modbus         | N/A                  | B8DE    | ---     | Контрольна сума|
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     */
    public static final String REG_0258 = "0103020258B8DE";

    // Пакет №4: RTC_TIME
    /**
     * Заголовок: 010306 (ID: 1, Func: 3 (Get), Len: 6)
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | Offset | Поле Modbus          | Поле CSV (12:07:37)  | Raw Hex | Значення| Реальне значення      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | 00     | Year (Рік)           | System Time (Year)   | 1A      | 26      | 2026 рік  |
     * | 01     | Month (Місяць)       | System Time (Month)  | 02      | 02      | Лютий     |
     * | 02     | Day (День)           | System Time (Day)    | 1C      | 28      | 28 число  |
     * | 03     | Hour (Година)        | System Time (Hour)   | 0E      | 14      | 14:00     | це 14 - по Києву в файлі - 12
     * | 04     | Minute (Хвилина)     | System Time (Min)    | 02      | 02      | 02 хвилини|
     * | 05     | Second (Секунда)     | System Time (Sec)    | 17      | 23      | 23 секунди|
     * | 06-07  | CRC16 Modbus         | N/A                  | 7DB2    | ---     | Контрольна сума|
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     */
    public static final String RTC_TIME = "0103061A021C0E02177DB2";

    /**
     * big(8) -1
     */
    public static final String BLOCK_08_EMPTY_FFFF = "01030800000000FFFFFFFF9443";
    /**
     * big(8) 0
     */
    public static final String BLOCK_08_EMPTY_ZERO = "010308000000000000000095D7";

    /**
     * double(12) 0
     */
    public static final String BLOCK_0C_EMPTY = "01030C0000000000000000000000009370";
    /**
     * biggest(44) 0
     */
    public static final String BLOCK_2C_EMPTY = "01032C00000000000000000000000000000000000000000000000000000000000000000000000000000000000000007859";

    public static final String BLOCK_04_SIMPLE = "010304003D01AEEBD3";

    public static final String BLOCK_14_ALL_FF = "010314FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFB6E8";
    public static final String BLOCK_18_STATUS = "01031810000004FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF16A";

    // Пакет №2: BLOCK_08_VALUES (Solar / PV Data)
    /**
     * Заголовок: 010308 (ID: 1, Func: 3 (Get), Len: 8)
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | Offset | Поле Modbus          | Поле CSV (12:07:37)  | Raw Hex | Значення| Реальне значення      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | 00-01  | PV1 Voltage          | DC Voltage PV1(V)    | 0016    | 22      | 2.2 V  |
     * | 02-03  | PV1 Current          | DC Current PV1(A)    | 006F    | 111     | 11.1 A |
     * | 04-05  | PV1 Power            | DC Power PV1(W)      | 00E5    | 229     | 229 W  |
     * | 06-07  | Daily PV Energy      | PV daily power. DC   | 016A    | 362     | 36.2 kWh|
     * | 08-09  | CRC16 Modbus         | N/A                  | A657    | ---     | Контрольна сума|
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     */
    public static final String BLOCK_08_VALUES = "0103080016006F00E5016AA657";

    // BLOCK_SN (Inverter Identification)
    /**
     * Заголовок: 01 03 30 (ID: 1, Func: 3 (Get), Len: 48).
     * +--------+----------------------+----------------------+----------+---------+----------------------------+
     * | Offset | Назва поля (Modbus)  | Поле з вашого списку | Raw Hex  | Значення| Реальне значення           |
     * +--------+----------------------+----------------------+----------+---------+----------------------------+
     * | 00-03  | Hardware Info        | N/A                  | 00050001 | HEX     | 0005 0001                  |
     * | 04-05  | Protocol Version     | Protocol Version     | 0104     | HEX     | 0104                       |
     * | 06-15  | Inverter Serial No   | Device SN            | 3232...  | ASCII   | "2211135328"               |
     * | 16-25  | Reserved / Zeroes    | N/A                  | 00...00  | HEX     | 00000000000000000000       |
     * | 26-27  | MAIN (Part 3)        | MAIN (1807)          | 1807     | HEX     | 1807                       |
     * | 28-29  | Lithium Battery Ver  | Lithium Battery Ver  | 0000     | HEX     | 0000                       |
     * | 30-31  | Arc Board Firmware   | Arc Board Firmware   | 0000     | HEX     | 0000                       |
     * | 32-33  | MAIN (Part 1)        | MAIN (2005)          | 2005     | HEX     | 2005                       |
     * | 34-35  | MAIN (Part 2)        | MAIN (1172)          | 1172     | HEX     | 1172                       |
     * | 36-37  | Reserved             | N/A                  | 0000     | HEX     | 0000                       |
     * | 38-39  | HMI (Part 1)         | HMI (1001)           | 1001     | HEX     | 1001                       |
     * | 40-41  | HMI (Part 2)         | HMI (C050)           | C050     | HEX     | C050                       |
     * | 42-43  | Technical Code 1     | N/A                  | 0000     | HEX     | 0000                       |
     * | 44-45  | Technical Code 2     | N/A                  | D4C0     | HEX     | D4C0 (Заводський параметр) |
     * | 46-47  | Status Flag          | N/A                  | 0001     | HEX     | 0001                       |
     * | 48-49  | Language Start ???   | English Version      | FFFF     | HEX     | FFFF (Empty)               |
     * | 50-51  | Padding              | N/A                  | 00FF     | HEX     | 00FF                       |
     * | 52-53  | CRC16 Modbus         | N/A                  | C726     | ---     | Контрольна сума            |
     * +--------+----------------------+----------------------+----------+---------+----------------------------+
     */
    public static final String BLOCK_SN =
            "010330000500010104323231313133353332380000000000001807000000002005117200001001C0500000D4C00001FFFF00FFC726";


    // BLOCK_BIG_118
    /**
     * Заголовок: 01 03 30 (ID: 1, Func: 3 (Get), Len: 118).
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | Offset | Поле Modbus          | Поле CSV (14:07:37)  | Raw Hex | Значення| Реальне значення      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | 00-01  | Running Status       | Work Mode            | 0002    | 2       | 2 (Normal)            |
     * | 02-31  | Reserved / Zeroes    | N/A                  | 00...00 | 0       | 0                     |
     * | 32-33  | Total Load Power     | Total Load Power(W)  | 005A    | 90      | 90 W                  |
     * | 34-35  | PV1 Voltage          | DC Voltage PV1(V)    | 0016    | 22      | 2.2 V                 |
     * | 36-43  | Internal Params      | N/A                  | 0F0D... | HEX     | Technical Data        |
     * | 44-45  | Total Load Energy    | Total Load(kWh)      | 01AE    | 430     | 43.0 kWh              |
     * | 46-47  | Reserved             | N/A                  | 0000    | 0       | 0                     |
     * | 48-49  | Daily Load Energy    | Daily Load(kWh)      | 002A    | 42      | 4.2 kWh               |
     * | 50-63  | Energy Accumulators  | N/A                  | 00...03 | HEX     | Technical Data        |
     * | 64-65  | Daily PV Energy      | PV daily power...    | 0039    | 57      | 5.7 kWh               |
     * | 66-71  | Reserved             | N/A                  | 00...00 | 0       | 0                     |
     * | 72-73  | Grid Power L1        | Grid Power L1(W)     | 0000    | 0       | 0 W                   |
     * | 74-75  | Grid Power L2        | Grid Power L2(W)     | 0000    | 0       | 0 W                   |
     * | 76-77  | Grid Power L3        | Grid Power L3(W)     | 0000    | 0       | 0 W                   |
     * | 78-79  | Total Grid Power     | Total Grid Power(W)  | 0000    | 0       | 0 W                   |
     * | 80-81  | Total Grid Energy    | Grid total energy... | D065    | 53349   | 5334.9 kWh            |
     * | 82-83  | Daily Grid Energy    | Grid daily energy... | 0002    | 2       | 0.2 kWh               |
     * | 84-117 | System End Padding   | N/A                  | 00...00 | 0       | 0                     |
     * | 118-119| CRC16 Modbus         | N/A                  | C1D6    | ---     | Контрольна сума       |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     */
    public static final String BLOCK_BIG_118 =
            "01037600020000000000000000000000000000000300000000000000000000005A00160F0D0001E02C000001AE00002A00000124BD000001943960000300390000000000000000D0650002000000000000000004E2059D000000000000000000000000000000000A0000010001000000000000000000000000C1D6";

    // Заголовок: 01 03 6A (ID: 1, Func: 3, Len: 106).
    /**
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | Offset | Поле Modbus          | Поле CSV (14:07:37)  | Raw Hex | Значення| Реальне значення      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | 00-05  | Internal Sensors     | N/A                  | 048B... | HEX     | Технічні дані (НЕ V)  |
     * | 66-67  | Inverter L1 Voltage  | AC Voltage R/U/A(V)  | 0898    | 2200    | 220.0 V (MATCH!)      |
     * | 68-69  | Inverter L2 Voltage  | AC Voltage S/V/B(V)  | 0898    | 2200    | 220.0 V (MATCH!)      |
     * | 70-71  | Inverter L3 Voltage  | AC Voltage T/W/C(V)  | 0895    | 2197    | 219.7 V (MATCH!)      |
     * | 74-75  | Inverter L2 Current  | AC Current S/V/B(A)  | 0028    | 40      | 4.0 A   (MATCH!)      |
     * | 76-77  | Inverter L3 Current  | AC Current T/W/C(A)  | 005A    | 90      | 9.0 A   (MATCH!)      |
     * | 82-83  | PV1 Power            | DC Power PV1(W)      | 00E1    | 225     | 225 W   (MATCH!)      |
     * | 88-89  | AC Frequency         | AC Output Freq(Hz)   | 1388    | 5000    | 50.00 Hz(MATCH!)      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     */
    public static final String BLOCK_BIG_106 =
            "01036A048B16440064000000040010026A00000000000000000000000000000000000000000000000000000000000000000000000000000100020001000000000000000000FFFF0000000000000000000008980898089500000028005A0016007000E1016701671388B7EB";

    // Пакет №1: BLOCK_NET_80
    /**
     * Заголовок: 010350 (ID: 1, Func: 3 (Get), Len: 80)
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | Offset | Поле Modbus          | Поле CSV (12:07:37)  | Raw Hex | Значення| Реальне значення      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | 00-01  | Grid Voltage L1      | Grid Voltage L1(V)   | 08A2    | 2210    | 221.0 V|
     * | 02-03  | Grid Voltage L2      | Grid Voltage L2(V)   | 089E    | 2206    | 220.6 V|
     * | 04-05  | Grid Voltage L3      | Grid Voltage L3(V)   | 0899    | 2201    | 220.1 V|
     * | 06-07  | Grid Current L1      | Grid Current L1(A)   | 0000    | 0       | 0.0 A  |
     * | 08-09  | Grid Current L2      | Grid Current L2(A)   | 0000    | 0       | 0.0 A  |
     * | 10-11  | Grid Current L3      | Grid Current L3(A)   | 0000    | 0       | 0.0 A  |
     * | 12-13  | PV1 Current (Low) ???| DC Current PV1(A) ???| 0016    | 22      | 2.2 A  |
     * | 14-15  | PV1 Current (High)???| DC Current PV1(A) ???| 0070    | 112     | 11.2 A |
     * | 16-17  | PV1 Voltage          | DC Voltage PV1(V)    | 00E1    | 225     | 22.5 V |
     * | 18-19  | Daily Load Energy    | Daily Load(kWh)      | 0167    | 359     | 35.9 kWh    |
     * | 20-21  | Total Load Energy    | Total Load(kWh)      | 0167    | 359     | 35.9 kWh    |
     * | 22-25  | Reserved ???         | N/A                  | 0000... | 0       | 0           |
     * | 26-27  | Grid Frequency       | Grid Frequency(Hz)   | 1388    | 5000    | 50.00 Hz|
     * | 28-39  | Reserved ???         | N/A                  | 00...00 | 0       | 0           |
     * | 40-41  | Running Status ???   | Work Mode            | 0800    | 2048    | 2048 (Mode) |
     * | 42-43  | Fault Code L ???     | Fault Code           | 0003    | 3       | 3           |
     * | 44-45  | Fault Code H ???     | Fault Code           | 0004    | 4       | 4           |
     * | 46-59  | Empty / Padding      | N/A                  | 00...00 | 0       | 0           |
     * | 60-61  | Inverter Temp        | Radiator Temp(C)     | 00C6    | 198     | 19.8 °C |
     * | 62-63  | Internal Temp        | Internal Temp(C)     | 00B5    | 181     | 18.1 °C |
     * | 64-67  | Reserved ???         | N/A                  | 00...00 | 0       | 0           |
     * | 68-69  | Battery Voltage      | Battery Voltage(V)   | 13FB    | 5115    | 51.15 V |
     * | 70-71  | Battery Current      | Battery Current(A)   | 0004    | 4       | 0.4 A  |
     * | 72-73  | Battery Temp         | Battery Temp(C)      | 1270    | 4720    | 47.2 °C |
     * | 74-75  | Battery SOC ???      | Battery SOC(%) ???   | 0004    | 4       | 4 % ???     |
     * | 76-79  | End Padding ???      | N/A                  | 00...00 | 0       | 0           |
     * | 80-81  | CRC16 Modbus         | N/A                  | A839    | ---     | Контрольна сума|
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     */

    public static final String BLOCK_NET_80 =
            "01035008A2089E08990000000000000016007000E10167016713880000000000000000000000000800030004000000000000000000000000000000C600B50000000013FB000412700004000000000000000000A839";

    // Пакет №6: BLOCK_HUGE_210
    /**
     * Заголовок: 0103D2 (ID: 1, Func: 3 (Get), Len: 210)
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | Offset | Поле Modbus          | Поле CSV (12:07:37)  | Raw Hex | Значення| Реальне значення      |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     * | 00-19  | System Status ???    | N/A                  | 00...00 | 0       | 0 (Резерв)            |
     * | 20-21  | Comm Status 1 ???    | N/A                  | 0001    | 1       | Connected ???         |
     * | 22-23  | Comm Status 2 ???    | N/A                  | 0001    | 1       | Active ???            |
     * | 24-25  | Comm Status 3 ???    | N/A                  | 0001    | 1       | Online ???            |
     * | 26-37  | Padding / Reserved   | N/A                  | 00...00 | 0       | 0                     |
     * | 38-39  | Battery Mode ???     | N/A                  | FFFF    | -1      | FFFF (Not Set) ???    |
     * | 40-51  | Padding / Reserved   | N/A                  | 00...00 | 0       | 0                     |
     * | 52-53  | Inverter Voltage L1  | AC Voltage R/U/A(V)  | 0898    | 2200    | 220.0 V               |
     * | 54-55  | Inverter Voltage L2  | AC Voltage S/V/B(V)  | 0897    | 2199    | 219.9 V               |
     * | 56-57  | Inverter Voltage L3  | AC Voltage T/W/C(V)  | 0895    | 2197    | 219.7 V               |
     * | 58-59  | Inverter Current L1  | AC Current R/U/A(A)  | 0000    | 0       | 0.0 A                 |
     * | 60-61  | Inverter Current L2  | AC Current S/V/B(A)  | 0028    | 40      | 4.0 A                 |
     * | 62-63  | Inverter Current L3  | AC Current T/W/C(A)  | 005A    | 90      | 9.0 A                 |
     * | 64-65  | PV1 Voltage          | DC Voltage PV1(V)    | 0016    | 22      | 2.2 V                 |
     * | 66-67  | PV1 Current          | DC Current PV1(A)    | 006E    | 110     | 11.0 A                |
     * | 68-69  | PV1 Power            | DC Power PV1(W)      | 00E1    | 225     | 225 W                 |
     * | 70-71  | PV Total Energy ???  | PV daily power...    | 0165    | 357     | 35.7 kWh ???          |
     * | 72-73  | PV Cumulative ???    | Cumulative Prod...   | 0165    | 357     | 35.7 kWh ???          |
     * | 74-75  | Output Frequency     | AC Output Freq(Hz)   | 1388    | 5000    | 50.00 Hz              |
     * | 76-83  | PV2-PV3 Data ???     | DC Voltage PV2-3     | 00...   | ---     | 0.0 V / 0.0 A ???     |
     * | 84-85  | Grid Voltage L1      | Grid Voltage L1(V)   | 08A4    | 2212    | 221.2 V               |
     * | 86-87  | Grid Voltage L2      | Grid Voltage L2(V)   | 089D    | 2205    | 220.5 V               |
     * | 88-89  | Grid Voltage L3      | Grid Voltage L3(V)   | 0896    | 2198    | 219.8 V               |
     * | 90-101 | Grid Current / Power | Grid Current/Power   | 00...   | 0       | 0.0 A / 0 W           |
     * | 102-103| Inverter Temp        | Radiator Temp(C)     | 00C6    | 198     | 19.8 °C               |
     * | 104-105| Internal Temp        | Internal Temp(C)     | 00B5    | 181     | 18.1 °C               |
     * | 106-115| Reserved / Padding    | N/A                  | 00...00 | 0       | 0                     |
     * | 116-117| Battery Voltage      | Battery Voltage(V)   | 14CB    | 5323    | 53.23 V               |
     * | 118-119| Battery Current      | Battery Current(A)   | 0000    | 0       | 0.0 A                 |
     * | 120-121| Battery SOC          | Battery SOC(%)       | 0064    | 100     | 100 %                 |
     * | 122-171| Large Padding ???    | N/A                  | 00...00 | 0       | 0                     |
     * | 172-173| Battery Capacity     | Battery Capacity(Ah) | 0258    | 600     | 600 Ah                |
     * | 174-207| End Padding ???      | N/A                  | 00...00 | 0       | 0                     |
     * | 208-209| CRC16 Modbus         | N/A                  | (Varies)| ---     | Контрольна сума       |
     * +--------+----------------------+----------------------+---------+---------+-----------------------+
     */
    public static final String BLOCK_HUGE =
            "0103D20000000000000000000000000000000000000000000000000001000100010000000000000000000000FFFF0000000000000000000008980897089500000028005A0016006E00E101650165138800000016006E00E1016508A4089D08960000000000000016006E00E101650165138800000000000000000000000800030004000000000000000000000000000000C400B50000000013FB00041272000400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000860F";

    // --- BROKEN / EDGE CASES ---
    public static final String BROKEN_LENGTH =
            "010310164400000029019A0064162F000";


    // --- FINAL LIST (для тестів) ---
    public static final List<String> ALL = List.of(
            REG_0005,
            REG_0000,
            REG_0001,
            REG_FFFF,
            REG_0258,

            RTC_TIME,

            BLOCK_08_VALUES,
            BLOCK_08_EMPTY_FFFF,
            BLOCK_08_EMPTY_ZERO,

            BLOCK_0C_EMPTY,
            BLOCK_2C_EMPTY,
            BLOCK_04_SIMPLE,
            BLOCK_14_ALL_FF,
            BLOCK_18_STATUS,

            BLOCK_SN,
            BLOCK_BIG_118
//            BLOCK_BIG_106,
//            BLOCK_NET_80,
//            BLOCK_HUGE

//            BROKEN_LENGTH
    );
    public static final List<String> ALL_Big = List.of(
            BLOCK_BIG_106,
            BLOCK_NET_80,
            BLOCK_HUGE

//            BROKEN_LENGTH
    );
}
