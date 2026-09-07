package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body para actualizar un pronóstico ya registrado.
 * Se permite cambiar el tipo de pronóstico y su opción.
 * El partido al que pertenece el pronóstico no cambia.
 */
public class ActualizarPronosticoRequest {

    @NotNull(message = "El id del tipo de pronostico es obligatorio.")
    private Long tipoPronosticoId;

    @NotNull(message = "El id de la opcion de pronostico es obligatorio.")
    private Long opcionPronosticoId;

    public ActualizarPronosticoRequest() {}

    public Long getTipoPronosticoId()                { return tipoPronosticoId; }
    public void setTipoPronosticoId(Long id)         { this.tipoPronosticoId = id; }

    public Long getOpcionPronosticoId()              { return opcionPronosticoId; }
    public void setOpcionPronosticoId(Long id)       { this.opcionPronosticoId = id; }
}
