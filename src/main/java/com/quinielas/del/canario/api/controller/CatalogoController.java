package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.OpcionPronosticoRequest;
import com.quinielas.del.canario.api.dto.OpcionPronosticoResponse;
import com.quinielas.del.canario.api.dto.TipoPronosticoRequest;
import com.quinielas.del.canario.api.dto.TipoPronosticoResponse;
import com.quinielas.del.canario.api.service.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/catalogos")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // ═════════════════════════════════════════════════════════════════
    //  TIPO PRONOSTICO
    // ═════════════════════════════════════════════════════════════════

    /**
     * Listar todos los tipos de pronóstico (activos e inactivos).
     * GET /api/admin/catalogos/tipos-pronostico
     */
    @GetMapping("/tipos-pronostico")
    public ResponseEntity<List<TipoPronosticoResponse>> listarTipos() {
        return ResponseEntity.ok(catalogoService.listarTipos());
    }

    /**
     * Obtener un tipo de pronóstico por id.
     * GET /api/admin/catalogos/tipos-pronostico/{id}
     */
    @GetMapping("/tipos-pronostico/{id}")
    public ResponseEntity<TipoPronosticoResponse> obtenerTipo(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.obtenerTipo(id));
    }

    /**
     * Crear un nuevo tipo de pronóstico.
     * POST /api/admin/catalogos/tipos-pronostico
     */
    @PostMapping("/tipos-pronostico")
    public ResponseEntity<TipoPronosticoResponse> crearTipo(
            @Valid @RequestBody TipoPronosticoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogoService.crearTipo(request));
    }

    /**
     * Actualizar un tipo de pronóstico.
     * PUT /api/admin/catalogos/tipos-pronostico/{id}
     */
    @PutMapping("/tipos-pronostico/{id}")
    public ResponseEntity<TipoPronosticoResponse> actualizarTipo(
            @PathVariable Long id,
            @Valid @RequestBody TipoPronosticoRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarTipo(id, request));
    }

    /**
     * Eliminación lógica de un tipo (activo = false).
     * Desactiva en cascada todas sus opciones.
     * DELETE /api/admin/catalogos/tipos-pronostico/{id}
     */
    @DeleteMapping("/tipos-pronostico/{id}")
    public ResponseEntity<TipoPronosticoResponse> eliminarTipo(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.eliminarTipo(id));
    }

    // ═════════════════════════════════════════════════════════════════
    //  OPCION PRONOSTICO
    // ═════════════════════════════════════════════════════════════════

    /**
     * Listar todas las opciones de un tipo (activas e inactivas).
     * GET /api/admin/catalogos/tipos-pronostico/{tipoId}/opciones
     */
    @GetMapping("/tipos-pronostico/{tipoId}/opciones")
    public ResponseEntity<List<OpcionPronosticoResponse>> listarOpciones(
            @PathVariable Long tipoId) {
        return ResponseEntity.ok(catalogoService.listarOpciones(tipoId));
    }

    /**
     * Obtener una opción por id.
     * GET /api/admin/catalogos/opciones-pronostico/{id}
     */
    @GetMapping("/opciones-pronostico/{id}")
    public ResponseEntity<OpcionPronosticoResponse> obtenerOpcion(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.obtenerOpcion(id));
    }

    /**
     * Crear una nueva opción para un tipo de pronóstico.
     * POST /api/admin/catalogos/tipos-pronostico/{tipoId}/opciones
     */
    @PostMapping("/tipos-pronostico/{tipoId}/opciones")
    public ResponseEntity<OpcionPronosticoResponse> crearOpcion(
            @PathVariable Long tipoId,
            @Valid @RequestBody OpcionPronosticoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogoService.crearOpcion(tipoId, request));
    }

    /**
     * Actualizar una opción de pronóstico.
     * PUT /api/admin/catalogos/opciones-pronostico/{id}
     */
    @PutMapping("/opciones-pronostico/{id}")
    public ResponseEntity<OpcionPronosticoResponse> actualizarOpcion(
            @PathVariable Long id,
            @Valid @RequestBody OpcionPronosticoRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarOpcion(id, request));
    }

    /**
     * Eliminación lógica de una opción (activo = false).
     * DELETE /api/admin/catalogos/opciones-pronostico/{id}
     */
    @DeleteMapping("/opciones-pronostico/{id}")
    public ResponseEntity<OpcionPronosticoResponse> eliminarOpcion(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.eliminarOpcion(id));
    }
}

