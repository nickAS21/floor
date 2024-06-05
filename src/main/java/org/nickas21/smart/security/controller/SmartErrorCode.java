package org.nickas21.smart.security.controller;

import org.springframework.http.HttpStatus;

public enum SmartErrorCode {

    GENERAL(2, HttpStatus.INTERNAL_SERVER_ERROR),
    AUTHENTICATION(10, HttpStatus.UNAUTHORIZED),
    JWT_TOKEN_EXPIRED(11, HttpStatus.UNAUTHORIZED),
    CREDENTIALS_EXPIRED(15, HttpStatus.UNAUTHORIZED),
    PERMISSION_DENIED(20, HttpStatus.FORBIDDEN),
    INVALID_ARGUMENTS(30, HttpStatus.BAD_REQUEST),
    BAD_REQUEST_PARAMS(31, HttpStatus.BAD_REQUEST),
    ITEM_NOT_FOUND(32, HttpStatus.NOT_FOUND),
    TOO_MANY_REQUESTS(33, HttpStatus.TOO_MANY_REQUESTS),
    TOO_MANY_UPDATES(34, HttpStatus.PAYLOAD_TOO_LARGE),
    SUBSCRIPTION_VIOLATION(40, HttpStatus.FORBIDDEN),
    PASSWORD_VIOLATION(45, HttpStatus.UNAUTHORIZED);

    private final int errorCode;
    private final HttpStatus httpStatus;

    SmartErrorCode(int errorCode, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }
}
