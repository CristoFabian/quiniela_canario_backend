package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.EstadoPago;
import com.quinielas.del.canario.api.entity.Pago;
import com.quinielas.del.canario.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Todos los pagos de un jugador ordenados por fecha descendente. */
    List<Pago> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    /** Pagos de un jugador filtrados por estado. */
    List<Pago> findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(Long usuarioId, EstadoPago estado);

    /** Todos los pagos filtrados por estado (admin). */
    List<Pago> findByEstadoOrderByFechaCreacionDesc(EstadoPago estado);

    /** Todos los pagos (admin). */
    List<Pago> findAllByOrderByFechaCreacionDesc();

    /**
     * Lista de usuarios distintos que tienen al menos un pago en el estado indicado.
     * Usada para generar el resumen agrupado por jugador.
     */
    @Query("SELECT DISTINCT p.usuario FROM Pago p WHERE p.estado = :estado ORDER BY p.usuario.username")
    List<User> findUsuariosConPagoEnEstado(@Param("estado") EstadoPago estado);

    /**
     * Verifica si una jugada ya está asociada a un pago en estado PENDIENTE o APROBADO.
     * Impide que la misma jugada se pague dos veces.
     */
    @Query("""
            SELECT COUNT(p) > 0
            FROM Pago p
            JOIN p.jugadas j
            WHERE j.id = :jugadaId
              AND p.estado IN :estados
            """)
    boolean existsPagoActivoParaJugada(@Param("jugadaId") Long jugadaId,
                                       @Param("estados") List<EstadoPago> estados);
}

