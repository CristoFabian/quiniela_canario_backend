package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotNull;

public class CrearJugadaRequest {

    @NotNull(message = "El id de la quiniela es obligatorio")
    private Long quinielaId;

    public Long getQuinielaId()              { return quinielaId; }
    public void setQuinielaId(Long qid)      { this.quinielaId = qid; }
}

