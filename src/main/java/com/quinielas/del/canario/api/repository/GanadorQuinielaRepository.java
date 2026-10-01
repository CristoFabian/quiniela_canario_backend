package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.GanadorQuiniela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GanadorQuinielaRepository extends JpaRepository<GanadorQuiniela, Long> {

    /** Lista los ganadores de un cierre ordenados por posición. */
    List<GanadorQuiniela> findByCierreQuinielaIdOrderByPosicionAsc(Long cierreQuinielaId);

    /** Lista los ganadores de una quiniela directamente por su id. */
    List<GanadorQuiniela> findByCierreQuinielaQuinielaId(Long quinielaId);

    /** Todos los premios ganados por un jugador, del más reciente al más antiguo. */
    List<GanadorQuiniela> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    /** Busca un registro de ganador validando que pertenezca al jugador (control de acceso). */
    Optional<GanadorQuiniela> findByIdAndUsuarioId(Long id, Long usuarioId);
}

