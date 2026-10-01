package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.CategoriaRegla;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReglaJuegoRequest {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
    private String titulo;

    @NotBlank(message = "La descripcion es obligatoria")
    private String descripcion;

    @NotNull(message = "La categoria es obligatoria")
    private CategoriaRegla categoria;

    private int orden = 0;

    private boolean activo = true;

    // ─── Getters / Setters ────────────────────────────────────────────
    public String getTitulo()                              { return titulo; }
    public void setTitulo(String titulo)                    { this.titulo = titulo; }

    public String getDescripcion()                          { return descripcion; }
    public void setDescripcion(String descripcion)          { this.descripcion = descripcion; }

    public CategoriaRegla getCategoria()                    { return categoria; }
    public void setCategoria(CategoriaRegla categoria)      { this.categoria = categoria; }

    public int getOrden()                                   { return orden; }
    public void setOrden(int orden)                         { this.orden = orden; }

    public boolean isActivo()                               { return activo; }
    public void setActivo(boolean activo)                   { this.activo = activo; }
}

