package com.redeban.bonos.infrastructure.in.rest.dto;

import java.util.List;

public class RecuperarBonoResponseDto extends OperationResponseDto {

    private List<BonoDto> bonos;

    public List<BonoDto> getBonos() {
        return bonos;
    }

    public void setBonos(List<BonoDto> bonos) {
        this.bonos = bonos;
    }
}
