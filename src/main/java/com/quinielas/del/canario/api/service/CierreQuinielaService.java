package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.CierreQuinielaResponse;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Proceso de cierre de quiniela.
 *
 * <h2>Condiciones necesarias para ejecutar el cierre</h2>
 * <ol>
 *   <li>La quiniela debe estar en estado {@code EN_JUEGO}.</li>
 *   <li>La quiniela NO debe tener ya un registro de cierre (idempotencia).</li>
 *   <li>Todos los partidos deben estar en estado terminal:
 *       {@code FINALIZADO | SUSPENDIDO | POSPUESTO}.</li>
 *   <li>Debe existir al menos una jugada en estado {@code ACTIVA}.</li>
 * </ol>
 *
 * <h2>Algoritmo de cierre (paso a paso)</h2>
 * <ol>
 *   <li>Verificar condiciones previas.</li>
 *   <li>Cargar todas las jugadas {@code ACTIVA} de la quiniela.</li>
 *   <li>Calcular el puntaje máximo entre todas ellas.</li>
 *   <li>Filtrar las jugadas con ese puntaje máximo → candidatas al primer lugar.</li>
 *   <li>Aplicar estrategia de desempate (ver abajo).</li>
 *   <li>Persistir {@link CierreQuiniela} + {@link GanadorQuiniela}.</li>
 *   <li>Cambiar todas las jugadas {@code ACTIVA} a {@code FINALIZADA}.</li>
 *   <li>Cambiar la quiniela a {@code FINALIZADA}.</li>
 * </ol>
 *
 * <h2>Estrategia de desempate (cuando más de 5 usuarios distintos empatan)</h2>
 * <ol>
 *   <li>Desempate 1 — Pronósticos más difíciles: gana quien acertó más pronósticos
 *       del tipo con mayor puntaje definido en el catálogo.</li>
 *   <li>Desempate 2 — Mayor número de aciertos totales: gana quien acertó más
 *       pronósticos en total (de cualquier tipo).</li>
 *   <li>Desempate 3 — Último partido acertado: gana quien acertó al menos un
 *       pronóstico del último partido de la quiniela (por fecha).</li>
 *   <li>Empate definitivo: si persiste el empate tras los 3 criterios, todos
 *       los finalistas son declarados ganadores.</li>
 * </ol>
 */
@Service
public class CierreQuinielaService {

    private static final String CRITERIO_MAYOR_PUNTAJE       = "MAYOR_PUNTAJE";
    private static final String CRITERIO_TIPO_DIFICIL        = "DESEMPATE_TIPO_DIFICIL";
    private static final String CRITERIO_TOTAL_ACIERTOS      = "DESEMPATE_TOTAL_ACIERTOS";
    private static final String CRITERIO_ULTIMO_PARTIDO      = "DESEMPATE_ULTIMO_PARTIDO";
    private static final String CRITERIO_EMPATE_DEFINITIVO   = "EMPATE_DEFINITIVO";

    /** Umbral: se aplican desempates cuando hay más de este número de usuarios distintos empatados. */
    private static final int UMBRAL_DESEMPATE = 5;

    private final QuinielaRepository        quinielaRepo;
    private final PartidoRepository         partidoRepo;
    private final JugadaRepository          jugadaRepo;
    private final CierreQuinielaRepository  cierreRepo;
    private final GanadorQuinielaRepository ganadorRepo;
    private final PronosticoJugadoRepository pronosticoRepo;
    private final TipoPronosticoRepository  tipoPronosticoRepo;
    private final UserProfileRepository     perfilRepo;

