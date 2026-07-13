package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.domain.port.out.MedicoRepositoryPort;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de MedicoService.
 *
 * Cubre:
 *   - RF-01: Registro de médicos (unicidad de email)
 *   - Obtener por ID
 *   - Listar todos
 *   - Actualizar (cambio de email con validación de conflicto)
 *   - Eliminar
 *   - Sanitización del nombre y especialidad
 */
@ExtendWith(MockitoExtension.class)
class MedicoServiceTest {

    @Mock
    private MedicoRepositoryPort medicoRepositoryPort;

    @Mock
    private MessageService messages;

    @InjectMocks
    private MedicoService medicoService;

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
    // RF-01: Registrar médico
    // =========================================================================
    @Nested
    @DisplayName("RF-01 - Registrar medico")
    class Registrar {

        @Test
        @DisplayName("Registrar medico con email unico -> exito")
        void registrar_EmailUnico_Exito() {
            Medico medico = Medico.builder()
                    .nombreCompleto("Dr. Juan Perez")
                    .especialidad("Cardiologia")
                    .email("juan@medisalud.com")
                    .telefono("555-1234")
                    .build();

            when(medicoRepositoryPort.existePorEmail("juan@medisalud.com")).thenReturn(false);
            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> {
                Medico m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            Medico resultado = medicoService.registrar(medico);

            assertNotNull(resultado.getId());
            verify(medicoRepositoryPort).guardar(any(Medico.class));
        }

        @Test
        @DisplayName("Registrar medico con email duplicado -> ConflictException")
        void registrar_EmailDuplicado_LanzaConflictException() {
            Medico medico = Medico.builder()
                    .nombreCompleto("Dr. Juan Perez")
                    .especialidad("Cardiologia")
                    .email("existente@medisalud.com")
                    .build();

            when(medicoRepositoryPort.existePorEmail("existente@medisalud.com")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> medicoService.registrar(medico));
            assertTrue(ex.getMessage().contains("existente@medisalud.com"));
            verify(medicoRepositoryPort, never()).guardar(any());
        }

        @Test
        @DisplayName("Registrar medico sin email (null) -> exito (email opcional)")
        void registrar_SinEmail_Exito() {
            Medico medico = Medico.builder()
                    .nombreCompleto("Dr. Sin Email")
                    .especialidad("Pediatria")
                    .email(null)
                    .build();

            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> {
                Medico m = inv.getArgument(0);
                m.setId(2L);
                return m;
            });

            Medico resultado = medicoService.registrar(medico);

            assertNotNull(resultado.getId());
            // No debe verificar email si es null
            verify(medicoRepositoryPort, never()).existePorEmail(any());
        }

        @Test
        @DisplayName("Registrar medico con email en blanco -> exito (no valida duplicado)")
        void registrar_EmailEnBlanco_Exito() {
            Medico medico = Medico.builder()
                    .nombreCompleto("Dr. Blanco")
                    .especialidad("Dermatologia")
                    .email("   ")
                    .build();

            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> {
                Medico m = inv.getArgument(0);
                m.setId(3L);
                return m;
            });

            Medico resultado = medicoService.registrar(medico);

            assertNotNull(resultado.getId());
            verify(medicoRepositoryPort, never()).existePorEmail(any());
        }

        @Test
        @DisplayName("Registrar medico: nombre sanitizado (tildes removidas, espacios recortados)")
        void registrar_NombreSanitizado() {
            Medico medico = Medico.builder()
                    .nombreCompleto("  Dra. María López  ")
                    .especialidad("  Cardiología  ")
                    .email(null)
                    .build();

            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

            Medico resultado = medicoService.registrar(medico);

            // Tildes removidas y espacios recortados
            assertFalse(resultado.getNombreCompleto().startsWith(" "));
            assertFalse(resultado.getNombreCompleto().endsWith(" "));
            assertFalse(resultado.getEspecialidad().startsWith(" "));
            assertFalse(resultado.getEspecialidad().endsWith(" "));
            // Sin tildes
            assertFalse(resultado.getNombreCompleto().contains("á"));
            assertFalse(resultado.getNombreCompleto().contains("é"));
            assertFalse(resultado.getEspecialidad().contains("ó"));
        }

