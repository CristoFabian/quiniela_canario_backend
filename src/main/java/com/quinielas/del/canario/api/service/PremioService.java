package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.PremioResponse;
import com.quinielas.del.canario.api.entity.EstadoPremio;
import com.quinielas.del.canario.api.entity.GanadorQuiniela;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.event.PremioConfirmadoEvent;
import com.quinielas.del.canario.api.event.PremioPagadoEvent;
import com.quinielas.del.canario.api.repository.GanadorQuinielaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona la entrega del premio monetario a los ganadores de una quiniela:
 *  - El administrador sube el comprobante de la transferencia/pago realizado.
 *  - El jugador consulta su premio, descarga el comprobante y confirma su recepción.
 *
 * Flujo de {@link EstadoPremio}:
 *   PENDIENTE (al cerrar la quiniela) → PAGADO (admin sube comprobante)
 *   → CONFIRMADO (el jugador confirma que recibió el dinero).
 */
@Service
public class PremioService {

    private final GanadorQuinielaRepository ganadorRepo;
    private final FileStorageService        fileStorageService;
    private final ApplicationEventPublisher eventPublisher;

    public PremioService(GanadorQuinielaRepository ganadorRepo,
                          FileStorageService fileStorageService,
                          ApplicationEventPublisher eventPublisher) {
        this.ganadorRepo        = ganadorRepo;
        this.fileStorageService = fileStorageService;
        this.eventPublisher     = eventPublisher;
    }

    // ═════════════════════════════════════════════════════════════════
    //  ADMIN
    // ═════════════════════════════════════════════════════════════════

    /** Lista todos los premios (ganadores) de una quiniela específica. */
    @Transactional(readOnly = true)
    public List<PremioResponse> listarPremiosPorQuiniela(Long quinielaId) {
        return ganadorRepo.findByCierreQuinielaQuinielaId(quinielaId).stream()
                .map(PremioResponse::from)
                .collect(Collectors.toList());
    }

    /** Detalle de un premio (vista administrador). */
    @Transactional(readOnly = true)
    public PremioResponse obtenerPremio(Long ganadorId) {
        return PremioResponse.from(buscarGanadorOException(ganadorId));
    }

    /**
     * El administrador sube el comprobante de la transferencia realizada al ganador
     * y marca el premio como PAGADO. Se puede reemplazar el comprobante mientras
     * el jugador no lo haya confirmado (CONFIRMADO es un estado final).
     */
    @Transactional
    public PremioResponse subirComprobantePremio(User admin, Long ganadorId, MultipartFile comprobante)
            throws IOException {
        GanadorQuiniela ganador = buscarGanadorOException(ganadorId);

        if (ganador.getEstadoPremio() == EstadoPremio.CONFIRMADO) {
            throw new IllegalArgumentException(
                    "El jugador ya confirmo la recepcion de este premio; no se puede modificar el comprobante.");
        }

        // Obtener el nombre de la quiniela para la ubicación del comprobante
        String nombreQuiniela = ganador.getCierreQuiniela().getQuiniela().getNombre();

        // Reemplazar comprobante anterior si existía
        fileStorageService.eliminarComprobante(ganador.getComprobantePremioUrl(), nombreQuiniela);
        String nombre = fileStorageService.guardarComprobantePremio(comprobante, ganadorId, nombreQuiniela);

        ganador.setComprobantePremioUrl(nombre);
        ganador.setEstadoPremio(EstadoPremio.PAGADO);
        ganador.setPagadoPor(admin);
        ganador.setFechaPagoPremio(LocalDateTime.now(ZoneId.of("America/Mexico_City")));

        GanadorQuiniela guardado = ganadorRepo.save(ganador);
        eventPublisher.publishEvent(new PremioPagadoEvent(
                guardado.getId(), guardado.getUsuario().getId(),
                guardado.getCierreQuiniela().getQuiniela().getNombre()));
        return PremioResponse.from(guardado);
    }