    public CierreQuinielaService(QuinielaRepository quinielaRepo,
                                 PartidoRepository partidoRepo,
                                 JugadaRepository jugadaRepo,
                                 CierreQuinielaRepository cierreRepo,
                                 GanadorQuinielaRepository ganadorRepo,
                                 PronosticoJugadoRepository pronosticoRepo,
                                 TipoPronosticoRepository tipoPronosticoRepo,
                                 UserProfileRepository perfilRepo) {
        this.quinielaRepo       = quinielaRepo;
        this.partidoRepo        = partidoRepo;
        this.jugadaRepo         = jugadaRepo;
        this.cierreRepo         = cierreRepo;
        this.ganadorRepo        = ganadorRepo;
        this.pronosticoRepo     = pronosticoRepo;
        this.tipoPronosticoRepo = tipoPronosticoRepo;
        this.perfilRepo         = perfilRepo;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Cierre
    // ─────────────────────────────────────────────────────────────────

    /**
     * Ejecuta el cierre de una quiniela de forma atómica e idempotente.
     *
     * @param quinielaId id de la quiniela a cerrar.
     * @param admin      usuario administrador que dispara el cierre.
     * @return resumen completo del cierre incluyendo ganadores.
     */
    @Transactional
    public CierreQuinielaResponse cerrarQuiniela(Long quinielaId, User admin) {

        // ── 1. Cargar quiniela ────────────────────────────────────────
        Quiniela quiniela = quinielaRepo.findById(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + quinielaId));

        // ── 2. La quiniela debe estar EN_JUEGO ────────────────────────
        if (quiniela.getEstado() != EstadoQuiniela.EN_JUEGO) {
            throw new IllegalArgumentException(
                    "Solo se puede cerrar una quiniela en estado EN_JUEGO " +
                    "(estado actual: " + quiniela.getEstado() + ").");
        }

        // ── 3. Control de idempotencia: solo un cierre por quiniela ───
        if (cierreRepo.existsByQuinielaId(quinielaId)) {
            // Ya se ejecutó: devolver el resultado persistido sin modificar nada
            CierreQuiniela cierre = cierreRepo.findByQuinielaId(quinielaId).get();
            List<GanadorQuiniela> ganadores =
                    ganadorRepo.findByCierreQuinielaIdOrderByPosicionAsc(cierre.getId());
            return CierreQuinielaResponse.from(cierre, ganadores);
        }

        // ── 4. Todos los partidos deben estar en estado terminal ──────
        long pendientes = partidoRepo.countPartidosPendientesDeTerminar(quinielaId);
        if (pendientes > 0) {
            throw new IllegalArgumentException(
                    "No se puede cerrar la quiniela: todavía hay " + pendientes +
                    " partido(s) que no han terminado (deben estar en estado " +
                    "FINALIZADO, SUSPENDIDO o POSPUESTO).");
        }

        // ── 5. Cargar jugadas elegibles (ACTIVA) ──────────────────────
        List<Jugada> elegibles = jugadaRepo
                .findByQuinielaIdAndEstado(quinielaId, EstadoJugada.ACTIVA);

        if (elegibles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay jugadas ACTIVAS en la quiniela. " +
                    "No se puede determinar un ganador.");
        }

        // ── 6. Calcular puntaje máximo ────────────────────────────────
        int puntajeMaximo = elegibles.stream()
                .mapToInt(j -> j.getPuntosObtenidos() == null ? 0 : j.getPuntosObtenidos())
                .max()
                .orElse(0);

        // ── 7. Candidatas: todas las jugadas con puntaje máximo ───────
        List<Jugada> candidatas = elegibles.stream()
                .filter(j -> (j.getPuntosObtenidos() == null ? 0 : j.getPuntosObtenidos())
                             == puntajeMaximo)
                .collect(Collectors.toList());

        // ── 8. Estrategia de desempate ────────────────────────────────────────
        // Contar usuarios distintos entre las candidatas
        long usuariosDistintos = candidatas.stream()
                .map(j -> j.getUsuario().getId())
                .distinct().count();

        List<Jugada> ganadoras;
        String       criterio;
        boolean      esEmpate;

