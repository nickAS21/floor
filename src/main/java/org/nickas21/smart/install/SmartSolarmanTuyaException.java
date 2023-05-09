package org.nickas21.smart.install;

import org.springframework.boot.ExitCodeGenerator;

public class SmartSolarmanTuyaException extends RuntimeException implements ExitCodeGenerator {

    public SmartSolarmanTuyaException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getExitCode() {
        return 1;
    }

}
