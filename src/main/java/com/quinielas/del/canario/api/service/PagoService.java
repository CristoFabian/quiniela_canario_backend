package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.PagoResponse;
import com.quinielas.del.canario.api.dto.ResumenPagosPorJugadorResponse;
import com.quinielas.del.canario.api.dto.ValidarPagoRequest;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.repository.JugadaRepository;
import com.quinielas.del.canario.api.repository.PagoRepository;
import com.quinielas.del.canario.api.repository.PartidoRepository;
import com.quinielas.del.canario.api.repository.PronosticoJugadoRepository;
import com.quinielas.del.canario.api.repository.QuinielaRepository;
import com.quinielas.del.canario.api.repository.UserProfileRepository;
import com.quinielas.del.canario.api.event.PagoPendienteValidacionEvent;
import com.quinielas.del.canario.api.event.PagoValidadoEvent;
import com.quinielas.del.canario.api.util.FechaUtil;
import org.springframework.context.ApplicationEventPublisher;
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
    private final QuinielaRepository          quinielaRepo;
    private final UserProfileRepository       userProfileRepo;
    private final ApplicationEventPublisher   eventPublisher;

    public PagoService(PagoRepository pagoRepo,
                       JugadaRepository jugadaRepo,
                       FileStorageService fileStorageService,
                       PronosticoJugadoRepository pronosticoJugadoRepo,
                       PartidoRepository partidoRepo,
                       QuinielaRepository quinielaRepo,
                       UserProfileRepository userProfileRepo,
                       ApplicationEventPublisher eventPublisher) {
        this.pagoRepo             = pagoRepo;
        this.jugadaRepo           = jugadaRepo;
        this.fileStorageService   = fileStorageService;
        this.pronosticoJugadoRepo = pronosticoJugadoRepo;
        this.partidoRepo          = partidoRepo;
        this.quinielaRepo         = quinielaRepo;
        this.userProfileRepo      = userProfileRepo;
        this.eventPublisher       = eventPublisher;
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
     *  - Debe enviarse un comprobante o marcar comprobanteWhatsapp=true.
     * Efecto:
     *  - Se crea el Pago en estado PENDIENTE.
     *  - Las jugadas pasan a estado PENDIENTE_VALIDACION.
     */
    @Transactional
    public PagoResponse crearPago(User usuario,
                                  List<Long> jugadaIds,
                                  BigDecimal monto,
                                  MultipartFile comprobante,
                                  boolean comprobanteWhatsapp) throws IOException {
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

        if ((comprobante == null || comprobante.isEmpty()) && !comprobanteWhatsapp) {
            throw new IllegalArgumentException(
                    "Debes subir un comprobante o indicar que lo enviarás por WhatsApp.");
        }

        List<Jugada> jugadas = validarYCargarJugadasParaPago(usuario, jugadaIds);

        // Crear el pago (sin comprobante todavía; necesitamos el id generado)
        Pago pago = new Pago();
        pago.setUsuario(usuario);
        pago.setMonto(monto);
        pago.setJugadas(jugadas);
        // Si no se adjunta archivo, el comprobante llegará por WhatsApp.
        boolean sinComprobanteDigital = (comprobante == null || comprobante.isEmpty());
        pago.setComprobanteWhatsapp(sinComprobanteDigital);
        // Solo se marca en true cuando el administrador lo suba desde el detalle del pago.
        pago.setComprobanteAdmin(false);
        pagoRepo.save(pago); // genera el id

        // Guardar comprobante con nombre pago_{id}_{uuid}.ext y actualizar
        // Organizados por quiniela
        if (comprobante != null && !comprobante.isEmpty()) {
            String nombreQuiniela = jugadas.get(0).getQuiniela().getNombre();
            String nombre = fileStorageService.guardarComprobante(comprobante, pago.getId(), nombreQuiniela);
            pago.setComprobanteUrl(nombre);
            pagoRepo.save(pago);
        }

        // Cambiar estado de las jugadas a PENDIENTE_VALIDACION
        jugadas.forEach(j -> j.setEstado(EstadoJugada.PENDIENTE_VALIDACION));
        jugadaRepo.saveAll(jugadas);

        eventPublisher.publishEvent(
                new PagoPendienteValidacionEvent(pago.getId(), usuario.getUsername(), monto));

        return PagoResponse.from(pago);
    }

    /**
     * El jugador paga con su saldo a favor acumulado (créditos de pagos verificados
     * manualmente fuera de ventana). Al ser saldo ya verificado, el pago queda
     * APROBADO de inmediato y las jugadas se activan sin pasar por revisión.
     */
    @Transactional
    public PagoResponse crearPagoConSaldo(User usuario, List<Long> jugadaIds, BigDecimal monto) {
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

        UserProfile perfil = userProfileRepo.findByUserId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Debes completar tu perfil antes de pagar con saldo a favor."));
        BigDecimal saldoDisponible = perfil.getSaldoAFavor();
        if (saldoDisponible.compareTo(BigDecimal.ZERO) <= 0 || saldoDisponible.compareTo(monto) < 0) {
            throw new IllegalArgumentException(
                    "Tu saldo a favor ($" + saldoDisponible + ") es insuficiente para cubrir $" + monto + ".");
        }

        List<Jugada> jugadas = validarYCargarJugadasParaPago(usuario, jugadaIds);

        perfil.setSaldoAFavor(saldoDisponible.subtract(monto));
        userProfileRepo.save(perfil);

        Pago pago = new Pago();
        pago.setUsuario(usuario);
        pago.setMonto(monto);
        pago.setJugadas(jugadas);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setObservacion("Pagado con saldo a favor.");
        pago.setFechaValidacion(LocalDateTime.now(ZoneId.of("America/Mexico_City")));
        pagoRepo.save(pago);

        jugadas.forEach(j -> j.setEstado(EstadoJugada.ACTIVA));
        jugadaRepo.saveAll(jugadas);

        jugadas.stream()
                .map(j -> j.getQuiniela().getId())
                .distinct()
                .forEach(this::recalcularBolsaAcumulada);

        return PagoResponse.from(pago);
    }

    /** Carga y valida las jugadas de un pago: propiedad, misma quiniela, estado y pronósticos completos. */
    private List<Jugada> validarYCargarJugadasParaPago(User usuario, List<Long> jugadaIds) {
        List<Jugada> jugadas = new ArrayList<>();
        List<EstadoPago> estadosActivos = List.of(EstadoPago.PENDIENTE, EstadoPago.APROBADO);
        Long quinielaIdPago = null;

        for (Long jugadaId : jugadaIds) {
            Jugada jugada = jugadaRepo.findById(jugadaId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Jugada no encontrada con id: " + jugadaId));

            // Pertenece al usuario
            if (!jugada.getUsuario().getId().equals(usuario.getId())) {
                throw new IllegalArgumentException(
                        "La jugada id=" + jugadaId + " no te pertenece.");
            }

            if (quinielaIdPago == null) {
                quinielaIdPago = jugada.getQuiniela().getId();
            } else if (!quinielaIdPago.equals(jugada.getQuiniela().getId())) {
                throw new IllegalArgumentException(
                        "Un pago solo puede cubrir jugadas de la misma quiniela.");
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

        return jugadas;
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
        // Organizados por quiniela
        if (comprobante != null && !comprobante.isEmpty()) {
            String nombreQuiniela = jugadas.get(0).getQuiniela().getNombre();
            String nombre = fileStorageService.guardarComprobante(comprobante, nuevoPago.getId(), nombreQuiniela);
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
     * APROBADO (dentro de la ventana) → jugadas pasan a ACTIVA.
     * APROBADO (primer partido ya inició) → el admin confirmó manualmente la
     *   transferencia fuera de tiempo; se otorga el monto como saldo a favor
     *   en lugar de activar la jugada, que ya no puede participar.
     * RECHAZADO → jugadas regresan a CREADA para reintentar, salvo que ya
     *   hayan expirado por el inicio del primer partido.
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
        List<Jugada> jugadas = pago.getJugadas();

        boolean ventanaVencida = jugadas.stream()
                .map(j -> j.getQuiniela().getId())
                .distinct()
                .anyMatch(this::primerPartidoYaInicio);

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        pago.setEstado(nuevoEstadoPago);
        pago.setValidadoPor(admin);
        pago.setFechaValidacion(ahora);

        // ── Aprobación fuera de ventana: crédito verificado manualmente, sin activar ──
        if (nuevoEstadoPago == EstadoPago.APROBADO && ventanaVencida) {
            jugadas.stream()
                    .filter(j -> j.getEstado() != EstadoJugada.ACTIVA
                              && j.getEstado() != EstadoJugada.FINALIZADA)
                    .forEach(j -> j.setEstado(EstadoJugada.EXPIRADA));
            jugadaRepo.saveAll(jugadas);

            acreditarSaldo(pago);
            pago.setObservacion(componerObservacion(request.getObservacion(),
                    "El primer partido ya había iniciado: el pago se verificó manualmente y " +
                    "el monto quedó acreditado como saldo a favor en lugar de activar la jugada."));

            Pago guardadoFueraVentana = pagoRepo.save(pago);
            eventPublisher.publishEvent(new PagoValidadoEvent(
                    guardadoFueraVentana.getId(), guardadoFueraVentana.getUsuario().getId(),
                    nuevoEstadoPago, jugadas.get(0).getQuiniela().getNombre()));
            return PagoResponse.from(guardadoFueraVentana);
        }

        pago.setObservacion(request.getObservacion());

        // Actualizar estado de las jugadas según la decisión (dentro de ventana)
        if (nuevoEstadoPago == EstadoPago.APROBADO) {
            jugadas.forEach(j -> j.setEstado(EstadoJugada.ACTIVA));
        } else if (jugadas.stream().allMatch(j -> j.getEstado() == EstadoJugada.PENDIENTE_VALIDACION)) {
            // RECHAZADO: jugadas regresan a CREADA para que el jugador pueda reintentar
            jugadas.forEach(j -> j.setEstado(EstadoJugada.CREADA));
        }
        jugadaRepo.saveAll(jugadas);

        Pago guardado = pagoRepo.save(pago);

        // Recalcular la bolsa acumulada de la quiniela afectada
        // (suma de montos de pagos APROBADOS asociados a sus jugadas).
        jugadas.stream()
                .map(j -> j.getQuiniela().getId())
                .distinct()
                .forEach(this::recalcularBolsaAcumulada);

        eventPublisher.publishEvent(new PagoValidadoEvent(
                guardado.getId(), guardado.getUsuario().getId(),
                nuevoEstadoPago, jugadas.get(0).getQuiniela().getNombre()));

        return PagoResponse.from(guardado);
    }

    /** True si el primer partido (por fecha) de la quiniela ya inició, por estado o por reloj. */
    private boolean primerPartidoYaInicio(Long quinielaId) {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        return partidoRepo.findByQuinielaId(quinielaId).stream()
                .min(Comparator.comparing(Partido::getFechaPartido))
                .map(p -> p.getEstado() != EstadoPartido.PENDIENTE || !ahora.isBefore(p.getFechaPartido()))
                .orElse(false);
    }

    /** Acredita el monto del pago al saldo del jugador una sola vez. */
    private void acreditarSaldo(Pago pago) {
        if (pago.isSaldoAcreditado()) return;

        UserProfile perfil = userProfileRepo.findByUserId(pago.getUsuario().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "El jugador no tiene un perfil asociado; no se puede acreditar el saldo."));

        BigDecimal monto = pago.getMonto();
        perfil.setSaldoAFavor(perfil.getSaldoAFavor().add(monto));
        userProfileRepo.save(perfil);

        pago.setSaldoAcreditado(true);
        pago.setMontoSaldoAcreditado(monto);
    }

    private String componerObservacion(String delAdmin, String automatico) {
        if (delAdmin == null || delAdmin.isBlank()) return automatico;
        return delAdmin.trim() + " — " + automatico;
    }
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
        // Obtener el nombre de la quiniela para la nueva ubicación
        String nombreQuiniela = pago.getJugadas().get(0).getQuiniela().getNombre();
        fileStorageService.eliminarComprobante(pago.getComprobanteUrl(), nombreQuiniela);

        // Guardar nuevo comprobante (nombre: pago_{id}_{uuid}.ext)
        // Organizados por quiniela
        String nombre = fileStorageService.guardarComprobante(archivo, pagoId, nombreQuiniela);
        pago.setComprobanteUrl(nombre);
        // El jugador ya subió el archivo él mismo; ya no se necesita la intervención del admin.
        pago.setComprobanteAdmin(false);

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

        // Obtener el nombre de la quiniela para la ubicación
        String nombreQuiniela = pago.getJugadas().get(0).getQuiniela().getNombre();

        if (archivoProvisto) {
            // Eliminar comprobante anterior si existe
            fileStorageService.eliminarComprobante(pago.getComprobanteUrl(), nombreQuiniela);
            String nombre = fileStorageService.guardarComprobante(archivo, pagoId, nombreQuiniela);
            pago.setComprobanteUrl(nombre);
            // El comprobante quedó registrado porque el administrador lo cargó él mismo.
            pago.setComprobanteAdmin(true);
        }

        // No se desactiva un comprobanteWhatsapp ya registrado desde la creación del pago.
        pago.setComprobanteWhatsapp(pago.isComprobanteWhatsapp() || comprobanteWhatsapp);

        return PagoResponse.from(pagoRepo.save(pago));
    }

    /**
     * Devuelve el comprobante como {@code Resource} para su descarga por el admin.
     * El archivo NUNCA se expone por URL pública.
     * Intenta cargar desde la carpeta organizada por quiniela; si no existe, intenta
     * desde la carpeta raíz de comprobantes (compatibilidad con archivos antiguos).
     */
    public Resource obtenerComprobante(Long pagoId) throws IOException {
        Pago pago = buscarPagoOException(pagoId);
        if (pago.getComprobanteUrl() == null || pago.getComprobanteUrl().isBlank()) {
            throw new IllegalStateException(
                    "El pago id=" + pagoId + " no tiene comprobante adjunto.");
        }

        try {
            // Intentar desde la carpeta organizada por quiniela
            String nombreQuiniela = pago.getJugadas().get(0).getQuiniela().getNombre();
            return fileStorageService.cargarComprobante(pago.getComprobanteUrl(), nombreQuiniela);
        } catch (IOException e) {
            // Fallback: intentar desde la carpeta raíz (para archivos antiguos)
            return fileStorageService.cargarComprobante(pago.getComprobanteUrl());
        }
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
                    "'. Valores permitidos: PENDIENTE, APROBADO, RECHAZADO, VENCIDO.");
        }
    }

    /**
     * Recalcula y persiste la bolsa acumulada de una quiniela:
     * suma de los montos de todos los pagos en estado APROBADO
     * asociados a las jugadas que corresponden a esa quiniela.
     */
    private void recalcularBolsaAcumulada(Long quinielaId) {
        Quiniela quiniela = quinielaRepo.findById(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + quinielaId));
        BigDecimal total = pagoRepo.sumMontoAprobadoByQuinielaId(quinielaId);
        quiniela.setBolsaAcumulada(total);
        quinielaRepo.save(quiniela);
    }
}



