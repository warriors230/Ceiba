package com.medisalud.citas.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MedicoRequestDto {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre completo debe tener entre 3 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 100, message = "La especialidad no puede superar los 100 caracteres")
    private String especialidad;

    @Pattern(regexp = "^$|^[0-9\\-\\+\\s]{7,20}$", message = "El teléfono debe tener mínimo 7 dígitos y solo puede contener números, guiones, '+' y espacios")
    private String telefono;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    public List<String> validarIntegridad() {
        List<String> errores = new ArrayList<>();
        if ((telefono == null || telefono.isBlank()) && (email == null || email.isBlank())) {
        }
        return errores;
    }
}
