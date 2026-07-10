package com.medisalud.citas.domain.port.out;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepositoryPort {
        Cita guardar(Cita cita);

        Optional<Cita> buscarPorId(Long id);

        List<Cita> findByParameters(Long medicoId, Long pacienteId, EstadoCita estado,
                        LocalDateTime fechaInicio, LocalDateTime fechaFin);

        List<Cita> buscarCitasProgramadasPorMedicoYFecha(Long medicoId, LocalDateTime fechaHora);

        List<Cita> buscarCitasProgramadasPorPacienteYFecha(Long pacienteId, LocalDateTime fechaHora);

        long contarPenalizacionesPorPaciente(Long pacienteId, LocalDateTime desde);

        void eliminar(Long id);

        boolean existePorId(Long id);
}
