package org.nickas21.smart.usr.io;

import static org.nickas21.smart.util.StringUtils.bytesToHex;
import static org.nickas21.smart.util.StringUtils.hexToBytes;

public record UsrTcpWiFiPacketRecord(
        long timestamp,
        int port,
        String type,
        int payloadLength,
        byte[] payload
) {

    public String toLine() {
        return timestamp + ";" +
                port + ";" +
                type + ";" +
                (payloadLength & 0xFFFF) + ";" +
                bytesToHex(payload);
    }

    public static UsrTcpWiFiPacketRecord fromLine(String line) {
        if (line == null || line.isBlank()) return null; // Додано перевірку

        // Розділяємо рядок на 5 частин
        String[] arr = line.split(";", 5);
        if (arr.length < 5) {
            // Можна додати логування помилки формату тут, якщо потрібно
            return null;
        }

        long ts = Long.parseLong(arr[0]);
        int port =Integer.parseInt(arr[1]);
        String type = arr[2];
        int len = Integer.parseInt(arr[3]);
        byte[] data = hexToBytes(arr[4]);

        return new UsrTcpWiFiPacketRecord(ts, port, type, len, data);
    }

    /* ---------- HEX UTILS ---------- */


}