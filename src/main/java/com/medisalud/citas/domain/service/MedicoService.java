package com.medisalud.citas.domain.service;

import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.domain.port.in.MedicoUseCase;
import com.medisalud.citas.domain.port.out.MedicoRepositoryPort;
import com.medisalud.citas.domain.util.Utils;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.ConflictException;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MedicoService implements MedicoUseCase {

    private final MedicoRepositoryPort medicoRepositoryPort;

    @Override
    public Medico registrar(Medico medico) {
        medico.setNombreCompleto(Utils.sanitizar(medico.getNombreCompleto()));
        medico.setEspecialidad(Utils.sanitizar(medico.getEspecialidad()));

        log.info("Registrando nuevo médico: {}", medico.getNombreCompleto());

        // Validar que no exista otro médico con el mismo email
        if (medico.getEmail() != null && !medico.getEmail().isBlank()
                && medicoRepositoryPort.existePorEmail(medico.getEmail())) {
            throw new ConflictException(
                    "Ya existe un médico registrado con el email: " + medico.getEmail());
        }

        Medico guardado = medicoRepositoryPort.guardar(medico);
        log.info("Médico registrado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Medico obtenerPorId(Long id) {
        log.debug("Buscando médico con ID: {}", id);
        return medicoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Médico", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medico> listarTodos() {
        log.debug("Listando todos los médicos");
        return medicoRepositoryPort.listarTodos();
    }

    @Override
    public Medico actualizar(Long id, Medico medico) {
        log.info("Actualizando médico con ID: {}", id);

        // Verificar que el médico existe
        Medico existente = medicoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Médico", id));

        // Validar conflicto de email si se está cambiando
        if (medico.getEmail() != null && !medico.getEmail().isBlank()
                && !medico.getEmail().equalsIgnoreCase(existente.getEmail())
                && medicoRepositoryPort.existePorEmail(medico.getEmail())) {
            throw new ConflictException(
                    "Ya existe un médico registrado con el email: " + medico.getEmail());
        }

        // Actualizar campos (sanitizados)
        existente.setNombreCompleto(Utils.sanitizar(medico.getNombreCompleto()));
        existente.setEspecialidad(Utils.sanitizar(medico.getEspecialidad()));
        existente.setTelefono(medico.getTelefono());
        existente.setEmail(medico.getEmail());

        return medicoRepositoryPort.guardar(existente);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando médico con ID: {}", id);
        if (!medicoRepositoryPort.existePorId(id)) {
            throw new NotFoundException("Médico", id);
        }
        medicoRepositoryPort.eliminar(id);
    }
}
