package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.ActualizarPronosticoRequest;
import com.quinielas.del.canario.api.dto.CrearJugadaRequest;
import com.quinielas.del.canario.api.dto.CrearPronosticoJugadoRequest;
import com.quinielas.del.canario.api.dto.JugadaResponse;
import com.quinielas.del.canario.api.dto.PronosticoJugadoResponse;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.service.JugadaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jugador")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class JugadaController {

    private final JugadaService jugadaService;

    public JugadaController(JugadaService jugadaService) {
        this.jugadaService = jugadaService;
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADAS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Crear una nueva jugada (ticket) para una quiniela ABIERTA.
     * POST /api/jugador/jugadas
     * Body: { "quinielaId": 1 }
     */
    @PostMapping("/jugadas")
    public ResponseEntity<JugadaResponse> crearJugada(
            @AuthenticationPrincipal User usuario,
            @Valid @RequestBody CrearJugadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jugadaService.crearJugada(usuario, request));
    }

    /**
     * Listar todas mis jugadas.
     * GET /api/jugador/jugadas
     */
    @GetMapping("/jugadas")
    public ResponseEntity<List<JugadaResponse>> listarMisJugadas(
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(jugadaService.listarMisJugadas(usuario));
    }

    /**
     * Detalle de una jugada con sus pronósticos.
     * GET /api/jugador/jugadas/{id}
     */
    @GetMapping("/jugadas/{id}")
    public ResponseEntity<JugadaResponse> obtenerDetalle(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(jugadaService.obtenerDetalle(usuario, id));
    }

    // ═════════════════════════════════════════════════════════════════
    //  PRONOSTICOS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Registrar un pronóstico en una jugada.
     * POST /api/jugador/jugadas/{jugadaId}/pronosticos
     * Body: { "partidoId": 1, "tipoPronosticoId": 1, "opcionPronosticoId": 2 }
     */
    @PostMapping("/jugadas/{jugadaId}/pronosticos")
    public ResponseEntity<PronosticoJugadoResponse> registrarPronostico(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long jugadaId,
            @Valid @RequestBody CrearPronosticoJugadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jugadaService.registrarPronostico(usuario, jugadaId, request));
    }

    /**
     * Listar los pronósticos de una jugada.
     * GET /api/jugador/jugadas/{jugadaId}/pronosticos
     */
    @GetMapping("/jugadas/{jugadaId}/pronosticos")
    public ResponseEntity<List<PronosticoJugadoResponse>> listarPronosticos(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long jugadaId) {
        return ResponseEntity.ok(jugadaService.listarPronosticos(usuario, jugadaId));
    }

    /**
     * Actualizar la opción de un pronóstico ya registrado.
     * Solo permitido cuando la jugada está en estado CREADA y antes del cierre.
     * El partido y el tipo de pronóstico no cambian; solo la opción elegida.
     * PUT /api/jugador/jugadas/{jugadaId}/pronosticos/{pronosticoId}
     * Body: { "opcionPronosticoId": 3 }
     */
    @PutMapping("/jugadas/{jugadaId}/pronosticos/{pronosticoId}")
    public ResponseEntity<PronosticoJugadoResponse> actualizarPronostico(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long jugadaId,
            @PathVariable Long pronosticoId,
            @Valid @RequestBody ActualizarPronosticoRequest request) {
        return ResponseEntity.ok(
                jugadaService.actualizarPronostico(usuario, jugadaId, pronosticoId, request));
    }

    /**
     * Eliminar una jugada (ticket) y todos sus pronósticos.
     * Solo permitido cuando la jugada está en estado CREADA.
     * DELETE /api/jugador/jugadas/{id}
     */
    @DeleteMapping("/jugadas/{id}")
    public ResponseEntity<Void> eliminarJugada(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long id) {
        jugadaService.eliminarJugada(usuario, id);
        return ResponseEntity.noContent().build();
    }
}

