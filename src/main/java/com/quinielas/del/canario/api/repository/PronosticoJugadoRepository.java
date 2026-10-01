package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.PronosticoJugado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PronosticoJugadoRepository extends JpaRepository<PronosticoJugado, Long> {

    /** Todos los pronósticos de una jugada. */
    List<PronosticoJugado> findByJugadaId(Long jugadaId);

    /** Pronósticos de una jugada para un partido concreto. */
    List<PronosticoJugado> findByJugadaIdAndPartidoId(Long jugadaId, Long partidoId);

    /** ¿Ya existe un pronóstico de ese tipo para ese partido en la jugada? (crear) */
    boolean existsByJugadaIdAndPartidoIdAndTipoPronosticoId(
            Long jugadaId, Long partidoId, Long tipoPronosticoId);

    /**
     * ¿Existe otro pronóstico de ese tipo para ese partido en la jugada,
     * excluyendo el pronóstico actual? (actualizar)
     */
    boolean existsByJugadaIdAndPartidoIdAndTipoPronosticoIdAndIdNot(
            Long jugadaId, Long partidoId, Long tipoPronosticoId, Long excludeId);

    /**
     * Busca un pronóstico concreto validando que pertenezca a la jugada indicada.
     * Evita que un jugador acceda a pronósticos ajenos usando solo el pronosticoId.
     */
    Optional<PronosticoJugado> findByIdAndJugadaId(Long id, Long jugadaId);

    /**
     * Cuenta los partidos distintos que tienen al menos un pronóstico en la jugada.
     * Se usa para validar que la jugada tenga pronosticados los 8 partidos antes de pagar.
     */
    @Query("SELECT COUNT(DISTINCT p.partido.id) FROM PronosticoJugado p WHERE p.jugada.id = :jugadaId")
    long countPartidosCubiertos(@Param("jugadaId") Long jugadaId);

    /**
     * Obtiene todos los pronósticos de un partido concreto cuya jugada esté en el estado indicado
     * y que aún no hayan sido evaluados.
     * Se usa en el motor de evaluación incremental.
     */
    @Query("""
           SELECT pj FROM PronosticoJugado pj
           JOIN pj.jugada j
           WHERE pj.partido.id   = :partidoId
             AND j.estado        = :estadoJugada
             AND pj.evaluado     = false
           """)
    List<PronosticoJugado> findPendientesDeEvaluacion(
            @Param("partidoId")    Long         partidoId,
            @Param("estadoJugada") com.quinielas.del.canario.api.entity.EstadoJugada estadoJugada);

    /**
     * Todos los pronósticos ya evaluados de un partido.
     * Se usa para revertir puntos antes de recalcular tras corregir un resultado.
     */
    List<PronosticoJugado> findByPartidoIdAndEvaluadoTrue(@Param("partidoId") Long partidoId);

    // ─── Queries para desempate en cierre de quiniela ─────────────────

    /**
     * Cuenta cuántos pronósticos acertados (puntosObtenidos > 0) tiene una jugada
     * para un conjunto de tipos de pronóstico (usado cuando varios tipos empatan
     * en el puntaje máximo del catálogo).
     * Usado en Desempate 1: pronósticos más difíciles.
     */
    @Query("""
           SELECT COUNT(pj) FROM PronosticoJugado pj
           WHERE pj.jugada.id              = :jugadaId
             AND pj.tipoPronostico.id      IN :tipoIds
             AND pj.evaluado               = true
             AND pj.puntosObtenidos        > 0
           """)
    long countAciertosPorTipos(@Param("jugadaId") Long jugadaId,
                               @Param("tipoIds")  List<Long> tipoIds);

    /**
     * Cuenta el total de pronósticos acertados (puntosObtenidos > 0) de una jugada.
     * Usado en Desempate 2: mayor número de aciertos.
     */
    @Query("""
           SELECT COUNT(pj) FROM PronosticoJugado pj
           WHERE pj.jugada.id       = :jugadaId
             AND pj.evaluado        = true
             AND pj.puntosObtenidos > 0
           """)
    long countTotalAciertos(@Param("jugadaId") Long jugadaId);

    /**
     * ¿La jugada acertó al menos un pronóstico en el partido indicado?
     * Usado en Desempate 3: último partido acertado.
     */
    @Query("""
           SELECT COUNT(pj) > 0 FROM PronosticoJugado pj
           WHERE pj.jugada.id       = :jugadaId
             AND pj.partido.id      = :partidoId
             AND pj.evaluado        = true
             AND pj.puntosObtenidos > 0
           """)
    boolean acertoPartido(@Param("jugadaId")  Long jugadaId,
                          @Param("partidoId") Long partidoId);
}

