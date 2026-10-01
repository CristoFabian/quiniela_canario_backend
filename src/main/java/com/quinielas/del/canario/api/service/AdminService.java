package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.UserProfileResponse;
import com.quinielas.del.canario.api.entity.Role;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ─── Listar todos los usuarios ────────────────────────────────────
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toProfile)
                .collect(Collectors.toList());
    }

    // ─── Cambiar el rol de un usuario ─────────────────────────────────
    public UserProfileResponse changeUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));

        try {
            user.setRole(Role.valueOf(newRole.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: '" + newRole + "'. Los valores permitidos son: USER, ADMIN");
        }

        userRepository.save(user);
        return toProfile(user);
    }

    // ─── Reactivar la cuenta de un usuario dado de baja ───────────────
    public UserProfileResponse reactivarUsuario(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));

        user.setActivo(true);
        userRepository.save(user);
        return toProfile(user);
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isActivo()
        );
    }
}

