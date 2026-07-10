package com.medisalud.citas.domain.port.in;

import com.medisalud.citas.domain.model.Medico;
import java.util.List;

public interface MedicoUseCase {
    Medico registrar(Medico medico);

    Medico obtenerPorId(Long id);

    List<Medico> listarTodos();

    Medico actualizar(Long id, Medico medico);

    void eliminar(Long id);
}
