package org.nickas21.smart.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class StringUtils {

    private static volatile boolean stopProgressBar = false;
    private static final int size = 60;

    public static boolean isBlank(String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
    }
    public static boolean isNotBlank(String source) {
        return !isBlank(source);
    }

    public static void printMsgProgressBar(Thread threadCur, String message, long timeAll, String version) {
        int timeInterval = Math.toIntExact(timeAll / size);
        for (int i = 1; i < size; i++) {
            if (stopProgressBar) {
                stopThread(threadCur);
                log.info("Progress bar stopped prematurely. Version: {}", version);
                return;
            }
            try {
                threadCur.sleep(timeInterval);
                System.out.print(message + "[" + "=".repeat(i) + ">" + " ".repeat(size-i) + "]\r");
            } catch (InterruptedException e) {
                System.out.print("|" + "=".repeat(size) + "|\r");
                log.info("Update after Interrupted progressBar... ver: {}", version);
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!stopProgressBar) {
            System.out.print("|" + "=".repeat(size) + "|\r");
            log.info("Update after progressBar... ver: {}", version);
        }
    }

    public static void stopProgressBar() {
        stopProgressBar = true;
    }
    public static void stopThread(Thread thread) {
        System.out.print("|" + "=".repeat(size) + "|\r");
        thread.interrupt();
    }

    public static boolean isDecimal(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("[+-]?\\d+(\\.\\d+)?");
    }
    public static Boolean isBoolean(String str) {
        return "true".equals(str) ? Boolean.TRUE : "false".equals(str) ? Boolean.FALSE : null;
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] arr = new byte[len / 2];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return arr;
    }
    public static byte[] intToBytesBigEndian(Integer value) {
        return new byte[]{(byte) ((value >> 8) & 0xFF), (byte) (value & 0xFF)};
    }

    public static byte[] stringToBytesBas64 (String errorMsg) {
        return Base64.getEncoder()
                .encode(errorMsg.getBytes(StandardCharsets.UTF_8));
    }

    public static String bytesBase64ToString(byte[] base64Bytes) {
        // 1. Отримуємо декодер Base64
        byte[] decodedBytes = Base64.getDecoder()
                // 2. Декодуємо Base64 масив байтів
                .decode(base64Bytes);

        // 3. Створюємо новий рядок із декодованих байтів, використовуючи UTF-8
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}

