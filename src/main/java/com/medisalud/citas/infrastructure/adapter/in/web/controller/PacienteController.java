package com.medisalud.citas.infrastructure.adapter.in.web.controller;

import com.medisalud.citas.domain.model.Paciente;
import com.medisalud.citas.domain.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Pacientes", description = "Gestión de pacientes registrados en el sistema")
public class PacienteController {

    private final PacienteUseCase pacienteUseCase;
    private final PacienteMapper pacienteMapper;
    private final MessageService messages;

    // -------------------------------------------------------------------------
    // POST /pacientes/registrar
    // -------------------------------------------------------------------------
    @PostMapping("/registrar")
    @Operation(summary = "Registrar un nuevo paciente", description = "Crea un nuevo paciente en el sistema. El número de documento de identidad "
            +
            "debe ser único; si ya existe un paciente con el mismo documento se retorna un error de conflicto.")
    public ResponseEntity<ApiResponse<PacienteResponseDto>> registrar(
            @Valid @RequestBody PacienteRequestDto request) {
        log.info("POST /pacientes/registrar - {}", request.getNombreCompleto());
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.validacion.dto", String.join(", ", erroresDto)));
        }
        Paciente paciente = pacienteMapper.toDomain(request);
        Paciente registrado = pacienteUseCase.registrar(paciente);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(messages.get("response.paciente.registrado"),
                        pacienteMapper.toResponse(registrado)));
    }

    // -------------------------------------------------------------------------
    // GET /pacientes/obtener/{id}
    // -------------------------------------------------------------------------
    @GetMapping("/obtener/{id}")
    @Operation(summary = "Obtener un paciente por su ID", description = "Retorna el detalle completo de un paciente a partir de su identificador único.")
    public ResponseEntity<ApiResponse<PacienteResponseDto>> obtenerPorId(
            @Parameter(description = "ID único del paciente", required = true) @PathVariable Long id) {
        log.info("GET /pacientes/obtener/{}", id);
        Paciente paciente = pacienteUseCase.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.paciente.encontrado"),
                pacienteMapper.toResponse(paciente)));
    }

    // -------------------------------------------------------------------------
    // GET /pacientes/listar
    // -------------------------------------------------------------------------
    @GetMapping("/listar")
    @Operation(summary = "Listar todos los pacientes registrados", description = "Retorna el listado completo de pacientes registrados en el sistema.")
    public ResponseEntity<ApiResponse<List<PacienteResponseDto>>> listarTodos() {
        log.info("GET /pacientes/listar");
        List<Paciente> pacientes = pacienteUseCase.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.pacientes.obtenidos"),
                pacienteMapper.toResponseList(pacientes)));
    }

    // -------------------------------------------------------------------------
    // PUT /pacientes/actualizar/{id}
    // -------------------------------------------------------------------------
    @PutMapping("/actualizar/{id}")
    @Operation(summary = "Actualizar la información de un paciente", description = "Actualiza los datos de un paciente existente. Si se intenta cambiar el documento "
            +
            "a uno ya registrado por otro paciente, se retorna un error de conflicto.")
    public ResponseEntity<ApiResponse<PacienteResponseDto>> actualizar(
            @Parameter(description = "ID único del paciente a actualizar", required = true) @PathVariable Long id,
            @Valid @RequestBody PacienteRequestDto request) {
        log.info("PUT /pacientes/actualizar/{}", id);
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.validacion.dto", String.join(", ", erroresDto)));
        }
        Paciente paciente = pacienteMapper.toDomain(request);
        Paciente actualizado = pacienteUseCase.actualizar(id, paciente);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.paciente.actualizado"),
                pacienteMapper.toResponse(actualizado)));
    }

    // -------------------------------------------------------------------------
    // DELETE /pacientes/eliminar/{id}
    // -------------------------------------------------------------------------
    @DeleteMapping("/eliminar/{id}")
    @Operation(summary = "Eliminar un paciente por su ID", description = "Elimina de forma permanente el registro de un paciente del sistema. "
            +
            "Esta operación es irreversible.")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @Parameter(description = "ID único del paciente a eliminar", required = true) @PathVariable Long id) {
        log.info("DELETE /pacientes/eliminar/{}", id);
        pacienteUseCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.paciente.eliminado")));
    }
}
