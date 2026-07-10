package com.medisalud.citas.infrastructure.adapter.in.web.dto.response;

import com.medisalud.citas.domain.model.EstadoCita;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class CitaResponseDto {
    private Long id;
    private Long pacienteId;
    private String nombrePaciente;
    private Long medicoId;
    private String nombreMedico;
    private String especialidadMedico;
    private LocalDateTime fechaHora;
    private EstadoCita estado;
    private LocalDateTime fechaCancelacion;
    private Boolean penalizado;
}
