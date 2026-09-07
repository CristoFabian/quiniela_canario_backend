package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotBlank;

public class ActualizarEstadoQuinielaRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    public String getEstado()              { return estado; }
    public void setEstado(String estado)   { this.estado = estado; }
}

