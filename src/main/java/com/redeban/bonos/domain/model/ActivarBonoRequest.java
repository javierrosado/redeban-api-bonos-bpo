package com.redeban.bonos.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ActivarBonoRequest extends CommonBonoData {

    @NotBlank(message = "El campo numeroBono no puede estar vacío")
    @Size(min = 13, max = 19, message = "La longitud del campo numeroBono debe estar entre 13 y 19")
    private String numeroBono;

    @NotBlank(message = "El campo valorCarga no puede estar vacío")
    @Size(min = 5, max = 10, message = "La longitud del campo valorCarga debe estar entre 5 y 10")
    @Pattern(regexp = "\\d+", message = "El campo valorCarga es numérico")
    private String valorCarga;

    @NotBlank(message = "El campo numeroAuditoria no puede estar vacío")
    @Size(min = 6, max = 6, message = "La longitud del campo numeroAuditoria debe ser 6")
    @Pattern(regexp = "\\d+", message = "El campo numeroAuditoria es numérico")
    private String numeroAuditoria;

    @NotBlank(message = "El campo consecutivo no puede estar vacío")
    @Size(min = 2, max = 12, message = "La longitud del campo consecutivo debe estar entre 2 y 12")
    @Pattern(regexp = "\\d+", message = "El campo consecutivo es numérico")
    private String consecutivo;

    public String getNumeroBono() {
        return numeroBono;
    }

    public void setNumeroBono(String numeroBono) {
        this.numeroBono = numeroBono;
    }

    public String getValorCarga() {
        return valorCarga;
    }

    public void setValorCarga(String valorCarga) {
        this.valorCarga = valorCarga;
    }

    public String getNumeroAuditoria() {
        return numeroAuditoria;
    }

    public void setNumeroAuditoria(String numeroAuditoria) {
        this.numeroAuditoria = numeroAuditoria;
    }

    public String getConsecutivo() {
        return consecutivo;
    }

    public void setConsecutivo(String consecutivo) {
        this.consecutivo = consecutivo;
    }
}
