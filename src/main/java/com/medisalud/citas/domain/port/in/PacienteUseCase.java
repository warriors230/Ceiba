package com.medisalud.citas.domain.port.in;

import com.medisalud.citas.domain.model.Paciente;
import java.util.List;

public interface PacienteUseCase {
    Paciente registrar(Paciente paciente);

    Paciente obtenerPorId(Long id);

    List<Paciente> listarTodos();

    Paciente actualizar(Long id, Paciente paciente);

    void eliminar(Long id);
}
