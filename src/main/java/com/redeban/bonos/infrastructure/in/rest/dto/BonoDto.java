package com.redeban.bonos.infrastructure.in.rest.dto;

public class BonoDto {

    private String numero;
    private Long valor;

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Long getValor() {
        return valor;
    }

    public void setValor(Long valor) {
        this.valor = valor;
    }
}
