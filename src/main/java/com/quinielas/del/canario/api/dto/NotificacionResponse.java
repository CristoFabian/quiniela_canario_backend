package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.Map;

/** Representación pública de una {@code Notificacion}; nunca expone el id interno secuencial. */
public class NotificacionResponse {

    private String            id; // publicId (UUID)
    private String            titulo;
    private String            mensaje;
    private TipoNotificacion  tipo;
    private boolean           leida;
    private LocalDateTime     fechaCreacion;
    private LocalDateTime     fechaLectura;
    private Map<String, Object> metadata;

    public NotificacionResponse() {}

    public NotificacionResponse(String id, String titulo, String mensaje, TipoNotificacion tipo,
                                boolean leida, LocalDateTime fechaCreacion, LocalDateTime fechaLectura,
                                Map<String, Object> metadata) {
        this.id            = id;
        this.titulo        = titulo;
        this.mensaje       = mensaje;
        this.tipo          = tipo;
        this.leida         = leida;
        this.fechaCreacion = fechaCreacion;
        this.fechaLectura  = fechaLectura;
        this.metadata      = metadata;
    }

    public String getId()                          { return id; }
    public void setId(String id)                    { this.id = id; }

    public String getTitulo()                       { return titulo; }
    public void setTitulo(String titulo)            { this.titulo = titulo; }

    public String getMensaje()                      { return mensaje; }
    public void setMensaje(String mensaje)          { this.mensaje = mensaje; }

    public TipoNotificacion getTipo()               { return tipo; }
    public void setTipo(TipoNotificacion tipo)      { this.tipo = tipo; }

    public boolean isLeida()                        { return leida; }
    public void setLeida(boolean leida)             { this.leida = leida; }

    public LocalDateTime getFechaCreacion()                   { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaLectura()                    { return fechaLectura; }
    public void setFechaLectura(LocalDateTime fechaLectura)   { this.fechaLectura = fechaLectura; }

    public Map<String, Object> getMetadata()                  { return metadata; }
    public void setMetadata(Map<String, Object> metadata)     { this.metadata = metadata; }
}
