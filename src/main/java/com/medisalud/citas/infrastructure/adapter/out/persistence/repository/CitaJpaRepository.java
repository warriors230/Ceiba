package com.medisalud.citas.infrastructure.adapter.out.persistence.repository;

import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.CitaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaJpaRepository extends JpaRepository<CitaEntity, Long> {
       @Query("SELECT c FROM CitaEntity c WHERE c.medico.id = :medicoId " +
                     "AND c.fechaHora = :fechaHora " +
                     "AND c.estado = 'PROGRAMADA'")
       List<CitaEntity> findCitasProgramadasByMedicoAndFecha(
                     @Param("medicoId") Long medicoId,
                     @Param("fechaHora") LocalDateTime fechaHora);

       @Query("SELECT c FROM CitaEntity c WHERE c.paciente.id = :pacienteId " +
                     "AND c.fechaHora = :fechaHora " +
                     "AND c.estado = 'PROGRAMADA'")
       List<CitaEntity> findCitasProgramadasByPacienteAndFecha(
                     @Param("pacienteId") Long pacienteId,
                     @Param("fechaHora") LocalDateTime fechaHora);

       @Query("SELECT COUNT(c) FROM CitaEntity c WHERE c.paciente.id = :pacienteId " +
                     "AND c.penalizado = true " +
                     "AND c.fechaCancelacion >= :desde")
       long countPenalizacionesByPacienteId(
                     @Param("pacienteId") Long pacienteId,
                     @Param("desde") LocalDateTime desde);

       @Query("SELECT c FROM CitaEntity c " +
                     "WHERE (cast(:medicoId as Long) IS NULL OR c.medico.id = :medicoId) " +
                     "AND (cast(:pacienteId as Long) IS NULL OR c.paciente.id = :pacienteId) " +
                     "AND (cast(:estado as string) IS NULL OR cast(c.estado as string) = cast(:estado as string)) " +
                     "AND (cast(:fechaInicio as timestamp) IS NULL OR c.fechaHora >= :fechaInicio) " +
                     "AND (cast(:fechaFin as timestamp) IS NULL OR c.fechaHora <= :fechaFin) " +
                     "ORDER BY c.fechaHora ASC")
       List<CitaEntity> findByParameters(
                     @Param("medicoId") Long medicoId,
                     @Param("pacienteId") Long pacienteId,
                     @Param("estado") EstadoCita estado,
                     @Param("fechaInicio") LocalDateTime fechaInicio,
                     @Param("fechaFin") LocalDateTime fechaFin);
}
