package com.brainserve.appointment.shared.api;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public DomainException(String errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String errorCode() {
        return errorCode;
    }

    public HttpStatus status() {
        return status;
    }
}
