package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.ActualizarPronosticoRequest;
import com.quinielas.del.canario.api.dto.CrearJugadaRequest;
import com.quinielas.del.canario.api.dto.CrearPronosticoJugadoRequest;
import com.quinielas.del.canario.api.dto.JugadaResponse;
import com.quinielas.del.canario.api.dto.PronosticoJugadoResponse;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.repository.*;
import com.quinielas.del.canario.api.util.FechaUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JugadaService {

    private final JugadaRepository            jugadaRepo;
    private final PronosticoJugadoRepository  pronosticoRepo;
    private final QuinielaRepository          quinielaRepo;
    private final PartidoRepository           partidoRepo;
    private final TipoPronosticoRepository    tipoRepo;
    private final OpcionPronosticoRepository  opcionRepo;
    private final UserProfileRepository       perfilRepo;

    public JugadaService(JugadaRepository jugadaRepo,
                         PronosticoJugadoRepository pronosticoRepo,
                         QuinielaRepository quinielaRepo,
                         PartidoRepository partidoRepo,
                         TipoPronosticoRepository tipoRepo,
                         OpcionPronosticoRepository opcionRepo,
                         UserProfileRepository perfilRepo) {
        this.jugadaRepo    = jugadaRepo;
        this.pronosticoRepo = pronosticoRepo;
        this.quinielaRepo  = quinielaRepo;
        this.partidoRepo   = partidoRepo;
        this.tipoRepo      = tipoRepo;
        this.opcionRepo    = opcionRepo;
        this.perfilRepo    = perfilRepo;
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADAS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Crear una nueva jugada (ticket) para una quiniela.
     * Reglas:
     *  - La quiniela debe estar en estado ABIERTA.
     *  - El jugador debe estar activo.
     *  - Se permiten múltiples tickets por quiniela.
     */
    @Transactional
    public JugadaResponse crearJugada(User usuario, CrearJugadaRequest request) {
        if (!usuario.isActivo()) {
            throw new IllegalArgumentException(
                    "Tu cuenta esta inactiva. Contacta al administrador.");
        }

        // Validar que el perfil del jugador esté COMPLETO antes de crear una jugada
        UserProfile perfil = perfilRepo.findByUserId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Debes completar tu perfil antes de participar en una quiniela."));
        if (perfil.getEstado() != EstadoPerfil.COMPLETO) {
            throw new IllegalArgumentException(
                    "Tu perfil esta incompleto. Completa nombre, apellidos, ciudad, " +
                    "telefono y fecha de nacimiento antes de crear una jugada.");
        }

        Quiniela quiniela = quinielaRepo.findById(request.getQuinielaId())
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + request.getQuinielaId()));

        if (quiniela.getEstado() != EstadoQuiniela.ABIERTA) {
            throw new IllegalArgumentException(
                    "Solo puedes crear jugadas para quinielas en estado ABIERTA " +
                    "(estado actual: " + quiniela.getEstado() + ").");
        }

        // Verificar que la fecha/hora actual (zona horaria del sistema) sea anterior al cierre
        LocalDateTime ahora  = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        LocalDateTime cierre = quiniela.getFechaCierre();
        if (!ahora.isBefore(cierre)) {
            throw new IllegalArgumentException(
                    "No se pueden crear jugadas: la quiniela cerro el " +
                    FechaUtil.format(cierre) + " (hora Mexico). Hora actual: " +
                    FechaUtil.format(ahora) + ".");
        }

        Jugada jugada = new Jugada();
        jugada.setUsuario(usuario);
        jugada.setQuiniela(quiniela);

        return JugadaResponse.from(jugadaRepo.save(jugada));
    }

    /** Listar todas las jugadas del usuario autenticado. */
    @Transactional(readOnly = true)
    public List<JugadaResponse> listarMisJugadas(User usuario) {
        return jugadaRepo.findByUsuarioId(usuario.getId())
                .stream()
                .map(JugadaResponse::from)
                .collect(Collectors.toList());
    }

    /** Obtener el detalle de una jugada con sus pronósticos. */
    @Transactional(readOnly = true)
    public JugadaResponse obtenerDetalle(User usuario, Long jugadaId) {
        Jugada jugada = buscarJugadaPropia(usuario, jugadaId);
        return JugadaResponse.fromDetalle(jugada);
    }

    // ═════════════════════════════════════════════════════════════════
    //  PRONOSTICOS JUGADOS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Registrar un pronóstico en una jugada.
     * Reglas:
     *  - La jugada debe pertenecer al usuario autenticado.
     *  - La jugada no puede estar RECHAZADA ni EXPIRADA.
     *  - No se puede registrar después de la fecha de cierre de la quiniela.
     *  - El partido debe pertenecer a la quiniela de la jugada.
     *  - Solo un pronóstico por partido y tipo dentro de la misma jugada.
     *  - El TipoPronostico debe estar activo.
     *  - La OpcionPronostico debe pertenecer al tipo indicado y estar activa.
     */
    @Transactional
    public PronosticoJugadoResponse registrarPronostico(User usuario, Long jugadaId,
                                                        CrearPronosticoJugadoRequest request) {
        Jugada jugada = buscarJugadaPropia(usuario, jugadaId);

        // La jugada no puede estar en estado terminal
        if (jugada.getEstado() == EstadoJugada.RECHAZADA ||
            jugada.getEstado() == EstadoJugada.EXPIRADA  ||
            jugada.getEstado() == EstadoJugada.FINALIZADA) {
            throw new IllegalArgumentException(
                    "No se pueden registrar pronosticos en una jugada con estado: " +
                    jugada.getEstado() + ".");
        }

        // No se puede modificar después del cierre de la quiniela
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        LocalDateTime cierre = jugada.getQuiniela().getFechaCierre();
        if (ahora.isAfter(cierre)) {
            throw new IllegalArgumentException(
                    "No se pueden registrar pronosticos: la quiniela cerro el " +
                    FechaUtil.format(cierre) + ".");
        }

        // El partido debe pertenecer a la quiniela de esta jugada
        Partido partido = partidoRepo.findById(request.getPartidoId())
                .orElseThrow(() -> new IllegalStateException(
                        "Partido no encontrado con id: " + request.getPartidoId()));

        if (!partido.getQuiniela().getId().equals(jugada.getQuiniela().getId())) {
            throw new IllegalArgumentException(
                    "El partido id=" + request.getPartidoId() +
                    " no pertenece a la quiniela de esta jugada.");
        }

        // Solo un pronostico por (jugada, partido, tipoPronostico)
        if (pronosticoRepo.existsByJugadaIdAndPartidoIdAndTipoPronosticoId(
                jugadaId, request.getPartidoId(), request.getTipoPronosticoId())) {
            throw new IllegalArgumentException(
                    "Ya existe un pronostico de ese tipo para el partido id=" +
                    request.getPartidoId() + " en esta jugada.");
        }

        // El tipo de pronostico debe estar activo
        TipoPronostico tipo = tipoRepo.findById(request.getTipoPronosticoId())
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de pronostico no encontrado con id: " + request.getTipoPronosticoId()));

        if (!tipo.isActivo()) {
            throw new IllegalArgumentException(
                    "El tipo de pronostico '" + tipo.getCodigo() + "' no esta activo.");
        }

        // La opcion debe pertenecer al tipo indicado y estar activa
        OpcionPronostico opcion = opcionRepo.findById(request.getOpcionPronosticoId())
                .orElseThrow(() -> new IllegalStateException(
                        "Opcion de pronostico no encontrada con id: " + request.getOpcionPronosticoId()));

        if (!opcion.getTipoPronostico().getId().equals(tipo.getId())) {
            throw new IllegalArgumentException(
                    "La opcion id=" + opcion.getId() +
                    " no pertenece al tipo de pronostico indicado.");
        }

        if (!opcion.isActivo()) {
            throw new IllegalArgumentException(
                    "La opcion '" + opcion.getCodigo() + "' no esta activa.");
        }

        PronosticoJugado pronostico = new PronosticoJugado();
        pronostico.setJugada(jugada);
        pronostico.setPartido(partido);
        pronostico.setTipoPronostico(tipo);
        pronostico.setOpcionPronostico(opcion);

        return PronosticoJugadoResponse.from(pronosticoRepo.save(pronostico));
    }

    /** Listar los pronósticos de una jugada del usuario. */
    @Transactional(readOnly = true)
    public List<PronosticoJugadoResponse> listarPronosticos(User usuario, Long jugadaId) {
        buscarJugadaPropia(usuario, jugadaId); // valida pertenencia
        return pronosticoRepo.findByJugadaId(jugadaId)
                .stream()
                .map(PronosticoJugadoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Actualizar el tipo y la opción de un pronóstico ya registrado.
     * Reglas:
     *  - La jugada debe estar en estado CREADA.
     *  - No se puede modificar después de la fecha de cierre de la quiniela.
     *  - El partido no cambia.
     *  - Si se cambia el tipo, no debe existir ya otro pronóstico con ese tipo
     *    para el mismo partido dentro de la misma jugada.
     *  - El nuevo tipo debe estar activo.
     *  - La nueva opción debe pertenecer al nuevo tipo y estar activa.
     */
    @Transactional
    public PronosticoJugadoResponse actualizarPronostico(User usuario, Long jugadaId,
                                                         Long pronosticoId,
                                                         ActualizarPronosticoRequest request) {
        Jugada jugada = buscarJugadaPropia(usuario, jugadaId);

        if (jugada.getEstado() != EstadoJugada.CREADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden modificar pronosticos en jugadas con estado CREADA " +
                    "(estado actual: " + jugada.getEstado() + ").");
        }

        LocalDateTime ahora  = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        LocalDateTime cierre = jugada.getQuiniela().getFechaCierre();
        if (ahora.isAfter(cierre)) {
            throw new IllegalArgumentException(
                    "No se pueden modificar pronosticos: la quiniela cerro el " +
                    FechaUtil.format(cierre) + ".");
        }

        PronosticoJugado pronostico = pronosticoRepo
                .findByIdAndJugadaId(pronosticoId, jugadaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pronostico id=" + pronosticoId +
                        " no encontrado en la jugada id=" + jugadaId + "."));

        // Validar que el nuevo tipo exista y esté activo
        TipoPronostico nuevoTipo = tipoRepo.findById(request.getTipoPronosticoId())
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de pronostico no encontrado con id: " +
                        request.getTipoPronosticoId()));

        if (!nuevoTipo.isActivo()) {
            throw new IllegalArgumentException(
                    "El tipo de pronostico '" + nuevoTipo.getCodigo() + "' no esta activo.");
        }

        // Si el tipo cambia, verificar que no exista ya otro pronóstico con ese tipo
        // para el mismo partido en la misma jugada
        boolean cambioTipo = !nuevoTipo.getId().equals(pronostico.getTipoPronostico().getId());
        if (cambioTipo && pronosticoRepo.existsByJugadaIdAndPartidoIdAndTipoPronosticoIdAndIdNot(
                jugadaId, pronostico.getPartido().getId(), nuevoTipo.getId(), pronosticoId)) {
            throw new IllegalArgumentException(
                    "Ya existe un pronostico de tipo '" + nuevoTipo.getCodigo() +
                    "' para el partido id=" + pronostico.getPartido().getId() +
                    " en esta jugada.");
        }

        // Validar que la nueva opción pertenezca al nuevo tipo y esté activa
        OpcionPronostico nuevaOpcion = opcionRepo.findById(request.getOpcionPronosticoId())
                .orElseThrow(() -> new IllegalStateException(
                        "Opcion de pronostico no encontrada con id: " +
                        request.getOpcionPronosticoId()));

        if (!nuevaOpcion.getTipoPronostico().getId().equals(nuevoTipo.getId())) {
            throw new IllegalArgumentException(
                    "La opcion id=" + nuevaOpcion.getId() +
                    " no pertenece al tipo de pronostico '" + nuevoTipo.getCodigo() + "'.");
        }

        if (!nuevaOpcion.isActivo()) {
            throw new IllegalArgumentException(
                    "La opcion '" + nuevaOpcion.getCodigo() + "' no esta activa.");
        }

        pronostico.setTipoPronostico(nuevoTipo);
        pronostico.setOpcionPronostico(nuevaOpcion);
        return PronosticoJugadoResponse.from(pronosticoRepo.save(pronostico));
    }

    /**
     * Eliminar una jugada (ticket) y todos sus pronósticos.
     * Regla: solo se puede eliminar si la jugada está en estado CREADA.
     */
    @Transactional
    public void eliminarJugada(User usuario, Long jugadaId) {
        Jugada jugada = buscarJugadaPropia(usuario, jugadaId);

        if (jugada.getEstado() != EstadoJugada.CREADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden eliminar jugadas en estado CREADA " +
                    "(estado actual: " + jugada.getEstado() + ").");
        }

        jugadaRepo.delete(jugada);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Privados
    // ═════════════════════════════════════════════════════════════════

    /** Busca una jugada y verifica que pertenezca al usuario autenticado. */
    private Jugada buscarJugadaPropia(User usuario, Long jugadaId) {
        Jugada jugada = jugadaRepo.findById(jugadaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Jugada no encontrada con id: " + jugadaId));

        if (!jugada.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException(
                    "No tienes permiso para acceder a esta jugada.");
        }
        return jugada;
    }
}

