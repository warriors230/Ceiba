package com.medisalud.citas.infrastructure.adapter.in.web.controller;

import com.medisalud.citas.domain.model.Medico;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Médicos", description = "Gestión de personal médico")
public class MedicoController {
    private final MedicoUseCase medicoUseCase;
    private final MedicoMapper medicoMapper;

    @PostMapping
    @Operation(summary = "Registrar un nuevo médico")
    public ResponseEntity<ApiResponse<MedicoResponseDto>> registrar(
            @Valid @RequestBody MedicoRequestDto request) {
        log.info("POST /medicos - Registrando médico: {}", request.getNombreCompleto());
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException("Errores de validación: " + String.join(", ", erroresDto));
        }
        Medico medico = medicoMapper.toDomain(request);
        Medico registrado = medicoUseCase.registrar(medico);
        MedicoResponseDto response = medicoMapper.toResponse(registrado);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Médico registrado exitosamente", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un médico por su ID")
    public ResponseEntity<ApiResponse<MedicoResponseDto>> obtenerPorId(@PathVariable Long id) {
        log.info("GET /medicos/{}", id);
        Medico medico = medicoUseCase.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok("Médico encontrado", medicoMapper.toResponse(medico)));
    }

    @GetMapping
    @Operation(summary = "Listar todos los médicos registrados")
    public ResponseEntity<ApiResponse<List<MedicoResponseDto>>> listarTodos() {
        log.info("GET /medicos - Listando todos los médicos");
        List<Medico> medicos = medicoUseCase.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok("Médicos obtenidos exitosamente",
                medicoMapper.toResponseList(medicos)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar la información de un médico")
    public ResponseEntity<ApiResponse<MedicoResponseDto>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MedicoRequestDto request) {
        log.info("PUT /medicos/{}", id);
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException("Errores de validación: " + String.join(", ", erroresDto));
        }
        Medico medico = medicoMapper.toDomain(request);
        Medico actualizado = medicoUseCase.actualizar(id, medico);
        return ResponseEntity.ok(ApiResponse.ok("Médico actualizado exitosamente",
                medicoMapper.toResponse(actualizado)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un médico por su ID")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        log.info("DELETE /medicos/{}", id);
        medicoUseCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Médico eliminado exitosamente"));
    }
}
