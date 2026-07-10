package com.medisalud.citas.infrastructure.adapter.out.persistence.mapper;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.CitaEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = { MedicoEntityMapper.class, PacienteEntityMapper.class })
public interface CitaEntityMapper {
    Cita toDomain(CitaEntity entity);

    CitaEntity toEntity(Cita domain);

    List<Cita> toDomainList(List<CitaEntity> entities);
}
