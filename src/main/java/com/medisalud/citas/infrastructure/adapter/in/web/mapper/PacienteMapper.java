package com.medisalud.citas.infrastructure.adapter.in.web.mapper;

import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.request.PacienteRequestDto;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.response.PacienteResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper
public interface PacienteMapper {
    @Mapping(target = "id", ignore = true)
    Paciente toDomain(PacienteRequestDto dto);

    PacienteResponseDto toResponse(Paciente paciente);

    List<PacienteResponseDto> toResponseList(List<Paciente> pacientes);
}
