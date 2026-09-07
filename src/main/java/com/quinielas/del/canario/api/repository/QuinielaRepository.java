package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.EstadoQuiniela;
import com.quinielas.del.canario.api.entity.Quiniela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuinielaRepository extends JpaRepository<Quiniela, Long> {

    /** Quinielas creadas por un usuario específico. */
    List<Quiniela> findByCreadoPorId(Long userId);

    /** Quinielas filtradas por estado. */
    List<Quiniela> findByEstado(EstadoQuiniela estado);

    /** Conteo rápido por estado (evita traer entidades completas). */
    long countByEstado(EstadoQuiniela estado);
}

