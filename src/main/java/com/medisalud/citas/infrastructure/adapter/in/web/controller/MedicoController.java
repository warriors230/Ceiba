package com.medisalud.citas.infrastructure.adapter.in.web.controller;

import com.medisalud.citas.domain.model.Medico;
import com.medisalud.citas.domain.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.medisalud.citas.domain.port.in.MedicoUseCase;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.ApiResponse;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.request.MedicoRequestDto;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.response.MedicoResponseDto;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.BusinessRuleException;
import com.medisalud.citas.infrastructure.adapter.in.web.mapper.MedicoMapper;
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
@RequestMapping("/medicos")
@RequiredArgsConstructor
@Tag(name = "Médicos", description = "Gestión del personal médico del sistema")
public class MedicoController {

    private final MedicoUseCase medicoUseCase;
    private final MedicoMapper medicoMapper;
    private final MessageService messages;

    // -------------------------------------------------------------------------
    // POST /medicos/registrar
    // -------------------------------------------------------------------------
    @PostMapping("/registrar")
    @Operation(summary = "Registrar un nuevo médico", description = "Crea un nuevo médico en el sistema. El email debe ser único; "
            +
            "si ya existe un médico registrado con el mismo email se retorna un error de conflicto.")
    public ResponseEntity<ApiResponse<MedicoResponseDto>> registrar(
            @Valid @RequestBody MedicoRequestDto request) {
        log.info("POST /medicos/registrar - {}", request.getNombreCompleto());
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.validacion.dto", String.join(", ", erroresDto)));
        }
        Medico medico = medicoMapper.toDomain(request);
        Medico registrado = medicoUseCase.registrar(medico);
        MedicoResponseDto response = medicoMapper.toResponse(registrado);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(messages.get("response.medico.registrado"), response));
    }

    // -------------------------------------------------------------------------
    // GET /medicos/obtener/{id}
    // -------------------------------------------------------------------------
    @GetMapping("/obtener/{id}")
    @Operation(summary = "Obtener un médico por su ID", description = "Retorna el detalle completo de un médico a partir de su identificador único.")
    public ResponseEntity<ApiResponse<MedicoResponseDto>> obtenerPorId(
            @Parameter(description = "ID único del médico", required = true) @PathVariable Long id) {
        log.info("GET /medicos/obtener/{}", id);
        Medico medico = medicoUseCase.obtenerPorId(id);
        return ResponseEntity
                .ok(ApiResponse.ok(messages.get("response.medico.encontrado"), medicoMapper.toResponse(medico)));
    }

    // -------------------------------------------------------------------------
    // GET /medicos/listar
    // -------------------------------------------------------------------------
    @GetMapping("/listar")
    @Operation(summary = "Listar todos los médicos registrados", description = "Retorna el listado completo de médicos activos en el sistema.")
    public ResponseEntity<ApiResponse<List<MedicoResponseDto>>> listarTodos() {
        log.info("GET /medicos/listar");
        List<Medico> medicos = medicoUseCase.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.medicos.obtenidos"),
                medicoMapper.toResponseList(medicos)));
    }

    // -------------------------------------------------------------------------
    // PUT /medicos/actualizar/{id}
    // -------------------------------------------------------------------------
    @PutMapping("/actualizar/{id}")
    @Operation(
        summary = "Actualizar la información de un médico",
        description = "Actualiza los datos de un médico existente. Si se intenta cambiar el email " +
                      "a uno ya registrado por otro médico, se retorna un error de conflicto."
    )
    public ResponseEntity<ApiResponse<MedicoResponseDto>> actualizar(
            @Parameter(description = "ID único del médico a actualizar", required = true)
            @PathVariable Long id,
            @Valid @RequestBody MedicoRequestDto request) {
        log.info("PUT /medicos/actualizar/{}", id);
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.validacion.dto", String.join(", ", erroresDto)));
        }
        Medico medico = medicoMapper.toDomain(request);
        Medico actualizado = medicoUseCase.actualizar(id, medico);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.medico.actualizado"),
                medicoMapper.toResponse(actualizado)));
    }

// -------------------------------------------------------------------------
// DELETE /medicos/eliminar/{id}
// -------------------------------------------------------------------------
@DeleteMapping("/eliminar/{id}")
    @Operation(
        summary = "Eliminar un médico por su ID",
        description = "Elimina de forma permanente el registro de un médico del sistema. " +
                      "Esta operación es irreversible."
    )
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @Parameter(description = "ID único del médico a eliminar", required = true)
            @PathVariable Long id) {
        log.info("DELETE /medicos/eliminar/{}", id);
        medicoUseCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.medico.eliminado")));
    }
}
