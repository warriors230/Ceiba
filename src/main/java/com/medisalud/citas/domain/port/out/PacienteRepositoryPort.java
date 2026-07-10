package com.medisalud.citas.domain.port.out;

import com.medisalud.citas.domain.model.Paciente;
import java.util.List;
import java.util.Optional;

public interface PacienteRepositoryPort {
    Paciente guardar(Paciente paciente);

    Optional<Paciente> buscarPorId(Long id);

    List<Paciente> listarTodos();

    Optional<Paciente> buscarPorDocumento(String documento);

    boolean existePorDocumento(String documento);

    void eliminar(Long id);

    boolean existePorId(Long id);
}
