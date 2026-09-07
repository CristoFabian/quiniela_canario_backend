package com.quinielas.del.canario.api.repository;

import com.quinielas.del.canario.api.entity.Role;
import com.quinielas.del.canario.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /** Cuenta usuarios por rol (p.ej. jugadores registrados con Role.USER). */
    long countByRole(Role role);

    /** Cuenta usuarios por rol y estado activo/inactivo. */
    long countByRoleAndActivo(Role role, boolean activo);
}

