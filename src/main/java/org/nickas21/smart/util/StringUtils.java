package org.nickas21.smart.util;

import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

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

    }
    public static void printMsgWithProgressBar(String message, long timeAll) {
        long timeInterval = 5000L;
        int length = Math.toIntExact(timeAll/timeInterval);
//        char incomplete = '░'; // U+2591 Unicode Character
//        char complete = '█'; // U+2588 Unicode Character
         char incomplete = '.'; // U+2591 Unicode Character
        char complete = '='; // U+2588 Unicode Character
        char completeFinish = '>'; // U+2588 Unicode Character
        StringBuilder builder = new StringBuilder();
        Stream.generate(() -> incomplete).limit(length).forEach(builder::append);
//        System.out.println(message);
        for (int i = 0; i < length; i++) {
            builder.replace(i, i + 1, String.valueOf(complete));
            builder.replace(i+1, i +2, String.valueOf(completeFinish));
//            String progressBar = "\r" + message + builder + "\r" ;
            String progressBar =  "\r" + message + "[" + builder + "]" + "\r";
            System.out.print(progressBar);
//            log.info(progressBar);
            try {
                Thread.sleep(timeInterval);
            } catch (InterruptedException ignored) {
                log.error("", ignored);
            }
        }
        log.info("Start update after progressBar");
    }

}
