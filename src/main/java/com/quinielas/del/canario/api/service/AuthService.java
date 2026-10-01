package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.AuthResponse;
import com.quinielas.del.canario.api.dto.LoginRequest;
import com.quinielas.del.canario.api.dto.RegisterRequest;
import com.quinielas.del.canario.api.dto.ResetPasswordRequest;
import com.quinielas.del.canario.api.entity.Role;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.entity.UserProfile;
import com.quinielas.del.canario.api.exception.CuentaInactivaException;
import com.quinielas.del.canario.api.repository.UserProfileRepository;
import com.quinielas.del.canario.api.repository.UserRepository;
import com.quinielas.del.canario.api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository        = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.jwtService            = jwtService;
        this.passwordEncoder       = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .activo(true)
                .build();

        userRepository.save(user);

        // Crear perfil vacío asociado al usuario
        UserProfile perfil = new UserProfile(user);
        userProfileRepository.save(perfil);
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (DisabledException ex) {
            throw new CuentaInactivaException("Tu cuenta ha sido desactivada.");
        } catch (AuthenticationException ex) {
            // Mensaje genérico → no filtra si el usuario existe o si el password es incorrecto
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Restablece la contraseña de un usuario verificando su nombre de usuario y teléfono
     * registrado en su perfil. No requiere sesión activa (recuperación por "olvidé mi contraseña").
     */
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // Mensaje genérico → no revela si el usuario existe o si el teléfono es incorrecto
        BadCredentialsException datosNoCoinciden =
                new BadCredentialsException("Los datos proporcionados no coinciden con ningún usuario registrado");

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> datosNoCoinciden);

        UserProfile perfil = userProfileRepository.findByUserId(user.getId()).orElse(null);
        String telefonoRegistrado = perfil != null ? perfil.getTelefono() : null;

        if (telefonoRegistrado == null || !telefonoRegistrado.equals(request.getTelefono())) {
            throw datosNoCoinciden;
        }

        user.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        userRepository.save(user);
    }
}
