package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.PagoResponse;
import com.quinielas.del.canario.api.dto.ResumenPagosPorJugadorResponse;
import com.quinielas.del.canario.api.dto.ValidarPagoRequest;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.service.FileStorageService;
import com.quinielas.del.canario.api.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
public class PagoController {

    private final PagoService        pagoService;
    private final FileStorageService fileStorageService;

    public PagoController(PagoService pagoService,
                          FileStorageService fileStorageService) {
        this.pagoService        = pagoService;
        this.fileStorageService = fileStorageService;
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADOR — /api/jugador/pagos
    // ═════════════════════════════════════════════════════════════════

    /**
     * El jugador informa que realizó el pago y asocia una o varias jugadas.
     * El comprobante es opcional si se marca comprobanteWhatsapp=true (lo enviará por WhatsApp),
     * o si usarSaldoAFavor=true (se paga con el crédito acumulado, sin comprobante).
     *
     * POST /api/jugador/pagos
     * Content-Type: multipart/form-data
     * Params:
     *   jugadaIds  → uno o varios (p.ej. jugadaIds=1&jugadaIds=2)
     *   monto      → número decimal (p.ej. 100.00)
     *   comprobante → archivo (jpg, jpeg, png, webp, pdf) [opcional]
     *   comprobanteWhatsapp → true si el jugador enviará el comprobante por WhatsApp [opcional, default false]
     *   usarSaldoAFavor → true para pagar con el saldo a favor del jugador [opcional, default false]
     */
    @PostMapping(value = "/api/jugador/pagos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagoResponse> crearPago(
            @AuthenticationPrincipal User usuario,
            @RequestParam("jugadaIds") List<Long> jugadaIds,
            @RequestParam("monto") BigDecimal monto,
            @RequestParam(value = "comprobante", required = false) MultipartFile comprobante,
            @RequestParam(value = "comprobanteWhatsapp", required = false,
                          defaultValue = "false") boolean comprobanteWhatsapp,
            @RequestParam(value = "usarSaldoAFavor", required = false,
                          defaultValue = "false") boolean usarSaldoAFavor)
            throws IOException {
        if (usarSaldoAFavor) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(pagoService.crearPagoConSaldo(usuario, jugadaIds, monto));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.crearPago(usuario, jugadaIds, monto, comprobante, comprobanteWhatsapp));
    }

    /**
     * Lista todos los pagos del jugador autenticado.
     * GET /api/jugador/pagos
     */
    @GetMapping("/api/jugador/pagos")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PagoResponse>> listarMisPagos(
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(pagoService.listarMisPagos(usuario));
    }

    /**
     * Detalle de un pago propio.
     * GET /api/jugador/pagos/{id}
     */
    @GetMapping("/api/jugador/pagos/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagoResponse> obtenerMiPago(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerMiPago(usuario, id));
    }

    /**
     * Sube o reemplaza el comprobante de un pago (JPEG, PNG o PDF, máx 5 MB).
     * Solo permitido si el pago está en estado PENDIENTE o RECHAZADO.
     * Si estaba RECHAZADO, el pago se resetea automáticamente a PENDIENTE.
     *
     * PUT /api/jugador/pagos/{id}/comprobante
     * Content-Type: multipart/form-data   campo: "comprobante"
     */
    @PutMapping(value = "/api/jugador/pagos/{id}/comprobante",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagoResponse> subirComprobante(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long id,
            @RequestParam("comprobante") MultipartFile comprobante) throws IOException {
        return ResponseEntity.ok(pagoService.subirComprobante(usuario, id, comprobante));
    }

    /**
     * El jugador crea un nuevo intento de pago a partir de un pago RECHAZADO.
     *
     * Condiciones para reintentar:
     *  - El pago referenciado debe pertenecer al usuario autenticado.
     *  - El pago debe estar en estado RECHAZADO.
     *  - Ninguna jugada involucrada debe estar EXPIRADA.
     *  - La quiniela de cada jugada NO debe haber cerrado.
     *  - No debe existir ya un pago PENDIENTE o APROBADO para las mismas jugadas.
     *
     * Efecto:
     *  - Se crea un NUEVO registro de pago vinculado al pago rechazado.
     *  - Las jugadas pasan a PENDIENTE_VALIDACION.
     *
     * POST /api/jugador/pagos/{id}/reintentar
     * Content-Type: multipart/form-data
     * Params:
     *   monto       → nuevo monto declarado (requerido)
     *   comprobante → nuevo comprobante [opcional]
     */
    @PostMapping(value = "/api/jugador/pagos/{id}/reintentar",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagoResponse> reintentarPago(
            @AuthenticationPrincipal User usuario,
            @PathVariable Long id,
            @RequestParam("monto") BigDecimal monto,
            @RequestParam(value = "comprobante", required = false) MultipartFile comprobante)
            throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.reintentarPago(usuario, id, monto, comprobante));
    }

