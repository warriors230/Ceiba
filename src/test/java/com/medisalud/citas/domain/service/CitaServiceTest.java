package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.domain.port.out.CitaRepositoryPort;
import com.medisalud.citas.domain.port.out.MedicoRepositoryPort;
import com.medisalud.citas.domain.port.out.PacienteRepositoryPort;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.BusinessRuleException;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de CitaService.
 *
 * Cubre todas las reglas de negocio del enunciado:
 *   RN-01 Franjas horarias de atención
 *   RN-02 No duplicidad de citas (médico ocupado)
 *   RN-03 Antigüedad mínima del paciente
 *   RN-04 Conflicto de paciente
 *   RN-05 Penalización por cancelación tardía
 *   RN-06 Reprogramación
 */
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

    // Lunes 20-May-2030 a las 10:00 (válido)
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

        // MessageService devuelve los mensajes reales del properties
        ResourceBundle bundle = ResourceBundle.getBundle("messages");
        lenient().when(messages.get(anyString()))
                .thenAnswer(inv -> bundle.getString(inv.getArgument(0)));
        lenient().when(messages.get(anyString(), any())).thenAnswer(inv -> {
            Object[] all = inv.getArguments();
            String key = (String) all[0];
            Object[] params = new Object[all.length - 1];
            System.arraycopy(all, 1, params, 0, params.length);
            return MessageFormat.format(bundle.getString(key), params);
        });
    }

    // =========================================================================
    // Helper para configurar mocks de reserva exitosa
    // =========================================================================
    private void configurarReservaExitosa(LocalDateTime fecha) {
        when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fecha))
                .thenReturn(Collections.emptyList());
        when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, fecha))
                .thenReturn(Collections.emptyList());
        when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
    }

    // =========================================================================
    // RN-01: Franjas horarias de atención
    // =========================================================================
    @Nested
    @DisplayName("RN-01 - Franjas horarias de atencion")
    class FranjasHorarias {

        // --- Casos de borde: límites exactos del horario ---

        @Test
        @DisplayName("Reservar lunes a las 08:00 (primera franja semana) -> exito")
        void reservar_LunesALas0800_Exito() {
            LocalDateTime lunes08 = LocalDateTime.of(2030, 5, 20, 8, 0);
            cita.setFechaHora(lunes08);
            configurarReservaExitosa(lunes08);

            Cita resultado = citaService.reservar(cita);

            assertNotNull(resultado.getId());
            assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
        }

        @Test
        @DisplayName("Reservar lunes a las 18:00 (ultima franja semana) -> exito")
        void reservar_LunesALas1800_Exito() {
            LocalDateTime lunes18 = LocalDateTime.of(2030, 5, 20, 18, 0);
            cita.setFechaHora(lunes18);
            configurarReservaExitosa(lunes18);

            Cita resultado = citaService.reservar(cita);

            assertNotNull(resultado.getId());
            assertEquals(lunes18, resultado.getFechaHora());
        }

        @Test
        @DisplayName("Reservar lunes a las 18:30 (despues del horario) -> excepcion")
        void reservar_LunesALas1830_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 18, 30));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("De lunes a viernes solo se atiende"));
        }

        @Test
        @DisplayName("Reservar lunes a las 07:30 (antes del horario) -> excepcion")
        void reservar_LunesALas0730_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 7, 30));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("08:00 a.m a 06:00 p.m"));
        }

        @Test
        @DisplayName("Reservar lunes a las 07:00 (fuera de horario) -> excepcion")
        void reservar_LunesALas0700_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 7, 0));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar lunes a las 19:00 (fuera de horario) -> excepcion")
        void reservar_LunesALas1900_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 19, 0));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar sabado a las 08:00 (primera franja sabado) -> exito")
        void reservar_SabadoALas0800_Exito() {
            LocalDateTime sabado08 = LocalDateTime.of(2030, 5, 25, 8, 0);
            cita.setFechaHora(sabado08);
            configurarReservaExitosa(sabado08);

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
        }

        @Test
        @DisplayName("Reservar sabado a las 13:00 (ultima franja sabado) -> exito")
        void reservar_SabadoALas1300_Exito() {
            LocalDateTime sabado13 = LocalDateTime.of(2030, 5, 25, 13, 0);
            cita.setFechaHora(sabado13);
            configurarReservaExitosa(sabado13);

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
            assertEquals(sabado13, resultado.getFechaHora());
        }

        @Test
        @DisplayName("Reservar sabado a las 13:30 (despues del horario de sabado) -> excepcion")
        void reservar_SabadoALas1330_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 25, 13, 30));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("Los sábados solo se atiende"));
        }

        @Test
        @DisplayName("Reservar sabado a las 14:00 (fuera del horario de sabado) -> excepcion")
        void reservar_SabadoALas1400_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 25, 14, 0));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar sabado a las 07:30 (antes del horario de sabado) -> excepcion")
        void reservar_SabadoALas0730_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 25, 7, 30));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar domingo -> excepcion (RN-01)")
        void reservar_Domingo_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 19, 10, 0)); // Domingo

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("No se atienden citas los domingos."));
        }

        @Test
        @DisplayName("Reservar festivo colombiano (1 enero) -> excepcion (RN-01)")
        void reservar_FestivoNuevoAnio_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            // 1 de enero 2031 (miercoles)
            cita.setFechaHora(LocalDateTime.of(2031, 1, 1, 10, 0));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar 25 diciembre (Navidad) -> excepcion (RN-01)")
        void reservar_FestivoNavidad_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            // 25 dic 2030 (miercoles)
            cita.setFechaHora(LocalDateTime.of(2030, 12, 25, 10, 0));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar en minuto :15 (no franja de 30 min) -> excepcion")
        void reservar_Minuto15_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 10, 15));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("franjas de 30 minutos"));
        }

        @Test
        @DisplayName("Reservar en minuto :45 (no franja de 30 min) -> excepcion")
        void reservar_Minuto45_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 10, 45));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar en minuto :01 (no franja de 30 min) -> excepcion")
        void reservar_Minuto01_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            cita.setFechaHora(LocalDateTime.of(2030, 5, 20, 10, 1));

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Reservar viernes a las 18:00 (borde fin de semana laboral) -> exito")
        void reservar_ViernesALas1800_Exito() {
            LocalDateTime viernes18 = LocalDateTime.of(2030, 5, 24, 18, 0);
            cita.setFechaHora(viernes18);
            configurarReservaExitosa(viernes18);

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
        }

        @Test
        @DisplayName("Reservar lunes a las 08:30 (franja :30 valida) -> exito")
        void reservar_LunesA0830_Exito() {
            LocalDateTime lunes0830 = LocalDateTime.of(2030, 5, 20, 8, 30);
            cita.setFechaHora(lunes0830);
            configurarReservaExitosa(lunes0830);

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
        }

        @Test
        @DisplayName("Reservar martes a las 12:30 (mitad de jornada) -> exito")
        void reservar_MartesA1230_Exito() {
            LocalDateTime martes1230 = LocalDateTime.of(2030, 5, 21, 12, 30);
            cita.setFechaHora(martes1230);
            configurarReservaExitosa(martes1230);

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
        }
    }

    // =========================================================================
    // RN-02: No duplicidad de citas (médico ocupado)
    // =========================================================================
    @Nested
    @DisplayName("RN-02 - No duplicidad de citas del medico")
    class DuplicidadCitasMedico {

        @Test
        @DisplayName("Medico ocupado en el horario -> excepcion")
        void reservar_MedicoConCitaPrevia_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(List.of(new Cita()));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("médico ya tiene una cita"));
        }

        @Test
        @DisplayName("Medico libre en el horario -> reserva exitosa")
        void reservar_MedicoLibre_Exito() {
            configurarReservaExitosa(fechaValida);

            Cita resultado = citaService.reservar(cita);

            assertNotNull(resultado.getId());
            assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
            assertFalse(resultado.getPenalizado());
        }
    }

    // =========================================================================
    // RN-03: Antigüedad mínima del paciente
    // =========================================================================
    @Nested
    @DisplayName("RN-03 - Antiguedad minima del paciente")
    class AntiguedadPaciente {

        @Test
        @DisplayName("Paciente con fecha de nacimiento futura -> excepcion")
        void reservar_PacienteConFechaNacimientoFutura_LanzaExcepcion() {
            Paciente pacienteFuturo = Paciente.builder()
                    .id(2L)
                    .fechaNacimiento(LocalDate.now().plusDays(1))
                    .build();
            Cita citaFuturo = Cita.builder()
                    .medico(medico)
                    .paciente(Paciente.builder().id(2L).build())
                    .fechaHora(fechaValida)
                    .build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(2L)).thenReturn(Optional.of(pacienteFuturo));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(citaFuturo));
            assertTrue(ex.getMessage().contains("fecha de nacimiento del paciente no puede ser en el futuro"));
        }

        @Test
        @DisplayName("Paciente nacido hoy -> exito (edad 0 = borde inferior valido)")
        void reservar_PacienteNacidoHoy_Exito() {
            Paciente pacienteHoy = Paciente.builder()
                    .id(3L)
                    .fechaNacimiento(LocalDate.now())
                    .build();
            Cita citaHoy = Cita.builder()
                    .medico(medico)
                    .paciente(Paciente.builder().id(3L).build())
                    .fechaHora(fechaValida)
                    .build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(3L)).thenReturn(Optional.of(pacienteHoy));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(3L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(3L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                c.setId(200L);
                return c;
            });

            Cita resultado = citaService.reservar(citaHoy);
            assertNotNull(resultado.getId());
        }

        @Test
        @DisplayName("Paciente sin fecha de nacimiento (null) -> exito (se asume edad 0)")
        void reservar_PacienteSinFechaNacimiento_Exito() {
            Paciente pacienteSinFecha = Paciente.builder()
                    .id(4L)
                    .fechaNacimiento(null)
                    .build();
            Cita citaSinFecha = Cita.builder()
                    .medico(medico)
                    .paciente(Paciente.builder().id(4L).build())
                    .fechaHora(fechaValida)
                    .build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(4L)).thenReturn(Optional.of(pacienteSinFecha));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(4L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(4L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                c.setId(201L);
                return c;
            });

            Cita resultado = citaService.reservar(citaSinFecha);
            assertNotNull(resultado.getId());
        }

        @Test
        @DisplayName("Paciente nacido ayer -> exito (edad 0, un dia despues de borde)")
        void reservar_PacienteNacidoAyer_Exito() {
            Paciente pacienteAyer = Paciente.builder()
                    .id(5L)
                    .fechaNacimiento(LocalDate.now().minusDays(1))
                    .build();
            Cita citaAyer = Cita.builder()
                    .medico(medico)
                    .paciente(Paciente.builder().id(5L).build())
                    .fechaHora(fechaValida)
                    .build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(pacienteAyer));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(5L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(5L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                c.setId(202L);
                return c;
            });

            Cita resultado = citaService.reservar(citaAyer);
            assertNotNull(resultado.getId());
        }
    }

    // =========================================================================
    // RN-04: Conflicto de paciente en el mismo horario
    // =========================================================================
    @Nested
    @DisplayName("RN-04 - Conflicto de paciente")
    class ConflictoPaciente {

        @Test
        @DisplayName("Paciente ya tiene cita en ese horario -> excepcion")
        void reservar_PacienteConCitaEnMismoHorario_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, fechaValida))
                    .thenReturn(List.of(new Cita()));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("El paciente ya tiene una cita programada"));
        }

        @Test
        @DisplayName("Paciente libre en el horario -> reserva exitosa")
        void reservar_PacienteLibreEnHorario_Exito() {
            configurarReservaExitosa(fechaValida);

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
        }
    }

    // =========================================================================
    // RN-05: Penalización por cancelación tardía
    // =========================================================================
    @Nested
    @DisplayName("RN-05 - Penalizacion por cancelacion tardia")
    class PenalizacionCancelacion {

        @Test
        @DisplayName("Cancelacion con menos de 2h de antelacion -> penalizacion aplicada")
        void cancelar_CancelacionTardia_AplicaPenalizacion() {
            Cita citaProgramada = Cita.builder()
                    .id(1L)
                    .estado(EstadoCita.PROGRAMADA)
                    .fechaHora(LocalDateTime.now().plusHours(1)) // 1h < 2h
                    .build();

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            Cita cancelada = citaService.cancelar(1L);

            assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
            assertTrue(cancelada.getPenalizado());
            assertNotNull(cancelada.getFechaCancelacion());
        }

        @Test
        @DisplayName("Cancelacion justo 1 minuto antes del limite 2h -> penalizacion (borde)")
        void cancelar_CancelacionJustoAntesDe2H_AplicaPenalizacion() {
            Cita citaProgramada = Cita.builder()
                    .id(2L)
                    .estado(EstadoCita.PROGRAMADA)
                    .fechaHora(LocalDateTime.now().plusHours(1).plusMinutes(59))
                    .build();

            when(citaRepositoryPort.buscarPorId(2L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            Cita cancelada = citaService.cancelar(2L);
            assertTrue(cancelada.getPenalizado());
        }

        @Test
        @DisplayName("Cancelacion con mas de 2h de antelacion -> sin penalizacion")
        void cancelar_CancelacionATiempo_SinPenalizacion() {
            Cita citaProgramada = Cita.builder()
                    .id(3L)
                    .estado(EstadoCita.PROGRAMADA)
                    .fechaHora(LocalDateTime.now().plusHours(3))
                    .build();

            when(citaRepositoryPort.buscarPorId(3L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            Cita cancelada = citaService.cancelar(3L);

            assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
            // El servicio solo asigna penalizado=true en tardias; si es a tiempo queda null o false
            assertNotEquals(Boolean.TRUE, cancelada.getPenalizado());
        }

        @Test
        @DisplayName("Cancelacion con 24h de antelacion -> sin penalizacion")
        void cancelar_CancelacionCon24Horas_SinPenalizacion() {
            Cita citaProgramada = Cita.builder()
                    .id(6L)
                    .estado(EstadoCita.PROGRAMADA)
                    .fechaHora(LocalDateTime.now().plusHours(24))
                    .build();

            when(citaRepositoryPort.buscarPorId(6L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            Cita cancelada = citaService.cancelar(6L);
            // El servicio solo asigna penalizado=true en tardias; si es a tiempo queda null o false
            assertNotEquals(Boolean.TRUE, cancelada.getPenalizado());
        }

        @Test
        @DisplayName("Cancelar cita ya CANCELADA -> excepcion de estado")
        void cancelar_CitaYaCancelada_LanzaExcepcion() {
            Cita citaCancelada = Cita.builder()
                    .id(4L)
                    .estado(EstadoCita.CANCELADA)
                    .fechaHora(LocalDateTime.now().plusHours(5))
                    .build();

            when(citaRepositoryPort.buscarPorId(4L)).thenReturn(Optional.of(citaCancelada));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.cancelar(4L));
            assertTrue(ex.getMessage().contains("PROGRAMADA"));
        }

        @Test
        @DisplayName("Cancelar cita ATENDIDA -> excepcion de estado")
        void cancelar_CitaAtendida_LanzaExcepcion() {
            Cita citaAtendida = Cita.builder()
                    .id(5L)
                    .estado(EstadoCita.ATENDIDA)
                    .fechaHora(LocalDateTime.now().plusHours(5))
                    .build();

            when(citaRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(citaAtendida));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.cancelar(5L));
            assertTrue(ex.getMessage().contains("PROGRAMADA"));
        }

        @Test
        @DisplayName("Cancelar cita inexistente -> NotFoundException")
        void cancelar_CitaInexistente_LanzaNotFoundException() {
            when(citaRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> citaService.cancelar(999L));
        }

        @Test
        @DisplayName("Paciente con 3 penalizaciones no puede reservar -> excepcion (borde >= 3)")
        void reservar_PacienteConTresPenalizaciones_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(3L);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reservar(cita));
            assertTrue(ex.getMessage().contains("3 o más penalizaciones"));
        }

        @Test
        @DisplayName("Paciente con 4 penalizaciones no puede reservar -> excepcion")
        void reservar_PacienteConCuatroPenalizaciones_LanzaExcepcion() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(4L);

            assertThrows(BusinessRuleException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Paciente con 2 penalizaciones puede reservar -> exito (borde < 3)")
        void reservar_PacienteConDosPenalizaciones_Exito() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, fechaValida))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(2L);
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                c.setId(300L);
                return c;
            });

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
        }

        @Test
        @DisplayName("Paciente con 0 penalizaciones puede reservar -> exito")
        void reservar_PacienteSinPenalizaciones_Exito() {
            configurarReservaExitosa(fechaValida);

            Cita resultado = citaService.reservar(cita);
            assertNotNull(resultado.getId());
        }
    }

    // =========================================================================
    // RN-06: Reprogramación
    // =========================================================================
    @Nested
    @DisplayName("RN-06 - Reprogramacion de citas")
    class Reprogramacion {

        @Test
        @DisplayName("Reprogramar con 0 penalizaciones -> exito")
        void reprogramar_SinPenalizaciones_Exito() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();
            LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0); // Martes

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(eq(1L), eq(nuevaFecha)))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(eq(1L), eq(nuevaFecha)))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                if (c.getId() == null) c.setId(99L);
                return c;
            });

            Cita resultado = citaService.reprogramar(1L, nuevaFecha);

            assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
            assertEquals(nuevaFecha, resultado.getFechaHora());
            assertFalse(resultado.getPenalizado());
        }

        @Test
        @DisplayName("Reprogramar con 1 penalizacion -> exito (borde < 2)")
        void reprogramar_PacienteConUnaPenalizacion_Exito() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();
            LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0);

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(1L);
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(eq(1L), eq(nuevaFecha)))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(eq(1L), eq(nuevaFecha)))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                if (c.getId() == null) c.setId(99L);
                return c;
            });

            Cita resultado = citaService.reprogramar(1L, nuevaFecha);

            assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
            assertEquals(nuevaFecha, resultado.getFechaHora());
        }

        @Test
        @DisplayName("Reprogramar con 2 penalizaciones -> excepcion (borde >= 2 bloquea)")
        void reprogramar_PacienteConDosPenalizaciones_LanzaExcepcion() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(2L);

            LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reprogramar(1L, nuevaFecha));
            assertTrue(ex.getMessage().contains("penalizaciones en los últimos 30 días"));
            assertTrue(ex.getMessage().contains("no puede reprogramar"));
            verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        @DisplayName("Reprogramar con 3 penalizaciones -> excepcion")
        void reprogramar_PacienteConTresPenalizaciones_LanzaExcepcion() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(3L);

            assertThrows(BusinessRuleException.class,
                    () -> citaService.reprogramar(1L, LocalDateTime.of(2030, 5, 21, 10, 0)));
            verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        @DisplayName("Reprogramar cita CANCELADA -> excepcion de estado")
        void reprogramar_CitaCancelada_LanzaExcepcion() {
            Cita citaCancelada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.CANCELADA).fechaHora(fechaValida).build();

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaCancelada));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reprogramar(1L, LocalDateTime.of(2030, 5, 21, 10, 0)));
            assertTrue(ex.getMessage().contains("PROGRAMADA"));
        }

        @Test
        @DisplayName("Reprogramar a horario donde medico esta ocupado -> excepcion, cita original NO cancelada")
        void reprogramar_MedicoOcupadoEnNuevaFecha_LanzaExcepcionYNoCancela() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();
            LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0);

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, nuevaFecha))
                    .thenReturn(List.of(new Cita()));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.reprogramar(1L, nuevaFecha));
            assertTrue(ex.getMessage().contains("El médico ya tiene una cita programada"));
            verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        @DisplayName("Reprogramar a horario donde paciente esta ocupado -> excepcion, cita original NO cancelada")
        void reprogramar_PacienteOcupadoEnNuevaFecha_LanzaExcepcion() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();
            LocalDateTime nuevaFecha = LocalDateTime.of(2030, 5, 21, 10, 0);

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(1L, nuevaFecha))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(1L, nuevaFecha))
                    .thenReturn(List.of(new Cita()));

            assertThrows(BusinessRuleException.class,
                    () -> citaService.reprogramar(1L, nuevaFecha));
            verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        @DisplayName("Reprogramar a sabado despues de las 13:00 -> excepcion de franja")
        void reprogramar_NuevaFechaFueraHorarioSabado_LanzaExcepcion() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(0L);

            assertThrows(BusinessRuleException.class,
                    () -> citaService.reprogramar(1L, LocalDateTime.of(2030, 5, 25, 14, 0)));
            verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        @DisplayName("Reprogramar a domingo -> excepcion de franja")
        void reprogramar_NuevaFechaDomingo_LanzaExcepcion() {
            Cita citaProgramada = Cita.builder()
                    .id(1L).paciente(paciente).medico(medico)
                    .estado(EstadoCita.PROGRAMADA).fechaHora(fechaValida).build();

            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(0L);

            assertThrows(BusinessRuleException.class,
                    () -> citaService.reprogramar(1L, LocalDateTime.of(2030, 5, 19, 10, 0)));
            verify(citaRepositoryPort, never()).guardar(any(Cita.class));
        }

        @Test
        @DisplayName("Reprogramar cita inexistente -> NotFoundException")
        void reprogramar_CitaInexistente_LanzaNotFoundException() {
            when(citaRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> citaService.reprogramar(999L, fechaValida));
        }
    }

    // =========================================================================
    // Reservar: validaciones de entidades inexistentes
    // =========================================================================
    @Nested
    @DisplayName("Reservar - Entidades no encontradas")
    class ReservarEntidadesNoEncontradas {

        @Test
        @DisplayName("Medico inexistente -> NotFoundException")
        void reservar_MedicoInexistente_LanzaNotFoundException() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> citaService.reservar(cita));
        }

        @Test
        @DisplayName("Paciente inexistente -> NotFoundException")
        void reservar_PacienteInexistente_LanzaNotFoundException() {
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> citaService.reservar(cita));
        }
    }

    // =========================================================================
    // ObtenerPorId
    // =========================================================================
    @Nested
    @DisplayName("ObtenerPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("Cita existente -> retorna cita")
        void obtenerPorId_CitaExistente_RetornaCita() {
            Cita citaExistente = Cita.builder().id(1L).estado(EstadoCita.PROGRAMADA).build();
            when(citaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(citaExistente));

            Cita resultado = citaService.obtenerPorId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
        }

        @Test
        @DisplayName("Cita inexistente -> NotFoundException")
        void obtenerPorId_CitaInexistente_LanzaNotFoundException() {
            when(citaRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> citaService.obtenerPorId(99L));
        }
    }

    // =========================================================================
    // Eliminar
    // =========================================================================
    @Nested
    @DisplayName("Eliminar cita")
    class Eliminar {

        @Test
        @DisplayName("Eliminar cita existente -> exito")
        void eliminar_CitaExistente_Exito() {
            when(citaRepositoryPort.existePorId(1L)).thenReturn(true);
            doNothing().when(citaRepositoryPort).eliminar(1L);

            assertDoesNotThrow(() -> citaService.eliminar(1L));
            verify(citaRepositoryPort).eliminar(1L);
        }

        @Test
        @DisplayName("Eliminar cita inexistente -> NotFoundException")
        void eliminar_CitaInexistente_LanzaNotFoundException() {
            when(citaRepositoryPort.existePorId(999L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> citaService.eliminar(999L));
            verify(citaRepositoryPort, never()).eliminar(any());
        }
    }

    // =========================================================================
    // Consultar citas disponibles
    // =========================================================================
    @Nested
    @DisplayName("Consultar citas disponibles (RF-04)")
    class ConsultarCitasDisponibles {

        @Test
        @DisplayName("Rango invertido (inicio > fin) -> excepcion")
        void consultarDisponibles_RangoInvertido_LanzaExcepcion() {
            LocalDate inicio = LocalDate.of(2030, 5, 25);
            LocalDate fin = LocalDate.of(2030, 5, 20);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> citaService.consultarCitasDisponibles(1L, inicio, fin));
            assertTrue(ex.getMessage().contains("fecha de inicio no puede ser posterior"));
        }

        @Test
        @DisplayName("Rango mismo dia (inicio == fin) -> no lanza excepcion")
        void consultarDisponibles_MismoDia_NoLanzaExcepcion() {
            LocalDate fecha = LocalDate.of(2030, 5, 21); // Martes
            when(citaRepositoryPort.listarCitas(eq(1L), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> citaService.consultarCitasDisponibles(1L, fecha, fecha));
        }

        @Test
        @DisplayName("Semana laboral sin citas ocupadas -> retorna franjas disponibles (no incluye domingos)")
        void consultarDisponibles_SemanaLaboral_RetornaFranjas() {
            LocalDate lunes = LocalDate.of(2030, 5, 20);
            LocalDate viernes = LocalDate.of(2030, 5, 24);

            when(citaRepositoryPort.listarCitas(eq(1L), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<LocalDateTime> disponibles = citaService.consultarCitasDisponibles(1L, lunes, viernes);

            assertFalse(disponibles.isEmpty());
            disponibles.forEach(f -> assertNotEquals(DayOfWeek.SUNDAY, f.getDayOfWeek()));
        }

        @Test
        @DisplayName("Franja ocupada no aparece en disponibles")
        void consultarDisponibles_FranjaOcupada_NoApareceEnResultado() {
            LocalDate lunes = LocalDate.of(2030, 5, 20);
            LocalDateTime franjaOcupada = LocalDateTime.of(2030, 5, 20, 10, 0);

            Cita citaOcupada = Cita.builder().fechaHora(franjaOcupada).build();
            when(citaRepositoryPort.listarCitas(eq(1L), any(), any(), any(), any()))
                    .thenReturn(List.of(citaOcupada));

            List<LocalDateTime> disponibles = citaService.consultarCitasDisponibles(1L, lunes, lunes);

            assertFalse(disponibles.contains(franjaOcupada));
        }
    }

    // =========================================================================
    // Casos generales de reserva
    // =========================================================================
    @Nested
    @DisplayName("Reservar - Casos generales")
    class ReservarGeneral {

        @Test
        @DisplayName("Reservar: estado inicial siempre PROGRAMADA y penalizado=false")
        void reservar_EstadoInicialSiempreProgramada() {
            configurarReservaExitosa(fechaValida);

            Cita resultado = citaService.reservar(cita);

            assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
            assertFalse(resultado.getPenalizado());
            verify(citaRepositoryPort).guardar(any(Cita.class));
        }

        @Test
        @DisplayName("Reservar: segundos y nanosegundos son truncados a cero")
        void reservar_FechaHoraTruncaSegundos() {
            LocalDateTime conSegundos = LocalDateTime.of(2030, 5, 20, 10, 0, 45, 123456789);
            cita.setFechaHora(conSegundos);
            LocalDateTime truncada = conSegundos.withSecond(0).withNano(0);

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
            when(citaRepositoryPort.buscarCitasProgramadasPorMedicoYFecha(eq(1L), eq(truncada)))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.buscarCitasProgramadasPorPacienteYFecha(eq(1L), eq(truncada)))
                    .thenReturn(Collections.emptyList());
            when(citaRepositoryPort.contarPenalizacionesPorPaciente(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                c.setId(400L);
                return c;
            });

            Cita resultado = citaService.reservar(cita);

            assertEquals(0, resultado.getFechaHora().getSecond());
            assertEquals(0, resultado.getFechaHora().getNano());
        }
    }
}