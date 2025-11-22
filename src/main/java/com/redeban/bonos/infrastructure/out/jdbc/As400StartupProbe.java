package com.redeban.bonos.infrastructure.out.jdbc;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Fuerza la inicialización temprana del conector AS400 para que el test de conectividad
 * se ejecute durante el arranque del pod.
 */
@ApplicationScoped
public class As400StartupProbe {

    private final As400Connector connector;

    @Inject
    public As400StartupProbe(As400Connector connector) {
        this.connector = connector;
    }

    void onStart(@Observes StartupEvent ignored) {
        // La simple inyección garantiza que el @PostConstruct de As400Connector ya se ejecutó.
        // No se requiere lógica adicional aquí.
    }
}
