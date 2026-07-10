package com.medisalud.citas.infrastructure.adapter.in.web.mapper;

import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.request.MedicoRequestDto;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.response.MedicoResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper
public interface MedicoMapper {
    @Mapping(target = "id", ignore = true)
    Medico toDomain(MedicoRequestDto dto);

    MedicoResponseDto toResponse(Medico medico);

    List<MedicoResponseDto> toResponseList(List<Medico> medicos);
}
