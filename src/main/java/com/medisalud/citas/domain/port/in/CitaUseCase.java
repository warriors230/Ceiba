package com.medisalud.citas.domain.port.in;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaUseCase {
	
    Cita reservar(Cita cita);

    Cita obtenerPorId(Long id);

    List<Cita> listarConParametros(Long medicoId, Long pacienteId, EstadoCita estado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin);

    Cita cancelar(Long id);

    Cita reprogramar(Long id, LocalDateTime nuevaFecha);

    void eliminar(Long id);
}
