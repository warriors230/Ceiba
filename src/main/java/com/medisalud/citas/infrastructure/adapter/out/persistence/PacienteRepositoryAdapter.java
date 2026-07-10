package com.medisalud.citas.infrastructure.adapter.out.persistence;

import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.domain.port.out.PacienteRepositoryPort;
import com.medisalud.citas.infrastructure.adapter.out.persistence.mapper.PacienteEntityMapper;
import com.medisalud.citas.infrastructure.adapter.out.persistence.repository.PacienteJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PacienteRepositoryAdapter implements PacienteRepositoryPort {
    private final PacienteJpaRepository pacienteJpaRepository;
    private final PacienteEntityMapper pacienteEntityMapper;

    @Override
    public Paciente guardar(Paciente paciente) {
        var entity = pacienteEntityMapper.toEntity(paciente);
        var saved = pacienteJpaRepository.save(entity);
        return pacienteEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Paciente> buscarPorId(Long id) {
        return pacienteJpaRepository.findById(id)
                .map(pacienteEntityMapper::toDomain);
    }

    @Override
    public List<Paciente> listarTodos() {
        return pacienteEntityMapper.toDomainList(pacienteJpaRepository.findAll());
    }

    @Override
    public Optional<Paciente> buscarPorDocumento(String documento) {
        return pacienteJpaRepository.findByDocumento(documento)
                .map(pacienteEntityMapper::toDomain);
    }

    @Override
    public boolean existePorDocumento(String documento) {
        return pacienteJpaRepository.existsByDocumento(documento);
    }

    @Override
    public void eliminar(Long id) {
        pacienteJpaRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return pacienteJpaRepository.existsById(id);
    }
}
