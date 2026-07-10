package com.medisalud.citas.infrastructure.adapter.out.persistence.repository;

import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.CitaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaJpaRepository extends JpaRepository<CitaEntity, Long>,
        JpaSpecificationExecutor<CitaEntity> {

       @Query("SELECT c FROM CitaEntity c WHERE c.medico.id = :medicoId " +
                      "AND c.fechaHora >= :inicio AND c.fechaHora < :fin " +
                      "AND c.estado = 'PROGRAMADA'")
       List<CitaEntity> findCitasProgramadasByMedicoAndFecha(
                      @Param("medicoId") Long medicoId,
                      @Param("inicio") LocalDateTime inicio,
                      @Param("fin") LocalDateTime fin);

       @Query("SELECT c FROM CitaEntity c WHERE c.paciente.id = :pacienteId " +
                      "AND c.fechaHora >= :inicio AND c.fechaHora < :fin " +
                      "AND c.estado = 'PROGRAMADA'")
       List<CitaEntity> findCitasProgramadasByPacienteAndFecha(
                      @Param("pacienteId") Long pacienteId,
                      @Param("inicio") LocalDateTime inicio,
                      @Param("fin") LocalDateTime fin);

       @Query("SELECT COUNT(c) FROM CitaEntity c WHERE c.paciente.id = :pacienteId " +
                     "AND c.penalizado = true " +
                     "AND c.fechaCancelacion >= :desde")
       long countPenalizacionesByPacienteId(
                     @Param("pacienteId") Long pacienteId,
                     @Param("desde") LocalDateTime desde);
}
