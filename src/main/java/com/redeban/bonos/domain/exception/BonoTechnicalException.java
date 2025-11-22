package com.redeban.bonos.domain.exception;

public class BonoTechnicalException extends RuntimeException {

    private final int httpStatus;

    public BonoTechnicalException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public BonoTechnicalException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
