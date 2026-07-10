package com.medisalud.citas.infrastructure.adapter.out.persistence.mapper;

import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.PacienteEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PacienteEntityMapper {
    Paciente toDomain(PacienteEntity entity);

    PacienteEntity toEntity(Paciente domain);

    List<Paciente> toDomainList(List<PacienteEntity> entities);
}
