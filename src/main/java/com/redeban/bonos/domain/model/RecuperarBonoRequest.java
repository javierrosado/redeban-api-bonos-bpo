package com.redeban.bonos.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RecuperarBonoRequest extends CommonBonoData {

    @NotBlank(message = "El campo tipoDocumento no puede estar vacío")
    @Size(min = 2, max = 2, message = "La longitud del campo tipoDocumento debe ser 2")
    private String tipoDocumento;

    @NotBlank(message = "El campo numeroDocumento no puede estar vacío")
    @Size(min = 6, max = 15, message = "La longitud del campo numeroDocumento debe estar entre 6 y 15")
    @Pattern(regexp = "\\d+", message = "El campo numeroDocumento es numérico")
    private String numeroDocumento;

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }
}
