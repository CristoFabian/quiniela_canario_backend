package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.ReglaJuegoRequest;
import com.quinielas.del.canario.api.dto.ReglaJuegoResponse;
import com.quinielas.del.canario.api.entity.CategoriaRegla;
import com.quinielas.del.canario.api.service.ReglaJuegoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión de las reglas del juego.
 * - Los jugadores autenticados solo pueden CONSULTAR las reglas activas.
 * - El administrador puede listar (incluyendo inactivas), crear, editar,
 *   eliminar (lógico o definitivo) y reactivar reglas.
 */
@RestController
@RequestMapping("/api/reglas")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class ReglaJuegoController {

    private final ReglaJuegoService reglaJuegoService;

    public ReglaJuegoController(ReglaJuegoService reglaJuegoService) {
        this.reglaJuegoService = reglaJuegoService;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Consulta para jugadores (solo reglas activas)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lista todas las reglas activas, ordenadas por categoría y orden.
     * GET /api/reglas
     */
    @GetMapping
    public ResponseEntity<List<ReglaJuegoResponse>> listarActivas() {
        return ResponseEntity.ok(reglaJuegoService.listarActivas());
    }

    /**
     * Lista las reglas activas de una categoría concreta.
     * GET /api/reglas/categoria/{categoria}
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ReglaJuegoResponse>> listarActivasPorCategoria(
            @PathVariable CategoriaRegla categoria) {
        return ResponseEntity.ok(reglaJuegoService.listarActivasPorCategoria(categoria));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Administración (CRUD completo, solo ADMIN)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lista todas las reglas (activas e inactivas). Uso administrativo.
     * GET /api/reglas/admin
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReglaJuegoResponse>> listarTodas() {
        return ResponseEntity.ok(reglaJuegoService.listarTodas());
    }

    /**
     * Obtiene una regla por id (activa o inactiva).
     * GET /api/reglas/admin/{id}
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReglaJuegoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(reglaJuegoService.obtener(id));
    }

    /**
     * Crea una nueva regla del juego.
     * POST /api/reglas/admin
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReglaJuegoResponse> crear(
            @Valid @RequestBody ReglaJuegoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reglaJuegoService.crear(request));
    }

    /**
     * Edita una regla existente.
     * PUT /api/reglas/admin/{id}
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReglaJuegoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ReglaJuegoRequest request) {
        return ResponseEntity.ok(reglaJuegoService.actualizar(id, request));
    }

    /**
     * Elimina (lógicamente) una regla: deja de mostrarse al jugador.
     * DELETE /api/reglas/admin/{id}
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReglaJuegoResponse> eliminar(@PathVariable Long id) {
        return ResponseEntity.ok(reglaJuegoService.eliminar(id));
    }

    /**
     * Elimina definitivamente una regla (borrado físico, irreversible).
     * DELETE /api/reglas/admin/{id}/definitivo
     */
    @DeleteMapping("/admin/{id}/definitivo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarDefinitivamente(@PathVariable Long id) {
        reglaJuegoService.eliminarDefinitivamente(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva una regla previamente eliminada de forma lógica.
     * PATCH /api/reglas/admin/{id}/reactivar
     */
    @PatchMapping("/admin/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReglaJuegoResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(reglaJuegoService.reactivar(id));
    }
}