    // ═════════════════════════════════════════════════════════════════
    //  ADMIN — /api/admin/pagos
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lista todos los pagos con filtros opcionales.
     *
     * GET /api/admin/pagos
     * GET /api/admin/pagos?estado=PENDIENTE
     * GET /api/admin/pagos?usuarioId=5
     * GET /api/admin/pagos?estado=RECHAZADO&usuarioId=5
     *
     * @param estado    PENDIENTE | APROBADO | RECHAZADO  [opcional]
     * @param usuarioId id del jugador                    [opcional]
     */
    @GetMapping("/api/admin/pagos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoResponse>> listarPagos(
            @RequestParam(value = "estado",    required = false) String estado,
            @RequestParam(value = "usuarioId", required = false) Long   usuarioId) {
        return ResponseEntity.ok(pagoService.listarPagos(estado, usuarioId));
    }

    /**
     * Resumen de pagos PENDIENTES agrupados por jugador.
     * Permite al administrador ver de un vistazo qué jugadores tienen pagos
     * en espera, el monto total y si adjuntaron comprobante.
     * Ordenado: el jugador con el pago pendiente más antiguo aparece primero.
     *
     * GET /api/admin/pagos/por-jugador
     */
    @GetMapping(path = {"/api/admin/pagos/por-jugador",
                        "/api/admin/pagos/pendientes-por-jugador"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResumenPagosPorJugadorResponse>> resumenPendientesPorJugador() {
        return ResponseEntity.ok(pagoService.listarResumenPendientesPorJugador());
    }

    /**
     * Historial completo de pagos de un jugador específico (admin).
     * Incluye todos los estados: intentos rechazados, reintentos, aprobados, etc.
     *
     * GET /api/admin/jugadores/{usuarioId}/pagos
     * GET /api/admin/jugadores/{usuarioId}/pagos?estado=RECHAZADO
     *
     * @param usuarioId id del jugador
     * @param estado    PENDIENTE | APROBADO | RECHAZADO [opcional]
     */
    @GetMapping("/api/admin/jugadores/{usuarioId}/pagos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoResponse>> pagosPorJugador(
            @PathVariable Long usuarioId,
            @RequestParam(value = "estado", required = false) String estado) {
        return ResponseEntity.ok(pagoService.listarPagosPorJugador(usuarioId, estado));
    }

    /**
     * Detalle de cualquier pago (admin).
     * GET /api/admin/pagos/{id}
     *
     * Se mantiene la ruta original para compatibilidad con integraciones
     * previas y también se soporta un alias explícito.
     */
    @GetMapping(path = {"/api/admin/pagos/{id}", "/api/admin/pagos/detalle/{id}"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> obtenerPago(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPago(id));
    }

    /**
     * El administrador sube o reemplaza el comprobante de un pago.
     * Útil cuando el jugador envió el comprobante por WhatsApp en lugar de subirlo
     * en la plataforma.
     *
     * Reglas:
     *  - El pago NO debe estar APROBADO.
     *  - Al menos uno de los parámetros debe ser proporcionado:
     *      · comprobante          → archivo JPG, PNG o PDF (máx 5 MB).
     *      · comprobanteWhatsapp  → true si el recibo llegó por WhatsApp.
     *  - Si se sube un archivo y ya existía uno previo, se reemplaza en disco.
     *
     * PUT /api/admin/pagos/{id}/comprobante
     * Content-Type: multipart/form-data
     * Params:
     *   comprobante         → archivo [opcional]
     *   comprobanteWhatsapp → boolean [opcional, default false]
     */
    @PutMapping(value = "/api/admin/pagos/{id}/comprobante",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> subirComprobanteAdmin(
            @PathVariable Long id,
            @RequestParam(value = "comprobante", required = false) MultipartFile comprobante,
            @RequestParam(value = "comprobanteWhatsapp", required = false,
                          defaultValue = "false") boolean comprobanteWhatsapp)
            throws IOException {
        return ResponseEntity.ok(
                pagoService.subirComprobanteAdmin(id, comprobante, comprobanteWhatsapp));
    }

    /**
     * El administrador descarga el comprobante de un pago.
     * El archivo NUNCA se expone por URL pública; solo por este endpoint protegido.
     * Se envía con Content-Disposition: inline para visualización en el navegador,
     * o attachment para forzar descarga.
     *
     * GET /api/admin/pagos/{id}/comprobante
     */
    @GetMapping("/api/admin/pagos/{id}/comprobante")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> descargarComprobante(@PathVariable Long id)
            throws IOException {
        Resource resource    = pagoService.obtenerComprobante(id);
        String   nombre      = pagoService.getNombreComprobante(id);
        String   contentType = fileStorageService.detectarContentType(nombre);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(nombre)
                                .build()
                                .toString())
                .body(resource);
    }

    /**
     * El administrador aprueba o rechaza un pago.
     * PATCH /api/admin/pagos/{id}/validar
     * Body: { "estado": "APROBADO", "observacion": "..." }
     *       { "estado": "RECHAZADO", "observacion": "El monto no coincide" }
     *
     * APROBADO  → jugadas pasan a ACTIVA.
     * RECHAZADO → jugadas regresan a CREADA (el jugador puede reintentar el pago).
     */
    @PatchMapping("/api/admin/pagos/{id}/validar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> validarPago(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody ValidarPagoRequest request) {
        return ResponseEntity.ok(pagoService.validarPago(admin, id, request));
    }
}

