package com.redeban.bonos.infrastructure.in.rest.dto;

public class CrearBonoResponseDto extends OperationResponseDto {

    private String numeroBono;

    public String getNumeroBono() {
        return numeroBono;
    }

    public void setNumeroBono(String numeroBono) {
        this.numeroBono = numeroBono;
    }
}