        @Test
        @DisplayName("Registrar medico con especialidad nula -> no lanza excepcion (campo sanitizable)")
        void registrar_EspecialidadNula_NoLanzaExcepcion() {
            Medico medico = Medico.builder()
                    .nombreCompleto("Dr. Null")
                    .especialidad(null)
                    .email(null)
                    .build();

            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> {
                Medico m = inv.getArgument(0);
                m.setId(10L);
                return m;
            });

            assertDoesNotThrow(() -> medicoService.registrar(medico));
        }
    }

    // =========================================================================
    // ObtenerPorId
    // =========================================================================
    @Nested
    @DisplayName("ObtenerPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("Medico existente -> retorna medico")
        void obtenerPorId_MedicoExistente_RetornaMedico() {
            Medico medico = Medico.builder().id(1L).nombreCompleto("Dr. Test").build();
            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(medico));

            Medico resultado = medicoService.obtenerPorId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Dr. Test", resultado.getNombreCompleto());
        }

        @Test
        @DisplayName("Medico inexistente -> NotFoundException")
        void obtenerPorId_MedicoInexistente_LanzaNotFoundException() {
            when(medicoRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> medicoService.obtenerPorId(99L));
        }

        @Test
        @DisplayName("Buscar medico con ID 0 (borde) -> NotFoundException")
        void obtenerPorId_IdCero_LanzaNotFoundException() {
            when(medicoRepositoryPort.buscarPorId(0L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> medicoService.obtenerPorId(0L));
        }
    }

    // =========================================================================
    // ListarTodos
    // =========================================================================
    @Nested
    @DisplayName("ListarTodos")
    class ListarTodos {

        @Test
        @DisplayName("Sin medicos registrados -> lista vacia")
        void listarTodos_SinMedicos_RetornaListaVacia() {
            when(medicoRepositoryPort.listarTodos()).thenReturn(Collections.emptyList());

            List<Medico> resultado = medicoService.listarTodos();

            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Con medicos registrados -> retorna todos")
        void listarTodos_ConMedicos_RetornaTodos() {
            List<Medico> medicos = List.of(
                    Medico.builder().id(1L).nombreCompleto("Dr. A").build(),
                    Medico.builder().id(2L).nombreCompleto("Dra. B").build(),
                    Medico.builder().id(3L).nombreCompleto("Dr. C").build()
            );
            when(medicoRepositoryPort.listarTodos()).thenReturn(medicos);

            List<Medico> resultado = medicoService.listarTodos();

            assertEquals(3, resultado.size());
        }
    }

    // =========================================================================
    // Actualizar médico
    // =========================================================================
    @Nested
    @DisplayName("Actualizar medico")
    class Actualizar {

        @Test
        @DisplayName("Actualizar medico con mismo email -> exito (sin conflicto)")
        void actualizar_MismoEmail_Exito() {
            Medico existente = Medico.builder()
                    .id(1L).nombreCompleto("Dr. Viejo").especialidad("Vieja")
                    .email("mismo@medisalud.com").build();
            Medico actualizado = Medico.builder()
                    .nombreCompleto("Dr. Nuevo").especialidad("Nueva")
                    .email("mismo@medisalud.com").build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

            Medico resultado = medicoService.actualizar(1L, actualizado);

            // No verifica unicidad porque el email no cambio
            verify(medicoRepositoryPort, never()).existePorEmail(any());
            assertNotNull(resultado);
        }

        @Test
        @DisplayName("Actualizar medico cambiando a email libre -> exito")
        void actualizar_NuevoEmailLibre_Exito() {
            Medico existente = Medico.builder()
                    .id(1L).nombreCompleto("Dr. Viejo").especialidad("Vieja")
                    .email("viejo@medisalud.com").build();
            Medico actualizado = Medico.builder()
                    .nombreCompleto("Dr. Nuevo").especialidad("Nueva")
                    .email("nuevo@medisalud.com").build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(medicoRepositoryPort.existePorEmail("nuevo@medisalud.com")).thenReturn(false);
            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

            Medico resultado = medicoService.actualizar(1L, actualizado);

            assertNotNull(resultado);
            verify(medicoRepositoryPort).existePorEmail("nuevo@medisalud.com");
        }

        @Test
        @DisplayName("Actualizar medico cambiando a email ocupado -> ConflictException")
        void actualizar_NuevoEmailOcupado_LanzaConflictException() {
            Medico existente = Medico.builder()
                    .id(1L).nombreCompleto("Dr. A").especialidad("X")
                    .email("original@medisalud.com").build();
            Medico actualizado = Medico.builder()
                    .nombreCompleto("Dr. A").especialidad("X")
                    .email("ocupado@medisalud.com").build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(medicoRepositoryPort.existePorEmail("ocupado@medisalud.com")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> medicoService.actualizar(1L, actualizado));
            assertTrue(ex.getMessage().contains("ocupado@medisalud.com"));
            verify(medicoRepositoryPort, never()).guardar(any());
        }

        @Test
        @DisplayName("Actualizar medico inexistente -> NotFoundException")
        void actualizar_MedicoInexistente_LanzaNotFoundException() {
            when(medicoRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> medicoService.actualizar(999L, Medico.builder().build()));
        }

        @Test
        @DisplayName("Actualizar medico: campos sanitizados (tildes y espacios)")
        void actualizar_CamposSanitizados() {
            Medico existente = Medico.builder()
                    .id(1L).nombreCompleto("Dr. Old").especialidad("Old")
                    .email("old@test.com").build();
            Medico conTildes = Medico.builder()
                    .nombreCompleto("  Dra. María  ")
                    .especialidad("  Cardiología  ")
                    .email("old@test.com")
                    .build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

            Medico resultado = medicoService.actualizar(1L, conTildes);

            assertFalse(resultado.getNombreCompleto().startsWith(" "));
            assertFalse(resultado.getNombreCompleto().endsWith(" "));
            assertFalse(resultado.getNombreCompleto().contains("á"));
        }

        @Test
        @DisplayName("Actualizar medico: email con diferente case es conflicto (case-insensitive)")
        void actualizar_MismoEmailDiferenteCase_NoGeneraConflicto() {
            // El mismo email pero en diferente case = mismo médico, no conflicto
            Medico existente = Medico.builder()
                    .id(1L).nombreCompleto("Dr. A").especialidad("X")
                    .email("Juan@MediSalud.COM").build();
            Medico actualizado = Medico.builder()
                    .nombreCompleto("Dr. A Nuevo").especialidad("X")
                    .email("juan@medisalud.com") // mismo email, minúsculas
                    .build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

            // No debe lanzar excepción (equalsIgnoreCase detecta que es el mismo email)
            assertDoesNotThrow(() -> medicoService.actualizar(1L, actualizado));
            verify(medicoRepositoryPort, never()).existePorEmail(any());
        }

        @Test
        @DisplayName("Actualizar medico: email null no verifica conflicto")
        void actualizar_EmailNulo_NoVerificaConflicto() {
            Medico existente = Medico.builder()
                    .id(1L).nombreCompleto("Dr. A").especialidad("X")
                    .email("old@test.com").build();
            Medico actualizado = Medico.builder()
                    .nombreCompleto("Dr. A").especialidad("X")
                    .email(null).build();

            when(medicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(medicoRepositoryPort.guardar(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> medicoService.actualizar(1L, actualizado));
            verify(medicoRepositoryPort, never()).existePorEmail(any());
        }
    }

    // =========================================================================
    // Eliminar médico
    // =========================================================================
    @Nested
    @DisplayName("Eliminar medico")
    class Eliminar {

        @Test
        @DisplayName("Eliminar medico existente -> exito")
        void eliminar_MedicoExistente_Exito() {
            when(medicoRepositoryPort.existePorId(1L)).thenReturn(true);
            doNothing().when(medicoRepositoryPort).eliminar(1L);

            assertDoesNotThrow(() -> medicoService.eliminar(1L));
            verify(medicoRepositoryPort).eliminar(1L);
        }

        @Test
        @DisplayName("Eliminar medico inexistente -> NotFoundException")
        void eliminar_MedicoInexistente_LanzaNotFoundException() {
            when(medicoRepositoryPort.existePorId(999L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> medicoService.eliminar(999L));
            verify(medicoRepositoryPort, never()).eliminar(any());
        }

        @Test
        @DisplayName("Eliminar medico: solo llama eliminar una vez")
        void eliminar_LlamaRepositorioUnaVez() {
            when(medicoRepositoryPort.existePorId(5L)).thenReturn(true);
            doNothing().when(medicoRepositoryPort).eliminar(5L);

            medicoService.eliminar(5L);

            verify(medicoRepositoryPort, times(1)).eliminar(5L);
        }
    }
}
