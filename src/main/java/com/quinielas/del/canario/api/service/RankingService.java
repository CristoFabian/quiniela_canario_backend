package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.PronosticoDetalleAdminResponse;
import com.quinielas.del.canario.api.dto.PronosticoJugadoResponse;
import com.quinielas.del.canario.api.dto.RankingQuinielaAdminDetalleResponse;
import com.quinielas.del.canario.api.dto.RankingQuinielaResponse;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de ranking de jugadores para una quiniela.
 *
 * <p>El ranking es accesible tanto por jugadores como por administradores.
 * Muestra todas las jugadas en estado ACTIVA o FINALIZADA ordenadas por puntos.
 *
 * <p>Reglas de posicionamiento (ranking denso):
 * <ul>
 *   <li>Si dos jugadas tienen el mismo puntaje, comparten la misma posición.</li>
 *   <li>La siguiente posición es la inmediatamente posterior al número de jugadas
 *       con mejor puntaje, no al número de posición anterior + 1.<br>
 *       Ejemplo: dos jugadas en posición 1 → la siguiente es posición 2.</li>
 *   <li>Jugadas sin puntos evaluados aún (null / 0) aparecen al final.</li>
 * </ul>
 *
 * <p>Cuando la quiniela está FINALIZADA, se marca {@code esGanador = true} a las jugadas
 * que aparezcan en el registro {@link GanadorQuiniela}.
 */
@Service
public class RankingService {

    private final QuinielaRepository         quinielaRepo;
    private final JugadaRepository           jugadaRepo;
    private final UserProfileRepository      perfilRepo;
    private final CierreQuinielaRepository   cierreRepo;
    private final GanadorQuinielaRepository  ganadorRepo;
    private final PronosticoJugadoRepository pronosticoRepo;

    public RankingService(QuinielaRepository quinielaRepo,
                          JugadaRepository jugadaRepo,
                          UserProfileRepository perfilRepo,
                          CierreQuinielaRepository cierreRepo,
                          GanadorQuinielaRepository ganadorRepo,
                          PronosticoJugadoRepository pronosticoRepo) {
        this.quinielaRepo   = quinielaRepo;
        this.jugadaRepo     = jugadaRepo;
        this.perfilRepo     = perfilRepo;
        this.cierreRepo     = cierreRepo;
        this.ganadorRepo    = ganadorRepo;
        this.pronosticoRepo = pronosticoRepo;
    }

    /**
     * Devuelve el ranking de una quiniela.
     * Disponible mientras la quiniela está EN_JUEGO (ranking en tiempo real)
     * y también cuando ya está FINALIZADA (ranking definitivo con ganadores marcados).
     *
     * @param quinielaId id de la quiniela.
     * @return ranking completo ordenado por posición.
     */
    @Transactional(readOnly = true)
    public RankingQuinielaResponse obtenerRanking(Long quinielaId) {

        // ── 1. Validar quiniela ───────────────────────────────────────
        Quiniela quiniela = quinielaRepo.findById(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + quinielaId));

        EstadoQuiniela estado = quiniela.getEstado();
        if (estado == EstadoQuiniela.CREADA || estado == EstadoQuiniela.ABIERTA) {
            throw new IllegalArgumentException(
                    "El ranking solo esta disponible cuando la quiniela esta EN_JUEGO o FINALIZADA " +
                    "(estado actual: " + estado + ").");
        }

        // ── 2. Jugadas visibles ordenadas por puntos DESC ─────────────
        List<Jugada> jugadas = jugadaRepo.findRankingByQuinielaId(quinielaId);

        // ── 3. Datos del cierre (solo si ya está FINALIZADA) ──────────
        Set<Long>  idsGanadoras     = new HashSet<>();
        String     criterioTexto    = null;

        if (estado == EstadoQuiniela.FINALIZADA) {
            Optional<CierreQuiniela> cierreOpt = cierreRepo.findByQuinielaId(quinielaId);
            if (cierreOpt.isPresent()) {
                CierreQuiniela cierre = cierreOpt.get();
                criterioTexto = traducirCriterio(cierre.getCriterioDesempate());
                ganadorRepo.findByCierreQuinielaIdOrderByPosicionAsc(cierre.getId())
                           .forEach(g -> idsGanadoras.add(g.getJugada().getId()));
            }
        }

        // ── 4. Construir filas con posición densa ─────────────────────
        List<RankingQuinielaResponse.PosicionRanking> filas = new ArrayList<>();
        int     posicionActual = 1;
        int     contadorFila   = 0;
        Integer puntosAntes    = null;

        for (Jugada jugada : jugadas) {
            contadorFila++;
            Integer puntos = jugada.getPuntosObtenidos();

            boolean cambioPuntos = !Objects.equals(puntosAntes, puntos);
            if (cambioPuntos) {
                posicionActual = contadorFila;
            }
            puntosAntes = puntos;

            RankingQuinielaResponse.PosicionRanking fila = new RankingQuinielaResponse.PosicionRanking();
            fila.setJugadaId(jugada.getId());
            fila.setPosicion(posicionActual);
            fila.setNombreJugador(resolverPrimerNombre(jugada.getUsuario()));
            fila.setPuntosObtenidos(puntos);
            fila.setEsGanador(idsGanadoras.contains(jugada.getId()));
            fila.setPronosticos(obtenerPronosticosEvaluados(jugada.getId()));
            filas.add(fila);
        }

