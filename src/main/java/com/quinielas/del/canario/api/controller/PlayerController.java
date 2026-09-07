package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.CierreQuinielaResponse;
import com.quinielas.del.canario.api.dto.PerfilResponse;
import com.quinielas.del.canario.api.dto.PartidoResponse;
import com.quinielas.del.canario.api.dto.QuinielaPublicaResponse;
import com.quinielas.del.canario.api.dto.RankingQuinielaResponse;
import com.quinielas.del.canario.api.dto.TipoPronosticoConOpcionesResponse;
import com.quinielas.del.canario.api.dto.UpdatePerfilRequest;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.service.PlayerService;
import com.quinielas.del.canario.api.service.RankingService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/jugador")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class PlayerController {

    private final PlayerService  playerService;
    private final RankingService rankingService;

    public PlayerController(PlayerService playerService,
                            RankingService rankingService) {
        this.playerService  = playerService;
        this.rankingService = rankingService;
    }

    /**
     * Devuelve el perfil completo del jugador autenticado.
     * GET /api/jugador/perfil
     */
    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponse> getPerfil(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.getPerfil(user));
    }

    /**
     * Completa o actualiza los datos del perfil (sin foto).
     * PUT /api/jugador/perfil
     */
    @PutMapping("/perfil")
    public ResponseEntity<PerfilResponse> actualizarPerfil(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePerfilRequest request) {
        return ResponseEntity.ok(playerService.actualizarPerfil(user, request));
    }

    /**
     * Sube o reemplaza la foto de perfil (opcional).
     * PUT /api/jugador/perfil/foto
     * Content-Type: multipart/form-data
     * Campo del archivo: "foto"
     */
    @PutMapping(value = "/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PerfilResponse> actualizarFoto(
            @AuthenticationPrincipal User user,
            @RequestParam("foto") MultipartFile foto) throws IOException {
        return ResponseEntity.ok(playerService.actualizarFoto(user, foto));
    }

    /**
     * Devuelve las quinielas disponibles (estado ABIERTA) para el jugador.
     * GET /api/jugador/quinielas
     */
    @GetMapping("/quinielas")
    public ResponseEntity<List<QuinielaPublicaResponse>> getQuinielas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.getQuinielas(user));
    }

    /**
     * Detalle de una quiniela (ABIERTA, EN_JUEGO o FINALIZADA), incluye la lista de sus partidos.
     * El jugador usa este endpoint para ver los juegos y seguir el avance de la quiniela.
     * GET /api/jugador/quinielas/{id}
     */
    @GetMapping("/quinielas/{id}")
    public ResponseEntity<QuinielaPublicaResponse> getDetalleQuiniela(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.obtenerDetalleQuiniela(id));
    }

    /**
     * Lista los partidos de una quiniela (ABIERTA, EN_JUEGO o FINALIZADA).
     * Util para mostrar la tabla de partidos en el formulario de pronóstico y en la vista de seguimiento.
     * GET /api/jugador/quinielas/{id}/partidos
     */
    @GetMapping("/quinielas/{id}/partidos")
    public ResponseEntity<List<PartidoResponse>> getPartidosDeQuiniela(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.listarPartidosDeQuiniela(id));
    }

    /**
     * Catálogo completo de tipos de pronóstico ACTIVOS con sus opciones ACTIVAS embebidas.
     * El jugador usa este endpoint para saber qué tipos de pronóstico puede realizar
     * y qué opciones tiene disponibles para cada tipo.
     *
     * Ejemplo de respuesta:
     * [
     *   { "id":1, "codigo":"RESULTADO", "nombre":"Resultado final", "puntos":3,
     *     "opciones": [ {"id":1,"codigo":"LOCAL_WIN","descripcion":"Local gana"}, ... ] },
     *   { "id":2, "codigo":"GOLES", "nombre":"Total de goles", "puntos":2,
     *     "opciones": [ {"id":4,"codigo":"G_0_1","descripcion":"0-1 goles",...}, ... ] },
     *   ...
     * ]
     * GET /api/jugador/catalogos/tipos-pronostico
     */
    @GetMapping("/catalogos/tipos-pronostico")
    public ResponseEntity<List<TipoPronosticoConOpcionesResponse>> getTiposPronostico() {
        return ResponseEntity.ok(playerService.listarTiposConOpciones());
    }

    /**
     * Resultado definitivo del cierre de una quiniela FINALIZADA.
     * Incluye ganadores, criterio de desempate y puntaje máximo.
     * GET /api/jugador/quinielas/{id}/cierre
     */
    @GetMapping("/quinielas/{id}/cierre")
    public ResponseEntity<CierreQuinielaResponse> getCierreQuiniela(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.obtenerCierreQuiniela(id));
    }

    /**
     * Ranking de jugadores de una quiniela.
     * Disponible cuando la quiniela está EN_JUEGO (tiempo real) o FINALIZADA (definitivo).
     * Muestra posición, nombre del jugador, puntos y si es ganador.
     *
     * GET /api/jugador/quinielas/{id}/ranking
     */
    @GetMapping("/quinielas/{id}/ranking")
    public ResponseEntity<RankingQuinielaResponse> getRanking(@PathVariable Long id) {
        return ResponseEntity.ok(rankingService.obtenerRanking(id));
    }
}
