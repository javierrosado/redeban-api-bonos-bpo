package com.redeban.bonos.application.usecase;

import com.redeban.bonos.domain.model.ActivarBonoRequest;
import com.redeban.bonos.domain.model.ActivarBonoResponse;
import com.redeban.bonos.domain.model.CrearBonoRequest;
import com.redeban.bonos.domain.model.CrearBonoResponse;
import com.redeban.bonos.domain.model.HeaderContext;
import com.redeban.bonos.domain.model.RecuperarBonoRequest;
import com.redeban.bonos.domain.model.RecuperarBonoResponse;
import com.redeban.bonos.domain.port.in.GestionBonosInPort;
import com.redeban.bonos.domain.port.out.GestionBonosOutPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GestionBonosUseCase implements GestionBonosInPort {

    private final GestionBonosOutPort gestionBonosOutPort;

    @Inject
    public GestionBonosUseCase(GestionBonosOutPort gestionBonosOutPort) {
        this.gestionBonosOutPort = gestionBonosOutPort;
    }

    @Override
    public CrearBonoResponse crearBono(CrearBonoRequest request, HeaderContext headerContext) {
        return gestionBonosOutPort.crearBono(request);
    }

    @Override
    public ActivarBonoResponse activarBono(ActivarBonoRequest request, HeaderContext headerContext) {
        return gestionBonosOutPort.activarBono(request);
    }

    @Override
    public RecuperarBonoResponse recuperarBono(RecuperarBonoRequest request, HeaderContext headerContext) {
        return gestionBonosOutPort.recuperarBono(request);
    }
}
