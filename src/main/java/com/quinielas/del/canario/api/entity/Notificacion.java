package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Notificación interna in-app dirigida a un único usuario (jugador o administrador).
 *
 * <p>Este canal (base de datos + campana en la UI) es el primero de una familia de
 * canales de entrega. La entidad y el {@code NotificacionService} NO conocen ningún
 * canal futuro (email, push, SMS); esos se agregarán como listeners adicionales de
 * los mismos eventos de dominio, sin tocar esta clase.</p>
 *
 * <p>{@code publicId} es el identificador expuesto en la API REST; el {@code id}
 * autogenerado (secuencial) nunca se expone al cliente.</p>
 */
@Entity
@Table(
        name = "notificaciones",
        indexes = {
                // Listado paginado del usuario ordenado por fecha (el más usado).
                @Index(name = "idx_notif_usuario_fecha", columnList = "usuario_id, fecha_creacion"),
                // Conteo/listado de no leídas.
                @Index(name = "idx_notif_usuario_leida", columnList = "usuario_id, leida"),
                @Index(name = "idx_notif_public_id", columnList = "public_id", unique = true)
        }
)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador público (UUID) usado en URLs; nunca se expone el id secuencial interno. */
    @Column(name = "public_id", nullable = false, updatable = false, unique = true, length = 36)
    private String publicId;

    /** Usuario destino de la notificación (FK → users.id). Cada notificación pertenece a un único usuario. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuarioDestino;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    /**
     * Datos adicionales para navegación/contexto en el frontend, serializados como JSON
     * (p.ej. {"pagoId":12} o {"quinielaId":5,"ganadorId":8}). Opcional.
     * Sin @Lob a propósito: forzar CLOB aquí falla al insertar con MySQL Connector/J +
     * HikariCP (SQLFeatureNotSupportedException al crear el Clob), y el error queda oculto
     * porque se dispara dentro de un @TransactionalEventListener(AFTER_COMMIT). Un TEXT
     * plano se escribe con setString() normal y evita ese problema.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public Notificacion() {}

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID().toString();
        }
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        }
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getPublicId()                { return publicId; }
    public void setPublicId(String publicId)   { this.publicId = publicId; }

    public User getUsuarioDestino()                    { return usuarioDestino; }
    public void setUsuarioDestino(User usuarioDestino) { this.usuarioDestino = usuarioDestino; }

    public String getTitulo()                  { return titulo; }
    public void setTitulo(String titulo)       { this.titulo = titulo; }

    public String getMensaje()                 { return mensaje; }
    public void setMensaje(String mensaje)     { this.mensaje = mensaje; }

    public TipoNotificacion getTipo()               { return tipo; }
    public void setTipo(TipoNotificacion tipo)      { this.tipo = tipo; }

    public boolean isLeida()                   { return leida; }
    public void setLeida(boolean leida)        { this.leida = leida; }

    public LocalDateTime getFechaCreacion()                 { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaLectura()                  { return fechaLectura; }
    public void setFechaLectura(LocalDateTime fechaLectura) { this.fechaLectura = fechaLectura; }

    public String getMetadata()                { return metadata; }
    public void setMetadata(String metadata)   { this.metadata = metadata; }
}
