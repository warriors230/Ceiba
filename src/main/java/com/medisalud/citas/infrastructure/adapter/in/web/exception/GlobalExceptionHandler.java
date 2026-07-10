package com.medisalud.citas.infrastructure.adapter.in.web.exception;

import com.medisalud.citas.infrastructure.adapter.in.web.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecursoNoEncontrado(NotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflicto(ConflictException ex) {
        log.warn("Conflicto de datos: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleReglaNegocio(BusinessRuleException ex) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidacion(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<String> camposRequeridos = fieldErrors.stream()
                .filter(e -> "NotBlank".equals(e.getCode()) || "NotNull".equals(e.getCode())
                        || "NotEmpty".equals(e.getCode()))
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.toList());
        List<String> otrosErrores = fieldErrors.stream()
                .filter(e -> !("NotBlank".equals(e.getCode()) || "NotNull".equals(e.getCode())
                        || "NotEmpty".equals(e.getCode())))
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        String mensajePrincipal = "Error de validación en los datos enviados";
        if (!camposRequeridos.isEmpty()) {
            if (camposRequeridos.size() == 1) {
                mensajePrincipal = "El campo " + camposRequeridos.get(0) + " es requerido!";
            } else {
                String camposUnidos = String.join(", ", camposRequeridos);
                mensajePrincipal = "Los campos " + camposUnidos + " son requeridos!";
            }
        }
        log.warn("Error de validación: {}", mensajePrincipal);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(mensajePrincipal, otrosErrores.isEmpty() ? null : otrosErrores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMensajeNoLeible(HttpMessageNotReadableException ex) {
        log.warn("Request body inválido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El cuerpo de la solicitud tiene un formato inválido o está malformado"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleParametroFaltante(MissingServletRequestParameterException ex) {
        String mensaje = "El parámetro requerido '" + ex.getParameterName() + "' no fue proporcionado";
        log.warn("Parámetro faltante: {}", mensaje);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parámetro '" + ex.getName() + "' tiene un tipo inválido. "
                + "Se esperaba: "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido");
        log.warn("Tipo de parámetro incorrecto: {}", mensaje);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(mensaje));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Error interno no controlado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ocurrió un error interno en el servidor. Por favor intente nuevamente."));
    }

    private String formatFieldError(FieldError error) {
        return "Campo '" + error.getField() + "': " + error.getDefaultMessage();
    }
}
