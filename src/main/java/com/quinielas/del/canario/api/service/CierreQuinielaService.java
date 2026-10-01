package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.CierreQuinielaResponse;
import com.quinielas.del.canario.api.entity.*;
import com.quinielas.del.canario.api.event.JugadaGanadoraEvent;
import com.quinielas.del.canario.api.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Proceso de cierre de quiniela.
 *
 * <h2>Regla fundamental: todos los ganadores comparten SIEMPRE el mismo puntaje</h2>
 * <p>
 * El desempate <b>nunca</b> elige a alguien con menos puntos que otro. Primero se
 * determina el {@code puntajeMaximo} entre todas las jugadas elegibles y se arma el
 * conjunto de <i>candidatas</i> = todas las jugadas que tienen exactamente ese puntaje.
 * La cadena de desempate solo actúa <b>dentro</b> de ese conjunto ya empatado, usando
 * criterios secundarios para decidir cuáles de esas jugadas (con el mismo puntaje)
 * se declaran ganadoras oficiales. En ningún caso el desempate cambia el puntaje
 * ganador ni promueve jugadas con menor puntaje.
 * </p>
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
 *   <li>Filtrar las jugadas con ese puntaje máximo → candidatas al primer lugar
 *       (todas con el mismo puntaje).</li>
 *   <li>Contar cuántos <b>usuarios distintos</b> hay entre las candidatas:
 *     <ul>
 *       <li>Si son ≤ {@link #UMBRAL_DESEMPATE} (5): NO se aplica desempate, todas
 *           las candidatas son ganadoras directamente (criterio {@code MAYOR_PUNTAJE}).</li>
 *       <li>Si son &gt; {@link #UMBRAL_DESEMPATE}: se aplica la cadena de desempate
 *           (ver abajo) para reducir el número de ganadores.</li>
 *     </ul>
 *   </li>
 *   <li>Persistir {@link CierreQuiniela} + {@link GanadorQuiniela}.</li>
 *   <li>Cambiar todas las jugadas {@code ACTIVA} a {@code FINALIZADA}.</li>
 *   <li>Cambiar la quiniela a {@code FINALIZADA}.</li>
 * </ol>
 *
 * <h2>Estrategia de desempate (solo cuando más de 5 usuarios distintos empatan en el puntaje máximo)</h2>
 * <p>Se aplica en cascada; en cuanto un criterio reduce el grupo a ≤5 usuarios distintos,
 * el proceso se detiene y esos son los ganadores. Si un criterio no logra reducir lo
 * suficiente, se pasa al siguiente usando como base el subconjunto ya filtrado.</p>
 * <ol>
 *   <li>Desempate 1 — Pronósticos más difíciles: gana quien acertó más pronósticos
 *       del/los tipo(s) con mayor puntaje definido en el catálogo. Si varios tipos
 *       empatan en el puntaje máximo, se cuentan los aciertos combinados de todos
 *       esos tipos en conjunto.</li>
 *   <li>Desempate 2 — Mayor número de aciertos totales: gana quien acertó más
 *       pronósticos en total (de cualquier tipo).</li>
 *   <li>Desempate 3 — Último partido acertado: gana quien acertó al menos un
 *       pronóstico del último partido de la quiniela (por fecha).</li>
 *   <li>Empate definitivo: si persiste el empate tras los 3 criterios, todos
 *       los finalistas son declarados ganadores (puede ser un número superior a 5;
 *       este umbral solo dispara el desempate, no es un tope máximo de ganadores).</li>
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

    /**
     * Comisión de la casa: porcentaje que se retiene de la bolsa acumulada
     * ANTES de repartir el premio entre los ganadores. 0.10 = 10%.
     */
    private static final BigDecimal PORCENTAJE_COMISION = new BigDecimal("0.10");

    private final QuinielaRepository        quinielaRepo;
    private final PartidoRepository         partidoRepo;
    private final JugadaRepository          jugadaRepo;
    private final CierreQuinielaRepository  cierreRepo;
    private final GanadorQuinielaRepository ganadorRepo;
    private final PronosticoJugadoRepository pronosticoRepo;
    private final TipoPronosticoRepository  tipoPronosticoRepo;
    private final UserProfileRepository     perfilRepo;
    private final ApplicationEventPublisher eventPublisher;

    public CierreQuinielaService(QuinielaRepository quinielaRepo,
                                 PartidoRepository partidoRepo,
                                 JugadaRepository jugadaRepo,
                                 CierreQuinielaRepository cierreRepo,
                                 GanadorQuinielaRepository ganadorRepo,
                                 PronosticoJugadoRepository pronosticoRepo,
                                 TipoPronosticoRepository tipoPronosticoRepo,
                                 UserProfileRepository perfilRepo,
                                 ApplicationEventPublisher eventPublisher) {
        this.quinielaRepo       = quinielaRepo;
        this.partidoRepo        = partidoRepo;
        this.jugadaRepo         = jugadaRepo;
        this.cierreRepo         = cierreRepo;
        this.ganadorRepo        = ganadorRepo;
        this.pronosticoRepo     = pronosticoRepo;
        this.tipoPronosticoRepo = tipoPronosticoRepo;
        this.perfilRepo         = perfilRepo;
        this.eventPublisher     = eventPublisher;
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
            return CierreQuinielaResponse.from(cierre, ganadores, resolverTelefonos(ganadores));
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

        // ── 10. Repartir el premio monetario en partes iguales entre ganadores ──
        // Premio disponible = bolsa acumulada (suma de pagos APROBADOS de la quiniela)
        // MENOS la comision de la casa (10%). Sobre ese resultado neto se reparte
        // en partes iguales entre los ganadores.
        // Se reparte en montos de 2 decimales; si la división deja centavos
        // sobrantes por redondeo, se asignan uno a uno a los primeros ganadores
        // para que la suma exacta de los premios individuales sea igual al premio neto.
        BigDecimal bolsa = quiniela.getBolsaAcumulada() == null
                            ? BigDecimal.ZERO : quiniela.getBolsaAcumulada();
        BigDecimal montoComision = bolsa.multiply(PORCENTAJE_COMISION)
                                         .setScale(2, RoundingMode.HALF_UP);
        BigDecimal premioNeto = bolsa.subtract(montoComision);

        cierre.setBolsaAcumuladaSnapshot(bolsa);
        cierre.setPorcentajeComision(PORCENTAJE_COMISION);
        cierre.setMontoComision(montoComision);
        cierre.setPremioTotalRepartido(premioNeto);
        cierreRepo.save(cierre);

        int totalGanadoresCierre = ganadoras.size();
        BigDecimal montoBase = totalGanadoresCierre > 0
                ? premioNeto.divide(BigDecimal.valueOf(totalGanadoresCierre), 2, RoundingMode.DOWN)
                : BigDecimal.ZERO;
        BigDecimal centavoUnidad = new BigDecimal("0.01");
        BigDecimal restante = premioNeto.subtract(montoBase.multiply(BigDecimal.valueOf(totalGanadoresCierre)));
        int centavosSobrantes = restante.divide(centavoUnidad, 0, RoundingMode.HALF_UP).intValue();

        // ── 11. Persistir GanadorQuiniela ──────────────────────────────
        List<GanadorQuiniela> registrosGanador = new ArrayList<>();
        for (int i = 0; i < ganadoras.size(); i++) {
            Jugada jugada = ganadoras.get(i);
            GanadorQuiniela g = new GanadorQuiniela();
            g.setCierreQuiniela(cierre);
            g.setJugada(jugada);
            g.setUsuario(jugada.getUsuario());
            g.setPuntosObtenidos(jugada.getPuntosObtenidos() == null
                                 ? 0 : jugada.getPuntosObtenidos());
            g.setPosicion(1); // todos los ganadores son posición 1 (empate o único)
            g.setCriterioAplicado(criterio);
            g.setNombreCompleto(resolverNombreCompleto(jugada.getUsuario()));

            BigDecimal montoPremio = montoBase;
            if (i < centavosSobrantes) {
                montoPremio = montoPremio.add(centavoUnidad);
            }
            g.setMontoPremio(montoPremio);
            g.setEstadoPremio(EstadoPremio.PENDIENTE);

            registrosGanador.add(g);
        }
        ganadorRepo.saveAll(registrosGanador);

        String nombreQuiniela = quiniela.getNombre();
        registrosGanador.forEach(g -> eventPublisher.publishEvent(new JugadaGanadoraEvent(
                g.getId(), g.getUsuario().getId(), nombreQuiniela, g.getMontoPremio())));

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

        return CierreQuinielaResponse.from(cierre, registrosGanador, resolverTelefonos(registrosGanador));
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
        return CierreQuinielaResponse.from(cierre, ganadores, resolverTelefonos(ganadores));
    }

    /**
     * Obtiene el cierre para un usuario jugador. Si el usuario NO participó en la quiniela
     * entonces la URL del comprobante 'otros' se oculta en las respuestas de los ganadores.
     */
    @Transactional(readOnly = true)
    public CierreQuinielaResponse obtenerCierre(Long quinielaId, Long usuarioId) {
        CierreQuiniela cierre = cierreRepo.findByQuinielaId(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "La quiniela id=" + quinielaId +
                                " aun no tiene un cierre registrado."));
        List<GanadorQuiniela> ganadores =
                ganadorRepo.findByCierreQuinielaIdOrderByPosicionAsc(cierre.getId());

        CierreQuinielaResponse resp = CierreQuinielaResponse.from(cierre, ganadores, resolverTelefonos(ganadores));

        if (usuarioId == null) return resp;

        // comprobar si el usuario tiene al menos una jugada registrada en la quiniela
        boolean participo = !jugadaRepo.findByUsuarioIdAndQuinielaId(usuarioId, quinielaId).isEmpty();

        if (!participo && resp.getGanadores() != null) {
            // ocultar comprobantePremioOtrosUrl para cada ganador
            resp.getGanadores().forEach(g -> g.setComprobantePremioOtrosUrl(null));
        }
        return resp;
    }

    /** Resuelve el teléfono de perfil de cada usuario ganador, indexado por su id. */
    private Map<Long, String> resolverTelefonos(List<GanadorQuiniela> ganadores) {
        Map<Long, String> telefonos = new HashMap<>();
        for (GanadorQuiniela g : ganadores) {
            Long usuarioId = g.getUsuario().getId();
            telefonos.computeIfAbsent(usuarioId, id ->
                    perfilRepo.findByUserId(id).map(UserProfile::getTelefono).orElse(null));
        }
        return telefonos;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Desempate
    // ─────────────────────────────────────────────────────────────────

    private ResultadoDesempate aplicarDesempate(List<Jugada> candidatas, Long quinielaId) {

        // ── Desempate 1: Pronósticos más difíciles ────────────────────
        // Encontrar el/los TipoPronostico con mayor puntaje en el catálogo.
        // Si varios tipos empatan en el puntaje máximo, se consideran todos en conjunto
        // (se cuentan aciertos combinados de cualquiera de esos tipos).
        List<TipoPronostico> tipos = tipoPronosticoRepo.findByActivoTrue();
        int maxPuntajeTipo = tipos.stream()
                .mapToInt(TipoPronostico::getPuntos)
                .max()
                .orElse(0);
        List<Long> tiposDificilesIds = tipos.stream()
                .filter(t -> t.getPuntos() == maxPuntajeTipo)
                .map(TipoPronostico::getId)
                .collect(Collectors.toList());

        if (!tiposDificilesIds.isEmpty()) {
            Map<Long, Long> aciertosPorJugada = candidatas.stream()
                    .collect(Collectors.toMap(
                            Jugada::getId,
                            j -> pronosticoRepo.countAciertosPorTipos(j.getId(), tiposDificilesIds)));

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
