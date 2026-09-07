package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Campos que el administrador puede modificar de una quiniela.
 * Solo se permite cuando la quiniela está en estado CREADA.
 */
public class ActualizarQuinielaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
    private BigDecimal costo;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de cierre es obligatoria")
    private LocalDateTime fechaCierre;

    // ─── Getters / Setters ────────────────────────────────────────────
    public String getNombre()                        { return nombre; }
    public void setNombre(String nombre)             { this.nombre = nombre; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String descripcion)   { this.descripcion = descripcion; }

    public BigDecimal getCosto()                     { return costo; }
    public void setCosto(BigDecimal costo)           { this.costo = costo; }

    public LocalDateTime getFechaInicio()            { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fi)     { this.fechaInicio = fi; }

    public LocalDateTime getFechaCierre()            { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fc)     { this.fechaCierre = fc; }
}

