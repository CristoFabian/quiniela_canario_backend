package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.PremioResponse;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.service.FileStorageService;
import com.quinielas.del.canario.api.service.PremioService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Entrega del premio monetario a los ganadores de una quiniela.
 *
 * Flujo:
 *  1. Al cerrar la quiniela ({@code POST /api/admin/quinielas/{id}/cerrar}) se
 *     calcula {@code montoPremio} para cada ganador (bolsa acumulada repartida
 *     en partes iguales) con estado inicial PENDIENTE.
 *  2. El administrador realiza la transferencia por fuera del sistema y sube
 *     el comprobante → estado PAGADO.
 *  3. El jugador revisa el comprobante y confirma que recibió el dinero →
 *     estado CONFIRMADO.
 */
@RestController
public class PremioController {

    private final PremioService      premioService;
    private final FileStorageService fileStorageService;

    public PremioController(PremioService premioService,
                            FileStorageService fileStorageService) {
        this.premioService      = premioService;
        this.fileStorageService = fileStorageService;
    }

    // ═════════════════════════════════════════════════════════════════
    //  ADMIN — /api/admin/...
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lista los premios (uno por ganador) de una quiniela ya cerrada.
     * GET /api/admin/quinielas/{quinielaId}/premios
     */
    @GetMapping("/api/admin/quinielas/{quinielaId}/premios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PremioResponse>> listarPremiosPorQuiniela(
            @PathVariable Long quinielaId) {
        return ResponseEntity.ok(premioService.listarPremiosPorQuiniela(quinielaId));
    }

    /**
     * Detalle de un premio.
     * GET /api/admin/premios/{ganadorId}
     */
    @GetMapping("/api/admin/premios/{ganadorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PremioResponse> obtenerPremio(@PathVariable Long ganadorId) {
        return ResponseEntity.ok(premioService.obtenerPremio(ganadorId));
    }

    /**
     * El administrador sube el comprobante de la transferencia del premio
     * (JPEG, PNG o PDF, máx 5 MB) y el premio pasa a estado PAGADO.
     * Puede reemplazarse mientras el jugador no lo haya confirmado.
     *
     * PUT /api/admin/premios/{ganadorId}/comprobante
     * Content-Type: multipart/form-data   campo: "comprobante"
     */
    @PutMapping(value = "/api/admin/premios/{ganadorId}/comprobante",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PremioResponse> subirComprobantePremio(
            @AuthenticationPrincipal User admin,
            @PathVariable Long ganadorId,
            @RequestParam("comprobante") MultipartFile comprobante) throws IOException {
        return ResponseEntity.ok(premioService.subirComprobantePremio(admin, ganadorId, comprobante));
    }

    /**
     * El administrador sube un comprobante adicional que será mostrado a otros jugadores.
     * PUT /api/admin/premios/{ganadorId}/comprobante-otros
     */
    @PutMapping(value = "/api/admin/premios/{ganadorId}/comprobante-otros",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PremioResponse> subirComprobantePremioOtros(
            @AuthenticationPrincipal User admin,
            @PathVariable Long ganadorId,
            @RequestParam("comprobante") MultipartFile comprobante) throws IOException {
        return ResponseEntity.ok(premioService.subirComprobantePremioOtros(admin, ganadorId, comprobante));
    }

    /**
     * El administrador descarga/visualiza el comprobante del premio.
     * GET /api/admin/premios/{ganadorId}/comprobante
     */
    @GetMapping("/api/admin/premios/{ganadorId}/comprobante")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> descargarComprobante(@PathVariable Long ganadorId) throws IOException {
        Resource resource    = premioService.obtenerComprobante(ganadorId);
        String   nombre      = premioService.getNombreComprobante(ganadorId);
        String   contentType = fileStorageService.detectarContentType(nombre);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nombre).build().toString())
                .body(resource);
    }

    /**
     * Descargar/visualizar el comprobante 'otros' (admin).
     * GET /api/admin/premios/{ganadorId}/comprobante-otros
     */
    @GetMapping("/api/admin/premios/{ganadorId}/comprobante-otros")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> descargarComprobanteOtros(@PathVariable Long ganadorId) throws IOException {
        Resource resource    = premioService.obtenerComprobanteOtros(ganadorId);
        String   nombre      = premioService.getNombreComprobanteOtros(ganadorId);
        String   contentType = fileStorageService.detectarContentType(nombre);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nombre).build().toString())
                .body(resource);
    }

    /**
     * Elimina el comprobante 'otros' (admin).
     * DELETE /api/admin/premios/{ganadorId}/comprobante-otros
     */
    @DeleteMapping("/api/admin/premios/{ganadorId}/comprobante-otros")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarComprobanteOtros(
            @AuthenticationPrincipal User admin,
            @PathVariable Long ganadorId) {
        premioService.eliminarComprobantePremioOtros(admin, ganadorId);
        return ResponseEntity.noContent().build();
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADOR — /api/jugador/...
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lista los premios ganados por el jugador autenticado (todas las quinielas).
     * GET /api/jugador/premios
     */
    @GetMapping("/api/jugador/premios")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PremioResponse>> listarMisPremios(@AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(premioService.listarMisPremios(usuario));
    }

    /**
     * Detalle de un premio propio.
     * GET /api/jugador/premios/{ganadorId}
     */
    @GetMapping("/api/jugador/premios/{ganadorId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PremioResponse> obtenerMiPremio(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long ganadorId) {
        return ResponseEntity.ok(premioService.obtenerMiPremio(usuario, ganadorId));
    }

    /**
     * El jugador descarga/visualiza el comprobante de la transferencia de su premio.
     * GET /api/jugador/premios/{ganadorId}/comprobante
     */
    @GetMapping("/api/jugador/premios/{ganadorId}/comprobante")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Resource> descargarMiComprobante(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long ganadorId) throws IOException {
        Resource resource    = premioService.obtenerMiComprobante(usuario, ganadorId);
        String   nombre      = premioService.getNombreMiComprobante(usuario, ganadorId);
        String   contentType = fileStorageService.detectarContentType(nombre);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nombre).build().toString())
                .body(resource);
    }

    /**
     * El jugador visualiza el comprobante adicional del pago del premio
     * cargado por la administración para evidenciar el deposito.
     * GET /api/jugador/premios/{ganadorId}/comprobante-otros
     */
    @GetMapping("/api/jugador/premios/{ganadorId}/comprobante-otros")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Resource> descargarMiComprobanteOtros(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long ganadorId) throws IOException {
        Resource resource    = premioService.obtenerMiComprobanteOtros(usuario, ganadorId);
        String   nombre      = premioService.getNombreMiComprobanteOtros(usuario, ganadorId);
        String   contentType = fileStorageService.detectarContentType(nombre);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nombre).build().toString())
                .body(resource);
    }

    /**
     * El jugador confirma que ya recibió el premio (dinero + comprobante verificados).
     * Solo disponible cuando el premio está en estado PAGADO.
     *
     * PATCH /api/jugador/premios/{ganadorId}/confirmar
     */
    @PatchMapping("/api/jugador/premios/{ganadorId}/confirmar")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PremioResponse> confirmarRecepcion(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long ganadorId) {
        return ResponseEntity.ok(premioService.confirmarRecepcion(usuario, ganadorId));
    }
}

