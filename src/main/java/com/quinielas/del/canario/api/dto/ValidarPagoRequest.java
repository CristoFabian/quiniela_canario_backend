package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ValidarPagoRequest {

    @NotBlank(message = "El estado es obligatorio.")
    @Pattern(regexp = "APROBADO|RECHAZADO",
             message = "El estado debe ser APROBADO o RECHAZADO.")
    private String estado;

    /** Observación opcional (útil para indicar el motivo de un rechazo). */
    private String observacion;

    public ValidarPagoRequest() {}

    public String getEstado()                          { return estado; }
    public void setEstado(String estado)               { this.estado = estado; }

    public String getObservacion()                     { return observacion; }
    public void setObservacion(String observacion)     { this.observacion = observacion; }
}

