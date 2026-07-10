package com.medisalud.citas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medico {
    private Long id;
    private String nombreCompleto;
    private String especialidad;
    private String telefono;
    private String email;
}
