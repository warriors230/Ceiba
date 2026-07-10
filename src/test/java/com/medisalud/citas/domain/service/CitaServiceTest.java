package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.domain.port.out.CitaRepositoryPort;
import com.medisalud.citas.domain.port.out.MedicoRepositoryPort;
import com.medisalud.citas.domain.port.out.PacienteRepositoryPort;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaRepositoryPort citaRepositoryPort;

    @Mock
    private MedicoRepositoryPort medicoRepositoryPort;

    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;

    @InjectMocks
    private CitaService citaService;

    private Medico medico;
    private Paciente paciente;
    private Cita cita;

    // Un Lunes a las 10:00 (válido)
    private final LocalDateTime fechaValida = LocalDateTime.of(2030, 5, 20, 10, 0);

    @BeforeEach
    void setUp() {
        medico = Medico.builder().id(1L).build();
        paciente = Paciente.builder().id(1L).fechaNacimiento(LocalDate.of(1990, 1, 1)).build();
        cita = Cita.builder()
                .medico(medico)
                .paciente(paciente)
                .fechaHora(fechaValida)
                .build();
    }

    @Test
    void reservar_CitaValida_Exito() {
        when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                .thenReturn(Collections.emptyList());
        when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, fechaValida))
                .thenReturn(Collections.emptyList());
        when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(i -> {
            Cita c = i.getArgument(0);
            c.setId(100L);
            return c;
        });

        Cita resultado = citaService.reservar(cita);

        assertNotNull(resultado.getId());
        assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
        assertFalse(resultado.getPenalizado());
        verify(citaRepositoryPort).guardar(any(Cita.class));
    }

    @Test
    void reservar_Domingo_LanzaExcepcion() {
        when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));

        // Un Domingo
        cita.setFechaHora(LocalDateTime.of(2030, 5, 19, 10, 0));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        assertTrue(exception.getMessage().contains("No se atienden citas los domingos."));
    }

    @Test
    void reservar_FueraHorarioLunesViernes_LanzaExcepcion() {
        when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));

        // Lunes a las 18:30
        cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 18, 30));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        assertTrue(exception.getMessage().contains("08:00 a.m a 06:00 p.m"));
    }

    @Test
    void reservar_MedicoConCitaPrevia_LanzaExcepcion() {
        when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                .thenReturn(List.of(new Cita())); // Ya tiene cita

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        assertTrue(exception.getMessage().contains("médico ya tiene una cita"));
    }

    @Test
    void cancelar_CancelacionTardia_AplicaPenalizacion() {
        Cita citaProgramada = Cita.builder()
                .id(1L)
                .estado(EstadoCita.PROGRAMADA)
                // Fecha de cita en 1 hora (menos de 2h de antelación)
                .fechaHora(LocalDateTime.now().plusHours(1))
                .build();

        when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
        when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

        Cita cancelada = citaService.cancelar(1L);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
        assertTrue(cancelada.getPenalizado());
    }
}
