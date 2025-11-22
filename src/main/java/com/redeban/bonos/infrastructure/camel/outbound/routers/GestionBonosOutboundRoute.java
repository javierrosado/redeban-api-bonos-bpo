package com.redeban.bonos.infrastructure.camel.outbound.routers;

import org.apache.camel.builder.RouteBuilder;

import com.redeban.bonos.infrastructure.out.jdbc.As400StoredProcedureClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GestionBonosOutboundRoute extends RouteBuilder {

    private final As400StoredProcedureClient storedProcedureClient;

    @Inject
    public GestionBonosOutboundRoute(As400StoredProcedureClient storedProcedureClient) {
        this.storedProcedureClient = storedProcedureClient;
    }

    @Override
    public void configure() {
        from("direct:gestion-bonos-crear")
                .routeId("rt-gestion-bonos-crear")
                .bean(storedProcedureClient, "crearBono");

        from("direct:gestion-bonos-activar")
                .routeId("rt-gestion-bonos-activar")
                .bean(storedProcedureClient, "activarBono");

        from("direct:gestion-bonos-recuperar")
                .routeId("rt-gestion-bonos-recuperar")
                .bean(storedProcedureClient, "recuperarBono");
    }
}
