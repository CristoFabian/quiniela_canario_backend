package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.*;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.event.QuinielaAbiertaEvent;
import com.quinielas.del.canario.api.repository.JugadaRepository;
import com.quinielas.del.canario.api.repository.PartidoRepository;
import com.quinielas.del.canario.api.repository.QuinielaRepository;
import com.quinielas.del.canario.api.util.FechaUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuinielaService {

    private final QuinielaRepository quinielaRepository;
    private final PartidoRepository  partidoRepository;
    private final EvaluacionService  evaluacionService;
    private final JugadaRepository   jugadaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public QuinielaService(QuinielaRepository quinielaRepository,
                           PartidoRepository  partidoRepository,
                           EvaluacionService  evaluacionService,
                           JugadaRepository   jugadaRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.quinielaRepository = quinielaRepository;
        this.partidoRepository  = partidoRepository;
        this.evaluacionService  = evaluacionService;
        this.jugadaRepository   = jugadaRepository;
        this.eventPublisher     = eventPublisher;
    }

    // ─────────────────────────────────────────────────────────────────
    //  QUINIELAS
    // ─────────────────────────────────────────────────────────────────

    /** Crear una nueva quiniela. El usuario autenticado queda como creador. */
    @Transactional
    public QuinielaResponse crearQuiniela(CrearQuinielaRequest request, User admin) {
        if (request.getFechaCierre().isBefore(request.getFechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha de cierre no puede ser anterior a la fecha de inicio");
        }

        Quiniela quiniela = new Quiniela();
        quiniela.setNombre(request.getNombre());
        quiniela.setDescripcion(request.getDescripcion());
        quiniela.setCosto(request.getCosto());
        quiniela.setFechaInicio(request.getFechaInicio());
        quiniela.setFechaCierre(request.getFechaCierre());
        quiniela.setCreadoPor(admin);

        return QuinielaResponse.from(quinielaRepository.save(quiniela));
    }

    /**
     * Actualizar los datos de una quiniela.
     * Solo se permite cuando el estado es CREADA.
     * La nueva fecha de cierre no puede ser anterior a la de inicio.
     * Si ya hay partidos registrados, todos deben seguir cumpliendo la regla
     * de que su fecha+hora es al menos 1 día después del nuevo cierre;
     * en caso contrario se rechaza la modificación.
     */
    @Transactional
    public QuinielaResponse actualizarQuiniela(Long id, ActualizarQuinielaRequest request) {
        Quiniela quiniela = buscarQuinielaOException(id);

        if (quiniela.getEstado() != EstadoQuiniela.CREADA) {
            throw new IllegalArgumentException(
                    "Solo se puede modificar una quiniela en estado CREADA " +
                    "(estado actual: " + quiniela.getEstado() + ").");
        }

        if (request.getFechaCierre().isBefore(request.getFechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha de cierre no puede ser anterior a la fecha de inicio.");
        }

        // Si ya hay partidos, verificar que todos sigan respetando la nueva fechaCierre
        List<Partido> partidos = partidoRepository.findByQuinielaId(id);
        LocalDateTime nuevaFechaMinPartido = request.getFechaCierre().plusDays(1);
        List<String> conflictos = partidos.stream()
                .filter(p -> p.getFechaPartido().isBefore(nuevaFechaMinPartido))
                .map(p -> "Partido #" + p.getId() + " (" + p.getEquipoLocal() +
                          " vs " + p.getEquipoVisitante() + ") — " +
                          FechaUtil.format(p.getFechaPartido()))
                .toList();

        if (!conflictos.isEmpty()) {
            throw new IllegalArgumentException(
                    "No se puede actualizar la fecha de cierre a " +
                    FechaUtil.format(request.getFechaCierre()) +
                    " porque los siguientes partidos quedarían antes de la fecha mínima permitida (" +
                    FechaUtil.format(nuevaFechaMinPartido) + "): " +
                    String.join(", ", conflictos) + ". Ajusta o elimina esos partidos primero.");
        }

        quiniela.setNombre(request.getNombre());
        quiniela.setDescripcion(request.getDescripcion());
        quiniela.setCosto(request.getCosto());
        quiniela.setFechaInicio(request.getFechaInicio());
        quiniela.setFechaCierre(request.getFechaCierre());

        return QuinielaResponse.from(quinielaRepository.save(quiniela));
    }

    /** Listar todas las quinielas. */
    @Transactional(readOnly = true)
    public List<QuinielaResponse> listarQuinielas() {
        return quinielaRepository.findAll()
                .stream()
                .map(QuinielaResponse::from)
                .collect(Collectors.toList());
    }

    /** Obtener el detalle completo de una quiniela (incluye sus partidos). */
    @Transactional(readOnly = true)
    public QuinielaResponse obtenerDetalle(Long id) {
        Quiniela quiniela = buscarQuinielaOException(id);
        QuinielaResponse response = QuinielaResponse.fromDetalle(quiniela);
        response.setTotalParticipantes(contarParticipantes(id));
        return response;
    }

    /** Jugadas con pago confirmado (ACTIVA o FINALIZADA) = participantes de la quiniela. */
    private int contarParticipantes(Long quinielaId) {
        long activas    = jugadaRepository.countByQuinielaIdAndEstado(quinielaId, EstadoJugada.ACTIVA);
        long finalizadas = jugadaRepository.countByQuinielaIdAndEstado(quinielaId, EstadoJugada.FINALIZADA);
        return (int) (activas + finalizadas);
    }

    /**
     * Actualizar el estado de una quiniela con validaciones de negocio:
     *  - → ABIERTA   : requiere exactamente 8 partidos creados.
     *  - → EN_JUEGO  : solo desde ABIERTA y la fecha actual debe ser >= fechaCierre.
     *  - → FINALIZADA: solo desde EN_JUEGO y todos los partidos deben estar FINALIZADOS.
     */
    @Transactional
    public QuinielaResponse actualizarEstado(Long id, ActualizarEstadoQuinielaRequest request) {
        Quiniela quiniela = buscarQuinielaOException(id);

        EstadoQuiniela nuevoEstado;
        try {
            nuevoEstado = EstadoQuiniela.valueOf(request.getEstado().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado invalido: '" + request.getEstado() +
                    "'. Valores permitidos: CREADA, ABIERTA, EN_JUEGO, FINALIZADA");
        }

        EstadoQuiniela estadoActual = quiniela.getEstado();

        // ── Validación: CREADA → ABIERTA ─────────────────────────────
        if (nuevoEstado == EstadoQuiniela.ABIERTA) {
            long totalPartidos = partidoRepository.countByQuinielaId(id);
            if (totalPartidos < Quiniela.MAX_PARTIDOS) {
                throw new IllegalArgumentException(
                        "La quiniela no puede abrirse: necesita " + Quiniela.MAX_PARTIDOS +
                        " partidos y actualmente tiene " + totalPartidos + ".");
            }
        }

        // ── Validación: ABIERTA → EN_JUEGO ───────────────────────────
        if (nuevoEstado == EstadoQuiniela.EN_JUEGO) {
            if (estadoActual != EstadoQuiniela.ABIERTA) {
                throw new IllegalArgumentException(
                        "Solo se puede pasar a EN_JUEGO desde ABIERTA (estado actual: " + estadoActual + ").");
            }
            LocalDateTime ahora = LocalDateTime.now(java.time.ZoneId.of("America/Mexico_City"));
            if (ahora.isBefore(quiniela.getFechaCierre())) {
                throw new IllegalArgumentException(
                        "No se puede iniciar la quiniela: la fecha de cierre de registro (" +
                        FechaUtil.format(quiniela.getFechaCierre()) +
                        ") aun no ha llegado. Fecha actual: " + FechaUtil.format(ahora));
            }
        }

        // ── Validación: EN_JUEGO → FINALIZADA ────────────────────────
        if (nuevoEstado == EstadoQuiniela.FINALIZADA) {
            if (estadoActual != EstadoQuiniela.EN_JUEGO) {
                throw new IllegalArgumentException(
                        "Solo se puede pasar a FINALIZADA desde EN_JUEGO (estado actual: " + estadoActual + ").");
            }
            long totalPartidos = partidoRepository.countByQuinielaId(id);
            long pendientes    = partidoRepository.countPartidosPendientesDeTerminar(id);
            if (pendientes > 0) {
                long terminados = totalPartidos - pendientes;
                throw new IllegalArgumentException(
                        "La quiniela no puede finalizarse: " + terminados + " de " +
                        totalPartidos + " partidos han concluido. " +
                        "Todos deben estar en estado FINALIZADO, SUSPENDIDO o POSPUESTO.");
            }
        }

        quiniela.setEstado(nuevoEstado);
        QuinielaResponse response = QuinielaResponse.from(quinielaRepository.save(quiniela));

        if (nuevoEstado == EstadoQuiniela.ABIERTA) {
            eventPublisher.publishEvent(new QuinielaAbiertaEvent(quiniela.getId(), quiniela.getNombre()));
        }

        // ── Al pasar a EN_JUEGO: expirar CREADAS + advertir sobre PENDIENTE_VALIDACION ──
        if (nuevoEstado == EstadoQuiniela.EN_JUEGO) {

            // 1. Expirar inmediatamente jugadas en CREADA (nunca pagaron)
            List<Jugada> noConfirmadas = jugadaRepository.findNoConfirmadasByQuinielaId(id);
            List<Jugada> creadas = noConfirmadas.stream()
                    .filter(j -> j.getEstado() == EstadoJugada.CREADA)
                    .collect(Collectors.toList());
            if (!creadas.isEmpty()) {
                creadas.forEach(j -> j.setEstado(EstadoJugada.EXPIRADA));
                jugadaRepository.saveAll(creadas);
            }

            // 2. Advertir sobre jugadas PENDIENTE_VALIDACION (tienen periodo de gracia)
            long pendientesValidacion = jugadaRepository
                    .countByQuinielaIdAndEstado(id, EstadoJugada.PENDIENTE_VALIDACION);
            if (pendientesValidacion > 0) {
                response.setAdvertencia(
                        "ATENCION: hay " + pendientesValidacion +
                        " jugada(s) con pago pendiente de validacion. " +
                        "El jugador conserva su participacion hasta que inicie el primer partido. " +
                        "Valida o rechaza los pagos antes de que comience el partido 1.");
            }
        }

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    //  PARTIDOS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Agregar un partido a la quiniela.
     * Reglas:
     *  - La quiniela debe estar en estado CREADA.
     *  - Máximo 8 partidos por quiniela.
     *  - La fecha del partido debe ser al menos 1 día después del cierre de la quiniela.
     */
    @Transactional
    public PartidoResponse agregarPartido(Long quinielaId, AgregarPartidoRequest request) {
        Quiniela quiniela = buscarQuinielaOException(quinielaId);

        // Regla: solo se pueden agregar partidos si la quiniela está en estado CREADA
        if (quiniela.getEstado() != EstadoQuiniela.CREADA) {
            throw new IllegalArgumentException(
                    "No se pueden agregar partidos. La quiniela debe estar en estado CREADA " +
                    "(estado actual: " + quiniela.getEstado() + ")");
        }

        // Regla: máximo 8 partidos
        long totalActual = partidoRepository.countByQuinielaId(quinielaId);
        if (totalActual >= Quiniela.MAX_PARTIDOS) {
            throw new IllegalArgumentException(
                    "La quiniela ya tiene el máximo permitido de " +
                    Quiniela.MAX_PARTIDOS + " partidos");
        }

        // Regla: la fecha+hora del partido debe ser al menos 1 día (exacto, preservando hora)
        // después del cierre de la quiniela.
        // Ej: cierre = 10/04/2026 12:00 → mínimo partido = 11/04/2026 12:00
        LocalDateTime fechaMinPartido = quiniela.getFechaCierre().plusDays(1);
        if (request.getFechaPartido().isBefore(fechaMinPartido)) {
            throw new IllegalArgumentException(
                    "La fecha y hora del partido (" + FechaUtil.format(request.getFechaPartido()) +
                    ") debe ser al menos un dia completo despues del cierre de la quiniela (" +
                    FechaUtil.format(quiniela.getFechaCierre()) +
                    "). Fecha y hora minima permitida: " + FechaUtil.format(fechaMinPartido));
        }

        Partido partido = new Partido();
        partido.setDescripcion(request.getDescripcion());
        partido.setEquipoLocal(request.getEquipoLocal());
        partido.setEquipoVisitante(request.getEquipoVisitante());
        partido.setFechaPartido(request.getFechaPartido());
        partido.setQuiniela(quiniela);

        return PartidoResponse.from(partidoRepository.save(partido));
    }

    /**
     * Editar los datos de un partido ya agregado a la quiniela.
     * Reglas:
     *  - La quiniela debe estar en estado CREADA.
     *  - El partido debe estar en estado PENDIENTE.
     *  - La nueva fecha+hora del partido debe ser al menos 1 día completo
     *    después del cierre de la quiniela (validando también la hora exacta).
     */
    @Transactional
    public PartidoResponse actualizarPartido(Long partidoId, ActualizarPartidoRequest request) {
        Partido partido = buscarPartidoOException(partidoId);
        Quiniela quiniela = partido.getQuiniela();

        // La quiniela debe estar CREADA
        if (quiniela.getEstado() != EstadoQuiniela.CREADA) {
            throw new IllegalArgumentException(
                    "No se puede editar el partido. La quiniela debe estar en estado CREADA " +
                    "(estado actual: " + quiniela.getEstado() + ").");
        }

        // El partido debe estar PENDIENTE
        if (partido.getEstado() != EstadoPartido.PENDIENTE) {
            throw new IllegalArgumentException(
                    "No se puede editar el partido. El partido debe estar en estado PENDIENTE " +
                    "(estado actual: " + partido.getEstado() + ").");
        }

        // La nueva fecha+hora debe ser >= fechaCierre + 1 día (preservando hora)
        LocalDateTime fechaMinPartido = quiniela.getFechaCierre().plusDays(1);
        if (request.getFechaPartido().isBefore(fechaMinPartido)) {
            throw new IllegalArgumentException(
                    "La fecha y hora del partido (" + FechaUtil.format(request.getFechaPartido()) +
                    ") debe ser al menos un dia completo despues del cierre de la quiniela (" +
                    FechaUtil.format(quiniela.getFechaCierre()) +
                    "). Fecha y hora minima permitida: " + FechaUtil.format(fechaMinPartido));
        }

        partido.setDescripcion(request.getDescripcion());
        partido.setEquipoLocal(request.getEquipoLocal());
        partido.setEquipoVisitante(request.getEquipoVisitante());
        partido.setFechaPartido(request.getFechaPartido());

        return PartidoResponse.from(partidoRepository.save(partido));
    }

    /** Listar todos los partidos de una quiniela. */
    @Transactional(readOnly = true)
    public List<PartidoResponse> listarPartidos(Long quinielaId) {
        buscarQuinielaOException(quinielaId); // valida que la quiniela exista
        return partidoRepository.findByQuinielaId(quinielaId)
                .stream()
                .map(PartidoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Actualizar los resultados de un partido:
     * marcador local, marcador visitante, total corners, ambos marcan.
     * Todos los campos son opcionales (solo se actualizan los que se envíen).
     * Si al finalizar la actualización todos los campos de resultado están
     * completos, el estado del partido cambia automáticamente a FINALIZADO.
     * Si el partido ya estaba FINALIZADO, permite corregir un error de captura:
     * los pronósticos se revierten y se vuelven a evaluar con el dato corregido.
     */
    @Transactional
    public PartidoResponse actualizarResultados(Long partidoId,
                                                ActualizarResultadoPartidoRequest request) {
        Partido partido = buscarPartidoOException(partidoId);
        Quiniela quiniela = partido.getQuiniela();

        // La quiniela debe estar EN_JUEGO para poder cargar o corregir resultados
        if (quiniela.getEstado() != EstadoQuiniela.EN_JUEGO) {
            throw new IllegalArgumentException(
                    "No se pueden registrar resultados. La quiniela debe estar en estado EN_JUEGO " +
                    "(estado actual: " + quiniela.getEstado() + ").");
        }

        // El partido debe estar EN_JUEGO, o FINALIZADO para corregir un error de captura
        boolean yaEstabaFinalizado = partido.getEstado() == EstadoPartido.FINALIZADO;
        if (partido.getEstado() != EstadoPartido.EN_JUEGO && !yaEstabaFinalizado) {
            throw new IllegalArgumentException(
                    "No se pueden registrar resultados. El partido debe estar en estado EN_JUEGO o " +
                    "FINALIZADO (estado actual: " + partido.getEstado() + ").");
        }

        if (request.getMarcadorLocal() != null)
            partido.setMarcadorLocal(request.getMarcadorLocal());

        if (request.getMarcadorVisitante() != null)
            partido.setMarcadorVisitante(request.getMarcadorVisitante());

        if (request.getTotalCorners() != null)
            partido.setTotalCorners(request.getTotalCorners());

        if (request.getAmbosMarcan() != null)
            partido.setAmbosMarcan(request.getAmbosMarcan());

        // Auto-finalización: si todos los campos de resultado están completos
        // el estado pasa automáticamente a FINALIZADO
        if (partido.getMarcadorLocal()     != null &&
            partido.getMarcadorVisitante() != null &&
            partido.getTotalCorners()      != null &&
            partido.getAmbosMarcan()       != null) {
            partido.setEstado(EstadoPartido.FINALIZADO);
        }

        Partido guardado = partidoRepository.save(partido);

        // Evaluación automática al finalizar; si ya estaba FINALIZADO, se revierte
        // la evaluación previa y se recalcula con el resultado corregido.
        if (guardado.getEstado() == EstadoPartido.FINALIZADO) {
            if (yaEstabaFinalizado) {
                evaluacionService.reevaluarPartido(guardado.getId());
            } else {
                evaluacionService.evaluarPartido(guardado.getId());
            }
        }

        return PartidoResponse.from(guardado);
    }

    /**
     * Actualizar manualmente el estado de un partido.
     * Valores permitidos: PENDIENTE, EN_JUEGO, FINALIZADO, SUSPENDIDO, POSPUESTO.
     *
     * Validaciones:
     *  - → EN_JUEGO  : la fecha+hora del sistema debe ser >= fechaPartido.
     *  - → FINALIZADO: la fecha+hora del sistema debe ser >= fechaPartido
     *                  Y los 4 campos de resultado deben estar cargados.
     */
    @Transactional
    public PartidoResponse actualizarEstadoPartido(Long partidoId,
                                                   ActualizarEstadoPartidoRequest request) {
        Partido partido = buscarPartidoOException(partidoId);

        EstadoPartido nuevoEstado;
        try {
            nuevoEstado = EstadoPartido.valueOf(request.getEstado().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado invalido: '" + request.getEstado() +
                    "'. Valores permitidos: PENDIENTE, EN_JUEGO, FINALIZADO, SUSPENDIDO, POSPUESTO");
        }

        LocalDateTime ahora = LocalDateTime.now(java.time.ZoneId.of("America/Mexico_City"));

        // ── Validación: EN_JUEGO → PENDIENTE no permitido ────────────
        if (partido.getEstado() == EstadoPartido.EN_JUEGO &&
            nuevoEstado == EstadoPartido.PENDIENTE) {
            throw new IllegalArgumentException(
                    "No se puede regresar un partido a PENDIENTE cuando ya esta EN_JUEGO.");
        }

        // ── Validación: * → EN_JUEGO ──────────────────────────────────
        if (nuevoEstado == EstadoPartido.EN_JUEGO) {
            if (ahora.isBefore(partido.getFechaPartido())) {
                throw new IllegalArgumentException(
                        "El partido no puede iniciarse: la fecha y hora del partido es " +
                        FechaUtil.format(partido.getFechaPartido()) +
                        " y la hora actual es " + FechaUtil.format(ahora) +
                        ". Espera a que llegue el momento del partido.");
            }
        }
        if (nuevoEstado == EstadoPartido.FINALIZADO) {
            // La fecha+hora del sistema debe haber superado la del partido
            if (ahora.isBefore(partido.getFechaPartido())) {
                throw new IllegalArgumentException(
                        "El partido no puede finalizarse: la fecha y hora del partido es " +
                        FechaUtil.format(partido.getFechaPartido()) +
                        " y la hora actual es " + FechaUtil.format(ahora) + ".");
            }
            // Los 4 campos de resultado deben estar cargados
            boolean resultadosIncompletos = partido.getMarcadorLocal()     == null ||
                                            partido.getMarcadorVisitante() == null ||
                                            partido.getTotalCorners()      == null ||
                                            partido.getAmbosMarcan()       == null;
            if (resultadosIncompletos) {
                throw new IllegalArgumentException(
                        "El partido no puede finalizarse: faltan resultados por registrar " +
                        "(marcador local, marcador visitante, total corners, ambos marcan).");
            }
        }

        partido.setEstado(nuevoEstado);
        Partido guardado = partidoRepository.save(partido);

        // ── Al iniciar un partido: expirar jugadas PENDIENTE_VALIDACION (periodo de gracia terminó)
        if (nuevoEstado == EstadoPartido.EN_JUEGO) {
            Long quinielaId = guardado.getQuiniela().getId();
            // Solo actuar si es el PRIMER partido en iniciar (no hay otros ya EN_JUEGO o terminados)
            long otrosEnCurso = partidoRepository.findByQuinielaId(quinielaId).stream()
                    .filter(p -> !p.getId().equals(guardado.getId()))
                    .filter(p -> p.getEstado() == EstadoPartido.EN_JUEGO
                              || p.getEstado() == EstadoPartido.FINALIZADO
                              || p.getEstado() == EstadoPartido.SUSPENDIDO
                              || p.getEstado() == EstadoPartido.POSPUESTO)
                    .count();

            if (otrosEnCurso == 0) {
                // Es el primer partido en iniciar → expirar pagos pendientes de validación
                List<Jugada> pendientes = jugadaRepository
                        .findNoConfirmadasByQuinielaId(quinielaId).stream()
                        .filter(j -> j.getEstado() == EstadoJugada.PENDIENTE_VALIDACION)
                        .collect(Collectors.toList());
                if (!pendientes.isEmpty()) {
                    pendientes.forEach(j -> j.setEstado(EstadoJugada.EXPIRADA));
                    jugadaRepository.saveAll(pendientes);
                }
            }
        }

        // Evaluación al llegar a un estado terminal; revierte y recalcula si ya se había evaluado
        // (p. ej. el admin cambia entre estados terminales corrigiendo una decisión anterior).
        if (nuevoEstado == EstadoPartido.FINALIZADO ||
            nuevoEstado == EstadoPartido.SUSPENDIDO ||
            nuevoEstado == EstadoPartido.POSPUESTO) {
            evaluacionService.reevaluarPartido(guardado.getId());
        }

        return PartidoResponse.from(guardado);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Privados
    // ─────────────────────────────────────────────────────────────────

    private Quiniela buscarQuinielaOException(Long id) {
        return quinielaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + id));
    }

    private Partido buscarPartidoOException(Long id) {
        return partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Partido no encontrado con id: " + id));
    }
}

