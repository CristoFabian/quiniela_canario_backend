package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.PagoResponse;
import com.quinielas.del.canario.api.dto.ResumenPagosPorJugadorResponse;
import com.quinielas.del.canario.api.dto.ValidarPagoRequest;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.repository.JugadaRepository;
import com.quinielas.del.canario.api.repository.PagoRepository;
import com.quinielas.del.canario.api.repository.PartidoRepository;
import com.quinielas.del.canario.api.repository.PronosticoJugadoRepository;
import com.quinielas.del.canario.api.util.FechaUtil;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PagoService {

    private final PagoRepository              pagoRepo;
    private final JugadaRepository            jugadaRepo;
    private final FileStorageService          fileStorageService;
    private final PronosticoJugadoRepository  pronosticoJugadoRepo;
    private final PartidoRepository           partidoRepo;

    public PagoService(PagoRepository pagoRepo,
                       JugadaRepository jugadaRepo,
                       FileStorageService fileStorageService,
                       PronosticoJugadoRepository pronosticoJugadoRepo,
                       PartidoRepository partidoRepo) {
        this.pagoRepo             = pagoRepo;
        this.jugadaRepo           = jugadaRepo;
        this.fileStorageService   = fileStorageService;
        this.pronosticoJugadoRepo = pronosticoJugadoRepo;
        this.partidoRepo          = partidoRepo;
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADOR — Informar pago
    // ═════════════════════════════════════════════════════════════════

    /**
     * El jugador informa que realizó el pago.
     * Reglas:
     *  - El jugador debe estar activo.
     *  - Todas las jugadas deben pertenecer al usuario autenticado.
     *  - Las jugadas deben estar en estado CREADA.
     *  - Ninguna jugada puede estar ya asociada a un pago PENDIENTE o APROBADO.
     *  - El monto debe ser positivo.
     * Efecto:
     *  - Se crea el Pago en estado PENDIENTE.
     *  - Las jugadas pasan a estado PENDIENTE_VALIDACION.
     */
    @Transactional
    public PagoResponse crearPago(User usuario,
                                  List<Long> jugadaIds,
                                  BigDecimal monto,
                                  MultipartFile comprobante) throws IOException {
        if (!usuario.isActivo()) {
            throw new IllegalArgumentException(
                    "Tu cuenta esta inactiva. Contacta al administrador.");
        }

        if (jugadaIds == null || jugadaIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes indicar al menos una jugada a pagar.");
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor a 0.");
        }

        // Cargar y validar cada jugada
        List<Jugada> jugadas = new ArrayList<>();
        List<EstadoPago> estadosActivos = List.of(EstadoPago.PENDIENTE, EstadoPago.APROBADO);

        for (Long jugadaId : jugadaIds) {
            Jugada jugada = jugadaRepo.findById(jugadaId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Jugada no encontrada con id: " + jugadaId));

            // Pertenece al usuario
            if (!jugada.getUsuario().getId().equals(usuario.getId())) {
                throw new IllegalArgumentException(
                        "La jugada id=" + jugadaId + " no te pertenece.");
            }

            // Solo jugadas en CREADA pueden pagarse
            if (jugada.getEstado() != EstadoJugada.CREADA) {
                throw new IllegalArgumentException(
                        "La jugada id=" + jugadaId +
                        " debe estar en estado CREADA para poder pagarse " +
                        "(estado actual: " + jugada.getEstado() + ").");
            }

            // No debe estar ya en un pago activo
            if (pagoRepo.existsPagoActivoParaJugada(jugadaId, estadosActivos)) {
                throw new IllegalArgumentException(
                        "La jugada id=" + jugadaId +
                        " ya tiene un pago PENDIENTE o APROBADO asociado.");
            }

            // Todos los 8 partidos de la quiniela deben tener un pronóstico registrado
            long totalPartidos  = partidoRepo.countByQuinielaId(jugada.getQuiniela().getId());
            long cubiertos      = pronosticoJugadoRepo.countPartidosCubiertos(jugadaId);
            if (cubiertos < totalPartidos) {
                throw new IllegalArgumentException(
                        "La jugada #" + jugadaId + " aun no tiene pronostico para todos " +
                        "los partidos de la quiniela. Partidos con pronostico: " + cubiertos +
                        " / " + totalPartidos + ". Debes completar los " +
                        totalPartidos + " pronosticos antes de registrar el pago.");
            }

            jugadas.add(jugada);
        }

        // Crear el pago (sin comprobante todavía; necesitamos el id generado)
        Pago pago = new Pago();
        pago.setUsuario(usuario);
        pago.setMonto(monto);
        pago.setJugadas(jugadas);
        pagoRepo.save(pago); // genera el id

        // Guardar comprobante con nombre pago_{id}_{uuid}.ext y actualizar
        if (comprobante != null && !comprobante.isEmpty()) {
            String nombre = fileStorageService.guardarComprobante(comprobante, pago.getId());
            pago.setComprobanteUrl(nombre);
            pagoRepo.save(pago);
        }

        // Cambiar estado de las jugadas a PENDIENTE_VALIDACION
        jugadas.forEach(j -> j.setEstado(EstadoJugada.PENDIENTE_VALIDACION));
        jugadaRepo.saveAll(jugadas);

        return PagoResponse.from(pago);
    }

    /** Lista todos los pagos del jugador autenticado. */
    @Transactional(readOnly = true)
    public List<PagoResponse> listarMisPagos(User usuario) {
        return pagoRepo.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(PagoResponse::from)
                .collect(Collectors.toList());
    }

    /** Detalle de un pago del jugador autenticado. */
    @Transactional(readOnly = true)
    public PagoResponse obtenerMiPago(User usuario, Long pagoId) {
        Pago pago = buscarPagoOException(pagoId);
        if (!pago.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException(
                    "No tienes permiso para ver este pago.");
        }
        return PagoResponse.from(pago);
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADOR — Reintentar pago rechazado
    // ═════════════════════════════════════════════════════════════════

    /**
     * El jugador crea un nuevo intento de pago a partir de un pago RECHAZADO.
     *
     * Condiciones para reintentar:
     *  - El pago referenciado debe pertenecer al usuario autenticado.
     *  - El pago debe estar en estado RECHAZADO.
     *  - Ninguna jugada involucrada debe estar EXPIRADA.
     *  - La quiniela de cada jugada NO debe haber cerrado (fechaCierre > ahora).
     *  - No debe existir ya un pago PENDIENTE o APROBADO para las mismas jugadas.
     *
     * Efecto:
     *  - Se crea un NUEVO registro {@link Pago} vinculado al pago rechazado original.
     *  - Las jugadas pasan a PENDIENTE_VALIDACION.
     */
    @Transactional
    public PagoResponse reintentarPago(User usuario,
                                       Long pagoRechazadoId,
                                       BigDecimal monto,
                                       MultipartFile comprobante) throws IOException {

        // 1. Cargar pago rechazado
        Pago pagoRechazado = buscarPagoOException(pagoRechazadoId);

        // 2. Debe pertenecer al usuario autenticado
        if (!pagoRechazado.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException(
                    "No tienes permiso para reintentar este pago.");
        }

        // 3. Solo se pueden reintentar pagos en estado RECHAZADO
        if (pagoRechazado.getEstado() != EstadoPago.RECHAZADO) {
            throw new IllegalArgumentException(
                    "Solo puedes reintentar pagos en estado RECHAZADO " +
                    "(estado actual: " + pagoRechazado.getEstado() + ").");
        }

        // 4. Monto válido
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        }

        LocalDateTime ahora        = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        List<EstadoPago> activos   = List.of(EstadoPago.PENDIENTE, EstadoPago.APROBADO);
        List<Jugada>     jugadas   = pagoRechazado.getJugadas();

        for (Jugada jugada : jugadas) {

            // 5a. La jugada no debe estar EXPIRADA
            if (jugada.getEstado() == EstadoJugada.EXPIRADA) {
                throw new IllegalArgumentException(
                        "La jugada #" + jugada.getId() +
                        " esta EXPIRADA y no puede incluirse en un nuevo intento de pago.");
            }

            // 5b. La quiniela no debe haber cerrado
            LocalDateTime fechaCierre = jugada.getQuiniela().getFechaCierre();
            if (!ahora.isBefore(fechaCierre)) {
                throw new IllegalArgumentException(
                        "No se puede reintentar el pago: la quiniela '" +
                        jugada.getQuiniela().getNombre() + "' ya cerro el " +
                        FechaUtil.format(fechaCierre) + " (hora Mexico). " +
                        "Hora actual: " + FechaUtil.format(ahora) + ".");
            }

            // 5c. No debe existir ya un pago activo para la jugada
            if (pagoRepo.existsPagoActivoParaJugada(jugada.getId(), activos)) {
                throw new IllegalArgumentException(
                        "La jugada #" + jugada.getId() +
                        " ya tiene un pago PENDIENTE o APROBADO asociado. " +
                        "No es necesario crear otro intento.");
            }
        }

        // 6. Crear nuevo pago vinculado al pago rechazado original
        Pago nuevoPago = new Pago();
        nuevoPago.setUsuario(usuario);
        nuevoPago.setMonto(monto);
        nuevoPago.setJugadas(new ArrayList<>(jugadas));
        nuevoPago.setPagoOrigen(pagoRechazado);
        pagoRepo.save(nuevoPago);   // genera el id

        // 7. Guardar comprobante si se proporciona
        if (comprobante != null && !comprobante.isEmpty()) {
            String nombre = fileStorageService.guardarComprobante(comprobante, nuevoPago.getId());
            nuevoPago.setComprobanteUrl(nombre);
            pagoRepo.save(nuevoPago);
        }

        // 8. Jugadas vuelven a PENDIENTE_VALIDACION
        jugadas.forEach(j -> j.setEstado(EstadoJugada.PENDIENTE_VALIDACION));
        jugadaRepo.saveAll(jugadas);

        return PagoResponse.from(nuevoPago);
    }

    // ═════════════════════════════════════════════════════════════════
    //  ADMIN — Gestión de pagos
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lista pagos con filtros opcionales.
     *
     * @param estado    PENDIENTE | APROBADO | RECHAZADO | null (todos)
     * @param usuarioId id del jugador [opcional]; si se provee, filtra sólo sus pagos
     */
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPagos(String estado, Long usuarioId) {
        // Con filtro de jugador
        if (usuarioId != null) {
            if (estado == null || estado.isBlank()) {
                return pagoRepo.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                        .stream().map(PagoResponse::from).collect(Collectors.toList());
            }
            return pagoRepo.findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(
                            usuarioId, parsearEstadoPago(estado))
                    .stream().map(PagoResponse::from).collect(Collectors.toList());
        }
        // Sin filtro de jugador
        if (estado == null || estado.isBlank()) {
            return pagoRepo.findAllByOrderByFechaCreacionDesc()
                    .stream().map(PagoResponse::from).collect(Collectors.toList());
        }
        return pagoRepo.findByEstadoOrderByFechaCreacionDesc(parsearEstadoPago(estado))
                .stream().map(PagoResponse::from).collect(Collectors.toList());
    }

    /** Detalle de cualquier pago (admin). */
    @Transactional(readOnly = true)
    public PagoResponse obtenerPago(Long pagoId) {
        return PagoResponse.from(buscarPagoOException(pagoId));
    }

    /**
     * Resumen de pagos PENDIENTES agrupados por jugador.
     * Devuelve un ítem por jugador, ordenado por el pago pendiente más antiguo
     * (prioridad: los más urgentes primero).
     *
     * Útil para la vista principal del panel de validación del administrador.
     */
    @Transactional(readOnly = true)
    public List<ResumenPagosPorJugadorResponse> listarResumenPendientesPorJugador() {
        List<Pago> pendientes = pagoRepo.findByEstadoOrderByFechaCreacionDesc(EstadoPago.PENDIENTE);

        Map<Long, List<Pago>> porJugador = pendientes.stream()
                .collect(Collectors.groupingBy(p -> p.getUsuario().getId()));

        return porJugador.entrySet().stream()
                .map(entry -> {
                    List<Pago> pagosList = entry.getValue();
                    User       usuario   = pagosList.get(0).getUsuario();
                    return ResumenPagosPorJugadorResponse.from(usuario, pagosList);
                })
                // Ordenar: el jugador con el pago pendiente más antiguo aparece primero
                .sorted(Comparator.comparing(
                        r -> r.getPagoMasAntiguo() != null
                             ? r.getPagoMasAntiguo()
                             : LocalDateTime.MAX))
                .collect(Collectors.toList());
    }

    /**
     * Lista todos los pagos de un jugador específico (admin).
     * Permite ver el historial completo incluyendo intentos rechazados y reintentos.
     *
     * @param usuarioId id del jugador
     * @param estado    PENDIENTE | APROBADO | RECHAZADO | null (todos)
     */
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPagosPorJugador(Long usuarioId, String estado) {
        if (estado == null || estado.isBlank()) {
            return pagoRepo.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                    .stream().map(PagoResponse::from).collect(Collectors.toList());
        }
        return pagoRepo.findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(
                        usuarioId, parsearEstadoPago(estado))
                .stream().map(PagoResponse::from).collect(Collectors.toList());
    }

    /**
     * El administrador aprueba o rechaza un pago.
     *
     * APROBADO → jugadas pasan a ACTIVA.
     * RECHAZADO → jugadas regresan a CREADA (el jugador puede reintentar el pago).
     */
    @Transactional
    public PagoResponse validarPago(User admin, Long pagoId, ValidarPagoRequest request) {
        Pago pago = buscarPagoOException(pagoId);

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new IllegalArgumentException(
                    "Solo se pueden validar pagos en estado PENDIENTE " +
                    "(estado actual: " + pago.getEstado() + ").");
        }

        EstadoPago nuevoEstadoPago = EstadoPago.valueOf(request.getEstado().toUpperCase());

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        pago.setEstado(nuevoEstadoPago);
        pago.setValidadoPor(admin);
        pago.setFechaValidacion(ahora);
        pago.setObservacion(request.getObservacion());

        // Actualizar estado de las jugadas según la decisión
        List<Jugada> jugadas = pago.getJugadas();
        if (nuevoEstadoPago == EstadoPago.APROBADO) {
            jugadas.forEach(j -> j.setEstado(EstadoJugada.ACTIVA));
        } else {
            // RECHAZADO: jugadas regresan a CREADA para que el jugador pueda reintentar
            jugadas.forEach(j -> j.setEstado(EstadoJugada.CREADA));
        }
        jugadaRepo.saveAll(jugadas);

        return PagoResponse.from(pagoRepo.save(pago));
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADOR — Subir / reemplazar comprobante
    // ═════════════════════════════════════════════════════════════════

    /**
     * El jugador sube o reemplaza el comprobante de un pago.
     * Reglas:
     *  - El pago debe pertenecer al usuario autenticado.
     *  - Solo se permite si el pago está en estado PENDIENTE o RECHAZADO.
     *  - Si el pago estaba RECHAZADO, se resetea a PENDIENTE para nueva revisión.
     *  - Si ya existía un comprobante previo, se elimina del disco.
     */
    @Transactional
    public PagoResponse subirComprobante(User usuario, Long pagoId,
                                         MultipartFile archivo) throws IOException {
        Pago pago = buscarPagoOException(pagoId);

        if (!pago.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException(
                    "No tienes permiso para modificar este pago.");
        }

        if (pago.getEstado() == EstadoPago.APROBADO) {
            throw new IllegalArgumentException(
                    "No se puede reemplazar el comprobante: el pago ya fue APROBADO.");
        }

        // Eliminar comprobante anterior si existe
        fileStorageService.eliminarComprobante(pago.getComprobanteUrl());

        // Guardar nuevo comprobante (nombre: pago_{id}_{uuid}.ext)
        String nombre = fileStorageService.guardarComprobante(archivo, pagoId);
        pago.setComprobanteUrl(nombre);

        // Si el pago estaba RECHAZADO, resetear a PENDIENTE para nueva revisión
        if (pago.getEstado() == EstadoPago.RECHAZADO) {
            pago.setEstado(EstadoPago.PENDIENTE);
            pago.setValidadoPor(null);
            pago.setFechaValidacion(null);
            pago.setObservacion(null);
            // Revertir jugadas a PENDIENTE_VALIDACION
            pago.getJugadas().forEach(j -> j.setEstado(EstadoJugada.PENDIENTE_VALIDACION));
            jugadaRepo.saveAll(pago.getJugadas());
        }

        return PagoResponse.from(pagoRepo.save(pago));
    }

    // ═════════════════════════════════════════════════════════════════
    //  ADMIN — Descargar comprobante
    // ═════════════════════════════════════════════════════════════════

    /**
     * El administrador sube o reemplaza el comprobante de un pago.
     * Útil cuando el jugador envió el comprobante por WhatsApp en lugar de
     * subirlo en la plataforma.
     *
     * Reglas:
     *  - El pago NO debe estar APROBADO (ya no es modificable).
     *  - Si se proporciona un archivo, se valida y almacena (reemplazando el anterior).
     *  - El flag {@code comprobanteWhatsapp} indica que el recibo llegó por WhatsApp.
     *  - Ambos parámetros son opcionales pero al menos uno debe tener valor.
     */
    @Transactional
    public PagoResponse subirComprobanteAdmin(Long pagoId,
                                              MultipartFile archivo,
                                              boolean comprobanteWhatsapp) throws IOException {
        Pago pago = buscarPagoOException(pagoId);

        if (pago.getEstado() == EstadoPago.APROBADO) {
            throw new IllegalArgumentException(
                    "No se puede modificar el comprobante: el pago ya fue APROBADO.");
        }

        boolean archivoProvisto = archivo != null && !archivo.isEmpty();
        if (!archivoProvisto && !comprobanteWhatsapp) {
            throw new IllegalArgumentException(
                    "Debes proporcionar un archivo o indicar que el comprobante " +
                    "fue enviado por WhatsApp (comprobanteWhatsapp=true).");
        }

        if (archivoProvisto) {
            // Eliminar comprobante anterior si existe
            fileStorageService.eliminarComprobante(pago.getComprobanteUrl());
            String nombre = fileStorageService.guardarComprobante(archivo, pagoId);
            pago.setComprobanteUrl(nombre);
        }

        pago.setComprobanteWhatsapp(comprobanteWhatsapp);

        return PagoResponse.from(pagoRepo.save(pago));
    }

    /**
     * Devuelve el comprobante como {@code Resource} para su descarga por el admin.
     * El archivo NUNCA se expone por URL pública.
     */
    public Resource obtenerComprobante(Long pagoId) throws IOException {
        Pago pago = buscarPagoOException(pagoId);
        if (pago.getComprobanteUrl() == null || pago.getComprobanteUrl().isBlank()) {
            throw new IllegalStateException(
                    "El pago id=" + pagoId + " no tiene comprobante adjunto.");
        }
        return fileStorageService.cargarComprobante(pago.getComprobanteUrl());
    }

    /**
     * Devuelve el nombre del comprobante (para construir la cabecera Content-Type).
     */
    public String getNombreComprobante(Long pagoId) {
        return buscarPagoOException(pagoId).getComprobanteUrl();
    }

    // ═════════════════════════════════════════════════════════════════
    //  Privados
    // ═════════════════════════════════════════════════════════════════

    private Pago buscarPagoOException(Long pagoId) {
        return pagoRepo.findById(pagoId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pago no encontrado con id: " + pagoId));
    }

    private EstadoPago parsearEstadoPago(String estado) {
        try {
            return EstadoPago.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado invalido: '" + estado +
                    "'. Valores permitidos: PENDIENTE, APROBADO, RECHAZADO.");
        }
    }
}



