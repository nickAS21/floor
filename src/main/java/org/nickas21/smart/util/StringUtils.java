package org.nickas21.smart.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtils {

    private static volatile boolean stopProgressBar = false;

    public static boolean isBlank(String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
    }
    public static boolean isNotBlank(String source) {
        return !isBlank(source);
    }

    public static void printMsgProgressBar(String message, long timeAll, String version) {
        int size = 60;
        int timeInterval = Math.toIntExact(timeAll / size);
        for (int i = 1; i < size; i++) {
            if (stopProgressBar) {
                System.out.print("|" + "=".repeat(size) + "|\r");
                log.info("Progress bar stopped prematurely. Version: {}", version);
                Thread.currentThread().interrupt();
                return;
            }
            try {
                Thread.sleep(timeInterval);
                System.out.print(message + "[" + "=".repeat(i) + ">" + " ".repeat(size-i) + "]\r");
            } catch (InterruptedException e) {
                System.out.print("|" + "=".repeat(size) + "|\r");
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
}

