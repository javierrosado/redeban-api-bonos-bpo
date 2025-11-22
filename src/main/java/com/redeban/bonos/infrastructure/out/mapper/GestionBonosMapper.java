package com.redeban.bonos.infrastructure.out.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.redeban.bonos.domain.model.ActivarBonoRequest;
import com.redeban.bonos.domain.model.ActivarBonoResponse;
import com.redeban.bonos.domain.model.Bono;
import com.redeban.bonos.domain.model.CrearBonoRequest;
import com.redeban.bonos.domain.model.CrearBonoResponse;
import com.redeban.bonos.domain.model.RecuperarBonoRequest;
import com.redeban.bonos.domain.model.RecuperarBonoResponse;
import com.redeban.bonos.infrastructure.in.rest.dto.ActivarBonoRequestDto;
import com.redeban.bonos.infrastructure.in.rest.dto.ActivarBonoResponseDto;
import com.redeban.bonos.infrastructure.in.rest.dto.BonoDto;
import com.redeban.bonos.infrastructure.in.rest.dto.CrearBonoRequestDto;
import com.redeban.bonos.infrastructure.in.rest.dto.CrearBonoResponseDto;
import com.redeban.bonos.infrastructure.in.rest.dto.RecuperarBonoRequestDto;
import com.redeban.bonos.infrastructure.in.rest.dto.RecuperarBonoResponseDto;

@ApplicationScoped
public class GestionBonosMapper {

    public CrearBonoRequest toDomain(CrearBonoRequestDto dto) {
        CrearBonoRequest request = new CrearBonoRequest();
        request.setBin(dto.getBin());
        request.setNit(dto.getNit());
        request.setSubtipo(dto.getSubtipo());
        request.setTipoDocumento(dto.getTipoDocumento());
        request.setNumeroDocumento(dto.getNumeroDocumento());
        request.setNombre(dto.getNombre());
        return request;
    }

    public ActivarBonoRequest toDomain(ActivarBonoRequestDto dto) {
        ActivarBonoRequest request = new ActivarBonoRequest();
        request.setBin(dto.getBin());
        request.setNit(dto.getNit());
        request.setSubtipo(dto.getSubtipo());
        request.setNumeroBono(dto.getNumeroBono());
        request.setValorCarga(dto.getValorCarga());
        request.setNumeroAuditoria(dto.getNumeroAuditoria());
        request.setConsecutivo(dto.getConsecutivo());
        return request;
    }

    public RecuperarBonoRequest toDomain(RecuperarBonoRequestDto dto) {
        RecuperarBonoRequest request = new RecuperarBonoRequest();
        request.setBin(dto.getBin());
        request.setNit(dto.getNit());
        request.setSubtipo(dto.getSubtipo());
        request.setTipoDocumento(dto.getTipoDocumento());
        request.setNumeroDocumento(dto.getNumeroDocumento());
        return request;
    }

    public CrearBonoResponseDto toDto(CrearBonoResponse response) {
        CrearBonoResponseDto dto = new CrearBonoResponseDto();
        dto.setCodigoRespuesta(response.getCodigoRespuesta());
        dto.setDescripcionRespuesta(response.getDescripcionRespuesta());
        dto.setNumeroBono(response.getNumeroBono());
        return dto;
    }

    public ActivarBonoResponseDto toDto(ActivarBonoResponse response) {
        ActivarBonoResponseDto dto = new ActivarBonoResponseDto();
        dto.setCodigoRespuesta(response.getCodigoRespuesta());
        dto.setDescripcionRespuesta(response.getDescripcionRespuesta());
        dto.setNumeroAutorizacion(response.getNumeroAutorizacion());
        dto.setTipoDocumento(response.getTipoDocumento());
        dto.setNumeroDocumento(response.getNumeroDocumento());
        dto.setNombreCliente(response.getNombreCliente());
        dto.setNumeroCuenta(response.getNumeroCuenta());
        dto.setEstado(response.getEstado());
        return dto;
    }

    public RecuperarBonoResponseDto toDto(RecuperarBonoResponse response) {
        RecuperarBonoResponseDto dto = new RecuperarBonoResponseDto();
        dto.setCodigoRespuesta(response.getCodigoRespuesta());
        dto.setDescripcionRespuesta(response.getDescripcionRespuesta());
        if (response.getBonos() != null) {
            dto.setBonos(response.getBonos().stream().filter(Objects::nonNull).map(this::toDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private BonoDto toDto(Bono bono) {
        BonoDto dto = new BonoDto();
        dto.setNumero(bono.getNumero());
        dto.setValor(bono.getValor());
        return dto;
    }

    public List<Bono> toDomainBonos(List<BonoDto> bonosDto) {
        if (bonosDto == null) {
            return List.of();
        }
        return bonosDto.stream().filter(Objects::nonNull).map(this::toDomain).collect(Collectors.toList());
    }

    private Bono toDomain(BonoDto dto) {
        Bono bono = new Bono();
        bono.setNumero(dto.getNumero());
        bono.setValor(dto.getValor());
        return bono;
    }
}
