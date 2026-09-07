package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Campos editables de un partido.
 * Solo se permite cuando la quiniela está en estado CREADA
 * y el partido en estado PENDIENTE.
 */
public class ActualizarPartidoRequest {

    @Size(max = 300, message = "La descripción no puede superar 300 caracteres")
    private String descripcion;

    @NotBlank(message = "El equipo local es obligatorio")
    @Size(max = 100, message = "El nombre del equipo local no puede superar 100 caracteres")
    private String equipoLocal;

    @NotBlank(message = "El equipo visitante es obligatorio")
    @Size(max = 100, message = "El nombre del equipo visitante no puede superar 100 caracteres")
    private String equipoVisitante;

    @NotNull(message = "La fecha del partido es obligatoria")
    private LocalDateTime fechaPartido;

    // ─── Getters / Setters ────────────────────────────────────────────
    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String d)             { this.descripcion = d; }

    public String getEquipoLocal()                   { return equipoLocal; }
    public void setEquipoLocal(String el)            { this.equipoLocal = el; }

    public String getEquipoVisitante()               { return equipoVisitante; }
    public void setEquipoVisitante(String ev)        { this.equipoVisitante = ev; }

    public LocalDateTime getFechaPartido()           { return fechaPartido; }
    public void setFechaPartido(LocalDateTime fp)    { this.fechaPartido = fp; }
}

