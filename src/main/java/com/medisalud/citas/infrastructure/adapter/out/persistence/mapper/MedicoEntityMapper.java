package com.medisalud.citas.infrastructure.adapter.out.persistence.mapper;

import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.MedicoEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MedicoEntityMapper {
    Medico toDomain(MedicoEntity entity);

    MedicoEntity toEntity(Medico domain);

    List<Medico> toDomainList(List<MedicoEntity> entities);
}
