package com.redeban.bonos.domain.model;

import java.util.List;

public class RecuperarBonoResponse extends OperationResponse {

    private List<Bono> bonos;

    public List<Bono> getBonos() {
        return bonos;
    }

    public void setBonos(List<Bono> bonos) {
        this.bonos = bonos;
    }
}
