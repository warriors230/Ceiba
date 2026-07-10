package com.medisalud.citas.infrastructure.adapter.in.web.mapper;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.request.CitaRequestDto;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.response.CitaResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper
public interface CitaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaCancelacion", ignore = true)
    @Mapping(target = "penalizado", ignore = true)
    @Mapping(target = "paciente", expression = "java(buildPaciente(dto.getPacienteId()))")
    @Mapping(target = "medico", expression = "java(buildMedico(dto.getMedicoId()))")
    Cita toDomain(CitaRequestDto dto);

    @Mapping(source = "paciente.id", target = "pacienteId")
    @Mapping(source = "paciente.nombreCompleto", target = "nombrePaciente")
    @Mapping(source = "medico.id", target = "medicoId")
    @Mapping(source = "medico.nombreCompleto", target = "nombreMedico")
    @Mapping(source = "medico.especialidad", target = "especialidadMedico")
    CitaResponseDto toResponse(Cita cita);

    List<CitaResponseDto> toResponseList(List<Cita> citas);

    default Paciente buildPaciente(Long pacienteId) {
        return Paciente.builder().id(pacienteId).build();
    }

    default Medico buildMedico(Long medicoId) {
        return Medico.builder().id(medicoId).build();
    }
}
