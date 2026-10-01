package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.*;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.service.CierreQuinielaService;
import com.quinielas.del.canario.api.service.EvaluacionService;
import com.quinielas.del.canario.api.service.QuinielaService;
import com.quinielas.del.canario.api.service.RankingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class QuinielaController {

    private final QuinielaService       quinielaService;
    private final EvaluacionService     evaluacionService;
    private final CierreQuinielaService cierreService;
    private final RankingService        rankingService;

    public QuinielaController(QuinielaService quinielaService,
                              EvaluacionService evaluacionService,
                              CierreQuinielaService cierreService,
                              RankingService rankingService) {
        this.quinielaService   = quinielaService;
        this.evaluacionService = evaluacionService;
        this.cierreService     = cierreService;
        this.rankingService    = rankingService;
    }

    // ─────────────────────────────────────────────────────────────────
    //  QUINIELAS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Crear una nueva quiniela.
     * POST /api/admin/quinielas
     */
    @PostMapping("/quinielas")
    public ResponseEntity<QuinielaResponse> crearQuiniela(
            @Valid @RequestBody CrearQuinielaRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quinielaService.crearQuiniela(request, admin));
    }

    /**
     * Listar todas las quinielas.
     * GET /api/admin/quinielas
     */
    @GetMapping("/quinielas")
    public ResponseEntity<List<QuinielaResponse>> listarQuinielas() {
        return ResponseEntity.ok(quinielaService.listarQuinielas());
    }

    /**
     * Obtener el detalle de una quiniela (incluye sus partidos).
     * GET /api/admin/quinielas/{id}
     */
    @GetMapping("/quinielas/{id}")
    public ResponseEntity<QuinielaResponse> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(quinielaService.obtenerDetalle(id));
    }

    /**
     * Modificar los datos de una quiniela (solo en estado CREADA).
     * PUT /api/admin/quinielas/{id}
     */
    @PutMapping("/quinielas/{id}")
    public ResponseEntity<QuinielaResponse> actualizarQuiniela(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarQuinielaRequest request) {
        return ResponseEntity.ok(quinielaService.actualizarQuiniela(id, request));
    }

    /**
     * Actualizar el estado de una quiniela.
     * PATCH /api/admin/quinielas/{id}/estado
     * Body: { "estado": "ABIERTA" }
     */
    @PatchMapping("/quinielas/{id}/estado")
    public ResponseEntity<QuinielaResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoQuinielaRequest request) {
        return ResponseEntity.ok(quinielaService.actualizarEstado(id, request));
    }

    // ─────────────────────────────────────────────────────────────────
    //  PARTIDOS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Agregar un partido a la quiniela (máximo 8, solo si estado = CREADA).
     * POST /api/admin/quinielas/{quinielaId}/partidos
     */
    @PostMapping("/quinielas/{quinielaId}/partidos")
    public ResponseEntity<PartidoResponse> agregarPartido(
            @PathVariable Long quinielaId,
            @Valid @RequestBody AgregarPartidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quinielaService.agregarPartido(quinielaId, request));
    }

    /**
     * Listar todos los partidos de una quiniela.
     * GET /api/admin/quinielas/{quinielaId}/partidos
     */
    @GetMapping("/quinielas/{quinielaId}/partidos")
    public ResponseEntity<List<PartidoResponse>> listarPartidos(@PathVariable Long quinielaId) {
        return ResponseEntity.ok(quinielaService.listarPartidos(quinielaId));
    }

    /**
     * Editar los datos de un partido (solo si quiniela=CREADA y partido=PENDIENTE).
     * PUT /api/admin/partidos/{id}
     */
    @PutMapping("/partidos/{id}")
    public ResponseEntity<PartidoResponse> actualizarPartido(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPartidoRequest request) {
        return ResponseEntity.ok(quinielaService.actualizarPartido(id, request));
    }

    /**
     * Actualizar resultados de un partido:
     * marcador local/visitante, total corners, ambos marcan.
     * Si todos los campos quedan completos el estado pasa a FINALIZADO automáticamente.
     * Requiere que la quiniela y el partido estén en estado EN_JUEGO.
     * PATCH /api/admin/partidos/{id}/resultados
     */
    @PatchMapping("/partidos/{id}/resultados")
    public ResponseEntity<PartidoResponse> actualizarResultados(
            @PathVariable Long id,
            @RequestBody ActualizarResultadoPartidoRequest request) {
        return ResponseEntity.ok(quinielaService.actualizarResultados(id, request));
    }

    /**
     * Actualizar el estado de un partido manualmente.
     * Valores: PENDIENTE | EN_JUEGO | FINALIZADO | SUSPENDIDO | POSPUESTO
     * PATCH /api/admin/partidos/{id}/estado
     */
    @PatchMapping("/partidos/{id}/estado")
    public ResponseEntity<PartidoResponse> actualizarEstadoPartido(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoPartidoRequest request) {
        return ResponseEntity.ok(quinielaService.actualizarEstadoPartido(id, request));
    }

    /**
     * Disparar la evaluación de pronósticos de un partido manualmente.
     * Vuelve a evaluar aunque ya se haya evaluado antes (revierte y recalcula),
     * útil si el trigger automático falló o se corrigió un resultado.
     * El partido debe estar en estado FINALIZADO, SUSPENDIDO o POSPUESTO.
     *
     * POST /api/admin/partidos/{id}/evaluar
     */
    @PostMapping("/partidos/{id}/evaluar")
    public ResponseEntity<EvaluacionPartidoResponse> evaluarPartido(@PathVariable Long id) {
        return ResponseEntity.ok(evaluacionService.reevaluarPartido(id));
    }

    // ─────────────────────────────────────────────────────────────────
    //  CIERRE DE QUINIELA
    // ─────────────────────────────────────────────────────────────────

    /**
     * Ejecutar el cierre de una quiniela.
     *
     * Condiciones:
     *  - La quiniela debe estar EN_JUEGO.
     *  - Todos los partidos deben estar en estado terminal (FINALIZADO/SUSPENDIDO/POSPUESTO).
     *  - Debe haber al menos una jugada ACTIVA.
     *  - El cierre es idempotente: si ya se ejecutó, devuelve el resultado persistido.
     *
     * Efecto:
     *  - Determina el/los ganador(es) de forma determinista.
     *  - Persiste CierreQuiniela + GanadorQuiniela (inmutable).
     *  - Pasa todas las jugadas ACTIVAS a FINALIZADA.
     *  - Pasa la quiniela a FINALIZADA.
     *
     * POST /api/admin/quinielas/{id}/cerrar
     */
    @PostMapping("/quinielas/{id}/cerrar")
    public ResponseEntity<CierreQuinielaResponse> cerrarQuiniela(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(cierreService.cerrarQuiniela(id, admin));
    }

    /**
     * Consultar el resultado de cierre de una quiniela ya finalizada.
     * Devuelve ganadores, puntaje máximo y auditoría del cierre.
     *
     * GET /api/admin/quinielas/{id}/cierre
     */
    @GetMapping("/quinielas/{id}/cierre")
    public ResponseEntity<CierreQuinielaResponse> obtenerCierre(@PathVariable Long id) {
        return ResponseEntity.ok(cierreService.obtenerCierre(id));
    }

    /**
     * Ranking de jugadores de una quiniela (vista administrador).
     * Disponible cuando la quiniela está EN_JUEGO (tiempo real) o FINALIZADA (definitivo).
     * Muestra posición, nombre del jugador, puntos, estado de la jugada y si es ganador.
     *
     * GET /api/admin/quinielas/{id}/ranking
     */
    @GetMapping("/quinielas/{id}/ranking")
    public ResponseEntity<RankingQuinielaResponse> getRanking(@PathVariable Long id) {
        return ResponseEntity.ok(rankingService.obtenerRanking(id));
    }

    /**
     * Ranking con detalle completo para el administrador: incluye, por cada jugada,
     * todos los pronósticos elegidos (evaluados o no) junto con el resultado real
     * del partido, para saber exactamente qué pronosticó el jugador y el porqué
     * de su puntaje frente a los demás.
     *
     * GET /api/admin/quinielas/{id}/ranking-detalle
     */
    @GetMapping("/quinielas/{id}/ranking-detalle")
    public ResponseEntity<RankingQuinielaAdminDetalleResponse> getRankingDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(rankingService.obtenerRankingDetalleAdmin(id));
    }
}

