package com.redeban.bonos.infrastructure.in.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import org.jboss.resteasy.reactive.RestHeader;

import com.redeban.bonos.domain.model.HeaderContext;
import com.redeban.bonos.domain.port.in.GestionBonosInPort;
import com.redeban.bonos.infrastructure.in.rest.dto.ActivarBonoRequestDto;
import com.redeban.bonos.infrastructure.in.rest.dto.ActivarBonoResponseDto;
import com.redeban.bonos.infrastructure.in.rest.dto.CrearBonoRequestDto;
import com.redeban.bonos.infrastructure.in.rest.dto.CrearBonoResponseDto;
import com.redeban.bonos.infrastructure.in.rest.dto.ErrorResponseDto;
import com.redeban.bonos.infrastructure.in.rest.dto.RecuperarBonoRequestDto;
import com.redeban.bonos.infrastructure.in.rest.dto.RecuperarBonoResponseDto;
import com.redeban.bonos.infrastructure.logging.RequestResponseLogger;
import com.redeban.bonos.infrastructure.out.mapper.GestionBonosMapper;

@Path("/servicios/bonos/gestionBonos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class GestionBonosController {

    private final GestionBonosInPort gestionBonosUseCase;
    private final GestionBonosMapper mapper;
    private final RequestResponseLogger requestResponseLogger;
    private final RestErrorMapper restErrorMapper;

    @Inject
    public GestionBonosController(GestionBonosInPort gestionBonosUseCase,
                                  GestionBonosMapper mapper,
                                  RequestResponseLogger requestResponseLogger,
                                  RestErrorMapper restErrorMapper) {
        this.gestionBonosUseCase = gestionBonosUseCase;
        this.mapper = mapper;
        this.requestResponseLogger = requestResponseLogger;
        this.restErrorMapper = restErrorMapper;
    }

    @POST
    @Path("/crearBono")
    public Response crearBono(@RestHeader("idTransaccion") String idTransaccion,
                              @RestHeader("nombreAplicacion") String nombreAplicacion,
                              @RestHeader("ipAplicacion") String ipAplicacion,
                              @RestHeader("timestamp") String timestamp,
                              @Valid CrearBonoRequestDto requestDto) {
        OffsetDateTime start = OffsetDateTime.now();
        HeaderContext headerContext = new HeaderContext(idTransaccion, nombreAplicacion, ipAplicacion, timestamp, start);
        try {
            CrearBonoResponseDto responseDto = mapper.toDto(
                    gestionBonosUseCase.crearBono(mapper.toDomain(requestDto), headerContext));
            OffsetDateTime end = OffsetDateTime.now();
            requestResponseLogger.logSuccess("crearBono", headerContext, requestDto, responseDto, start, end);
            return Response.ok(responseDto).build();
        } catch (RuntimeException ex) {
            OffsetDateTime end = OffsetDateTime.now();
            RestErrorMapper.ErrorWrapper errorWrapper = restErrorMapper.toError(ex);
            ErrorResponseDto errorResponseDto = errorWrapper.body();
            requestResponseLogger.logError("crearBono", headerContext, requestDto, errorResponseDto, start, end, ex);
            return Response.status(errorWrapper.status()).entity(errorResponseDto).build();
        }
    }

    @PUT
    @Path("/activarBono")
    public Response activarBono(@RestHeader("idTransaccion") String idTransaccion,
                                @RestHeader("nombreAplicacion") String nombreAplicacion,
                                @RestHeader("ipAplicacion") String ipAplicacion,
                                @RestHeader("timestamp") String timestamp,
                                @Valid ActivarBonoRequestDto requestDto) {
        OffsetDateTime start = OffsetDateTime.now();
        HeaderContext headerContext = new HeaderContext(idTransaccion, nombreAplicacion, ipAplicacion, timestamp, start);
        try {
            ActivarBonoResponseDto responseDto = mapper.toDto(
                    gestionBonosUseCase.activarBono(mapper.toDomain(requestDto), headerContext));
            OffsetDateTime end = OffsetDateTime.now();
            requestResponseLogger.logSuccess("activarBono", headerContext, requestDto, responseDto, start, end);
            return Response.ok(responseDto).build();
        } catch (RuntimeException ex) {
            OffsetDateTime end = OffsetDateTime.now();
            RestErrorMapper.ErrorWrapper errorWrapper = restErrorMapper.toError(ex);
            ErrorResponseDto errorResponseDto = errorWrapper.body();
            requestResponseLogger.logError("activarBono", headerContext, requestDto, errorResponseDto, start, end, ex);
            return Response.status(errorWrapper.status()).entity(errorResponseDto).build();
        }
    }

    @POST
    @Path("/recuperarBono")
    public Response recuperarBono(@RestHeader("idTransaccion") String idTransaccion,
                                  @RestHeader("nombreAplicacion") String nombreAplicacion,
                                  @RestHeader("ipAplicacion") String ipAplicacion,
                                  @RestHeader("timestamp") String timestamp,
                                  @Valid RecuperarBonoRequestDto requestDto) {
        OffsetDateTime start = OffsetDateTime.now();
        HeaderContext headerContext = new HeaderContext(idTransaccion, nombreAplicacion, ipAplicacion, timestamp, start);
        try {
            RecuperarBonoResponseDto responseDto = mapper.toDto(
                    gestionBonosUseCase.recuperarBono(mapper.toDomain(requestDto), headerContext));
            OffsetDateTime end = OffsetDateTime.now();
            requestResponseLogger.logSuccess("recuperarBono", headerContext, requestDto, responseDto, start, end);
            return Response.ok(responseDto).build();
        } catch (RuntimeException ex) {
            OffsetDateTime end = OffsetDateTime.now();
            RestErrorMapper.ErrorWrapper errorWrapper = restErrorMapper.toError(ex);
            ErrorResponseDto errorResponseDto = errorWrapper.body();
            requestResponseLogger.logError("recuperarBono", headerContext, requestDto, errorResponseDto, start, end, ex);
            return Response.status(errorWrapper.status()).entity(errorResponseDto).build();
        }
    }
}
