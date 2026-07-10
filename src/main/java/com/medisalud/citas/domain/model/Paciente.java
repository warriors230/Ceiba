package com.medisalud.citas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {
    private Long id;
    private String nombreCompleto;
    private String documento;
    private String telefono;
    private String email;
    private LocalDate fechaNacimiento;
}
