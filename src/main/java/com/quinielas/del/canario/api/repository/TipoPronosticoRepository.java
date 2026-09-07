package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.TipoPronostico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoPronosticoRepository extends JpaRepository<TipoPronostico, Long> {

    Optional<TipoPronostico> findByCodigo(String codigo);

    List<TipoPronostico> findByActivoTrue();

    boolean existsByCodigo(String codigo);
}

