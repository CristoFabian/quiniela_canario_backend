package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.DashboardResponse;
import com.quinielas.del.canario.api.dto.QuinielaResponse;
import com.quinielas.del.canario.api.entity.EstadoQuiniela;
import com.quinielas.del.canario.api.entity.Role;
import com.quinielas.del.canario.api.repository.QuinielaRepository;
import com.quinielas.del.canario.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final QuinielaRepository quinielaRepository;
    private final UserRepository     userRepository;

    public DashboardService(QuinielaRepository quinielaRepository,
                            UserRepository userRepository) {
        this.quinielaRepository = quinielaRepository;
        this.userRepository     = userRepository;
    }

    /**
     * Resumen general para el dashboard del administrador.
     * Incluye contadores por estado de quiniela, usuarios registrados
     * y el detalle de las quinielas ABIERTA y EN_JUEGO.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {
        DashboardResponse summary = new DashboardResponse();

        // ─── Contadores de quinielas ──────────────────────────────────
        summary.setTotalQuinielas(quinielaRepository.count());
        summary.setQuinielasCreadas(quinielaRepository.countByEstado(EstadoQuiniela.CREADA));
        summary.setQuinielasAbiertas(quinielaRepository.countByEstado(EstadoQuiniela.ABIERTA));
        summary.setQuinielasEnJuego(quinielaRepository.countByEstado(EstadoQuiniela.EN_JUEGO));
        summary.setQuinielasFinalizadas(quinielaRepository.countByEstado(EstadoQuiniela.FINALIZADA));

        // ─── Contadores de usuarios ───────────────────────────────────
        long totalJugadores = userRepository.countByRole(Role.USER);
        summary.setTotalJugadores(totalJugadores);
        summary.setJugadoresActivos(userRepository.countByRoleAndActivo(Role.USER, true));
        summary.setJugadoresInactivos(userRepository.countByRoleAndActivo(Role.USER, false));
        summary.setTotalAdmins(userRepository.countByRole(Role.ADMIN));

        // ─── Detalle quinielas ABIERTA ────────────────────────────────
        List<QuinielaResponse> activas = quinielaRepository
                .findByEstado(EstadoQuiniela.ABIERTA)
                .stream()
                .map(QuinielaResponse::from)
                .collect(Collectors.toList());
        summary.setQuinielasActivasDetalle(activas);

        // ─── Detalle quinielas EN_JUEGO ───────────────────────────────
        List<QuinielaResponse> enJuego = quinielaRepository
                .findByEstado(EstadoQuiniela.EN_JUEGO)
                .stream()
                .map(QuinielaResponse::from)
                .collect(Collectors.toList());
        summary.setQuinielasEnJuegoDetalle(enJuego);

        return summary;
    }
}

