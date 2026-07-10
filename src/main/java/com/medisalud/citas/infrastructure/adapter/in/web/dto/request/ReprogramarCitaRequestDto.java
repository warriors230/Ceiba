package com.medisalud.citas.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReprogramarCitaRequestDto {
    @NotNull(message = "La nueva fecha y hora es obligatoria")
    @Future(message = "La nueva fecha y hora debe ser en el futuro")
    private LocalDateTime nuevaFechaHora;

    public List<String> validarIntegridad() {
        List<String> errores = new ArrayList<>();
        if (nuevaFechaHora != null) {
            int minutos = nuevaFechaHora.getMinute();
            if (minutos != 0 && minutos != 30) {
                errores.add("La nueva cita debe programarse al inicio de una franja horaria (HH:00 o HH:30)");
            }
        }
        return errores;
    }
}
