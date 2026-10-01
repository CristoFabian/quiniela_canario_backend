package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.dto.ChangeRoleRequest;
import com.quinielas.del.canario.api.dto.DashboardResponse;
import com.quinielas.del.canario.api.dto.UserProfileResponse;
import com.quinielas.del.canario.api.service.AdminService;
import com.quinielas.del.canario.api.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService     adminService;
    private final DashboardService dashboardService;

    public AdminController(AdminService adminService, DashboardService dashboardService) {
        this.adminService     = adminService;
        this.dashboardService = dashboardService;
    }

    /**
     * Resumen general para el dashboard.
     * GET /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    /**
     * Lista todos los usuarios registrados.
     * GET /api/admin/usuarios
     */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    /**
     * Cambia el rol de un usuario (USER <-> ADMIN).
     * PUT /api/admin/usuarios/{id}/rol
     * Body: { "role": "ADMIN" }
     */
    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<UserProfileResponse> changeRole(
            @PathVariable Long id,
            @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(adminService.changeUserRole(id, request.getRole()));
    }

    /**
     * Reactiva la cuenta de un usuario previamente desactivado (baja lógica).
     * PUT /api/admin/usuarios/{id}/activar
     */
    @PutMapping("/usuarios/{id}/activar")
    public ResponseEntity<UserProfileResponse> reactivarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.reactivarUsuario(id));
    }
}



