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
public class CitaRequestDto {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del médico es obligatorio")
    private Long medicoId;

    @NotNull(message = "La fecha y hora de la cita es obligatoria")
    @Future(message = "La fecha y hora de la cita debe ser en el futuro")
    private LocalDateTime fechaHora;

    public List<String> validarDatosReserva() {
        List<String> errores = new ArrayList<>();
        if (pacienteId != null && pacienteId <= 0) {
            errores.add("El ID del paciente debe ser un número positivo");
        }
        if (medicoId != null && medicoId <= 0) {
            errores.add("El ID del médico debe ser un número positivo");
        }
        if (fechaHora != null) {
            int minutos = fechaHora.getMinute();
            if (minutos != 0 && minutos != 30) {
                errores.add("La cita debe programarse al inicio de una franja horaria (HH:00 o HH:30)");
            }
            if (fechaHora.getSecond() != 0 || fechaHora.getNano() != 0) {
                errores.add("La fecha/hora no debe incluir segundos ni nanosegundos");
            }
        }
        return errores;
    }
}
