package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OpcionPronosticoRequest {

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 30, message = "El codigo no puede superar 30 caracteres")
    private String codigo;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 200, message = "La descripcion no puede superar 200 caracteres")
    private String descripcion;

    /** Valor mínimo del rango (aplica a GOLES y CORNERS, nulo para otros) */
    private Integer valorMin;

    /** Valor máximo del rango (null = "o más", aplica a GOLES y CORNERS) */
    private Integer valorMax;

    private boolean activo = true;

    // ─── Getters / Setters ────────────────────────────────────────────
    public String getCodigo()                        { return codigo; }
    public void setCodigo(String codigo)             { this.codigo = codigo.toUpperCase(); }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String d)             { this.descripcion = d; }

    public Integer getValorMin()                     { return valorMin; }
    public void setValorMin(Integer valorMin)        { this.valorMin = valorMin; }

    public Integer getValorMax()                     { return valorMax; }
    public void setValorMax(Integer valorMax)        { this.valorMax = valorMax; }

    public boolean isActivo()                        { return activo; }
    public void setActivo(boolean activo)            { this.activo = activo; }
}

