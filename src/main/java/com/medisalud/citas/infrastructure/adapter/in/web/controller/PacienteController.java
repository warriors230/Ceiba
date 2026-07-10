package com.medisalud.citas.infrastructure.adapter.in.web.controller;

import com.medisalud.citas.domain.model.Paciente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.medisalud.citas.domain.port.in.PacienteUseCase;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.ApiResponse;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.request.PacienteRequestDto;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.response.PacienteResponseDto;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.BusinessRuleException;
import com.medisalud.citas.infrastructure.adapter.in.web.mapper.PacienteMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Gestión de pacientes del sistema")
public class PacienteController {
    private final PacienteUseCase pacienteUseCase;
    private final PacienteMapper pacienteMapper;

    @PostMapping
    @Operation(summary = "Registrar un nuevo paciente")
    public ResponseEntity<ApiResponse<PacienteResponseDto>> registrar(
            @Valid @RequestBody PacienteRequestDto request) {
        log.info("POST /pacientes - Registrando paciente: {}", request.getNombreCompleto());
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException("Errores de validación: " + String.join(", ", erroresDto));
        }
        Paciente paciente = pacienteMapper.toDomain(request);
        Paciente registrado = pacienteUseCase.registrar(paciente);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Paciente registrado exitosamente",
                        pacienteMapper.toResponse(registrado)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un paciente por su ID")
    public ResponseEntity<ApiResponse<PacienteResponseDto>> obtenerPorId(@PathVariable Long id) {
        log.info("GET /pacientes/{}", id);
        Paciente paciente = pacienteUseCase.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok("Paciente encontrado",
                pacienteMapper.toResponse(paciente)));
    }

    @GetMapping
    @Operation(summary = "Listar todos los pacientes registrados")
    public ResponseEntity<ApiResponse<List<PacienteResponseDto>>> listarTodos() {
        log.info("GET /pacientes - Listando todos los pacientes");
        List<Paciente> pacientes = pacienteUseCase.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok("Pacientes obtenidos exitosamente",
                pacienteMapper.toResponseList(pacientes)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar la información de un paciente")
    public ResponseEntity<ApiResponse<PacienteResponseDto>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequestDto request) {
        log.info("PUT /pacientes/{}", id);
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException("Errores de validación: " + String.join(", ", erroresDto));
        }
        Paciente paciente = pacienteMapper.toDomain(request);
        Paciente actualizado = pacienteUseCase.actualizar(id, paciente);
        return ResponseEntity.ok(ApiResponse.ok("Paciente actualizado exitosamente",
                pacienteMapper.toResponse(actualizado)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un paciente por su ID")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        log.info("DELETE /pacientes/{}", id);
        pacienteUseCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Paciente eliminado exitosamente"));
    }
}
