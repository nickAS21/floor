package org.nickas21.smart.util;

public class StringUtils {

    public static boolean isBlank(String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
    }
}
