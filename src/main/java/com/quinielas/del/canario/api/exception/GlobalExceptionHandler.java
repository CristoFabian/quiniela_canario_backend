package com.quinielas.del.canario.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.quinielas.del.canario.api.util.FechaUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Credenciales incorrectas (usuario o password) ────────────────
    @ExceptionHandler({ BadCredentialsException.class, AuthenticationException.class })
    public ResponseEntity<Map<String, Object>> handleBadCredentials(RuntimeException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    // ─── Sin permisos para el recurso (jugador accede a admin, etc.) ──
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso");
    }

    // ─── Duplicado: username o email ya registrado ────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ─── Recurso no encontrado (perfil, usuario, etc.) ────────────────
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ─── Error al guardar/leer archivo de foto ────────────────────────
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo de imagen");
    }

    // ─── Archivo supera el límite configurado en Spring ───────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (2 MB)");
    }

    // ─── Validaciones de @NotBlank, @Email, @Size, etc. ──────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", FechaUtil.format(LocalDateTime.now(ZoneId.of("America/Mexico_City"))));
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Datos inválidos");
        body.put("campos", campos);

        return ResponseEntity.badRequest().body(body);
    }

    // ─── Cualquier otro error no esperado ─────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", FechaUtil.format(LocalDateTime.now(ZoneId.of("America/Mexico_City"))));
        body.put("status", status.value());
        body.put("error", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}

