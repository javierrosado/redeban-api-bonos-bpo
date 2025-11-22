package com.redeban.bonos.domain.model;

import java.time.OffsetDateTime;
import java.util.Optional;

public class HeaderContext {

    private final String idTransaccion;
    private final String nombreAplicacion;
    private final String ipAplicacion;
    private final String timestamp;
    private final OffsetDateTime receivedAt;

    public HeaderContext(String idTransaccion,
                         String nombreAplicacion,
                         String ipAplicacion,
                         String timestamp,
                         OffsetDateTime receivedAt) {
        this.idTransaccion = idTransaccion;
        this.nombreAplicacion = nombreAplicacion;
        this.ipAplicacion = ipAplicacion;
        this.timestamp = timestamp;
        this.receivedAt = receivedAt;
    }

    public Optional<String> idTransaccion() {
        return Optional.ofNullable(blankToNull(idTransaccion));
    }

    public Optional<String> nombreAplicacion() {
        return Optional.ofNullable(blankToNull(nombreAplicacion));
    }

    public Optional<String> ipAplicacion() {
        return Optional.ofNullable(blankToNull(ipAplicacion));
    }

    public Optional<String> timestamp() {
        return Optional.ofNullable(blankToNull(timestamp));
    }

    public OffsetDateTime receivedAt() {
        return receivedAt;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
