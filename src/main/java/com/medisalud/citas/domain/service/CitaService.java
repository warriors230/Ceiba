package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.domain.model.Medico;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CitaService implements CitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final MedicoRepositoryPort medicoRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final MessageService messages;

    @Override
    public Cita reservar(Cita cita) {
        if (cita.getFechaHora() != null) {
            cita.setFechaHora(cita.getFechaHora().withSecond(0).withNano(0));
        }

        log.info("Reservando cita para paciente ID: {} con médico ID: {}",
                cita.getPaciente().getId(), cita.getMedico().getId());

        // Verificamos que el médico existe y actualizar cita
        Medico medico = medicoRepositoryPort.buscarPorId(cita.getMedico().getId())
                .orElseThrow(() -> new NotFoundException("Médico", cita.getMedico().getId()));
        cita.setMedico(medico);

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
    public List<Cita> listarCitas(Long medicoId, Long pacienteId, EstadoCita estado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        log.debug("Listando citas con filtros - medicoId:{} pacienteId:{} estado:{}", medicoId, pacienteId, estado);
        return citaRepositoryPort.listarCitas(medicoId, pacienteId, estado, fechaInicio, fechaFin);
    }

    @Override
    public List<LocalDateTime> consultarCitasDisponibles(Long medicoId, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio.isAfter(fechaFin)) {
            throw new BusinessRuleException(messages.get("error.cita.fecha.rango"));
        }

        List<LocalDateTime> franjasDisponibles = new ArrayList<>();

        List<Cita> citasProgramadas = citaRepositoryPort.listarCitas(medicoId, null, EstadoCita.PROGRAMADA,
                fechaInicio.atStartOfDay(), fechaFin.plusDays(1).atStartOfDay());

        Set<LocalDateTime> ocupadas = citasProgramadas.stream()
                .map(Cita::getFechaHora)
                .collect(Collectors.toSet());

        LocalDate actual = fechaInicio;
        while (!actual.isAfter(fechaFin)) {
            DayOfWeek day = actual.getDayOfWeek();
            if (day != DayOfWeek.SUNDAY && !Utils.esFestivoColombia(actual)) {
                int startHour = 8;
                int endHour = (day == DayOfWeek.SATURDAY) ? 13 : 18;

                for (int hour = startHour; hour <= endHour; hour++) {
                    LocalDateTime slot1 = LocalDateTime.of(actual, LocalTime.of(hour, 0));
                    if (!ocupadas.contains(slot1) && slot1.isAfter(LocalDateTime.now())) {
                        franjasDisponibles.add(slot1);
                    }

                    if (hour < endHour) {
                        LocalDateTime slot2 = LocalDateTime.of(actual, LocalTime.of(hour, 30));
                        if (!ocupadas.contains(slot2) && slot2.isAfter(LocalDateTime.now())) {
                            franjasDisponibles.add(slot2);
                        }
                    }
                }
            }
            actual = actual.plusDays(1);
        }

        return franjasDisponibles;
    }

    @Override
    public Cita cancelar(Long id) {
        log.info("Cancelando cita con ID: {}", id);

        Cita cita = citaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cita", id));

        // Solo se pueden cancelar citas en estado PROGRAMADA
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new BusinessRuleException(
                    messages.get("error.cita.cancelar.estado", cita.getEstado()));
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
        if (nuevaFecha != null) {
            nuevaFecha = nuevaFecha.withSecond(0).withNano(0);
        }
        log.info("Reprogramando cita ID: {} a nueva fecha: {}", id, nuevaFecha);

        Cita citaOriginal = citaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cita", id));

        if (citaOriginal.getEstado() != EstadoCita.PROGRAMADA) {
            throw new BusinessRuleException(
                    messages.get("error.cita.reprogramar.estado", citaOriginal.getEstado()));
        }

        // RN-06 — Reprogramación completa
        // Validar penalizaciones ANTES de cancelar: si el paciente tiene 2 o más
        // penalizaciones recientes, la cancelación generaría la 3ra y la nueva
        // reserva fallaría, dejándolo sin cita. Se bloquea aquí de forma preventiva.
        validarPenalizacionesParaReprogramar(citaOriginal.getPaciente().getId());

        // Validar reglas para la nueva fecha/hora antes de proceder con la cancelación
        // de la cita original
        validarFranjaHoraria(nuevaFecha);
        validarDisponibilidadMedico(citaOriginal.getMedico().getId(), nuevaFecha);
        validarConflictoPaciente(citaOriginal.getPaciente().getId(), nuevaFecha);

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
            throw new BusinessRuleException(messages.get("error.cita.domingo"));
        }

        int hour = fechaHora.getHour();
        int minute = fechaHora.getMinute();

        if (minute != 0 && minute != 30) {
            throw new BusinessRuleException(messages.get("error.cita.franja.minutos"));
        }

        if (day == DayOfWeek.SATURDAY) {
            if (hour < 8 || hour > 13 || (hour == 13 && minute > 0)) {
                throw new BusinessRuleException(messages.get("error.cita.franja.sabado"));
            }
        } else {
            // Lunes a Viernes 08:00 a 18:00 inclusive (última cita a las 18:00)
            if (hour < 8 || hour > 18 || (hour == 18 && minute > 0)) {
                throw new BusinessRuleException(messages.get("error.cita.franja.semana"));
            }
        }
    }

    private void validarDisponibilidadMedico(Long medicoId, LocalDateTime fechaHora) {
        List<Cita> citas = citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(medicoId, fechaHora);
        if (!citas.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.cita.medico.ocupado"));
        }
    }

    private void validarEdadPaciente(Paciente paciente) {
        if (paciente.getFechaNacimiento() != null && paciente.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new BusinessRuleException(messages.get("error.cita.paciente.fechaNacimiento"));
        }
    }

    private void validarConflictoPaciente(Long pacienteId, LocalDateTime fechaHora) {
        List<Cita> citas = citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(pacienteId, fechaHora);
        if (!citas.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.cita.paciente.ocupado"));
        }
    }

    private void validarPenalizacionesPaciente(Long pacienteId) {
        long penalizaciones = citaRepositoryPort.contarPenalizacionesPorPaciente(pacienteId,
                LocalDateTime.now().minusDays(30));
        if (penalizaciones >= 3) {
            throw new BusinessRuleException(messages.get("error.cita.penalizaciones.reservar"));
        }
    }

    private void validarPenalizacionesParaReprogramar(Long pacienteId) {
        long penalizaciones = citaRepositoryPort.contarPenalizacionesPorPaciente(pacienteId,
                LocalDateTime.now().minusDays(30));
        if (penalizaciones >= 2) {
            throw new BusinessRuleException(
                    messages.get("error.cita.penalizaciones.reprogramar", penalizaciones));
        }
    }
}
