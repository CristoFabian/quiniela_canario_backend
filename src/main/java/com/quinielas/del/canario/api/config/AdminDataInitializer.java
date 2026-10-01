package com.quinielas.del.canario.api.config;

import com.quinielas.del.canario.api.entity.Role;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.entity.UserProfile;
import com.quinielas.del.canario.api.repository.UserProfileRepository;
import com.quinielas.del.canario.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializador de usuario administrador.
 *
 * Crea un usuario ADMIN al arrancar la aplicación SOLO SI:
 * 1. No existe ningún usuario ADMIN en la BD
 * 2. Las variables de entorno ADMIN_USERNAME y ADMIN_PASSWORD están definidas
 *
 * USO EN PRODUCCIÓN (Oracle Cloud Free Always):
 *
 * 1. En la consola de OCI:
 *    - Compute > Instances > (selecciona tu instancia)
 *    - Cloud Shell > Variables de entorno > Agregar
 *
 * 2. O en el archivo de deployment (terraform/variables.tfvars):
 *    admin_username = "admin_secreto"
 *    admin_password = "password_muy_largo_aleatorio_32_caracteres_minimo"
 *
 * 3. O en Docker (si usas Docker en OCI):
 *    docker run -e ADMIN_USERNAME=admin -e ADMIN_PASSWORD=xxxxx ...
 *
 * USO EN DESARROLLO (LOCAL):
 *    export ADMIN_USERNAME=admin
 *    export ADMIN_PASSWORD=Admin123!@#
 *    mvn spring-boot:run
 */
@Component
public class AdminDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:#{null}}")
    private String adminUsername;

    @Value("${admin.password:#{null}}")
    private String adminPassword;

    public AdminDataInitializer(UserRepository userRepository,
                                UserProfileRepository userProfileRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Verificar si ya existe un admin
        boolean adminExists = userRepository.findAll()
                .stream()
                .anyMatch(user -> user.getRole() == Role.ADMIN);

        if (adminExists) {
            log.info("✓ Administrador ya existe en la base de datos. Se omite creación.");
            return;
        }

        // Si no están definidas las variables de entorno, no crear admin
        if (adminUsername == null || adminUsername.isBlank() ||
            adminPassword == null || adminPassword.isBlank()) {
            log.warn("⚠ No se encontraron variables de entorno ADMIN_USERNAME y/o ADMIN_PASSWORD.");
            log.warn("  Para crear el primer admin, establece estas variables de entorno.");
            log.warn("  Ejemplo en Linux/Mac: export ADMIN_USERNAME=admin; export ADMIN_PASSWORD=xxxxx");
            log.warn("  Ejemplo en Windows: set ADMIN_USERNAME=admin && set ADMIN_PASSWORD=xxxxx");
            return;
        }

        // Validaciones básicas de seguridad
        if (adminUsername.length() < 3) {
            log.error("✗ El nombre de usuario admin debe tener al menos 3 caracteres.");
            return;
        }

        if (adminPassword.length() < 10) {
            log.error("✗ La contraseña admin debe tener al menos 10 caracteres para mayor seguridad.");
            return;
        }

        // Verificar que el usuario no exista ya
        if (userRepository.existsByUsername(adminUsername)) {
            log.warn("⚠ El usuario '{}' ya existe. No se crea nuevo admin.", adminUsername);
            return;
        }

        // Crear el usuario admin
        try {
            User admin = User.builder()
                    .username(adminUsername)
                    .email("admin@" + adminUsername + ".local")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .activo(true)
                    .build();

            userRepository.save(admin);

            // Crear perfil vacío asociado
            UserProfile perfil = new UserProfile(admin);
            userProfileRepository.save(perfil);

            log.info("✓ Administrador creado exitosamente:");
            log.info("  Usuario: {}", adminUsername);
            log.info("  Rol: ADMIN");
            log.info("  Estado: Activo");
            log.info("  Perfil: Inicializado");

        } catch (Exception e) {
            log.error("✗ Error al crear el administrador inicial: {}", e.getMessage(), e);
        }
    }
}