        if (usuariosDistintos <= UMBRAL_DESEMPATE) {
            // Ganador único o grupo pequeño: se resuelve solo por mayor puntaje, sin desempate
            ganadoras = candidatas;
            criterio  = CRITERIO_MAYOR_PUNTAJE;
            esEmpate  = candidatas.size() > 1;
        } else {
            // Más de UMBRAL usuarios distintos empatados: aplicar cadena de desempate
            ResultadoDesempate rd = aplicarDesempate(candidatas, quinielaId);
            ganadoras = rd.jugadas;
            criterio  = rd.criterio;
            esEmpate  = rd.jugadas.size() > 1;
        }

        // ── 9. Persistir CierreQuiniela ───────────────────────────────
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"));

        CierreQuiniela cierre = new CierreQuiniela();
        cierre.setQuiniela(quiniela);
        cierre.setPuntajeMaximo(puntajeMaximo);
        cierre.setTotalJugadasElegibles(elegibles.size());
        cierre.setTotalGanadores(ganadoras.size());
        cierre.setEsEmpate(esEmpate);
        cierre.setCriterioDesempate(criterio);
        cierre.setCerradoPor(admin);
        cierre.setFechaCierre(ahora);
        cierreRepo.save(cierre);

        // ── 10. Persistir GanadorQuiniela ─────────────────────────────
        List<GanadorQuiniela> registrosGanador = new ArrayList<>();
        for (Jugada jugada : ganadoras) {
            GanadorQuiniela g = new GanadorQuiniela();
            g.setCierreQuiniela(cierre);
            g.setJugada(jugada);
            g.setUsuario(jugada.getUsuario());
            g.setPuntosObtenidos(jugada.getPuntosObtenidos() == null
                                 ? 0 : jugada.getPuntosObtenidos());
            g.setPosicion(1); // todos los ganadores son posición 1 (empate o único)
            g.setCriterioAplicado(criterio);
            g.setNombreCompleto(resolverNombreCompleto(jugada.getUsuario()));
            registrosGanador.add(g);
        }
        ganadorRepo.saveAll(registrosGanador);

        // ── 11. Actualizar posición final y esGanadora en TODAS las jugadas elegibles
        Set<Long> idsGanadoras = ganadoras.stream()
                .map(Jugada::getId)
                .collect(Collectors.toSet());

        // Ordenar elegibles por puntos DESC para asignar posición (ranking denso)
        elegibles.sort(Comparator.comparingInt(
                (Jugada j) -> j.getPuntosObtenidos() == null ? -1 : j.getPuntosObtenidos())
                .reversed());

        int posActual    = 1;
        int contador     = 0;
        Integer ptosAnt  = null;

        for (Jugada jugada : elegibles) {
            contador++;
            Integer pts = jugada.getPuntosObtenidos();
            if (!Objects.equals(ptosAnt, pts)) {
                posActual = contador;
            }
            ptosAnt = pts;
            jugada.setPosicionFinal(posActual);
            jugada.setEsGanadora(idsGanadoras.contains(jugada.getId()));
            jugada.setEstado(EstadoJugada.FINALIZADA);
        }
        jugadaRepo.saveAll(elegibles);

        // ── 12. Pasar quiniela a FINALIZADA ───────────────────────────
        quiniela.setEstado(EstadoQuiniela.FINALIZADA);
        quinielaRepo.save(quiniela);

        return CierreQuinielaResponse.from(cierre, registrosGanador);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Consulta del resultado
    // ─────────────────────────────────────────────────────────────────

    /**
     * Obtiene el resultado de cierre de una quiniela ya finalizada.
     * Si la quiniela no tiene registro de cierre lanza excepción.
     */
    @Transactional(readOnly = true)
    public CierreQuinielaResponse obtenerCierre(Long quinielaId) {
        CierreQuiniela cierre = cierreRepo.findByQuinielaId(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "La quiniela id=" + quinielaId +
                        " aun no tiene un cierre registrado."));
        List<GanadorQuiniela> ganadores =
                ganadorRepo.findByCierreQuinielaIdOrderByPosicionAsc(cierre.getId());
        return CierreQuinielaResponse.from(cierre, ganadores);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Desempate
    // ─────────────────────────────────────────────────────────────────

