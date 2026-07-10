package com.medisalud.citas.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "pacientes")
public class PacienteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;
    @Column(name = "documento", nullable = false, unique = true, length = 20)
    private String documento;
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;
    @Column(name = "email", nullable = false, length = 150)
    private String email;
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
}
