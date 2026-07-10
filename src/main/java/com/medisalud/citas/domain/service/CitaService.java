package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.domain.port.in.CitaUseCase;
import com.medisalud.citas.domain.util.Utils;
import com.medisalud.citas.domain.port.out.CitaRepositoryPort;
import com.medisalud.citas.domain.port.out.MedicoRepositoryPort;
import com.medisalud.citas.domain.port.out.PacienteRepositoryPort;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.NotFoundException;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CitaService implements CitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final MedicoRepositoryPort medicoRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;

    @Override
    public Cita reservar(Cita cita) {
        log.info("Reservando cita para paciente ID: {} con médico ID: {}",
                cita.getPaciente().getId(), cita.getMedico().getId());

        // Verificamos que el médico existe
        medicoRepositoryPort.buscarPorId(cita.getMedico().getId())
                .orElseThrow(() -> new NotFoundException("Médico", cita.getMedico().getId()));

        // Verificamos que el paciente existe y actualizar cita
        Paciente paciente = pacienteRepositoryPort.buscarPorId(cita.getPaciente().getId())
                .orElseThrow(() -> new NotFoundException("Paciente", cita.getPaciente().getId()));
        cita.setPaciente(paciente);

        validarFranjaHoraria(cita.getFechaHora());
        validarDisponibilidadMedico(cita.getMedico().getId(), cita.getFechaHora());
        validarEdadPaciente(paciente);
        validarConflictoPaciente(cita.getPaciente().getId(), cita.getFechaHora());
        validarPenalizacionesPaciente(cita.getPaciente().getId());

        // Estado inicial siempre PROGRAMADA
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setPenalizado(false);

        Cita guardada = citaRepositoryPort.guardar(cita);
        log.info("Cita reservada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public Cita obtenerPorId(Long id) {
        log.debug("Buscando cita con ID: {}", id);
        return citaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cita", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> listarConParametros(Long medicoId, Long pacienteId, EstadoCita estado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        log.debug("Listando citas con filtros - medicoId:{} pacienteId:{} estado:{}", medicoId, pacienteId, estado);
        return citaRepositoryPort.findByParameters(medicoId, pacienteId, estado, fechaInicio, fechaFin);
    }

    @Override
    public Cita cancelar(Long id) {
        log.info("Cancelando cita con ID: {}", id);

        Cita cita = citaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cita", id));

        // Solo se pueden cancelar citas en estado PROGRAMADA
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new BusinessRuleException(
                    "Solo se pueden cancelar citas en estado PROGRAMADA. Estado actual: " + cita.getEstado());
        }

        // RN-05 — Penalización por cancelación tardía
        boolean esCancelacionTardia = LocalDateTime.now().isAfter(cita.getFechaHora().minusHours(2));
        if (esCancelacionTardia) {
            cita.setPenalizado(true);
        }

        cita.setEstado(EstadoCita.CANCELADA);
        cita.setFechaCancelacion(LocalDateTime.now());

        return citaRepositoryPort.guardar(cita);
    }

    @Override
    public Cita reprogramar(Long id, LocalDateTime nuevaFecha) {
        log.info("Reprogramando cita ID: {} a nueva fecha: {}", id, nuevaFecha);

        Cita citaOriginal = citaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cita", id));

        if (citaOriginal.getEstado() != EstadoCita.PROGRAMADA) {
            throw new BusinessRuleException(
                    "Solo se pueden reprogramar citas en estado PROGRAMADA. Estado actual: "
                            + citaOriginal.getEstado());
        }

        // RN-06 — Reprogramación completa
        // Cancelar la cita original (aplica RN-05)
        Cita citaCancelada = cancelar(id);
        log.info("Cita original {} cancelada para reprogramación", id);

        // Crear nueva cita con la nueva fecha
        Cita nuevaCita = Cita.builder()
                .paciente(citaCancelada.getPaciente())
                .medico(citaCancelada.getMedico())
                .fechaHora(nuevaFecha)
                .estado(EstadoCita.PROGRAMADA)
                .penalizado(false)
                .build();

        // Validar reglas para nueva cita antes de guardarla
        validarFranjaHoraria(nuevaCita.getFechaHora());
        
        validarDisponibilidadMedico(nuevaCita.getMedico().getId(), nuevaCita.getFechaHora());
        
        validarConflictoPaciente(nuevaCita.getPaciente().getId(), nuevaCita.getFechaHora());
        // No se valida penalizaciones porque estamos reprogramando una cita existente

        return citaRepositoryPort.guardar(nuevaCita);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando cita con ID: {}", id);
        if (!citaRepositoryPort.existePorId(id)) {
            throw new NotFoundException("Cita", id);
        }
        citaRepositoryPort.eliminar(id);
    }

    private void validarFranjaHoraria(LocalDateTime fechaHora) {
        DayOfWeek day = fechaHora.getDayOfWeek();
        
        if (day == DayOfWeek.SUNDAY || Utils.esFestivoColombia(fechaHora.toLocalDate())) {
            throw new BusinessRuleException("No se atienden citas los domingos.");
        }
      
        int hour = fechaHora.getHour();
        int minute = fechaHora.getMinute();
      
        if (minute != 0 && minute != 30) {
            throw new BusinessRuleException("Las citas solo pueden programarse en franjas de 30 minutos.");
        }
        
        if (day == DayOfWeek.SATURDAY) {
            if (hour < 8 || hour >= 13 || (hour == 12 && minute > 30)) {
                throw new BusinessRuleException("Los sábados solo se atiende de 08:00 a.m a 01:00 p.m");
            }
        } else {
            // Lunes a Viernes 08:00 a 18:00 (última cita a las 17:30)
            if (hour < 8 || hour >= 18 || (hour == 17 && minute > 30)) {
                throw new BusinessRuleException("De lunes a viernes solo se atiende de 08:00 a.m a 06:00 p.m");
            }
        }
    }

    private void validarDisponibilidadMedico(Long medicoId, LocalDateTime fechaHora) {
        List<Cita> citas = citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(medicoId, fechaHora);
        if (!citas.isEmpty()) {
            throw new BusinessRuleException("El médico ya tiene una cita programada en ese horario.");
        }
    }

    private void validarEdadPaciente(Paciente paciente) {
        if (paciente.getFechaNacimiento() != null && paciente.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de nacimiento del paciente no puede ser en el futuro.");
        }
    }

    private void validarConflictoPaciente(Long pacienteId, LocalDateTime fechaHora) {
        List<Cita> citas = citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(pacienteId, fechaHora);
        if (!citas.isEmpty()) {
            throw new BusinessRuleException("El paciente ya tiene una cita programada en ese horario.");
        }
    }

    private void validarPenalizacionesPaciente(Long pacienteId) {
        long penalizaciones = citaRepositoryPort.contarPenalizacionesPorPaciente(pacienteId,
                LocalDateTime.now().minusDays(30));
        if (penalizaciones >= 3) {
            throw new BusinessRuleException(
                    "El paciente tiene 3 o más penalizaciones en los últimos 30 días y no puede agendar nuevas citas.");
        }
    }
}
