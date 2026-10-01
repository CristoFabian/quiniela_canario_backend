package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.EstadoPerfil;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.entity.UserProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class PerfilResponse {

    // ─── Datos de cuenta ─────────────────────────────────────────────
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean activo;
    private BigDecimal saldoAFavor;

    // ─── Datos de perfil ─────────────────────────────────────────────
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String ciudad;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String foto;
    private EstadoPerfil estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public PerfilResponse() {}

    // ─── Factory estático ─────────────────────────────────────────────
    public static PerfilResponse from(User user, UserProfile perfil) {
        PerfilResponse r = new PerfilResponse();

        // cuenta
        r.id       = user.getId();
        r.username = user.getUsername();
        r.email    = user.getEmail();
        r.role     = user.getRole().name();
        r.activo   = user.isActivo();
        r.saldoAFavor = perfil != null ? perfil.getSaldoAFavor() : BigDecimal.ZERO;

        // perfil
        if (perfil != null) {
            r.nombre             = perfil.getNombre();
            r.apellidoPaterno    = perfil.getApellidoPaterno();
            r.apellidoMaterno    = perfil.getApellidoMaterno();
            r.ciudad             = perfil.getCiudad();
            r.fechaNacimiento    = perfil.getFechaNacimiento();
            r.telefono           = perfil.getTelefono();
            r.foto               = perfil.getFoto();
            r.estado             = perfil.getEstado();
            r.fechaCreacion      = perfil.getFechaCreacion();
            r.fechaActualizacion = perfil.getFechaActualizacion();
        }

        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                  { return id; }
    public void setId(Long id)                           { this.id = id; }

    public String getUsername()                          { return username; }
    public void setUsername(String username)             { this.username = username; }

    public String getEmail()                             { return email; }
    public void setEmail(String email)                   { this.email = email; }

    public String getRole()                              { return role; }
    public void setRole(String role)                     { this.role = role; }

    public boolean isActivo()                            { return activo; }
    public void setActivo(boolean activo)                { this.activo = activo; }
    public BigDecimal getSaldoAFavor()                   { return saldoAFavor; }
    public void setSaldoAFavor(BigDecimal saldoAFavor)   { this.saldoAFavor = saldoAFavor; }

    public String getNombre()                            { return nombre; }
    public void setNombre(String nombre)                 { this.nombre = nombre; }

    public String getApellidoPaterno()                   { return apellidoPaterno; }
    public void setApellidoPaterno(String ap)            { this.apellidoPaterno = ap; }

    public String getApellidoMaterno()                   { return apellidoMaterno; }
    public void setApellidoMaterno(String am)            { this.apellidoMaterno = am; }

    public String getCiudad()                            { return ciudad; }
    public void setCiudad(String ciudad)                 { this.ciudad = ciudad; }

    public LocalDate getFechaNacimiento()                { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fn)         { this.fechaNacimiento = fn; }

    public String getTelefono()                          { return telefono; }
    public void setTelefono(String telefono)             { this.telefono = telefono; }

    public String getFoto()                              { return foto; }
    public void setFoto(String foto)                     { this.foto = foto; }

    public EstadoPerfil getEstado()                      { return estado; }
    public void setEstado(EstadoPerfil estado)           { this.estado = estado; }

    public LocalDateTime getFechaCreacion()              { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fc)       { this.fechaCreacion = fc; }

    public LocalDateTime getFechaActualizacion()         { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fa)  { this.fechaActualizacion = fa; }
}

