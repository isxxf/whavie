package com.whavie.controller;

import com.whavie.dto.ErrorResponse;
import com.whavie.exception.BadRequestException;
import com.whavie.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * ERROR 404: Cuando alguien busca algo que no existe.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .error("RECURSO_NO_ENCONTRADO")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * ERROR 400: Cuando el cliente hace una petición que no tiene sentido.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {
        log.warn("Solicitud inválida: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .error("SOLICITUD_INVALIDA")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * ERROR 400 o 403: Cuando se intenta hacer algo que el estado actual no permite.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {
        log.warn("Estado ilegal: {}", ex.getMessage());
        boolean isFull = ex.getMessage() != null &&
                ex.getMessage().toLowerCase().contains("máximo");
        HttpStatus status = isFull ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        String errorCode = isFull ? "SALA_LLENA" : "ESTADO_INVALIDO";
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .message(ex.getMessage())
                .error(errorCode)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * ERROR 400: Cuando se pasa un argumento a un método que no cumple las reglas.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .error("ARGUMENTO_INVALIDO")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * ERROR 400 (Validación): Este es el que atrapa los errores del "@Valid".
     * Si el frontend envía un email mal escrito o una contraseña muy corta, esto
     * junta todos esos errores y los devuelve en una lista para mostrar en los inputs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.warn("Error de validación en solicitud: {}", request.getRequestURI());
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Error de validación en los datos enviados")
                .error("VALIDACION_FALLIDA")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * ERROR 400 (Tipo de dato equivocado): Si el frontend envía un texto donde se espera un número,
     * o una fecha mal escrita, esto lo atrapa y devuelve un mensaje de qué salió mal.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        log.warn("Tipo de argumento incorrecto para parámetro '{}': {}",
                ex.getName(), ex.getValue());
        String message = String.format(
                "El parámetro '%s' debe ser de tipo %s",
                ex.getName(),
                ex.getRequiredType().getSimpleName()
        );
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .error("TIPO_PARAMETRO_INVALIDO")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * ERROR 400 (JSON Roto):
     * Cuando el frontend envía un JSON que está mal escrito (le falta una coma,
     * una llave de cierre, etc.) o cuando el cuerpo de la solicitud no se puede leer por alguna razón.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Cuerpo de solicitud no legible: {}", ex.getMessage());
        String message = "El cuerpo de la solicitud no es válido o está mal formado";
        if (ex.getCause() != null) {
            String cause = ex.getCause().getMessage();
            if (cause != null && cause.contains("JSON")) {
                message = "JSON inválido en el cuerpo de la solicitud";
            }
        }
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .error("CUERPO_SOLICITUD_INVALIDO")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Atrapa excepciones genéricas de estado HTTP que lanzamos
     * intencionalmente en alguna parte del código
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {
        log.warn("Exception de estado HTTP [{}]: {}", ex.getStatusCode(), ex.getReason());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ex.getStatusCode().value())
                .message(ex.getReason() != null ? ex.getReason() : "Error HTTP")
                .error("HTTP_ERROR")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    /**
     * ERROR 404 (Endpoint no encontrado):
     * Cuando el frontend intenta llamar a una URL de la API que no está hecha.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        log.warn("Ruta no encontrada: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("El endpoint solicitado no existe")
                .error("ENDPOINT_NO_ENCONTRADO")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * ERROR 500 (Error en tiempo de ejecución):
     * La red de seguridad. Si el código falla por algo de lógica interna (como intentar
     * acceder a algo que es Null), entra aca para que el servidor no se caiga del todo.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        log.error("RuntimeException no esperada", ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Ocurrió un error inesperado en el servidor")
                .error("ERROR_INTERNO")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .details(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * ERROR 409 (Conflicto de Edición Simultánea):
     * Esto ocurre si dos personas intentan modificar exactamente
     * el mismo dato en la base de datos al mismo milisegundo. En vez de romper todo,
     * le pedimos que recargue e intente de nuevo.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("Ups, parece que alguien más ha intentado esto al mismo tiempo. " +
                        "Por favor, recarga la página y vuelve a intentarlo.");
    }

    /**
     * ERROR 500 (Cualquier otra Excepción):
     * Si ocurre un error que no está cubierto arriba,
     * lo atrapamos acá para asegurar que siempre haya una respuesta.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Exception no controlada: ", ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Ocurrió un error inesperado en el servidor")
                .error("ERROR_INTERNO_NO_CONTROLADO")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
