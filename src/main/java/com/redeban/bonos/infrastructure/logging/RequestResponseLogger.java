package com.redeban.bonos.infrastructure.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redeban.bonos.domain.model.HeaderContext;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RequestResponseLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLogger.class);

    private final ObjectMapper objectMapper;

    @Inject
    public RequestResponseLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void logSuccess(String operation, HeaderContext headerContext, Object request, Object response, OffsetDateTime start, OffsetDateTime end) {
        log(operation, headerContext, request, response, end, start, null);
    }

    public void logError(String operation, HeaderContext headerContext, Object request, Object response, OffsetDateTime start, OffsetDateTime end, Throwable error) {
        log(operation, headerContext, request, response, end, start, error);
    }

    private void log(String operation, HeaderContext headerContext, Object request, Object response, OffsetDateTime end, OffsetDateTime start, Throwable error) {
        Duration elapsed = Duration.between(start, end);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", operation);
        payload.put("elapsedMillis", elapsed.toMillis());

        Map<String, Object> headers = new LinkedHashMap<>();
        headerContext.idTransaccion().ifPresent(value -> headers.put("idTransaccion", value));
        headerContext.nombreAplicacion().ifPresent(value -> headers.put("nombreAplicacion", value));
        headerContext.ipAplicacion().ifPresent(value -> headers.put("ipAplicacion", value));
        headerContext.timestamp().ifPresent(value -> headers.put("timestamp", value));
        payload.put("headers", headers);
        payload.put("request", request);
        payload.put("response", response);

        if (error != null) {
            payload.put("error", Map.of("type", error.getClass().getSimpleName(), "message", error.getMessage()));
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            if (error == null) {
                LOGGER.info(json);
            } else {
                LOGGER.error(json);
            }
        } catch (JsonProcessingException e) {
            String fallback = String.format("operation=%s elapsedMillis=%d error='%s'", operation, elapsed.toMillis(), e.getMessage());
            if (error == null) {
                LOGGER.info(fallback);
            } else {
                LOGGER.error(fallback, error);
            }
        }
    }
}
