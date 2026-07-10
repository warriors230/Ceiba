package com.medisalud.citas.infrastructure.adapter.in.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class PacienteResponseDto {
    private Long id;
    private String nombreCompleto;
    private String documento;
    private String telefono;
    private String email;
    private LocalDate fechaNacimiento;
}
