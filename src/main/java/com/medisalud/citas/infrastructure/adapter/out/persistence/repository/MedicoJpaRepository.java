package com.medisalud.citas.infrastructure.adapter.out.persistence.repository;

import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.MedicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MedicoJpaRepository extends JpaRepository<MedicoEntity, Long> {
    boolean existsByEmail(String email);

    Optional<MedicoEntity> findByEmail(String email);
}
