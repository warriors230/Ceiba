package com.medisalud.citas.infrastructure.adapter.out.persistence.repository;

import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.PacienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PacienteJpaRepository extends JpaRepository<PacienteEntity, Long> {
    boolean existsByDocumento(String documento);

    Optional<PacienteEntity> findByDocumento(String documento);
}
