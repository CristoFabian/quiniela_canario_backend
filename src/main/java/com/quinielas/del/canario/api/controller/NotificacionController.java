package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.NotificacionCountResponse;
import com.quinielas.del.canario.api.dto.NotificacionPageResponse;
import com.quinielas.del.canario.api.dto.NotificacionResponse;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Notificaciones internas del usuario autenticado (jugador o administrador).
 * Todas las consultas y mutaciones están acotadas al usuario del token JWT;
 * nunca se recibe ni se expone el id secuencial interno, solo el {@code publicId} (UUID).
 */
@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("isAuthenticated()")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * Historial paginado de notificaciones propias, más reciente primero.
     * GET /api/notificaciones?pagina=0&tamanio=20
     */
    @GetMapping
    public ResponseEntity<NotificacionPageResponse> listar(
            @AuthenticationPrincipal User usuario,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(notificacionService.listar(usuario, pagina, tamanio));
    }

    /**
     * Solo las notificaciones no leídas (para el panel desplegable de la campana).
     * GET /api/notificaciones/no-leidas?pagina=0&tamanio=10
     */
    @GetMapping("/no-leidas")
    public ResponseEntity<NotificacionPageResponse> listarNoLeidas(
            @AuthenticationPrincipal User usuario,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return ResponseEntity.ok(notificacionService.listarNoLeidas(usuario, pagina, tamanio));
    }

    /**
     * Conteo de no leídas, para el badge de la campana. Se consulta con más frecuencia
     * que el resto de endpoints, por eso es una fila COUNT ligera y no una página completa.
     * GET /api/notificaciones/count
     */
    @GetMapping("/count")
    public ResponseEntity<NotificacionCountResponse> contarNoLeidas(@AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(new NotificacionCountResponse(notificacionService.contarNoLeidas(usuario)));
    }

    /**
     * Marca una notificación propia como leída.
     * PUT /api/notificaciones/{publicId}/leida
     */
    @PutMapping("/{publicId}/leida")
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @AuthenticationPrincipal User usuario,
            @PathVariable String publicId) {
        return ResponseEntity.ok(notificacionService.marcarLeida(usuario, publicId));
    }

    /**
     * Marca todas las notificaciones propias como leídas.
     * PUT /api/notificaciones/leidas
     */
    @PutMapping("/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal User usuario) {
        notificacionService.marcarTodasLeidas(usuario);
        return ResponseEntity.noContent().build();
    }
}
