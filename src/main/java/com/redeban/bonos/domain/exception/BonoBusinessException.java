package com.redeban.bonos.domain.exception;

public class BonoBusinessException extends RuntimeException {

    private final String codigo;
    private final String descripcion;
    private final int httpStatus;

    public BonoBusinessException(String codigo, String descripcion, int httpStatus) {
        super(descripcion);
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.httpStatus = httpStatus;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