    private ResultadoDesempate aplicarDesempate(List<Jugada> candidatas, Long quinielaId) {

        // ── Desempate 1: Pronósticos más difíciles ────────────────────
        // Encontrar el TipoPronostico con mayor puntaje en el catálogo
        List<TipoPronostico> tipos = tipoPronosticoRepo.findByActivoTrue();
        TipoPronostico tipoDificil = tipos.stream()
                .max(Comparator.comparingInt(TipoPronostico::getPuntos))
                .orElse(null);

        if (tipoDificil != null) {
            final Long tipoId = tipoDificil.getId();
            Map<Long, Long> aciertosPorJugada = candidatas.stream()
                    .collect(Collectors.toMap(
                            Jugada::getId,
                            j -> pronosticoRepo.countAciertosPorTipo(j.getId(), tipoId)));

            long maxAciertos1 = aciertosPorJugada.values().stream()
                    .mapToLong(Long::longValue).max().orElse(0);

            List<Jugada> tras1 = candidatas.stream()
                    .filter(j -> aciertosPorJugada.get(j.getId()) == maxAciertos1)
                    .collect(Collectors.toList());

            if (distinctUsers(tras1) <= UMBRAL_DESEMPATE) {
                return new ResultadoDesempate(tras1, CRITERIO_TIPO_DIFICIL);
            }
            candidatas = tras1; // reducir para el siguiente criterio
        }

        // ── Desempate 2: Mayor número de aciertos totales ─────────────
        Map<Long, Long> totalAciertosPorJugada = candidatas.stream()
                .collect(Collectors.toMap(
                        Jugada::getId,
                        j -> pronosticoRepo.countTotalAciertos(j.getId())));

        long maxTotal = totalAciertosPorJugada.values().stream()
                .mapToLong(Long::longValue).max().orElse(0);

        List<Jugada> tras2 = candidatas.stream()
                .filter(j -> totalAciertosPorJugada.get(j.getId()) == maxTotal)
                .collect(Collectors.toList());

        if (distinctUsers(tras2) <= UMBRAL_DESEMPATE) {
            return new ResultadoDesempate(tras2, CRITERIO_TOTAL_ACIERTOS);
        }
        candidatas = tras2;

        // ── Desempate 3: Último partido acertado ──────────────────────
        List<Partido> partidos = partidoRepo.findByQuinielaIdOrderByFechaPartidoDesc(quinielaId);
        if (!partidos.isEmpty()) {
            Long ultimoPartidoId = partidos.get(0).getId();

            List<Jugada> tras3 = candidatas.stream()
                    .filter(j -> pronosticoRepo.acertoPartido(j.getId(), ultimoPartidoId))
                    .collect(Collectors.toList());

            // Si tras el filtro quedan candidatas, usarlas; si ninguna acertó, mantener todas
            if (!tras3.isEmpty()) {
                if (distinctUsers(tras3) <= UMBRAL_DESEMPATE) {
                    return new ResultadoDesempate(tras3, CRITERIO_ULTIMO_PARTIDO);
                }
                candidatas = tras3;
            }
        }

        // ── Empate definitivo ─────────────────────────────────────────
        return new ResultadoDesempate(candidatas, CRITERIO_EMPATE_DEFINITIVO);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Utilidades privadas
    // ─────────────────────────────────────────────────────────────────

    private long distinctUsers(List<Jugada> jugadas) {
        return jugadas.stream().map(j -> j.getUsuario().getId()).distinct().count();
    }

    /**
    /**
     * Guarda solo el primer nombre del ganador desde su perfil.
     * Si el perfil no existe o no tiene nombre, retorna cadena vacía.
     */
    private String resolverNombreCompleto(User usuario) {
        return perfilRepo.findByUserId(usuario.getId())
                .map(p -> p.getNombre() != null ? p.getNombre().trim() : "")
                .orElse("");
    }

    // ─── Clase auxiliar para retorno de desempate ─────────────────────
    private record ResultadoDesempate(List<Jugada> jugadas, String criterio) {}
}
