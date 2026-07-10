package com.medisalud.citas.infrastructure.adapter.out.persistence.specification;

import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.infrastructure.adapter.out.persistence.entity.CitaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CitaSpecification {

    private CitaSpecification() {}

    public static Specification<CitaEntity> filtrar(
            Long medicoId,
            Long pacienteId,
            EstadoCita estado,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (medicoId != null) {
                predicates.add(cb.equal(root.get("medico").get("id"), medicoId));
            }
            if (pacienteId != null) {
                predicates.add(cb.equal(root.get("paciente").get("id"), pacienteId));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (fechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), fechaInicio));
            }
            if (fechaFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaHora"), fechaFin));
            }

            query.orderBy(cb.asc(root.get("fechaHora")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
