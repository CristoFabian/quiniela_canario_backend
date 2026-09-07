package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.EstadoJugada;
import com.quinielas.del.canario.api.entity.Jugada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JugadaRepository extends JpaRepository<Jugada, Long> {

    /** Todas las jugadas de un usuario (todos sus tickets en todas las quinielas). */
    List<Jugada> findByUsuarioId(Long usuarioId);

    /** Todas las jugadas de una quiniela (admin). */
    List<Jugada> findByQuinielaId(Long quinielaId);

    /** Todos los tickets de un usuario en una quiniela concreta. */
    List<Jugada> findByUsuarioIdAndQuinielaId(Long usuarioId, Long quinielaId);

    /** Jugadas filtradas por estado. */
    List<Jugada> findByEstado(EstadoJugada estado);

    /** Conteo de jugadas activas en una quiniela (para el dashboard). */
    long countByQuinielaIdAndEstado(Long quinielaId, EstadoJugada estado);

    /** Jugadas de una quiniela filtradas por estado. Usado en el cierre. */
    List<Jugada> findByQuinielaIdAndEstado(Long quinielaId, EstadoJugada estado);

    /**
     * Jugadas de una quiniela visibles en el ranking:
     * estado ACTIVA o FINALIZADA, ordenadas por puntos DESC (nulls al final).
     * Se usa para construir el ranking en tiempo real y al cierre.
     */
    @Query("""
           SELECT j FROM Jugada j
           WHERE j.quiniela.id = :quinielaId
             AND j.estado IN (
                 com.quinielas.del.canario.api.entity.EstadoJugada.ACTIVA,
                 com.quinielas.del.canario.api.entity.EstadoJugada.FINALIZADA)
           ORDER BY COALESCE(j.puntosObtenidos, -1) DESC
           """)
    List<Jugada> findRankingByQuinielaId(
            @Param("quinielaId") Long quinielaId);

    /**
     * Jugadas de una quiniela en estado CREADA o PENDIENTE_VALIDACION.
     * Se usa para expirar jugadas sin pago confirmado al iniciar la quiniela.
     */
    @Query("""
           SELECT j FROM Jugada j
           WHERE j.quiniela.id = :quinielaId
             AND j.estado IN (
                 com.quinielas.del.canario.api.entity.EstadoJugada.CREADA,
                 com.quinielas.del.canario.api.entity.EstadoJugada.PENDIENTE_VALIDACION)
           """)
    List<Jugada> findNoConfirmadasByQuinielaId(@Param("quinielaId") Long quinielaId);
}
