package org.nickas21.smart.usr.service;

public class DeyeDecoderTests {
//
//    @Test
//    void testAllValidPacketsCrc() {
//        // Передаємо null замість залежностей.
//
//        List<String> packets = HelpetDeyeDataTest.ALL;
//
//        for (String hexPacket : packets) {
//            if (hexPacket.equals(HelpetDeyeDataTest.BROKEN_LENGTH)) continue;
//
//            byte[] buffer = hexToBytes(hexPacket);
//            assertDoesNotThrow(() -> {
//                isFrameDyeyCrcValid(buffer, 0, buffer.length);
//            }, "Packet failed CRC check: " + hexPacket);
//
//        }
//    }
//
//    @Test
//    void testAllValidPacketsCrcBigf() {
//        List<String> packets = HelpetDeyeDataTest.ALL_Big;
//
//        for (String hexPacket : packets) {
//            if (hexPacket.equals(HelpetDeyeDataTest.BROKEN_LENGTH)) continue;
//
//            byte[] buffer = hexToBytes(hexPacket);
//            int actualLen = buffer.length;
//
//            // 1. ПЕРЕВІРКА СТРУКТУРИ ТА ДОВЖИНИ
//            // Для Deye Modbus: [ID][Func][Len]...[CRC_L][CRC_H]
//            // Мінімальний пакет - 5 байтів (3 заголовок + 0 даних + 2 CRC)
//            assertTrue(actualLen >= 5, "Пакет занадто короткий: " + hexPacket);
//
//            int payloadLen = buffer[2] & 0xFF; // Байт довжини даних
//            int expectedFullLen = payloadLen + 5; // Уся довжина кадру
//
//            // Якщо довжина в байті не збігається з реальним масивом - пакет битий
//            if (actualLen != expectedFullLen) {
//                System.err.printf("[SKIP] Пакет битий (Length mismatch). Очікували %d, маємо %d. Hex: %s%n",
//                        expectedFullLen, actualLen, hexPacket);
//                continue; // Або fail(), якщо ти впевнений, що всі пакети в ALL_Big мають бути ідеальними
//            }
//
//            // 2. ПЕРЕВІРКА CRC
//            // Тепер ми впевнені, що довжина правильна, і можна перевіряти контрольну суму
//            assertDoesNotThrow(() -> {
//                isFrameDyeyCrcValid(buffer, 0, actualLen);
//            }, "CRC mismatch у валідному (по довжині) пакеті: " + hexPacket);
//        }
//    }
//
//    @Test
//    void testHardcodedCrc() {
//        byte[] data = hexToBytes("01035008A2089E08990000000000000016007000E10167016713880000000000000000000000000800030004000000000000000000000000000000C600B50000000013FB000412700004000000000000000000");
//        int crc = 0xFFFF;
//        for (byte b : data) {
//            crc ^= (b & 0xFF);
//            for (int i = 0; i < 8; i++) {
//                if ((crc & 1) != 0) crc = (crc >>> 1) ^ 0xA001;
//                else crc >>>= 1;
//            }
//        }
//        crc &= 0xFFFF;
//        System.out.println("HARDCODE RESULT: " + Integer.toHexString(crc));
//        // Має вивести 39a8
//    }
//
//    @Test
//    void testFindTrueCrc() {
//        String hexData = "01035008A2089E08990000000000000016007000E10167016713880000000000000000000000000800030004000000000000000000000000000000C600B50000000013FB000412700004000000000000000000";
//        byte[] data = hexToBytes(hexData);
//
//        // ВАРІАНТ А: Стандарт (що дає d6e1)
//        System.out.println("A: Standard (0xFFFF, 0xA001): " + Integer.toHexString(runCrc(data, 0xFFFF, 0xA001)));
//
//        // ВАРІАНТ Б: Починаємо з 0x0000 (буває в деяких логерах)
//        System.out.println("B: Init 0x0000: " + Integer.toHexString(runCrc(data, 0x0000, 0xA001)));
//
//        // ВАРІАНТ В: Рахуємо БЕЗ перших двох байтів (01 03)
//        // Деякі "розумні" логери рахують CRC тільки від Payload
//        byte[] payloadOnly = Arrays.copyOfRange(data, 3, data.length);
//        System.out.println("C: Payload Only (Skip 01 03 50): " + Integer.toHexString(runCrc(payloadOnly, 0xFFFF, 0xA001)));
//
//        // ВАРІАНТ Г: Перевірка на Big-Endian XOR (рідко, але раптом)
//        System.out.println("D: Big-Endian style: " + Integer.toHexString(runCrcReverse(data)));
//    }
//
//    // Допоміжний метод для швидкого тесту
//    private int runCrc(byte[] data, int init, int poly) {
//        int crc = init;
//        for (byte b : data) {
//            crc ^= (b & 0xFF);
//            for (int i = 0; i < 8; i++) {
//                if ((crc & 1) != 0) crc = (crc >>> 1) ^ poly;
//                else crc >>>= 1;
//            }
//        }
//        return crc & 0xFFFF;
//    }
//
//    private int runCrcReverse(byte[] data) {
//        int crc = 0xFFFF;
//        for (byte b : data) {
//            crc ^= ((b & 0xFF) << 8);
//            for (int i = 0; i < 8; i++) {
//                if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021; // Поліном CCITT
//                else crc <<= 1;
//            }
//        }
//        return crc & 0xFFFF;
//    }
//
//    @Test
//    void testDeyeSpecialVariants() {
//        byte[] data = hexToBytes("01035008A2089E08990000000000000016007000E10167016713880000000000000000000000000800030004000000000000000000000000000000C600B50000000013FB000412700004000000000000000000");
//
//        // Варіант Е: Стандарт + XOR 0xFFFF в кінці
//        int resE = 0xd6e1 ^ 0xFFFF;
//        System.out.println("E: Standard + Final XOR: " + Integer.toHexString(resE & 0xFFFF));
//
//        // Варіант F: CRC-16/ARC (Init 0x0000, Poly 0x8005 - реверс A001)
//        // Але ми вже бачили, що Init 0x0000 дає c4cc.
//
//        // Варіант G: А що якщо байти даних треба перевернути перед розрахунком?
//        // (Буває і таке в китайських протоколах)
//    }
//
//    @Test
//    void testMCRF4XX() {
//        byte[] data = hexToBytes("01035008A2089E08990000000000000016007000E10167016713880000000000000000000000000800030004000000000000000000000000000000C600B50000000013FB000412700004000000000000000000");
//        int crc = 0xFFFF;
//        for (byte b : data) {
//            crc ^= (b & 0xFF);
//            for (int i = 0; i < 8; i++) {
//                if ((crc & 1) != 0) crc = (crc >>> 1) ^ 0x8408; // Реверс 0x1021
//                else crc >>>= 1;
//            }
//        }
//        System.out.println("MCRF4XX Result: " + Integer.toHexString(crc & 0xFFFF));
//    }
//
//    @Test
//    void testWithHiddenPrefix() {
//        byte[] data = hexToBytes("01035008A2089E08990000000000000016007000E10167016713880000000000000000000000000800030004000000000000000000000000000000C600B50000000013FB000412700004000000000000000000");
//        int crc = 0xFFFF;
//
//        // ПРИПУЩЕННЯ: логер додає щось перед розрахунком, наприклад 0xA5
//        crc ^= 0xA5;
//        for (int i = 0; i < 8; i++) {
//            if ((crc & 1) != 0) crc = (crc >>> 1) ^ 0xA001;
//            else crc >>>= 1;
//        }
//
//        for (byte b : data) {
//            crc ^= (b & 0xFF);
//            for (int i = 0; i < 8; i++) {
//                if ((crc & 1) != 0) crc = (crc >>> 1) ^ 0xA001;
//                else crc >>>= 1;
//            }
//        }
//        System.out.println("Hidden Prefix Result: " + Integer.toHexString(crc & 0xFFFF));
//    }


}