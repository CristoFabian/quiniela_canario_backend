package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaCreacion;

    // ─── Relación inversa con UserProfile (UserProfile tiene la FK) ───
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, optional = true)
    private UserProfile perfil;

    // ─── Constructores ────────────────────────────────────────────────
    public User() {}

    public User(Long id, String username, String email, String password, Role role,
                boolean activo, LocalDateTime fechaCreacion) {
        this.id            = id;
        this.username      = username;
        this.email         = email;
        this.password      = password;
        this.role          = role;
        this.activo        = activo;
        this.fechaCreacion = fechaCreacion;
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        }
        if (!activo) {
            activo = true;
        }
    }

    // ─── Builder ──────────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private String password;
        private Role role;
        private boolean activo = true;
        private LocalDateTime fechaCreacion;
        private UserProfile perfil;

        public Builder id(Long id)                         { this.id            = id;            return this; }
        public Builder username(String u)                  { this.username      = u;             return this; }
        public Builder email(String e)                     { this.email         = e;             return this; }
        public Builder password(String p)                  { this.password      = p;             return this; }
        public Builder role(Role r)                        { this.role          = r;             return this; }
        public Builder activo(boolean a)                   { this.activo        = a;             return this; }
        public Builder fechaCreacion(LocalDateTime fecha)  { this.fechaCreacion = fecha;         return this; }
        public Builder perfil(UserProfile perfil)          { this.perfil        = perfil;        return this; }

        public User build() {
            return new User(id, username, email, password, role, activo, fechaCreacion);
        }
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                   { return id; }
    public void setId(Long id)            { this.id = id; }

    public String getUsername()           { return username; }
    public void setUsername(String u)     { this.username = u; }

    public String getEmail()              { return email; }
    public void setEmail(String e)        { this.email = e; }

    public String getPassword()           { return password; }
    public void setPassword(String p)     { this.password = p; }

    public Role getRole()                 { return role; }
    public void setRole(Role r)           { this.role = r; }

    public boolean isActivo()             { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion()            { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fecha)  { this.fechaCreacion = fecha; }

    public UserProfile getPerfil()                { return perfil; }
    public void setPerfil(UserProfile perfil)     { this.perfil = perfil; }

    // ─── UserDetails ─────────────────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return activo; }
}
