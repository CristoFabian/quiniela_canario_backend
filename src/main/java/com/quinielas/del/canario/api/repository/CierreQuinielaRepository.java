package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.CierreQuiniela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CierreQuinielaRepository extends JpaRepository<CierreQuiniela, Long> {

    /** ¿Ya existe un cierre para esta quiniela? (control de idempotencia) */
    boolean existsByQuinielaId(Long quinielaId);

    /** Obtener el cierre de una quiniela (para consultas). */
    Optional<CierreQuiniela> findByQuinielaId(Long quinielaId);
}

