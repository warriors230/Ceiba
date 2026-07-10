package com.medisalud.citas.infrastructure.adapter.in.web.controller;

import com.medisalud.citas.domain.model.Cita;
import com.medisalud.citas.domain.model.EstadoCita;
import com.medisalud.citas.domain.port.in.CitaUseCase;
import com.medisalud.citas.domain.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
@Tag(name = "Citas Médicas", description = "Operaciones de agendamiento y gestión de citas médicas")
public class CitaController {

    private final CitaUseCase citaUseCase;
    private final CitaMapper citaMapper;
    private final MessageService messages;

    // -------------------------------------------------------------------------
    // POST /citas/reservar
    // -------------------------------------------------------------------------
    @PostMapping("/reservar")
    @Operation(summary = "Reservar una cita médica", description = "Agenda una nueva cita médica validando: franja horaria permitida, "
            +
            "disponibilidad del médico, conflictos de horario del paciente y " +
            "límite de penalizaciones activas (máx. 2 en los últimos 30 días).")
    public ResponseEntity<ApiResponse<CitaResponseDto>> reservar(
            @Valid @RequestBody CitaRequestDto request) {

        log.info("POST /citas/reservar - pacienteId:{} medicoId:{}",
                request.getPacienteId(), request.getMedicoId());

        List<String> erroresDto = request.validarDatosReserva();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.validacion.dto", String.join(", ", erroresDto)));
        }

        Cita cita = citaMapper.toDomain(request);
        Cita reservada = citaUseCase.reservar(cita);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(messages.get("response.cita.reservada"), citaMapper.toResponse(reservada)));
    }

    // -------------------------------------------------------------------------
    // GET /citas/obtener/{id}
    // -------------------------------------------------------------------------
    @GetMapping("/obtener/{id}")
    @Operation(summary = "Obtener una cita por su ID", description = "Retorna el detalle completo de una cita médica a partir de su identificador único.")
    public ResponseEntity<ApiResponse<CitaResponseDto>> obtenerPorId(
            @Parameter(description = "ID único de la cita", required = true) @PathVariable Long id) {
        log.info("GET /citas/obtener/{}", id);
        Cita cita = citaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.cita.encontrada"), citaMapper.toResponse(cita)));
    }

    // -------------------------------------------------------------------------
    // GET /citas/listar
    // -------------------------------------------------------------------------
    @GetMapping("/listar")
    @Operation(summary = "Listar citas con filtros opcionales", description = "Obtiene el listado de citas aplicando filtros combinables: médico, paciente, "
            +
            "estado (PROGRAMADA | CANCELADA | COMPLETADA) y rango de fechas. " +
            "Todos los parámetros son opcionales; si no se envía ninguno se retornan todas las citas.")
    public ResponseEntity<ApiResponse<List<CitaResponseDto>>> listarCitas(
            @Parameter(description = "ID del médico para filtrar") @RequestParam(required = false) Long medicoId,
            @Parameter(description = "ID del paciente para filtrar") @RequestParam(required = false) Long pacienteId,
            @Parameter(description = "Estado de la cita: PROGRAMADA, CANCELADA o COMPLETADA") @RequestParam(required = false) EstadoCita estado,
            @Parameter(description = "Fecha/hora de inicio del rango (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @Parameter(description = "Fecha/hora de fin del rango (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        log.info("GET /citas/listar - medicoId:{} pacienteId:{} estado:{}", medicoId, pacienteId, estado);
        List<Cita> citas = citaUseCase.listarCitas(medicoId, pacienteId, estado, fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.citas.obtenidas"),
                citaMapper.toResponseList(citas)));
    }

    // -------------------------------------------------------------------------
    // GET /citas/disponibles
    // -------------------------------------------------------------------------
    @GetMapping("/disponibles")
    @Operation(summary = "Consultar franjas horarias disponibles de un médico", description = "Retorna los bloques horarios libres de 30 minutos de un médico para un rango de fechas. "
            +
            "Solo se incluyen días hábiles (lunes a sábado, sin domingos ni festivos colombianos).")
    public ResponseEntity<ApiResponse<List<LocalDateTime>>> consultarCitasDisponibles(
            @Parameter(description = "ID del médico", required = true) @RequestParam Long medicoId,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin del rango (YYYY-MM-DD)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.info("GET /citas/disponibles - medicoId:{} fechaInicio:{} fechaFin:{}", medicoId, fechaInicio, fechaFin);
        List<LocalDateTime> disponibles = citaUseCase.consultarCitasDisponibles(medicoId, fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.cita.disponibles"), disponibles));
    }

    // -------------------------------------------------------------------------
    // PATCH /citas/cancelar/{id}
    // -------------------------------------------------------------------------
    @PatchMapping("/cancelar/{id}")
    @Operation(summary = "Cancelar una cita médica", description = "Cambia el estado de la cita a CANCELADA. Si la cancelación ocurre con menos de "
            +
            "2 horas de antelación respecto a la hora programada, se registra una penalización " +
            "sobre el paciente (RN-05).")
    public ResponseEntity<ApiResponse<CitaResponseDto>> cancelar(
            @Parameter(description = "ID único de la cita a cancelar", required = true) @PathVariable Long id) {
        log.info("PATCH /citas/cancelar/{}", id);
        Cita cancelada = citaUseCase.cancelar(id);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.cita.cancelada"),
                citaMapper.toResponse(cancelada)));
    }

    // -------------------------------------------------------------------------
    // PUT /citas/reprogramar/{id}
    // -------------------------------------------------------------------------
    @PutMapping("/reprogramar/{id}")
    @Operation(summary = "Reprogramar una cita médica", description = "Cancela la cita original y genera una nueva cita en la franja horaria indicada. "
            +
            "Si el paciente tiene 2 o más penalizaciones activas en los últimos 30 días, " +
            "la reprogramación es bloqueada para evitar que acumule una penalización adicional (RN-06).")
    public ResponseEntity<ApiResponse<CitaResponseDto>> reprogramar(
            @Parameter(description = "ID de la cita a reprogramar", required = true) @PathVariable Long id,
            @Valid @RequestBody ReprogramarCitaRequestDto request) {
        log.info("PUT /citas/reprogramar/{} a nueva fecha: {}", id, request.getNuevaFechaHora());
        List<String> erroresDto = request.validarIntegridad();
        if (!erroresDto.isEmpty()) {
            throw new BusinessRuleException(messages.get("error.validacion.dto", String.join(", ", erroresDto)));
        }
        Cita reprogramada = citaUseCase.reprogramar(id, request.getNuevaFechaHora());
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.cita.reprogramada"),
                citaMapper.toResponse(reprogramada)));
    }

    // -------------------------------------------------------------------------
    // DELETE /citas/eliminar/{id}
    // -------------------------------------------------------------------------
    @DeleteMapping("/eliminar/{id}")
    @Operation(summary = "Eliminar una cita médica", description = "Elimina de forma permanente el registro de una cita del sistema. "
            +
            "Esta operación es irreversible y no aplica penalizaciones.")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @Parameter(description = "ID único de la cita a eliminar", required = true) @PathVariable Long id) {
        log.info("DELETE /citas/eliminar/{}", id);
        citaUseCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok(messages.get("response.cita.eliminada")));
    }
}
