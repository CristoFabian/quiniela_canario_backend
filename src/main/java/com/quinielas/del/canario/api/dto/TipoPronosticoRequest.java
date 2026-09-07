package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TipoPronosticoRequest {

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 30, message = "El codigo no puede superar 30 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    private String descripcion;

    @NotNull(message = "Los puntos son obligatorios")
    @Min(value = 1, message = "Los puntos deben ser al menos 1")
    private Integer puntos;

    private boolean activo = true;

    // ─── Getters / Setters ────────────────────────────────────────────
    public String getCodigo()                        { return codigo; }
    public void setCodigo(String codigo)             { this.codigo = codigo.toUpperCase(); }

    public String getNombre()                        { return nombre; }
    public void setNombre(String nombre)             { this.nombre = nombre; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String d)             { this.descripcion = d; }

    public Integer getPuntos()                       { return puntos; }
    public void setPuntos(Integer puntos)            { this.puntos = puntos; }

    public boolean isActivo()                        { return activo; }
    public void setActivo(boolean activo)            { this.activo = activo; }
}

