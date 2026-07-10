package com.medisalud.citas.infrastructure.adapter.in.web.controller;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.domain.port.in.CitaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.ApiResponse;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.request.CitaRequestDto;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.request.ReprogramarCitaRequestDto;
import com.medisalud.citas.infrastructure.adapter.in.web.dto.response.CitaResponseDto;
import com.medisalud.citas.infrastructure.adapter.in.web.exception.BusinessRuleException;
import com.medisalud.citas.infrastructure.adapter.in.web.mapper.CitaMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
@Tag(name = "Citas Médicas", description = "Operaciones de agendamiento y gestión de citas")
public class CitaController {

    private final CitaUseCase citaUseCase;

    private final CitaMapper citaMapper;

    @PostMapping
    @Operation(summary = "Reservar una cita médica", description = "Permite agendar una cita validando franjas horarias, disponibilidad y reglas de negocio.")
    public ResponseEntity<ApiResponse<CitaResponseDto>> reservar(
            @Valid @RequestBody CitaRequestDto request) {

        log.info("POST /citas - Reservando cita para pacienteId:{} medicoId:{}",
                request.getPacienteId(), request.getMedicoId());

        List<String> erroresDto = request.validarDatosReserva();

        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException("Errores de validación: " + String.join(", ", erroresDto));
        }

        Cita cita = citaMapper.toDomain(request);
        Cita reservada = citaUseCase.reservar(cita);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cita reservada exitosamente", citaMapper.toResponse(reservada)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cita por ID")
    public ResponseEntity<ApiResponse<CitaResponseDto>> obtenerPorId(@PathVariable Long id) {
        log.info("GET /citas/{}", id);
        Cita cita = citaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok("Cita encontrada", citaMapper.toResponse(cita)));
    }

    @GetMapping
    @Operation(summary = "Listar citas con filtros", description = "Obtiene las citas aplicando filtros opcionales como paciente, médico, fechas o estado.")
    public ResponseEntity<ApiResponse<List<CitaResponseDto>>> listarCitas(
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        log.info("GET /citas - Listando con filtros medicoId:{} pacienteId:{} estado:{}",
                medicoId, pacienteId, estado);
        List<Cita> citas = citaUseCase.listarCitas(medicoId, pacienteId, estado, fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.ok("Citas obtenidas exitosamente",
                citaMapper.toResponseList(citas)));
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Consultar citas disponibles", description = "Obtiene las franjas horarias libres de un médico en un rango de fechas.")
    public ResponseEntity<ApiResponse<List<LocalDateTime>>> consultarCitasDisponibles(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin) {
        log.info("GET /citas/disponibles - medicoId:{} fechaInicio:{} fechaFin:{}", medicoId, fechaInicio, fechaFin);
        List<LocalDateTime> disponibles = citaUseCase.consultarCitasDisponibles(medicoId, fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.ok("Franjas horarias disponibles obtenidas", disponibles));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una cita (Patch)", description = "Cambia el estado de la cita a CANCELADA y aplica penalizaciones si corresponde.")
    public ResponseEntity<ApiResponse<CitaResponseDto>> cancelar(@PathVariable Long id) {
        log.info("PATCH /citas/{}/cancelar", id);
        Cita cancelada = citaUseCase.cancelar(id);
        return ResponseEntity.ok(ApiResponse.ok("Cita cancelada exitosamente",
                citaMapper.toResponse(cancelada)));
    }

    @PutMapping("/{id}/reprogramar")
    @Operation(summary = "Reprogramar una cita", description = "Cancela la cita original y genera una nueva en la franja horaria indicada.")
    public ResponseEntity<ApiResponse<CitaResponseDto>> reprogramar(
            @PathVariable Long id,
            @Valid @RequestBody ReprogramarCitaRequestDto request) {
        log.info("PUT /citas/{}/reprogramar a nueva fecha: {}", id, request.getNuevaFechaHora());
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException("Errores de validación: " + String.join(", ", erroresDto));
        }
        Cita reprogramada = citaUseCase.reprogramar(id, request.getNuevaFechaHora());
        return ResponseEntity.ok(ApiResponse.ok("Cita reprogramada exitosamente",
                citaMapper.toResponse(reprogramada)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar una cita", description = "Cancela una cita. Aplica penalización automáticamente si ocurre con menos de 2 horas de antelación.")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        log.info("DELETE /citas/{}", id);
        citaUseCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Cita eliminada exitosamente"));
    }
}
