package com.redeban.bonos.infrastructure.out.jdbc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;

import com.redeban.bonos.domain.model.ActivarBonoRequest;
import com.redeban.bonos.domain.model.ActivarBonoResponse;
import com.redeban.bonos.domain.model.CrearBonoRequest;
import com.redeban.bonos.domain.model.CrearBonoResponse;
import com.redeban.bonos.domain.model.RecuperarBonoRequest;
import com.redeban.bonos.domain.model.RecuperarBonoResponse;
import com.redeban.bonos.domain.port.out.GestionBonosOutPort;

@ApplicationScoped
public class As400GestionBonosRepository implements GestionBonosOutPort {

    private static final String ENDPOINT_CREAR = "direct:gestion-bonos-crear";
    private static final String ENDPOINT_ACTIVAR = "direct:gestion-bonos-activar";
    private static final String ENDPOINT_RECUPERAR = "direct:gestion-bonos-recuperar";

    private final ProducerTemplate producerTemplate;

    @Inject
    public As400GestionBonosRepository(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @Override
    public CrearBonoResponse crearBono(CrearBonoRequest request) {
        return producerTemplate.requestBody(ENDPOINT_CREAR, request, CrearBonoResponse.class);
    }

    @Override
    public ActivarBonoResponse activarBono(ActivarBonoRequest request) {
        return producerTemplate.requestBody(ENDPOINT_ACTIVAR, request, ActivarBonoResponse.class);
    }

    @Override
    public RecuperarBonoResponse recuperarBono(RecuperarBonoRequest request) {
        return producerTemplate.requestBody(ENDPOINT_RECUPERAR, request, RecuperarBonoResponse.class);
    }
}
