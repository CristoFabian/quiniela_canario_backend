package com.quinielas.del.canario.api.dto;

/** Respuesta ligera para el badge de la campana: solo el conteo de no leídas. */
public class NotificacionCountResponse {

    private long noLeidas;

    public NotificacionCountResponse() {}

    public NotificacionCountResponse(long noLeidas) {
        this.noLeidas = noLeidas;
    }

    public long getNoLeidas()             { return noLeidas; }
    public void setNoLeidas(long noLeidas) { this.noLeidas = noLeidas; }
}
