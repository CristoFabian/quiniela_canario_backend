package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.ReglaJuegoRequest;
import com.quinielas.del.canario.api.dto.ReglaJuegoResponse;
import com.quinielas.del.canario.api.entity.CategoriaRegla;
import com.quinielas.del.canario.api.entity.ReglaJuego;
import com.quinielas.del.canario.api.entity.TipoNotificacion;
import com.quinielas.del.canario.api.event.ReglaJuegoEvent;
import com.quinielas.del.canario.api.repository.ReglaJuegoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Administración de las reglas del juego mostradas al jugador
 * (puntuación, desempate, premios, pagos, etc.).
 * Permite al administrador crear, editar, reordenar y eliminar (lógicamente)
 * las reglas sin necesidad de modificar código.
 */
@Service
public class ReglaJuegoService {

    private final ReglaJuegoRepository reglaRepo;
    private final ApplicationEventPublisher eventPublisher;

    public ReglaJuegoService(ReglaJuegoRepository reglaRepo, ApplicationEventPublisher eventPublisher) {
        this.reglaRepo      = reglaRepo;
        this.eventPublisher = eventPublisher;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Consultas
    // ═════════════════════════════════════════════════════════════════

    /** Listar todas las reglas (activas e inactivas). Uso administrativo. */
    @Transactional(readOnly = true)
    public List<ReglaJuegoResponse> listarTodas() {
        return reglaRepo.findAllByOrderByCategoriaAscOrdenAscIdAsc()
                .stream()
                .map(ReglaJuegoResponse::from)
                .collect(Collectors.toList());
    }

    /** Listar solo las reglas activas, ordenadas. Uso del jugador/público. */
    @Transactional(readOnly = true)
    public List<ReglaJuegoResponse> listarActivas() {
        return reglaRepo.findByActivoTrueOrderByCategoriaAscOrdenAscIdAsc()
                .stream()
                .map(ReglaJuegoResponse::from)
                .collect(Collectors.toList());
    }

    /** Listar reglas activas de una categoría concreta. */
    @Transactional(readOnly = true)
    public List<ReglaJuegoResponse> listarActivasPorCategoria(CategoriaRegla categoria) {
        return reglaRepo.findByActivoTrueAndCategoriaOrderByOrdenAscIdAsc(categoria)
                .stream()
                .map(ReglaJuegoResponse::from)
                .collect(Collectors.toList());
    }

    /** Obtener una regla por id (administración). */
    @Transactional(readOnly = true)
    public ReglaJuegoResponse obtener(Long id) {
        return ReglaJuegoResponse.from(buscarOExcepcion(id));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Mutaciones (solo administrador)
    // ═════════════════════════════════════════════════════════════════

    /** Crear una nueva regla del juego. */
    @Transactional
    public ReglaJuegoResponse crear(ReglaJuegoRequest request) {
        if (reglaRepo.existsByTituloIgnoreCase(request.getTitulo())) {
            throw new IllegalArgumentException(
                    "Ya existe una regla con el titulo: " + request.getTitulo());
        }

        ReglaJuego regla = new ReglaJuego(
                request.getTitulo(),
                request.getDescripcion(),
                request.getCategoria(),
                request.getOrden(),
                request.isActivo());

        ReglaJuego guardada = reglaRepo.save(regla);
        eventPublisher.publishEvent(
                new ReglaJuegoEvent(guardada.getId(), guardada.getTitulo(), TipoNotificacion.REGLA_AGREGADA));
        return ReglaJuegoResponse.from(guardada);
    }

    /** Actualizar (editar) una regla existente. */
    @Transactional
    public ReglaJuegoResponse actualizar(Long id, ReglaJuegoRequest request) {
        ReglaJuego regla = buscarOExcepcion(id);

        if (!regla.getTitulo().equalsIgnoreCase(request.getTitulo()) &&
                reglaRepo.existsByTituloIgnoreCaseAndIdNot(request.getTitulo(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe otra regla con el titulo: " + request.getTitulo());
        }

        regla.setTitulo(request.getTitulo());
        regla.setDescripcion(request.getDescripcion());
        regla.setCategoria(request.getCategoria());
        regla.setOrden(request.getOrden());
        regla.setActivo(request.isActivo());

        ReglaJuego guardada = reglaRepo.save(regla);
        eventPublisher.publishEvent(
                new ReglaJuegoEvent(guardada.getId(), guardada.getTitulo(), TipoNotificacion.REGLA_EDITADA));
        return ReglaJuegoResponse.from(guardada);
    }

    /**
     * Eliminación lógica de una regla (activo = false).
     * Deja de mostrarse al jugador pero se conserva en el histórico administrativo.
     */
    @Transactional
    public ReglaJuegoResponse eliminar(Long id) {
        ReglaJuego regla = buscarOExcepcion(id);
        regla.setActivo(false);
        ReglaJuego guardada = reglaRepo.save(regla);
        eventPublisher.publishEvent(
                new ReglaJuegoEvent(guardada.getId(), guardada.getTitulo(), TipoNotificacion.REGLA_ELIMINADA));
        return ReglaJuegoResponse.from(guardada);
    }

    /** Eliminación física (borrado permanente) de una regla. */
    @Transactional
    public void eliminarDefinitivamente(Long id) {
        ReglaJuego regla = buscarOExcepcion(id);
        reglaRepo.delete(regla);
    }

    /** Reactivar una regla previamente eliminada de forma lógica. */
    @Transactional
    public ReglaJuegoResponse reactivar(Long id) {
        ReglaJuego regla = buscarOExcepcion(id);
        regla.setActivo(true);
        ReglaJuego guardada = reglaRepo.save(regla);
        eventPublisher.publishEvent(
                new ReglaJuegoEvent(guardada.getId(), guardada.getTitulo(), TipoNotificacion.REGLA_REACTIVADA));
        return ReglaJuegoResponse.from(guardada);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════

    private ReglaJuego buscarOExcepcion(Long id) {
        return reglaRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Regla del juego no encontrada con id: " + id));
    }
}

