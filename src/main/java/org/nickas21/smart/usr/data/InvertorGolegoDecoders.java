package org.nickas21.smart.usr.data;

import org.nickas21.smart.usr.entity.InvertorGolegoData32;
import org.nickas21.smart.usr.entity.InvertorGolegoData90;

import java.util.Arrays;

public class InvertorGolegoDecoders {

    public static InvertorGolegoData90 decodeInverterGolegoPayload90(byte[] payload, int port) {
        // Відрізаємо 3 байти заголовку, беремо 90 байт даних
        if (payload.length < 93) return null;
        byte[] data = Arrays.copyOfRange(payload, 3, 93);

        InvertorGolegoData90 entity = new InvertorGolegoData90();
        entity.setPort(port);

        // 1. ЗАПОВНЮЄМО hexMap ПРАВИЛЬНО
        String[] hMap = new String[45];
        for (int i = 0; i < 45; i++) {
            // Беремо два байти по порядку: data[0] data[1], потім data[2] data[3]...
            hMap[i] = String.format("%02X %02X", data[i * 2] & 0xFF, data[i * 2 + 1] & 0xFF);
        }
        entity.setHexMap(hMap);

        // 2. ЧИТАЄМО ЯК LITTLE-ENDIAN (04 00 -> 4)
        entity.setStatus(readInt16LE(data, 0));
        entity.setAcInputVoltage(readInt16LE(data, 2) / 10.0);
        entity.setAcInputFrequency(readInt16LE(data, 4) / 10.0);
        entity.setRezerv06(readInt16LE(data, 6));
        entity.setRezerv08(readInt16LE(data, 8));
        entity.setBatteryVoltage(readInt16LE(data, 10) / 10.0);
        entity.setSoc(readInt16LE(data, 12));
        entity.setBatteryChargingCurrent(readInt16LE(data, 14));
        entity.setBatteryDischargingCurrent(readInt16LE(data, 16));
        entity.setLoadAcOutputVoltage(readInt16LE(data, 18) / 10.0);
        entity.setAcOutputFrequency(readInt16LE(data, 20) / 10.0);
        entity.setLoadOutputApparentPower(readInt16LE(data, 22));
        entity.setLoadOutputActivePower(readInt16LE(data, 24));
        entity.setAcOutputLoadPercent(readInt16LE(data, 26));
        entity.setRezerv28(readInt16LE(data, 28));
        entity.setRezerv30(readInt16LE(data, 30));
        entity.setCutOffVoltage(readInt16LE(data, 32) / 10.0);
        entity.setMainCpuVersion(readInt16LE(data, 34));
        entity.setRezerv36(readInt16LE(data, 36));
        entity.setRezerv38(readInt16LE(data, 38));
        entity.setNominalOutputApparentPower(readInt16LE(data, 40));
        entity.setNominalOutputActivePower(readInt16LE(data, 42));
        entity.setNominalOutputVoltage(readInt16LE(data, 44));
        entity.setNominalAcCurrent(readInt16LE(data, 46));
        entity.setRatedBatteryVoltage(readInt16LE(data, 48) / 10.0);
        entity.setNominalOutputVoltageNom(readInt16LE(data, 50));
        entity.setNominalOutputFrequency(readInt16LE(data, 52) / 10.0);
        entity.setNominalOutputCurrent(readInt16LE(data, 54));
        entity.setRezerv56(readInt16LE(data, 56));
        entity.setRezerv58(readInt16LE(data, 58));
        entity.setRezerv60(readInt16LE(data, 60) / 100.0);
        entity.setRezerv62(readInt16LE(data, 62) / 100.0);
        entity.setRezerv64(readInt16LE(data, 64) / 100.0);
        entity.setRezerv66(readInt16LE(data, 66) / 1.0);
        entity.setRezerv68(readInt16LE(data, 68) / 10.0);
        entity.setRezerv70(readInt16LE(data, 70));
        entity.setRezerv72(readInt16LE(data, 72));
        entity.setRezerv74(readInt16LE(data, 74));
        entity.setRezerv76(readInt16LE(data, 76));
        entity.setRezerv78(readInt16LE(data, 78));
        entity.setMaxTotalChargeCurrent(readInt16LE(data, 80));
        entity.setRezerv82(readInt16LE(data, 82));
        entity.setMaxUtilityChargeCurrent(readInt16LE(data, 84));
        entity.setComebackUtilityModeVoltage(readInt16LE(data, 86) / 10.0);
        entity.setComebackBatteryModeVoltage(readInt16LE(data, 88) / 10.0);

        return entity;
    }

    public static InvertorGolegoData32 decodeInverterGolegoPayload32(byte[] payload, int port) {
        // Відрізаємо 3 байти заголовку
        if (payload.length < 3) return null;
        byte[] data = Arrays.copyOfRange(payload, 3, payload.length);

        InvertorGolegoData32 entity = new InvertorGolegoData32();
        entity.setPort(port);

        // 1. Заповнюємо hexMap (16 елементів для 32 байт)
        String[] hMap = new String[16];
        for (int i = 0; i < 16; i++) {
            if (i * 2 + 1 < data.length) {
                hMap[i] = String.format("%02X %02X", data[i * 2] & 0xFF, data[i * 2 + 1] & 0xFF);
            }
        }
        entity.setHexMap(hMap);

        // 2. Декодуємо Little-Endian (LE)
        entity.setBatteryVoltage1(readInt16LE(data, 0) / 10.0);    // 21 02 -> 54.5
        entity.setBatteryVoltage2(readInt16LE(data, 2) / 10.0);    // 21 02 -> 54.5
        entity.setRatedBatteryVoltage(readInt16LE(data, 4) / 10.0);// E0 01 -> 48.0
        entity.setBatteryVoltage3(readInt16LE(data, 6) / 10.0);    // 21 02 -> 54.5

        // Тут множник 1.0 згідно з вашим прикладом (5 -> 5V, 900 -> 900min)
        entity.setCurrentCollectionFrequency(readInt16LE(data, 8) * 1.0);     // 05 00 -> 5.0
        entity.setBatteryEqualizeTimeout(readInt16LE(data, 10));   // 84 03 -> 900
        entity.setBatteryEqualizeInterval(readInt16LE(data, 12));  // 5A 00 -> 90

        entity.setRezerv14(readInt16LE(data, 14));                 // 62 05 -> 1378
        entity.setRezerv16(readInt16LE(data, 16));
        entity.setRezerv18(readInt16LE(data, 18));
        entity.setRezerv20(readInt16LE(data, 20));                 // 14 00 -> 20 (у вас було 16, але 14 hex це 20)
        entity.setRezerv22(readInt16LE(data, 22));
        entity.setRezerv24(readInt16LE(data, 24));
        entity.setRezerv26(readInt16LE(data, 26));
        entity.setStandardCollectionFrequency(readInt16LE(data, 28));
        entity.setRezerv30(readInt16LE(data, 30));

        return entity;
    }

    private static int readInt16LE(byte[] data, int index) {
        // Little-Endian: молодший байт (index), старший байт (index + 1)
        return ((data[index + 1] & 0xFF) << 8) | (data[index] & 0xFF);
    }
}