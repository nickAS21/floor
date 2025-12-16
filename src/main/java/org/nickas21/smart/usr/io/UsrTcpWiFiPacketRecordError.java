package org.nickas21.smart.usr.io;

import java.nio.charset.StandardCharsets;

import static org.nickas21.smart.util.StringUtils.bytesToHex;
import static org.nickas21.smart.util.StringUtils.formatTimestamp;
import static org.nickas21.smart.util.StringUtils.hexToBytes;

public record UsrTcpWiFiPacketRecordError(
        long timestamp,
        int port,
        String type,
        String codeError,
        int payloadLength,
        byte[] payload
) {

    public String toLine() {
        return timestamp + ";" +
                port + ";" +
                type + ";" +
                codeError + ";" +
                (payloadLength & 0xFFFF) + ";" +
                bytesToHex(payload) + "\n";
    }

    public static UsrTcpWiFiPacketRecordError fromLine(String line) {
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
        String code = arr[3];
        int len = Integer.parseInt(arr[4]);
        byte[] data = hexToBytes(arr[5]);

        return new UsrTcpWiFiPacketRecordError(ts, port, type, code, len, data);
    }

    public String toMsgForBot() {
        return "--" + formatTimestamp(timestamp) + ";" +
                port + ";" +
                type + ";" +
                codeError + ";" + "\n" +
                new String(payload, StandardCharsets.UTF_8) + "\n";
    }
}