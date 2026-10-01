package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.CategoriaRegla;
import com.quinielas.del.canario.api.entity.ReglaJuego;

import java.time.LocalDateTime;

public class ReglaJuegoResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private CategoriaRegla categoria;
    private int orden;
    private boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public ReglaJuegoResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static ReglaJuegoResponse from(ReglaJuego r) {
        ReglaJuegoResponse res = new ReglaJuegoResponse();
        res.id                 = r.getId();
        res.titulo             = r.getTitulo();
        res.descripcion        = r.getDescripcion();
        res.categoria          = r.getCategoria();
        res.orden              = r.getOrden();
        res.activo             = r.isActivo();
        res.fechaCreacion      = r.getFechaCreacion();
        res.fechaActualizacion = r.getFechaActualizacion();
        return res;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                     { return id; }
    public void setId(Long id)                              { this.id = id; }

    public String getTitulo()                               { return titulo; }
    public void setTitulo(String titulo)                    { this.titulo = titulo; }

    public String getDescripcion()                          { return descripcion; }
    public void setDescripcion(String descripcion)          { this.descripcion = descripcion; }

    public CategoriaRegla getCategoria()                    { return categoria; }
    public void setCategoria(CategoriaRegla categoria)      { this.categoria = categoria; }

    public int getOrden()                                   { return orden; }
    public void setOrden(int orden)                         { this.orden = orden; }

    public boolean isActivo()                               { return activo; }
    public void setActivo(boolean activo)                   { this.activo = activo; }

    public LocalDateTime getFechaCreacion()                 { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime f)           { this.fechaCreacion = f; }

    public LocalDateTime getFechaActualizacion()            { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime f)      { this.fechaActualizacion = f; }
}

