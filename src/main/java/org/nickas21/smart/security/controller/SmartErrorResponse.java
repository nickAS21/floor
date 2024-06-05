package org.nickas21.smart.security.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public class SmartErrorResponse extends LoginResponse {
    // HTTP Response Status Code
    private final HttpStatus status;

    // Error code
    private final SmartErrorCode errorCode;


    private final long timestamp;

    protected SmartErrorResponse(final String message, final SmartErrorCode errorCode) {
        super(null, message);
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
        this.timestamp = System.currentTimeMillis();
    }

    public static SmartErrorResponse of(final String message, final SmartErrorCode errorCode) {
        return new SmartErrorResponse(message, errorCode);
    }

    @Schema(description = "HTTP Response Status Code", example = "401", accessMode = Schema.AccessMode.READ_ONLY)
    public Integer getStatus() {
        return status.value();
    }

    @Schema(description = "Error message", example = "Authentication failed", accessMode = Schema.AccessMode.READ_ONLY)
    public String getErrorMessage() {
        return this.getMessage();
    }

    public SmartErrorCode getErrorCode() {
        return errorCode;
    }

    @Schema(description = "Timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    public long getTimestamp() {
        return timestamp;
    }
}

