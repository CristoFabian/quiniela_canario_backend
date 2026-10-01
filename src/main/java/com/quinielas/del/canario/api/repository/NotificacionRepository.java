package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Historial paginado del usuario, más reciente primero. */
    Page<Notificacion> findByUsuarioDestinoIdOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    /** Solo las no leídas del usuario, paginado. */
    Page<Notificacion> findByUsuarioDestinoIdAndLeidaFalseOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    /** Contador para el badge de la campana (consulta ligera, sin traer filas). */
    long countByUsuarioDestinoIdAndLeidaFalse(Long usuarioId);

    /** Búsqueda por id público con verificación de propietario en la misma consulta. */
    Optional<Notificacion> findByPublicIdAndUsuarioDestinoId(String publicId, Long usuarioId);

    /** Marca todas las no leídas del usuario como leídas en una sola sentencia (evita N updates). */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLectura = :ahora " +
           "WHERE n.usuarioDestino.id = :usuarioId AND n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
