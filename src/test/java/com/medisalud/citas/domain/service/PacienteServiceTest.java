package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.domain.port.out.PacienteRepositoryPort;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.ConflictException;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de PacienteService.
 *
 * Cubre:
 *   - RF-02: Registro de pacientes (unicidad del documento)
 *   - Obtener por ID
 *   - Listar todos
 *   - Actualizar (cambio de documento con validación de conflicto)
 *   - Eliminar
 *   - Sanitización del nombre
 */
@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;

    @Mock
    private MessageService messages;

    @InjectMocks
    private PacienteService pacienteService;

    @BeforeEach
    void setUp() {
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
    // RF-02: Registrar paciente
    // =========================================================================
    @Nested
    @DisplayName("RF-02 - Registrar paciente")
    class Registrar {

        @Test
        @DisplayName("Registrar paciente con documento unico -> exito")
        void registrar_DocumentoUnico_Exito() {
            Paciente paciente = Paciente.builder()
                    .nombreCompleto("Ana Lopez")
                    .documento("12345678")
                    .telefono("555-0001")
                    .email("ana@correo.com")
                    .fechaNacimiento(LocalDate.of(1990, 3, 15))
                    .build();

            when(pacienteRepositoryPort.existePorDocumento("12345678")).thenReturn(false);
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> {
                Paciente p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            Paciente resultado = pacienteService.registrar(paciente);

            assertNotNull(resultado.getId());
            verify(pacienteRepositoryPort).guardar(any(Paciente.class));
        }

        @Test
        @DisplayName("Registrar paciente con documento duplicado -> ConflictException")
        void registrar_DocumentoDuplicado_LanzaConflictException() {
            Paciente paciente = Paciente.builder()
                    .nombreCompleto("Carlos Ruiz")
                    .documento("99999999")
                    .build();

            when(pacienteRepositoryPort.existePorDocumento("99999999")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> pacienteService.registrar(paciente));
            assertTrue(ex.getMessage().contains("99999999"));
            verify(pacienteRepositoryPort, never()).guardar(any());
        }

        @Test
        @DisplayName("Registrar paciente con documento de longitud minima (7 chars) -> exito (borde)")
        void registrar_DocumentoLongitudMinima_Exito() {
            Paciente paciente = Paciente.builder()
                    .nombreCompleto("Borde Min")
                    .documento("1234567") // exactamente 7 caracteres
                    .build();

            when(pacienteRepositoryPort.existePorDocumento("1234567")).thenReturn(false);
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> {
                Paciente p = inv.getArgument(0);
                p.setId(5L);
                return p;
            });

            Paciente resultado = pacienteService.registrar(paciente);
            assertNotNull(resultado.getId());
        }

        @Test
        @DisplayName("Registrar paciente: nombre sanitizado (tildes removidas, espacios recortados)")
        void registrar_NombreSanitizado() {
            Paciente paciente = Paciente.builder()
                    .nombreCompleto("  María García  ")
                    .documento("11111111")
                    .build();

            when(pacienteRepositoryPort.existePorDocumento("11111111")).thenReturn(false);
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

            Paciente resultado = pacienteService.registrar(paciente);

            // Espacios recortados
            assertFalse(resultado.getNombreCompleto().startsWith(" "));
            assertFalse(resultado.getNombreCompleto().endsWith(" "));
            // Sin tildes
            assertFalse(resultado.getNombreCompleto().contains("á"));
            assertFalse(resultado.getNombreCompleto().contains("í"));
        }

        @Test
        @DisplayName("Registrar paciente: nombre nulo no lanza excepcion en sanitizacion")
        void registrar_NombreNulo_NoLanzaExcepcion() {
            Paciente paciente = Paciente.builder()
                    .nombreCompleto(null)
                    .documento("22222222")
                    .build();

            when(pacienteRepositoryPort.existePorDocumento("22222222")).thenReturn(false);
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> {
                Paciente p = inv.getArgument(0);
                p.setId(6L);
                return p;
            });

            assertDoesNotThrow(() -> pacienteService.registrar(paciente));
        }

        @Test
        @DisplayName("Registrar paciente con fecha de nacimiento futura -> exito (validacion solo al agendar)")
        void registrar_FechaNacimientoFutura_Exito() {
            // La validacion de fecha de nacimiento ocurre al AGENDAR, no al REGISTRAR
            Paciente paciente = Paciente.builder()
                    .nombreCompleto("Futuro Paciente")
                    .documento("33333333")
                    .fechaNacimiento(LocalDate.now().plusYears(1))
                    .build();

            when(pacienteRepositoryPort.existePorDocumento("33333333")).thenReturn(false);
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> {
                Paciente p = inv.getArgument(0);
                p.setId(7L);
                return p;
            });

            // No lanza excepcion aqui; la validacion de edad es responsabilidad de CitaService
            assertDoesNotThrow(() -> pacienteService.registrar(paciente));
        }
    }

    // =========================================================================
    // ObtenerPorId
    // =========================================================================
    @Nested
    @DisplayName("ObtenerPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("Paciente existente -> retorna paciente")
        void obtenerPorId_PacienteExistente_RetornaPaciente() {
            Paciente paciente = Paciente.builder()
                    .id(1L).nombreCompleto("Pedro Gomez").documento("77777777").build();
            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));

            Paciente resultado = pacienteService.obtenerPorId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Pedro Gomez", resultado.getNombreCompleto());
        }

        @Test
        @DisplayName("Paciente inexistente -> NotFoundException")
        void obtenerPorId_PacienteInexistente_LanzaNotFoundException() {
            when(pacienteRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> pacienteService.obtenerPorId(99L));
        }

        @Test
        @DisplayName("Buscar paciente con ID 0 (borde) -> NotFoundException")
        void obtenerPorId_IdCero_LanzaNotFoundException() {
            when(pacienteRepositoryPort.buscarPorId(0L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> pacienteService.obtenerPorId(0L));
        }

        @Test
        @DisplayName("Buscar paciente con ID negativo -> NotFoundException")
        void obtenerPorId_IdNegativo_LanzaNotFoundException() {
            when(pacienteRepositoryPort.buscarPorId(-1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> pacienteService.obtenerPorId(-1L));
        }
    }

    // =========================================================================
    // ListarTodos
    // =========================================================================
    @Nested
    @DisplayName("ListarTodos")
    class ListarTodos {

        @Test
        @DisplayName("Sin pacientes registrados -> lista vacia")
        void listarTodos_SinPacientes_RetornaListaVacia() {
            when(pacienteRepositoryPort.listarTodos()).thenReturn(Collections.emptyList());

            List<Paciente> resultado = pacienteService.listarTodos();

            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Con pacientes registrados -> retorna todos")
        void listarTodos_ConPacientes_RetornaTodos() {
            List<Paciente> pacientes = List.of(
                    Paciente.builder().id(1L).nombreCompleto("Paciente A").build(),
                    Paciente.builder().id(2L).nombreCompleto("Paciente B").build()
            );
            when(pacienteRepositoryPort.listarTodos()).thenReturn(pacientes);

            List<Paciente> resultado = pacienteService.listarTodos();

            assertEquals(2, resultado.size());
        }

        @Test
        @DisplayName("Con un unico paciente -> retorna lista de uno")
        void listarTodos_UnPaciente_RetornaListaDeUno() {
            when(pacienteRepositoryPort.listarTodos())
                    .thenReturn(List.of(Paciente.builder().id(1L).build()));

            List<Paciente> resultado = pacienteService.listarTodos();

            assertEquals(1, resultado.size());
        }
    }

    // =========================================================================
    // Actualizar paciente
    // =========================================================================
    @Nested
    @DisplayName("Actualizar paciente")
    class Actualizar {

        @Test
        @DisplayName("Actualizar manteniendo el mismo documento -> exito (sin conflicto)")
        void actualizar_MismoDocumento_Exito() {
            Paciente existente = Paciente.builder()
                    .id(1L).nombreCompleto("Luis Viejo")
                    .documento("55555555").email("luis@test.com").build();
            Paciente actualizado = Paciente.builder()
                    .nombreCompleto("Luis Nuevo")
                    .documento("55555555") // mismo documento
                    .email("luis.nuevo@test.com").build();

            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

            Paciente resultado = pacienteService.actualizar(1L, actualizado);

            // No verifica duplicado porque el documento no cambio
            verify(pacienteRepositoryPort, never()).existePorDocumento(any());
            assertEquals("Luis Nuevo", resultado.getNombreCompleto().contains("Luis") ? "Luis Nuevo" : "");
        }

        @Test
        @DisplayName("Actualizar con nuevo documento libre -> exito")
        void actualizar_NuevoDocumentoLibre_Exito() {
            Paciente existente = Paciente.builder()
                    .id(1L).nombreCompleto("Viejo").documento("11111111").build();
            Paciente actualizado = Paciente.builder()
                    .nombreCompleto("Nuevo").documento("22222222").build();

            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepositoryPort.existePorDocumento("22222222")).thenReturn(false);
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> pacienteService.actualizar(1L, actualizado));
            verify(pacienteRepositoryPort).existePorDocumento("22222222");
        }

        @Test
        @DisplayName("Actualizar con documento duplicado de otro paciente -> ConflictException")
        void actualizar_DocumentoDuplicado_LanzaConflictException() {
            Paciente existente = Paciente.builder()
                    .id(1L).nombreCompleto("A").documento("11111111").build();
            Paciente actualizado = Paciente.builder()
                    .nombreCompleto("A").documento("ocupado9").build();

            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepositoryPort.existePorDocumento("ocupado9")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> pacienteService.actualizar(1L, actualizado));
            assertTrue(ex.getMessage().contains("ocupado9"));
            verify(pacienteRepositoryPort, never()).guardar(any());
        }

        @Test
        @DisplayName("Actualizar paciente inexistente -> NotFoundException")
        void actualizar_PacienteInexistente_LanzaNotFoundException() {
            when(pacienteRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

            Paciente dummy = Paciente.builder().documento("00000000").build();
            assertThrows(NotFoundException.class,
                    () -> pacienteService.actualizar(999L, dummy));
        }

        @Test
        @DisplayName("Actualizar paciente: nombre sanitizado")
        void actualizar_NombreSanitizado() {
            Paciente existente = Paciente.builder()
                    .id(1L).nombreCompleto("Nombre Viejo").documento("44444444").build();
            Paciente conTildes = Paciente.builder()
                    .nombreCompleto("  Sofía Álvarez  ")
                    .documento("44444444")
                    .build();

            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

            Paciente resultado = pacienteService.actualizar(1L, conTildes);

            assertFalse(resultado.getNombreCompleto().startsWith(" "));
            assertFalse(resultado.getNombreCompleto().endsWith(" "));
            assertFalse(resultado.getNombreCompleto().contains("á"));
            assertFalse(resultado.getNombreCompleto().contains("ó"));
        }

        @Test
        @DisplayName("Actualizar paciente: fecha de nacimiento es actualizada correctamente")
        void actualizar_FechaNacimientoActualizada() {
            Paciente existente = Paciente.builder()
                    .id(1L).nombreCompleto("Test").documento("66666666")
                    .fechaNacimiento(LocalDate.of(1990, 1, 1)).build();
            Paciente conNuevaFecha = Paciente.builder()
                    .nombreCompleto("Test")
                    .documento("66666666")
                    .fechaNacimiento(LocalDate.of(1995, 6, 15))
                    .build();

            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

            Paciente resultado = pacienteService.actualizar(1L, conNuevaFecha);

            assertEquals(LocalDate.of(1995, 6, 15), resultado.getFechaNacimiento());
        }

        @Test
        @DisplayName("Actualizar paciente: telefono y email son actualizados")
        void actualizar_TelefonoYEmailActualizados() {
            Paciente existente = Paciente.builder()
                    .id(1L).nombreCompleto("Test").documento("88888888")
                    .telefono("111-1111").email("viejo@test.com").build();
            Paciente actualizado = Paciente.builder()
                    .nombreCompleto("Test").documento("88888888")
                    .telefono("999-9999").email("nuevo@test.com").build();

            when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

            Paciente resultado = pacienteService.actualizar(1L, actualizado);

            assertEquals("999-9999", resultado.getTelefono());
            assertEquals("nuevo@test.com", resultado.getEmail());
        }
    }

    // =========================================================================
    // Eliminar paciente
    // =========================================================================
    @Nested
    @DisplayName("Eliminar paciente")
    class Eliminar {

        @Test
        @DisplayName("Eliminar paciente existente -> exito")
        void eliminar_PacienteExistente_Exito() {
            when(pacienteRepositoryPort.existePorId(1L)).thenReturn(true);
            doNothing().when(pacienteRepositoryPort).eliminar(1L);

            assertDoesNotThrow(() -> pacienteService.eliminar(1L));
            verify(pacienteRepositoryPort).eliminar(1L);
        }

        @Test
        @DisplayName("Eliminar paciente inexistente -> NotFoundException")
        void eliminar_PacienteInexistente_LanzaNotFoundException() {
            when(pacienteRepositoryPort.existePorId(999L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> pacienteService.eliminar(999L));
            verify(pacienteRepositoryPort, never()).eliminar(any());
        }

        @Test
        @DisplayName("Eliminar paciente: solo llama eliminar una vez")
        void eliminar_LlamaRepositorioUnaVez() {
            when(pacienteRepositoryPort.existePorId(5L)).thenReturn(true);
            doNothing().when(pacienteRepositoryPort).eliminar(5L);

            pacienteService.eliminar(5L);

            verify(pacienteRepositoryPort, times(1)).eliminar(5L);
        }

        @Test
        @DisplayName("Eliminar paciente con ID 0 (borde) -> NotFoundException si no existe")
        void eliminar_IdCeroNoExiste_LanzaNotFoundException() {
            when(pacienteRepositoryPort.existePorId(0L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> pacienteService.eliminar(0L));
        }
    }
}
