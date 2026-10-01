package com.quinielas.del.canario.api.event;

import com.quinielas.del.canario.api.entity.TipoNotificacion;

/**
 * Se publica cuando el administrador agrega, edita o elimina/desactiva una regla del juego.
 * Disparado por {@code ReglaJuegoService.crear/actualizar/eliminar}. Se notifica a todos
 * los jugadores activos (broadcast).
 */
public class ReglaJuegoEvent {

    private final Long             reglaId;
    private final String           titulo;
    private final TipoNotificacion tipo;

    public ReglaJuegoEvent(Long reglaId, String titulo, TipoNotificacion tipo) {
        this.reglaId = reglaId;
        this.titulo  = titulo;
        this.tipo    = tipo;
    }

    public Long getReglaId()             { return reglaId; }
    public String getTitulo()            { return titulo; }
    public TipoNotificacion getTipo()    { return tipo; }
}
