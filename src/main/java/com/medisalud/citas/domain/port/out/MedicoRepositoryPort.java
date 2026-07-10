package com.medisalud.citas.domain.port.out;

import com.medisalud.citas.domain.model.Medico;
import java.util.List;
import java.util.Optional;

public interface MedicoRepositoryPort {
    Medico guardar(Medico medico);

    Optional<Medico> buscarPorId(Long id);

    List<Medico> listarTodos();

    boolean existePorEmail(String email);

    void eliminar(Long id);

    boolean existePorId(Long id);
}