        // ── 5. Armar respuesta ────────────────────────────────────────
        RankingQuinielaResponse response = new RankingQuinielaResponse();
        response.setQuinielaId(quinielaId);
        response.setNombreQuiniela(quiniela.getNombre());
        response.setEstadoQuiniela(estado.name());
        response.setTotalParticipantes(jugadas.size());
        response.setCriteriosAplicados(criterioTexto);
        response.setRanking(filas);
        return response;
    }

    /**
     * Ranking con detalle completo para el administrador: por cada jugada incluye
     * TODOS sus pronosticos (evaluados o no) junto con el resultado real del partido,
     * para que se sepa exactamente que eligio el jugador y el porque de su puntaje.
     * A diferencia de {@link #obtenerRanking}, no oculta pronosticos de partidos aun
     * no jugados, ya que el administrador no obtiene ventaja competitiva al verlos.
     */
    @Transactional(readOnly = true)
    public RankingQuinielaAdminDetalleResponse obtenerRankingDetalleAdmin(Long quinielaId) {

        Quiniela quiniela = quinielaRepo.findById(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + quinielaId));

        EstadoQuiniela estado = quiniela.getEstado();
        if (estado == EstadoQuiniela.CREADA || estado == EstadoQuiniela.ABIERTA) {
            throw new IllegalArgumentException(
                    "El ranking solo esta disponible cuando la quiniela esta EN_JUEGO o FINALIZADA " +
                    "(estado actual: " + estado + ").");
        }

        List<Jugada> jugadas = jugadaRepo.findRankingByQuinielaId(quinielaId);

        Set<Long> idsGanadoras  = new HashSet<>();
        String    criterioTexto = null;
        if (estado == EstadoQuiniela.FINALIZADA) {
            Optional<CierreQuiniela> cierreOpt = cierreRepo.findByQuinielaId(quinielaId);
            if (cierreOpt.isPresent()) {
                CierreQuiniela cierre = cierreOpt.get();
                criterioTexto = traducirCriterio(cierre.getCriterioDesempate());
                ganadorRepo.findByCierreQuinielaIdOrderByPosicionAsc(cierre.getId())
                           .forEach(g -> idsGanadoras.add(g.getJugada().getId()));
            }
        }

        List<RankingQuinielaAdminDetalleResponse.PosicionRankingDetalle> filas = new ArrayList<>();
        int     posicionActual = 1;
        int     contadorFila   = 0;
        Integer puntosAntes    = null;

        for (Jugada jugada : jugadas) {
            contadorFila++;
            Integer puntos = jugada.getPuntosObtenidos();

            boolean cambioPuntos = !Objects.equals(puntosAntes, puntos);
            if (cambioPuntos) {
                posicionActual = contadorFila;
            }
            puntosAntes = puntos;

            RankingQuinielaAdminDetalleResponse.PosicionRankingDetalle fila =
                    new RankingQuinielaAdminDetalleResponse.PosicionRankingDetalle();
            fila.setJugadaId(jugada.getId());
            fila.setPosicion(posicionActual);
            fila.setNombreJugador(resolverPrimerNombre(jugada.getUsuario()));
            fila.setPuntosObtenidos(puntos);
            fila.setEsGanador(idsGanadoras.contains(jugada.getId()));
            fila.setPronosticos(
                    pronosticoRepo.findByJugadaId(jugada.getId()).stream()
                            .map(PronosticoDetalleAdminResponse::from)
                            .collect(Collectors.toList()));
            filas.add(fila);
        }

        RankingQuinielaAdminDetalleResponse response = new RankingQuinielaAdminDetalleResponse();
        response.setQuinielaId(quinielaId);
        response.setNombreQuiniela(quiniela.getNombre());
        response.setEstadoQuiniela(estado.name());
        response.setTotalParticipantes(jugadas.size());
        response.setCriteriosAplicados(criterioTexto);
        response.setRanking(filas);
        return response;
    }

    // ──────────────────────────────────────────────────────────────
    //  Privados
    // ─────────────────────────────────────────────────────────────────

    /**
     * Pronósticos ya evaluados de una jugada, para dar transparencia sobre el porqué
     * de su puntaje en el ranking. Se excluyen los pronósticos de partidos aun no jugados
     * para no revelar elecciones de otros jugadores en partidos todavia pronosticables.
     */
    private List<PronosticoJugadoResponse> obtenerPronosticosEvaluados(Long jugadaId) {
        return pronosticoRepo.findByJugadaId(jugadaId).stream()
                .filter(PronosticoJugado::isEvaluado)
                .map(PronosticoJugadoResponse::from)
                .collect(Collectors.toList());
    }

    /** Devuelve solo el primer nombre del perfil del jugador. */
    private String resolverPrimerNombre(User usuario) {
        return perfilRepo.findByUserId(usuario.getId())
                .map(p -> p.getNombre() != null ? p.getNombre().trim() : "")
                .orElse("");
    }

    /** Traduce el código interno del criterio a un texto legible para el jugador. */
    private String traducirCriterio(String codigo) {
        if (codigo == null) return null;
        return switch (codigo) {
            case "MAYOR_PUNTAJE"             -> "Mayor puntaje";
            case "DESEMPATE_TIPO_DIFICIL"    -> "Desempate: pronosticos mas dificiles";
            case "DESEMPATE_TOTAL_ACIERTOS"  -> "Desempate: mayor numero de aciertos";
            case "DESEMPATE_ULTIMO_PARTIDO"  -> "Desempate: ultimo partido acertado";
            case "EMPATE_DEFINITIVO"         -> "Empate definitivo";
            default                          -> codigo;
        };
    }
}
