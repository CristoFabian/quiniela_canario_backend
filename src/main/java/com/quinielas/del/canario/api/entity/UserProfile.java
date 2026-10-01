package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Datos personales ─────────────────────────────────────────────
    @Column(length = 80)
    private String nombre;

    @Column(name = "apellido_paterno", length = 80)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 80)
    private String apellidoMaterno;

    @Column(length = 100)
    private String ciudad;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 20)
    private String telefono;

    /** Nombre del archivo de foto, p.ej.: user1_20260402153045.png */
    @Column(length = 120)
    private String foto;

    // ─── Auditoría ───────────────────────────────────────────────────
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPerfil estado = EstadoPerfil.INCOMPLETO;

    /** Crédito disponible por pagos verificados manualmente fuera de la ventana de participación. */
    @Column(name = "saldo_a_favor", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoAFavor = BigDecimal.ZERO;

    // ─── Relación (lado propietario → tiene la FK en la tabla) ───────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ─── Constructores ────────────────────────────────────────────────
    public UserProfile() {}

    /** Constructor mínimo usado al registrar un usuario nuevo. */
    public UserProfile(User user) {
        this.user = user;
    }

    // ─── Ciclo de vida JPA ────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        fechaCreacion      = ahora;
        fechaActualizacion = ahora;
        evaluarEstado();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        evaluarEstado();
    }

    /**
     * El perfil se considera COMPLETO cuando los campos clave
     * están todos informados.
     */
    private void evaluarEstado() {
        boolean completo = isNotBlank(nombre)
                && isNotBlank(apellidoPaterno)
                && isNotBlank(apellidoMaterno)
                && isNotBlank(ciudad)
                && isNotBlank(telefono)
                && fechaNacimiento != null;

        estado = completo ? EstadoPerfil.COMPLETO : EstadoPerfil.INCOMPLETO;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getNombre()                        { return nombre; }
    public void setNombre(String nombre)             { this.nombre = nombre; }

    public String getApellidoPaterno()               { return apellidoPaterno; }
    public void setApellidoPaterno(String ap)        { this.apellidoPaterno = ap; }

    public String getApellidoMaterno()               { return apellidoMaterno; }
    public void setApellidoMaterno(String am)        { this.apellidoMaterno = am; }

    public String getCiudad()                        { return ciudad; }
    public void setCiudad(String ciudad)             { this.ciudad = ciudad; }

    public LocalDate getFechaNacimiento()            { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fn)     { this.fechaNacimiento = fn; }

    public String getTelefono()                      { return telefono; }
    public void setTelefono(String telefono)         { this.telefono = telefono; }

    public String getFoto()                          { return foto; }
    public void setFoto(String foto)                 { this.foto = foto; }

    public LocalDateTime getFechaCreacion()          { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fc)   { this.fechaCreacion = fc; }

    public LocalDateTime getFechaActualizacion()         { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fa)  { this.fechaActualizacion = fa; }

    public EstadoPerfil getEstado()                  { return estado; }
    public void setEstado(EstadoPerfil estado)        { this.estado = estado; }

    public BigDecimal getSaldoAFavor() { return saldoAFavor; }
    public void setSaldoAFavor(BigDecimal saldoAFavor) {
        this.saldoAFavor = saldoAFavor == null ? BigDecimal.ZERO : saldoAFavor;
    }

    public User getUser()                            { return user; }
    public void setUser(User user)                   { this.user = user; }
}
