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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

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

        @Mock
        private MessageService messages;

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

                // Configurar MessageService para que devuelva los mensajes reales del
                // properties
                ResourceBundle bundle = ResourceBundle.getBundle("messages");
                lenient().when(messages.get(anyString())).thenAnswer(inv -> bundle.getString(inv.getArgument(0)));
                // Para varargs, getArguments() devuelve [key, arg0, arg1...] — reconstruimos el
                // array
                lenient().when(messages.get(anyString(), any())).thenAnswer(inv -> {
                        Object[] all = inv.getArguments();
                        String key = (String) all[0];
                        Object[] params = new Object[all.length - 1];
                        System.arraycopy(all, 1, params, 0, params.length);
                        return MessageFormat.format(bundle.getString(key), params);
                });
        }

        @Test
        void reservar_CitaValida_Exito() {
                when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
                when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
                when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                                .thenReturn(Collections.emptyList());
                when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, fechaValida))
                                .thenReturn(Collections.emptyList());
                when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                                .thenReturn(0L);
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

                BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                                () -> citaService.reservar(cita));
                assertTrue(exception.getMessage().contains("No se atienden citas los domingos."));
        }

        @Test
        void reservar_FueraHorarioLunesViernes_LanzaExcepcion() {
                when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
                when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));

                // Lunes a las 18:30
                cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 18, 30));

                BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                                () -> citaService.reservar(cita));
                assertTrue(exception.getMessage().contains("08:00 a.m a 06:00 p.m"));
        }

        @Test
        void reservar_MedicoConCitaPrevia_LanzaExcepcion() {
                when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
                when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
                when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                                .thenReturn(List.of(new Cita())); // Ya tiene cita

                BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                                () -> citaService.reservar(cita));
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

        // -----------------------------------------------------------------------
        // Tests RN-06: Validación de penalizaciones al reprogramar
        // -----------------------------------------------------------------------

        @Test
        void reprogramar_PacienteConDosPenalizaciones_LanzaExcepcion() {
                // El paciente ya tiene 2 penalizaciones recientes; la reprogramación
                // generaría una 3ra y dejaría al paciente sin cita → se bloquea antes.
                Cita citaProgramada = Cita.builder()
                                .id(1L)
                                .paciente(paciente)
                                .medico(medico)
                                .estado(EstadoCita.PROGRAMADA)
                                .fechaHora(fechaValida)
                                .build();

                when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
                when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                                .thenReturn(2L);

                LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0); // Martes válido

                BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                                () -> citaService.reprogramar(1L, nuevaFecha));

                assertTrue(exception.getMessage().contains("penalizaciones en los últimos 30 días"));
                assertTrue(exception.getMessage().contains("no puede reprogramar"));
                // La cita original NO debe haberse cancelado
                verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        void reprogramar_PacienteConTresPenalizaciones_LanzaExcepcion() {
                // Con 3 penalizaciones también debe bloquearse (ya no puede reservar ni
                // reprogramar)
                Cita citaProgramada = Cita.builder()
                                .id(1L)
                                .paciente(paciente)
                                .medico(medico)
                                .estado(EstadoCita.PROGRAMADA)
                                .fechaHora(fechaValida)
                                .build();

                when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
                when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                                .thenReturn(3L);

                LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0);

                assertThrows(BusinessRuleException.class, () -> citaService.reprogramar(1L, nuevaFecha));
                // La cita original NO debe haberse cancelado
                verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        void reprogramar_PacienteConUnaPenalizacion_Exito() {
                // Con solo 1 penalización el paciente puede reprogramar sin problema
                Cita citaProgramada = Cita.builder()
                                .id(1L)
                                .paciente(paciente)
                                .medico(medico)
                                .estado(EstadoCita.PROGRAMADA)
                                .fechaHora(fechaValida)
                                .build();

                LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0); // Martes válido

                when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
                when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                                .thenReturn(1L);
                when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(i -> {
                        Cita c = i.getArgument(0);
                        if (c.getId() == null)
                                c.setId(99L);
                        return c;
                });
                when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(eq(1L), eq(nuevaFecha)))
                                .thenReturn(Collections.emptyList());
                when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(eq(1L), eq(nuevaFecha)))
                                .thenReturn(Collections.emptyList());

                Cita resultado = citaService.reprogramar(1L, nuevaFecha);

                assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
                assertEquals(nuevaFecha, resultado.getFechaHora());
                assertFalse(resultado.getPenalizado());
        }
}