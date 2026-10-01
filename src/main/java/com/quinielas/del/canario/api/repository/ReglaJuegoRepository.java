package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.CategoriaRegla;
import com.quinielas.del.canario.api.entity.ReglaJuego;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaJuegoRepository extends JpaRepository<ReglaJuego, Long> {

    /** Todas las reglas (activas e inactivas), ordenadas para administración. */
    List<ReglaJuego> findAllByOrderByCategoriaAscOrdenAscIdAsc();

    /** Solo las reglas activas, ordenadas para mostrarlas al jugador. */
    List<ReglaJuego> findByActivoTrueOrderByCategoriaAscOrdenAscIdAsc();

    /** Reglas activas de una categoría concreta. */
    List<ReglaJuego> findByActivoTrueAndCategoriaOrderByOrdenAscIdAsc(CategoriaRegla categoria);

    boolean existsByTituloIgnoreCase(String titulo);

    boolean existsByTituloIgnoreCaseAndIdNot(String titulo, Long id);
}

