package com.redeban.bonos.infrastructure.out.jdbc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

import com.redeban.bonos.domain.exception.BonoBusinessException;
import com.redeban.bonos.domain.model.ActivarBonoRequest;
import com.redeban.bonos.domain.model.ActivarBonoResponse;
import com.redeban.bonos.domain.model.Bono;
import com.redeban.bonos.domain.model.CrearBonoRequest;
import com.redeban.bonos.domain.model.CrearBonoResponse;
import com.redeban.bonos.domain.model.RecuperarBonoRequest;
import com.redeban.bonos.domain.model.RecuperarBonoResponse;
import com.redeban.bonos.infrastructure.camel.common.configs.GestionBonosProperties;

@ApplicationScoped
public class As400StoredProcedureClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss", Locale.ROOT);

    private final GestionBonosProperties properties;
    private final As400Connector connector;

    @Inject
    public As400StoredProcedureClient(GestionBonosProperties properties, As400Connector connector) {
        this.properties = properties;
        this.connector = connector;
    }

    public CrearBonoResponse crearBono(CrearBonoRequest request) {
        String payload = buildCrearPayload(request);
        String resultado = connector.execute(properties.storedProcedure(), payload);
        return mapCrearResultado(resultado);
    }

    public ActivarBonoResponse activarBono(ActivarBonoRequest request) {
        String payload = buildActivarPayload(request);
        String resultado = connector.execute(properties.storedProcedure(), payload);
        return mapActivarResultado(resultado);
    }

    public RecuperarBonoResponse recuperarBono(RecuperarBonoRequest request) {
        String payload = buildRecuperarPayload(request);
        String resultado = connector.execute(properties.storedProcedure(), payload);
        return mapRecuperarResultado(resultado);
    }

    private String buildCrearPayload(CrearBonoRequest request) {
        String documentoCodigo = resolvedocumentoCodigo(request.getTipoDocumento());
        StringBuilder trama = buildCommonTrama(properties.codigo().novedad().crear());

        appendRightPad(trama, request.getBin(), 6);
        appendRightPad(trama, request.getSubtipo(), 3);
        appendLeftPad(trama, request.getNit(), 15);
        appendRightPad(trama, "", 2);
        appendRightPad(trama, "", 75);
        appendRightPad(trama, "", 19);
        appendRightPad(trama, documentoCodigo, 2);
        appendLeftPad(trama, request.getNumeroDocumento(), 15);
        appendRightPad(trama, request.getNombre(), 22);
        appendRightPad(trama, "", 19);
        appendRightPad(trama, "", 1824);

        return trama.toString();
    }

    private String buildActivarPayload(ActivarBonoRequest request) {
        StringBuilder trama = buildCommonTrama(properties.codigo().novedad().activar());
        appendRightPad(trama, request.getBin(), 6);
        appendRightPad(trama, request.getSubtipo(), 3);
        appendLeftPad(trama, request.getNit(), 15);
        appendRightPad(trama, "", 2);
        appendRightPad(trama, "", 75);
        appendRightPad(trama, "", 19);
        appendRightPad(trama, request.getNumeroBono(), 19);
        appendLeftPad(trama, request.getValorCarga() + "00", 12);
        trama.append(properties.codigo().transaccion().activacion());
        trama.append(properties.dispositivo().activacion());
        appendRightPad(trama, request.getNumeroAuditoria(), 6);
        appendRightPad(trama, request.getConsecutivo(), 12);
        trama.append(properties.tipo().transaccion().activacion());
        appendRightPad(trama, "", 6);
        appendRightPad(trama, "", 2);
        appendRightPad(trama, "", 16);
        appendRightPad(trama, "", 25);
        appendRightPad(trama, "", 19);
        appendRightPad(trama, "", 1);
        appendRightPad(trama, "", 1752);

        return trama.toString();
    }

    private String buildRecuperarPayload(RecuperarBonoRequest request) {
        String documentoCodigo = resolvedocumentoCodigo(request.getTipoDocumento());
        StringBuilder trama = buildCommonTrama(properties.codigo().novedad().consultar());
        appendRightPad(trama, request.getBin(), 6);
        appendRightPad(trama, request.getSubtipo(), 3);
        appendLeftPad(trama, request.getNit(), 15);
        appendRightPad(trama, "", 2);
        appendRightPad(trama, "", 75);
        appendRightPad(trama, "", 19);
        appendRightPad(trama, documentoCodigo, 2);
        appendLeftPad(trama, request.getNumeroDocumento(), 15);
        appendRightPad(trama, "", 1300);
        appendRightPad(trama, "", 565);
        return trama.toString();
    }

    private CrearBonoResponse mapCrearResultado(String resultado) {
        CrearBonoResponse response = new CrearBonoResponse();
        response.setCodigoRespuesta(substring(resultado, 46, 48));
        response.setDescripcionRespuesta(trimToNull(substring(resultado, 48, 123)));
        String numeroBono = trimToNull(substring(resultado, 181, 200));
        if (numeroBono != null) {
            response.setNumeroBono(numeroBono);
        }
        return response;
    }

    private ActivarBonoResponse mapActivarResultado(String resultado) {
        ActivarBonoResponse response = new ActivarBonoResponse();
        response.setCodigoRespuesta(substring(resultado, 46, 48));
        response.setDescripcionRespuesta(trimToNull(substring(resultado, 48, 123)));
        response.setNumeroAutorizacion(trimToNull(substring(resultado, 203, 209)));
        response.setTipoDocumento(resolveDocumentoTipo(trimToNull(substring(resultado, 209, 211))));
        response.setNumeroDocumento(trimToNull(substring(resultado, 211, 227)));
        response.setNombreCliente(trimToNull(substring(resultado, 227, 252)));
        response.setNumeroCuenta(trimToNull(substring(resultado, 252, 271)));
        response.setEstado(trimToNull(substring(resultado, 271, 272)));
        return response;
    }

    private RecuperarBonoResponse mapRecuperarResultado(String resultado) {
        RecuperarBonoResponse response = new RecuperarBonoResponse();
        response.setCodigoRespuesta(substring(resultado, 46, 48));
        response.setDescripcionRespuesta(trimToNull(substring(resultado, 48, 123)));
        String bonosSegment = trimToNull(substring(resultado, 160, 1459));
        if (StringUtils.isNotBlank(bonosSegment)) {
            String[] registros = bonosSegment.split(";");
            List<Bono> bonos = new ArrayList<>();
            for (String registro : registros) {
                if (StringUtils.isBlank(registro)) {
                    continue;
                }
                String[] data = registro.split(",");
                if (data.length >= 2) {
                    Bono bono = new Bono();
                    bono.setNumero(StringUtils.trimToNull(data[0]));
                    String valor = data[1];
                    if (valor != null && valor.length() >= 2) {
                        String numero = valor.substring(0, valor.length() - 2);
                        bono.setValor(Long.valueOf(numero));
                    }
                    bonos.add(bono);
                }
            }
            response.setBonos(bonos);
        } else {
            response.setBonos(List.of());
        }
        return response;
    }

    private StringBuilder buildCommonTrama(String codigoNovedad) {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder trama = new StringBuilder();
        trama.append(properties.codigo().aplicacion());
        trama.append(properties.codigo().switchCode());
        trama.append(codigoNovedad);
        trama.append(DATE_FORMATTER.format(now));
        trama.append(TIME_FORMATTER.format(now));
        return trama;
    }

    private void appendRightPad(StringBuilder builder, String value, int size) {
        builder.append(StringUtils.rightPad(Optional.ofNullable(value).orElse(""), size));
    }

    private void appendLeftPad(StringBuilder builder, String value, int size) {
        builder.append(StringUtils.leftPad(Optional.ofNullable(value).orElse(""), size, '0'));
    }

    private String resolvedocumentoCodigo(String tipoDocumento) {
        if (tipoDocumento == null) {
            throw new BonoBusinessException(properties.codigo().e01(), properties.descripcion().e01(), 400);
        }
        Map<String, String> documentos = properties.documentos();
        String codigo = documentos.get(tipoDocumento);
        if (codigo == null) {
            throw new BonoBusinessException(properties.codigo().e01(), properties.descripcion().e01(), 400);
        }
        return codigo;
    }

    private String resolveDocumentoTipo(String codigoDocumento) {
        if (StringUtils.isBlank(codigoDocumento)) {
            return null;
        }
        return properties.documentos().entrySet().stream()
                .filter(entry -> codigoDocumento.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String substring(String source, int start, int end) {
        if (source == null) {
            return null;
        }
        return StringUtils.substring(source, start, end);
    }

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }
}