    /**
     * El administrador sube un comprobante adicional que será mostrado a otros
     * jugadores como evidencia del pago. No modifica el estado del premio.
     */
    @Transactional
    public PremioResponse subirComprobantePremioOtros(User admin, Long ganadorId, MultipartFile comprobante)
            throws IOException {
        GanadorQuiniela ganador = buscarGanadorOException(ganadorId);

        if (ganador.getEstadoPremio() != EstadoPremio.CONFIRMADO) {
            throw new IllegalArgumentException(
                    "Solo puede registrarse el comprobante alternativo cuando el jugador ya confirmo haber recibido el premio.");
        }

        // Obtener el nombre de la quiniela para la ubicación del comprobante
        String nombreQuiniela = ganador.getCierreQuiniela().getQuiniela().getNombre();

        // Reemplazar comprobante anterior si existía
        fileStorageService.eliminarComprobante(ganador.getComprobantePremioOtrosUrl(), nombreQuiniela);
        String nombre = fileStorageService.guardarComprobantePremioOtros(comprobante, ganadorId, nombreQuiniela);

        ganador.setComprobantePremioOtrosUrl(nombre);

        GanadorQuiniela guardado = ganadorRepo.save(ganador);
        return PremioResponse.from(guardado);
    }

    /** Devuelve el comprobante "otros" como Resource (admin). */
    @Transactional(readOnly = true)
    public Resource obtenerComprobanteOtros(Long ganadorId) throws IOException {
        GanadorQuiniela ganador = buscarGanadorOException(ganadorId);
        if (ganador.getComprobantePremioOtrosUrl() == null || ganador.getComprobantePremioOtrosUrl().isBlank()) {
            throw new IllegalStateException("El premio id=" + ganador.getId() + " aun no tiene comprobante 'otros' adjunto.");
        }

        try {
            String nombreQuiniela = ganador.getCierreQuiniela().getQuiniela().getNombre();
            return fileStorageService.cargarComprobante(ganador.getComprobantePremioOtrosUrl(), nombreQuiniela);
        } catch (IOException e) {
            return fileStorageService.cargarComprobante(ganador.getComprobantePremioOtrosUrl());
        }
    }

    @Transactional(readOnly = true)
    public String getNombreComprobanteOtros(Long ganadorId) {
        return buscarGanadorOException(ganadorId).getComprobantePremioOtrosUrl();
    }

    /** Elimina el comprobante "otros" (admin). */
    @Transactional
    public void eliminarComprobantePremioOtros(User admin, Long ganadorId) {
        GanadorQuiniela ganador = buscarGanadorOException(ganadorId);
        String nombreQuiniela = ganador.getCierreQuiniela().getQuiniela().getNombre();
        fileStorageService.eliminarComprobante(ganador.getComprobantePremioOtrosUrl(), nombreQuiniela);
        ganador.setComprobantePremioOtrosUrl(null);
        ganadorRepo.save(ganador);
    }

    /** Devuelve el comprobante de premio como {@code Resource} (admin). */
    @Transactional(readOnly = true)
    public Resource obtenerComprobante(Long ganadorId) throws IOException {
        GanadorQuiniela ganador = buscarGanadorOException(ganadorId);
        return cargarComprobanteOException(ganador);
    }

