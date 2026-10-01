package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.EstadoQuiniela;
import com.quinielas.del.canario.api.entity.Quiniela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface QuinielaRepository extends JpaRepository<Quiniela, Long> {

    /** Quinielas creadas por un usuario específico. */
    List<Quiniela> findByCreadoPorId(Long userId);

    /** Quinielas filtradas por estado. */
    List<Quiniela> findByEstado(EstadoQuiniela estado);

    /** Conteo rápido por estado (evita traer entidades completas). */
    long countByEstado(EstadoQuiniela estado);

    /**
     * Quinielas ABIERTA cuyo cierre cae dentro de la ventana [desde, hasta) y a las que
     * todavía no se les envió el aviso de "próxima a cerrar". Usada por la tarea programada.
     */
    List<Quiniela> findByEstadoAndAvisoCierreEnviadoFalseAndFechaCierreBetween(
            EstadoQuiniela estado, LocalDateTime desde, LocalDateTime hasta);
}

