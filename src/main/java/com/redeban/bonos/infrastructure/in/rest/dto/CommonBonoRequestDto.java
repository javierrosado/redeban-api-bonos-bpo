package com.redeban.bonos.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CommonBonoRequestDto {

    @NotBlank(message = "El campo bin no puede estar vacío")
    @Size(min = 6, max = 6, message = "La longitud del campo bin debe ser 6")
    @Pattern(regexp = "\\d+", message = "El campo bin es numérico")
    private String bin;

    @NotBlank(message = "El campo nit no puede estar vacío")
    @Size(min = 9, max = 15, message = "La longitud del campo nit debe estar entre 9 y 15")
    @Pattern(regexp = "\\d+", message = "El campo nit es numérico")
    private String nit;

    @NotBlank(message = "El campo subtipo no puede estar vacío")
    @Size(min = 3, max = 3, message = "La longitud del campo subtipo debe ser 3")
    private String subtipo;

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getSubtipo() {
        return subtipo;
    }

    public void setSubtipo(String subtipo) {
        this.subtipo = subtipo;
    }
}
