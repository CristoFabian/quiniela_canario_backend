package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.EvaluacionPartidoResponse;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.repository.JugadaRepository;
import com.quinielas.del.canario.api.repository.PartidoRepository;
import com.quinielas.del.canario.api.repository.PronosticoJugadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de evaluación de pronósticos.
 *
 * <p>La evaluación es <b>incremental</b>: se dispara partido por partido
 * en cuanto éste queda en estado {@link EstadoPartido#FINALIZADO}.
 *
 * <p>Reglas generales:
 * <ul>
 *   <li>Solo se evalúan jugadas en estado {@link EstadoJugada#ACTIVA}.</li>
 *   <li>Partidos {@link EstadoPartido#SUSPENDIDO} o {@link EstadoPartido#POSPUESTO}
 *       NO suman puntos — sus pronósticos se marcan como evaluados con 0 puntos.</li>
 *   <li>Un pronóstico ya evaluado ({@code evaluado = true}) nunca se procesa dos veces.</li>
 * </ul>
 *
 * <p>Lógica de puntuación por tipo de pronóstico:
 * <table border="1">
 *   <tr><th>CODIGO tipo</th><th>¿Acierta si…?</th></tr>
 *   <tr><td>RESULTADO</td>
 *       <td>El código de la opción elegida coincide con el resultado
 *           calculado (LOCAL_WIN | EMPATE | VISITANTE_WIN).</td></tr>
 *   <tr><td>GOLES</td>
 *       <td>La suma de goles (marcadorLocal + marcadorVisitante) cae
 *           dentro del rango [valorMin, valorMax] de la opción (valorMax
 *           null = sin límite superior).</td></tr>
 *   <tr><td>BTTS</td>
 *       <td>El código de la opción empieza por "BTTS_SI" o "BTTS_NO"
 *           y coincide con el valor real de {@code partido.ambosMarcan}.</td></tr>
 *   <tr><td>CORNERS</td>
 *       <td>El total de córners cae dentro del rango [valorMin, valorMax]
 *           de la opción (igual que GOLES).</td></tr>
 * </table>
 */
@Service
public class EvaluacionService {

    private final PartidoRepository          partidoRepo;
    private final PronosticoJugadoRepository pronosticoRepo;
    private final JugadaRepository           jugadaRepo;

    public EvaluacionService(PartidoRepository partidoRepo,
                             PronosticoJugadoRepository pronosticoRepo,
                             JugadaRepository jugadaRepo) {
        this.partidoRepo    = partidoRepo;
        this.pronosticoRepo = pronosticoRepo;
        this.jugadaRepo     = jugadaRepo;
    }

    // ─────────────────────────────────────────────────────────────────
    //  API pública
    // ─────────────────────────────────────────────────────────────────

    /**
     * Evalúa todos los pronósticos pendientes de un partido FINALIZADO,
     * SUSPENDIDO o POSPUESTO y actualiza los puntos acumulados de cada jugada.
     *
     * @param partidoId id del partido a evaluar.
     * @return resumen de la evaluación.
     * @throws IllegalArgumentException si el partido no cumple las condiciones.
     */
    @Transactional
    public EvaluacionPartidoResponse evaluarPartido(Long partidoId) {

        Partido partido = partidoRepo.findById(partidoId)
                .orElseThrow(() -> new IllegalStateException(
                        "Partido no encontrado con id: " + partidoId));

        // Solo se pueden evaluar partidos en estado terminal
        if (partido.getEstado() != EstadoPartido.FINALIZADO &&
            partido.getEstado() != EstadoPartido.SUSPENDIDO &&
            partido.getEstado() != EstadoPartido.POSPUESTO) {
            throw new IllegalArgumentException(
                    "Solo se pueden evaluar partidos en estado FINALIZADO, " +
                    "SUSPENDIDO o POSPUESTO (estado actual: " + partido.getEstado() + ").");
        }

        // Para FINALIZADO se requieren resultados completos
        if (partido.getEstado() == EstadoPartido.FINALIZADO) {
            if (partido.getMarcadorLocal()     == null ||
                partido.getMarcadorVisitante() == null ||
                partido.getTotalCorners()      == null ||
                partido.getAmbosMarcan()       == null) {
                throw new IllegalArgumentException(
                        "El partido no tiene todos los resultados cargados " +
                        "(marcador local, marcador visitante, total corners, ambos marcan).");
            }
        }

        // Obtener pronósticos pendientes de evaluación de jugadas ACTIVAS
        List<PronosticoJugado> pendientes = pronosticoRepo
                .findPendientesDeEvaluacion(partidoId, EstadoJugada.ACTIVA);

        if (pendientes.isEmpty()) {
            // Construir respuesta vacía (ya evaluado o sin jugadas activas)
            EvaluacionPartidoResponse respuesta = new EvaluacionPartidoResponse();
            respuesta.setPartidoId(partidoId);
            respuesta.setEquipoLocal(partido.getEquipoLocal());
            respuesta.setEquipoVisitante(partido.getEquipoVisitante());
            respuesta.setPronosticosEvaluados(0);
            respuesta.setJugadasAfectadas(0);
            respuesta.setJugadas(Collections.emptyList());
            return respuesta;
        }

        // Determinar si el partido puntúa (SUSPENDIDO/POSPUESTO → 0 puntos)
        boolean puntua = partido.getEstado() == EstadoPartido.FINALIZADO;

        // Agrupar por jugada para actualizar puntosObtenidos una sola vez
        Map<Long, List<PronosticoJugado>> porJugada = pendientes.stream()
                .collect(Collectors.groupingBy(pj -> pj.getJugada().getId()));

        List<EvaluacionPartidoResponse.ResumenJugadaEvaluada> resumenes = new ArrayList<>();
        List<PronosticoJugado> aGuardar = new ArrayList<>();
        List<Jugada>           jugadasActualizadas = new ArrayList<>();

        for (Map.Entry<Long, List<PronosticoJugado>> entry : porJugada.entrySet()) {
            Jugada jugada = entry.getValue().get(0).getJugada();
            int puntosEstePartido = 0;

            for (PronosticoJugado pj : entry.getValue()) {
                int puntosPronostico = 0;
                if (puntua) {
                    puntosPronostico = calcularPuntos(pj, partido);
                }
                pj.setPuntosObtenidos(puntosPronostico);
                pj.setEvaluado(true);
                aGuardar.add(pj);
                puntosEstePartido += puntosPronostico;
            }

            // Acumular puntos en la jugada
            int acumulado = (jugada.getPuntosObtenidos() == null ? 0 : jugada.getPuntosObtenidos())
                            + puntosEstePartido;
            jugada.setPuntosObtenidos(acumulado);
            jugadasActualizadas.add(jugada);

            // Armar resumen
            EvaluacionPartidoResponse.ResumenJugadaEvaluada resumen =
                    new EvaluacionPartidoResponse.ResumenJugadaEvaluada();
            resumen.setJugadaId(jugada.getId());
            resumen.setUsuarioId(jugada.getUsuario().getId());
            resumen.setUsername(jugada.getUsuario().getUsername());
            resumen.setPuntosEstePartido(puntosEstePartido);
            resumen.setPuntosAcumulados(acumulado);
            resumenes.add(resumen);
        }

        pronosticoRepo.saveAll(aGuardar);
        jugadaRepo.saveAll(jugadasActualizadas);

        EvaluacionPartidoResponse respuesta = new EvaluacionPartidoResponse();
        respuesta.setPartidoId(partidoId);
        respuesta.setEquipoLocal(partido.getEquipoLocal());
        respuesta.setEquipoVisitante(partido.getEquipoVisitante());
        respuesta.setPronosticosEvaluados(aGuardar.size());
        respuesta.setJugadasAfectadas(jugadasActualizadas.size());
        respuesta.setJugadas(resumenes);
        return respuesta;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Lógica de puntuación
    // ─────────────────────────────────────────────────────────────────

    /**
     * Calcula los puntos que merece un pronóstico dada la información real del partido.
     * Devuelve los puntos del {@link TipoPronostico} si acierta, o 0 si falla.
     */
    private int calcularPuntos(PronosticoJugado pj, Partido partido) {
        TipoPronostico tipo = pj.getTipoPronostico();
        OpcionPronostico opcion = pj.getOpcionPronostico();

        return switch (tipo.getCodigo().toUpperCase()) {
            case "RESULTADO"  -> evaluarResultado(opcion, partido)  ? tipo.getPuntos() : 0;
            case "GOLES"      -> evaluarGoles(opcion, partido)      ? tipo.getPuntos() : 0;
            case "BTTS"       -> evaluarBtts(opcion, partido)       ? tipo.getPuntos() : 0;
            case "CORNERS"    -> evaluarCorners(opcion, partido)    ? tipo.getPuntos() : 0;
            default           -> 0; // tipo desconocido → no puntúa
        };
    }

    /**
     * RESULTADO: LOCAL_WIN | DRAW | AWAY_WIN
     * El resultado real se calcula comparando los marcadores
     * y se contrasta con el código de la opción del catálogo.
     */
    private boolean evaluarResultado(OpcionPronostico opcion, Partido partido) {
        int local     = partido.getMarcadorLocal();
        int visitante = partido.getMarcadorVisitante();

        String resultadoReal;
        if      (local > visitante)  resultadoReal = "LOCAL_WIN";
        else if (local == visitante) resultadoReal = "DRAW";
        else                         resultadoReal = "AWAY_WIN";

        return resultadoReal.equalsIgnoreCase(opcion.getCodigo());
    }

    /**
     * GOLES: la suma de goles debe caer dentro de [valorMin, valorMax].
     * Si valorMax es null, no hay límite superior (6+).
     */
    private boolean evaluarGoles(OpcionPronostico opcion, Partido partido) {
        int totalGoles = partido.getMarcadorLocal() + partido.getMarcadorVisitante();
        return enRango(totalGoles, opcion.getValorMin(), opcion.getValorMax());
    }

    /**
     * BTTS: la opción con código que empieza por "BTTS_SI" = ambos marcan (true),
     * "BTTS_NO" = no ambos marcan (false).
     */
    private boolean evaluarBtts(OpcionPronostico opcion, Partido partido) {
        boolean ambosMarcanReal = Boolean.TRUE.equals(partido.getAmbosMarcan());
        String  codigo          = opcion.getCodigo().toUpperCase();

        if (codigo.startsWith("BTTS_SI"))  return ambosMarcanReal;
        if (codigo.startsWith("BTTS_NO"))  return !ambosMarcanReal;
        return false;
    }

    /**
     * CORNERS: totalCorners del partido debe caer en [valorMin, valorMax].
     */
    private boolean evaluarCorners(OpcionPronostico opcion, Partido partido) {
        if (partido.getTotalCorners() == null) return false;
        return enRango(partido.getTotalCorners(), opcion.getValorMin(), opcion.getValorMax());
    }

    /**
     * Comprueba si {@code valor} está dentro del rango [min, max].
     * Si max es null, el rango es abierto por arriba (≥ min).
     */
    private boolean enRango(int valor, Integer min, Integer max) {
        if (min != null && valor < min) return false;
        if (max != null && valor > max) return false;
        return true;
    }
}

