package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.domain.port.in.PacienteUseCase;
import com.medisalud.citas.domain.port.out.PacienteRepositoryPort;
import com.medisalud.citas.domain.util.Utils;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.ConflictException;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de dominio para la gestión de pacientes.
 * Implementa el use case y aplica las validaciones de unicidad del documento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PacienteService implements PacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final MessageService messages;

    @Override
    public Paciente registrar(Paciente paciente) {
        paciente.setNombreCompleto(Utils.sanitizar(paciente.getNombreCompleto()));

        log.info("Registrando nuevo paciente: {}", paciente.getNombreCompleto());

        // RN: El documento de identidad debe ser único en el sistema
        if (pacienteRepositoryPort.existePorDocumento(paciente.getDocumento())) {
            throw new ConflictException(
                    messages.get("error.paciente.documento.duplicado", paciente.getDocumento()));
        }

        Paciente guardado = pacienteRepositoryPort.guardar(paciente);
        log.info("Paciente registrado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Paciente obtenerPorId(Long id) {
        log.debug("Buscando paciente con ID: {}", id);
        return pacienteRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Paciente", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Paciente> listarTodos() {
        log.debug("Listando todos los pacientes");
        return pacienteRepositoryPort.listarTodos();
    }

    @Override
    public Paciente actualizar(Long id, Paciente paciente) {
        log.info("Actualizando paciente con ID: {}", id);

        // Verificar que el paciente existe
        Paciente existente = pacienteRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Paciente", id));

        // Validar conflicto de documento si se está cambiando
        if (!paciente.getDocumento().equals(existente.getDocumento())
                && pacienteRepositoryPort.existePorDocumento(paciente.getDocumento())) {
            throw new ConflictException(
                    messages.get("error.paciente.documento.duplicado", paciente.getDocumento()));
        }

        // Actualizar campos (sanitizados)
        existente.setNombreCompleto(Utils.sanitizar(paciente.getNombreCompleto()));
        existente.setDocumento(paciente.getDocumento());
        existente.setTelefono(paciente.getTelefono());
        existente.setEmail(paciente.getEmail());
        existente.setFechaNacimiento(paciente.getFechaNacimiento());

        return pacienteRepositoryPort.guardar(existente);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando paciente con ID: {}", id);
        if (!pacienteRepositoryPort.existePorId(id)) {
            throw new NotFoundException("Paciente", id);
        }
        pacienteRepositoryPort.eliminar(id);
    }
}
