package com.quinielas.del.canario.api.util;

import com.quinielas.del.canario.api.entity.Role;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.entity.UserProfile;
import com.quinielas.del.canario.api.repository.UserProfileRepository;
import com.quinielas.del.canario.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Utilidad CLI para crear un administrador manualmente.
 *
 * USO:
 *   java -jar api-0.0.1-SNAPSHOT.jar --create-admin
 *
 * Nota: Actualmente comentado porque puede interferir con arranques normales.
 * Descomenta @Component si necesitas usar esta funcionalidad.
 */
//@Component
public class CreateAdminCLI implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateAdminCLI(UserRepository userRepository,
                          UserProfileRepository userProfileRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Solo ejecutar si se pasa --create-admin
        boolean shouldCreateAdmin = java.util.Arrays.stream(args)
                .anyMatch(arg -> arg.equals("--create-admin"));

        if (!shouldCreateAdmin) {
            return;
        }

        System.out.println("\n=== CREAR ADMINISTRADOR ===\n");

        Scanner scanner = new Scanner(System.in);

        // Verificar si ya existe un admin
        boolean adminExists = userRepository.findAll()
                .stream()
                .anyMatch(user -> user.getRole() == Role.ADMIN);

        if (adminExists) {
            System.out.println("✗ Ya existe un administrador en la base de datos.");
            System.out.println("  Si necesitas crear otro admin, usa el panel administrativo.");
            return;
        }

        // Pedir nombre de usuario
        System.out.print("Nombre de usuario: ");
        String username = scanner.nextLine().trim();

        if (username.length() < 3) {
            System.out.println("✗ El nombre de usuario debe tener al menos 3 caracteres.");
            return;
        }

        if (userRepository.existsByUsername(username)) {
            System.out.println("✗ El usuario '" + username + "' ya existe.");
            return;
        }

        // Pedir email
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        if (!email.contains("@")) {
            System.out.println("✗ Email inválido.");
            return;
        }

        if (userRepository.existsByEmail(email)) {
            System.out.println("✗ El email '" + email + "' ya está registrado.");
            return;
        }

        // Pedir contraseña
        System.out.print("Contraseña (mínimo 10 caracteres): ");
        String password = scanner.nextLine();

        if (password.length() < 10) {
            System.out.println("✗ La contraseña debe tener al menos 10 caracteres.");
            return;
        }

        // Confirmar contraseña
        System.out.print("Confirmar contraseña: ");
        String confirmPassword = scanner.nextLine();

        if (!password.equals(confirmPassword)) {
            System.out.println("✗ Las contraseñas no coinciden.");
            return;
        }

        // Crear el admin
        try {
            User admin = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(Role.ADMIN)
                    .activo(true)
                    .build();

            userRepository.save(admin);

            UserProfile perfil = new UserProfile(admin);
            userProfileRepository.save(perfil);

            System.out.println("\n✓ Administrador creado exitosamente!");
            System.out.println("  Usuario: " + username);
            System.out.println("  Email: " + email);
            System.out.println("  Rol: ADMIN");
            System.out.println("\nPuedes iniciar sesión en la aplicación con estas credenciales.\n");

        } catch (Exception e) {
            System.out.println("✗ Error al crear administrador: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}

