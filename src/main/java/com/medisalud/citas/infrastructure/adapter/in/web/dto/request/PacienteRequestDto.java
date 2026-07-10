package com.medisalud.citas.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PacienteRequestDto {
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre completo debe tener entre 3 y 100 caracteres")
    private String nombreCompleto;
    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(min = 7, max = 20, message = "El documento debe tener entre 7 y 20 caracteres")
    private String documento;
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9\\-]{7,20}$", message = "El teléfono debe tener mínimo 7 dígitos y solo puede contener números y guiones")
    private String telefono;
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura o la fecha actual")
    private LocalDate fechaNacimiento;

    public List<String> validarIntegridad() {
        List<String> errores = new ArrayList<>();
        if (documento != null && !documento.matches("^[a-zA-Z0-9\\-]+$")) {
            errores.add("El documento solo puede contener letras, números y guiones");
        }
        if (email != null && email.contains(" ")) {
            errores.add("El email no puede contener espacios");
        }
        return errores;
    }
}
