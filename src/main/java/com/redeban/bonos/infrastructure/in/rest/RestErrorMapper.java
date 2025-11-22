package com.redeban.bonos.infrastructure.in.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

import com.redeban.bonos.domain.exception.BonoBusinessException;
import com.redeban.bonos.domain.exception.BonoTechnicalException;
import com.redeban.bonos.infrastructure.camel.common.configs.GestionBonosProperties;
import com.redeban.bonos.infrastructure.in.rest.dto.ErrorResponseDto;

@ApplicationScoped
public class RestErrorMapper {

    private final GestionBonosProperties properties;

    @Inject
    public RestErrorMapper(GestionBonosProperties properties) {
        this.properties = properties;
    }

    public ErrorWrapper toError(Throwable throwable) {
        if (throwable instanceof BonoBusinessException business) {
            return new ErrorWrapper(business.getHttpStatus(),
                    new ErrorResponseDto(business.getCodigo(), business.getDescripcion()));
        }

        if (throwable instanceof BonoTechnicalException technical) {
            int status = technical.getHttpStatus() > 0 ? technical.getHttpStatus()
                    : safeToInt(properties.codigoRespuesta().error().general(), 500);
            return new ErrorWrapper(status,
                    new ErrorResponseDto(properties.codigo().e09(), properties.descripcion().e09()));
        }

        if (throwable instanceof ConstraintViolationException violationException) {
            String message = violationException.getConstraintViolations()
                    .stream()
                    .map(this::formatViolation)
                    .collect(Collectors.joining(", "));
            ErrorResponseDto responseDto = new ErrorResponseDto(properties.codigo().e01(),
                    properties.descripcion().e01() + ": " + message);
            int status = safeToInt(properties.codigoRespuesta().error().estructura(), 400);
            return new ErrorWrapper(status, responseDto);
        }

        int status = safeToInt(properties.codigoRespuesta().error().general(), 500);
        return new ErrorWrapper(status,
                new ErrorResponseDto(properties.codigo().e09(), properties.descripcion().e09()));
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + " => " + violation.getMessage();
    }

    private int safeToInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public record ErrorWrapper(int status, ErrorResponseDto body) {
    }
}
