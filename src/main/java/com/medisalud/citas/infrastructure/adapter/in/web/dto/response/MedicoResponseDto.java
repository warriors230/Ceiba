package com.medisalud.citas.infrastructure.adapter.in.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MedicoResponseDto {
    private Long id;
    private String nombreCompleto;
    private String especialidad;
    private String telefono;
    private String email;
}
