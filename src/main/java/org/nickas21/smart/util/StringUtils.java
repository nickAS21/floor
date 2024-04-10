package org.nickas21.smart.util;

import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
public class StringUtils {

    public static boolean isBlank(String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
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
            String progressBar = "\r" + message + builder + "\r" ;
            System.out.print(progressBar);
            try {
                Thread.sleep(timeInterval);
            } catch (InterruptedException ignored) {
                log.error("", ignored);
            }
        }
    }

}
