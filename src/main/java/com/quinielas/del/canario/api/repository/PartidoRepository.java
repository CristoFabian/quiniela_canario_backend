package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.EstadoPartido;
import com.quinielas.del.canario.api.entity.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    /** Todos los partidos de una quiniela. */
    List<Partido> findByQuinielaId(Long quinielaId);

    /**
     * Cuenta los partidos de una quiniela.
     * Se usa para aplicar la regla de negocio: máximo 8 partidos por quiniela.
     */
    long countByQuinielaId(Long quinielaId);

    /**
     * Cuenta los partidos de una quiniela filtrados por estado.
     * Se usa para validar que todos los partidos estén FINALIZADOS antes
     * de cerrar la quiniela.
     */
    long countByQuinielaIdAndEstado(Long quinielaId, EstadoPartido estado);

    /**
     * Cuenta los partidos de una quiniela que NO están en ninguno de los estados
     * terminales (FINALIZADO, SUSPENDIDO, POSPUESTO).
     * Si devuelve 0, todos los partidos han concluido y la quiniela puede cerrarse.
     */
    @org.springframework.data.jpa.repository.Query("""
           SELECT COUNT(p) FROM Partido p
           WHERE p.quiniela.id = :quinielaId
             AND p.estado NOT IN (
                 com.quinielas.del.canario.api.entity.EstadoPartido.FINALIZADO,
                 com.quinielas.del.canario.api.entity.EstadoPartido.SUSPENDIDO,
                 com.quinielas.del.canario.api.entity.EstadoPartido.POSPUESTO)
           """)
    long countPartidosPendientesDeTerminar(
            @org.springframework.data.repository.query.Param("quinielaId") Long quinielaId);

    /**
     * Devuelve los partidos de la quiniela ordenados por fechaPartido descendente.
     * El primero es el "último partido" — usado en Desempate 3.
     */
    List<Partido> findByQuinielaIdOrderByFechaPartidoDesc(Long quinielaId);
}

