package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.OpcionPronostico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcionPronosticoRepository extends JpaRepository<OpcionPronostico, Long> {

    /** Todas las opciones activas de un tipo. */
    List<OpcionPronostico> findByTipoPronosticoIdAndActivoTrue(Long tipoPronosticoId);

    /** Todas las opciones de un tipo (incluye inactivas, para admin). */
    List<OpcionPronostico> findByTipoPronosticoId(Long tipoPronosticoId);

    /** ¿Existe una opción con ese código dentro del mismo tipo? (para crear) */
    boolean existsByTipoPronosticoIdAndCodigo(Long tipoPronosticoId, String codigo);

    /** ¿Existe otra opción con ese código dentro del mismo tipo? (para actualizar) */
    boolean existsByTipoPronosticoIdAndCodigoAndIdNot(Long tipoPronosticoId, String codigo, Long id);
}



