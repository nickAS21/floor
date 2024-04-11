package org.nickas21.smart.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtils {

    public static boolean isBlank(String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
    }

    public static void printMsgProgressBar(String message, long timeAll) {
        long timeInterval = 5000L;
        int size = Math.toIntExact(timeAll/timeInterval);
        for (int i=1; i<size; i++) {
            try {
                Thread.sleep(timeInterval);
                System.out.print(message + "[" + "=".repeat(i) + ">" + " ".repeat(size-i) + "]\r");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.print("|" + "=".repeat(size) + "|\r");
        log.info("Update after progressBar... ver: 2.3");
    }
}

