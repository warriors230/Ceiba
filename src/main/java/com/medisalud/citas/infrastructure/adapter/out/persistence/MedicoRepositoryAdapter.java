	package com.medisalud.citas.infrastructure.adapter.out.persistence;

import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.domain.port.out.MedicoRepositoryPort;
import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.MedicoEntity;
import com.medisalud.citas.infrastructure.adapter.out.persistence.mapper.MedicoEntityMapper;
import com.medisalud.citas.infrastructure.adapter.out.persistence.repository.MedicoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicoRepositoryAdapter implements MedicoRepositoryPort {
    private final MedicoJpaRepository medicoJpaRepository;
    private final MedicoEntityMapper medicoEntityMapper;

    @Override
    public Medico guardar(Medico medico) {
        MedicoEntity entity = medicoEntityMapper.toEntity(medico);
        MedicoEntity saved = medicoJpaRepository.save(entity);
        return medicoEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Medico> buscarPorId(Long id) {
        return medicoJpaRepository.findById(id)
                .map(medicoEntityMapper::toDomain);
    }

    @Override
    public List<Medico> listarTodos() {
        return medicoEntityMapper.toDomainList(medicoJpaRepository.findAll());
    }

    @Override
    public boolean existePorEmail(String email) {
        return medicoJpaRepository.existsByEmail(email);
    }

    @Override
    public void eliminar(Long id) {
        medicoJpaRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return medicoJpaRepository.existsById(id);
    }
}
