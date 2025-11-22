package com.redeban.bonos.infrastructure.in.rest.dto;

public class ErrorResponseDto {

    private String codigoError;
    private String descripcionError;

    public ErrorResponseDto() {
    }

    public ErrorResponseDto(String codigoError, String descripcionError) {
        this.codigoError = codigoError;
        this.descripcionError = descripcionError;
    }

    public String getCodigoError() {
        return codigoError;
    }

    public void setCodigoError(String codigoError) {
        this.codigoError = codigoError;
    }

    public String getDescripcionError() {
        return descripcionError;
    }

    public void setDescripcionError(String descripcionError) {
        this.descripcionError = descripcionError;
    }
}
