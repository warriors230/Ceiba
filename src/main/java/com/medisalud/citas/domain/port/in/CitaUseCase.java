package com.medisalud.citas.domain.port.in;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaUseCase {
	
    Cita reservar(Cita cita);

    Cita obtenerPorId(Long id);

    List<Cita> listarCitas(Long medicoId, Long pacienteId, EstadoCita estado, LocalDateTime fechaInicio,
            LocalDateTime fechaFin);
            
    List<LocalDateTime> consultarCitasDisponibles(Long medicoId, LocalDate fechaInicio, LocalDate fechaFin);

    Cita cancelar(Long id);

    Cita reprogramar(Long id, LocalDateTime nuevaFecha);

    void eliminar(Long id);
}