    /** Nombre del archivo de comprobante (para el Content-Type de la descarga). */
    @Transactional(readOnly = true)
    public String getNombreComprobante(Long ganadorId) {
        return buscarGanadorOException(ganadorId).getComprobantePremioUrl();
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADOR
    // ═════════════════════════════════════════════════════════════════

    /** Lista todos los premios del jugador autenticado (de todas sus quinielas ganadas). */
    @Transactional(readOnly = true)
    public List<PremioResponse> listarMisPremios(User usuario) {
        return ganadorRepo.findByUsuarioIdOrderByIdDesc(usuario.getId()).stream()
                .map(PremioResponse::from)
                .collect(Collectors.toList());
    }

    /** Detalle de un premio propio. */
    @Transactional(readOnly = true)
    public PremioResponse obtenerMiPremio(User usuario, Long ganadorId) {
        return PremioResponse.from(buscarGanadorDelUsuarioOException(usuario, ganadorId));
    }

    /** Descarga/visualiza el comprobante de un premio propio. */
    @Transactional(readOnly = true)
    public Resource obtenerMiComprobante(User usuario, Long ganadorId) throws IOException {
        GanadorQuiniela ganador = buscarGanadorDelUsuarioOException(usuario, ganadorId);
        return cargarComprobanteOException(ganador);
    }

    /** Nombre del comprobante propio (para el Content-Type de la descarga). */
    @Transactional(readOnly = true)
    public String getNombreMiComprobante(User usuario, Long ganadorId) {
        return buscarGanadorDelUsuarioOException(usuario, ganadorId).getComprobantePremioUrl();
    }

    /** Descarga/visualiza el comprobante alternativo de premio visible para otros jugadores. */
    @Transactional(readOnly = true)
    public Resource obtenerMiComprobanteOtros(User usuario, Long ganadorId) throws IOException {
        GanadorQuiniela ganador = buscarGanadorDelUsuarioOException(usuario, ganadorId);
        if (ganador.getComprobantePremioOtrosUrl() == null || ganador.getComprobantePremioOtrosUrl().isBlank()) {
            throw new IllegalStateException(
                    "Aun no existe un comprobante adicional para este premio.");
        }

        try {
            String nombreQuiniela = ganador.getCierreQuiniela().getQuiniela().getNombre();
            return fileStorageService.cargarComprobante(ganador.getComprobantePremioOtrosUrl(), nombreQuiniela);
        } catch (IOException e) {
            return fileStorageService.cargarComprobante(ganador.getComprobantePremioOtrosUrl());
        }
    }

    @Transactional(readOnly = true)
    public String getNombreMiComprobanteOtros(User usuario, Long ganadorId) {
        GanadorQuiniela ganador = buscarGanadorDelUsuarioOException(usuario, ganadorId);
        return ganador.getComprobantePremioOtrosUrl();
    }

    /**
     * El jugador confirma que ya recibió el premio (verificó el comprobante y el dinero).
     * Solo se permite si el administrador ya subió el comprobante (estado PAGADO).
     */
    @Transactional
    public PremioResponse confirmarRecepcion(User usuario, Long ganadorId) {
        GanadorQuiniela ganador = buscarGanadorDelUsuarioOException(usuario, ganadorId);

        if (ganador.getEstadoPremio() == EstadoPremio.PENDIENTE) {
            throw new IllegalArgumentException(
                    "Aun no se ha registrado el pago de este premio. " +
                    "Espera a que el administrador suba el comprobante de la transferencia.");
        }
        if (ganador.getEstadoPremio() == EstadoPremio.CONFIRMADO) {
            // Idempotente: ya estaba confirmado, no hacer nada más.
            return PremioResponse.from(ganador);
        }

        ganador.setEstadoPremio(EstadoPremio.CONFIRMADO);
        ganador.setFechaConfirmacionJugador(LocalDateTime.now(ZoneId.of("America/Mexico_City")));

        GanadorQuiniela guardado = ganadorRepo.save(ganador);
        eventPublisher.publishEvent(new PremioConfirmadoEvent(
                guardado.getId(), usuario.getUsername(), guardado.getCierreQuiniela().getQuiniela().getNombre()));
        return PremioResponse.from(guardado);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Privados
    // ═════════════════════════════════════════════════════════════════

    private GanadorQuiniela buscarGanadorOException(Long ganadorId) {
        return ganadorRepo.findById(ganadorId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registro de ganador/premio no encontrado con id: " + ganadorId));
    }

    private GanadorQuiniela buscarGanadorDelUsuarioOException(User usuario, Long ganadorId) {
        return ganadorRepo.findByIdAndUsuarioId(ganadorId, usuario.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Premio no encontrado o no pertenece al jugador autenticado (id: " + ganadorId + ")."));
    }

    private Resource cargarComprobanteOException(GanadorQuiniela ganador) throws IOException {
        if (ganador.getComprobantePremioUrl() == null || ganador.getComprobantePremioUrl().isBlank()) {
            throw new IllegalStateException(
                    "El premio id=" + ganador.getId() + " aun no tiene comprobante de pago adjunto.");
        }

        try {
            // Intentar desde la carpeta organizada por quiniela
            String nombreQuiniela = ganador.getCierreQuiniela().getQuiniela().getNombre();
            return fileStorageService.cargarComprobante(ganador.getComprobantePremioUrl(), nombreQuiniela);
        } catch (IOException e) {
            // Fallback: intentar desde la carpeta raíz (para archivos antiguos)
            return fileStorageService.cargarComprobante(ganador.getComprobantePremioUrl());
        }
    }
}

