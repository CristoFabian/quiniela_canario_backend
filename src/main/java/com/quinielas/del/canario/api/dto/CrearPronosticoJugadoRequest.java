package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotNull;

public class CrearPronosticoJugadoRequest {

    @NotNull(message = "El id del partido es obligatorio")
    private Long partidoId;

    @NotNull(message = "El id del tipo de pronostico es obligatorio")
    private Long tipoPronosticoId;

    @NotNull(message = "El id de la opcion de pronostico es obligatoria")
    private Long opcionPronosticoId;

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getPartidoId()                         { return partidoId; }
    public void setPartidoId(Long partidoId)           { this.partidoId = partidoId; }

    public Long getTipoPronosticoId()                  { return tipoPronosticoId; }
    public void setTipoPronosticoId(Long tpId)         { this.tipoPronosticoId = tpId; }

    public Long getOpcionPronosticoId()                { return opcionPronosticoId; }
    public void setOpcionPronosticoId(Long opId)       { this.opcionPronosticoId = opId; }
}

