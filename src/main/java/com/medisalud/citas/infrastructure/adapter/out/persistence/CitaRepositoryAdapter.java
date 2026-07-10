package com.medisalud.citas.infrastructure.adapter.out.persistence;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.domain.port.out.CitaRepositoryPort;
import com.medisalud.citas.infrastructure.adapter.out.persistence.mapper.CitaEntityMapper;
import com.medisalud.citas.infrastructure.adapter.out.persistence.repository.CitaJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CitaRepositoryAdapter implements CitaRepositoryPort {
    private final CitaJpaRepository citaJpaRepository;
    private final CitaEntityMapper citaEntityMapper;

    @Override
    public Cita guardar(Cita cita) {
        var entity = citaEntityMapper.toEntity(cita);
        var saved = citaJpaRepository.save(entity);
        return citaEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Cita> buscarPorId(Long id) {
        return citaJpaRepository.findById(id)
                .map(citaEntityMapper::toDomain);
    }

    @Override
    public List<Cita> findByParameters(Long medicoId, Long pacienteId, EstadoCita estado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return citaEntityMapper.toDomainList(
                citaJpaRepository.findByParameters(medicoId, pacienteId, estado, fechaInicio, fechaFin));
    }

    @Override
    public List<Cita> buscarCitasProgramadasPorMedicoYFecha(Long medicoId, LocalDateTime fechaHora) {
        return citaEntityMapper.toDomainList(
                citaJpaRepository.findCitasProgramadasByMedicoAndFecha(medicoId, fechaHora));
    }

    @Override
    public List<Cita> buscarCitasProgramadasPorPacienteYFecha(Long pacienteId, LocalDateTime fechaHora) {
        return citaEntityMapper.toDomainList(
                citaJpaRepository.findCitasProgramadasByPacienteAndFecha(pacienteId, fechaHora));
    }

    @Override
    public long contarPenalizacionesPorPaciente(Long pacienteId, LocalDateTime desde) {
        return citaJpaRepository.countPenalizacionesByPacienteId(pacienteId, desde);
    }

    @Override
    public void eliminar(Long id) {
        citaJpaRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return citaJpaRepository.existsById(id);
    }
}
