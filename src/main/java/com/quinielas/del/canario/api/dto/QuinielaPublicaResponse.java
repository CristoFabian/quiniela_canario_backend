package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.EstadoQuiniela;
import com.quinielas.del.canario.api.entity.Quiniela;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Respuesta de quiniela orientada al jugador.
 * No expone datos administrativos como el usuario que la creó.
 */
public class QuinielaPublicaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal costo;
    private EstadoQuiniela estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;
    private LocalDateTime fechaCreacion;
    private int totalPartidos;

    /** Partidos incluidos solo en la vista de detalle; null en listados. */
    private List<PartidoResponse> partidos;

    public QuinielaPublicaResponse() {}

    // ─── Factory: solo cabecera (listar) ─────────────────────────────
    public static QuinielaPublicaResponse from(Quiniela q) {
        QuinielaPublicaResponse r = new QuinielaPublicaResponse();
        r.id            = q.getId();
        r.nombre        = q.getNombre();
        r.descripcion   = q.getDescripcion();
        r.costo         = q.getCosto();
        r.estado        = q.getEstado();
        r.fechaInicio   = q.getFechaInicio();
        r.fechaCierre   = q.getFechaCierre();
        r.fechaCreacion = q.getFechaCreacion();
        r.totalPartidos = q.getPartidos().size();
        return r;
    }

    // ─── Factory: cabecera + partidos (detalle) ───────────────────────
    public static QuinielaPublicaResponse fromDetalle(Quiniela q) {
        QuinielaPublicaResponse r = from(q);
        r.partidos = q.getPartidos()
                      .stream()
                      .map(PartidoResponse::from)
                      .collect(Collectors.toList());
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getNombre()                        { return nombre; }
    public void setNombre(String nombre)             { this.nombre = nombre; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String descripcion)   { this.descripcion = descripcion; }

    public BigDecimal getCosto()                     { return costo; }
    public void setCosto(BigDecimal costo)           { this.costo = costo; }

    public EstadoQuiniela getEstado()                { return estado; }
    public void setEstado(EstadoQuiniela estado)     { this.estado = estado; }

    public LocalDateTime getFechaInicio()            { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fi)     { this.fechaInicio = fi; }

    public LocalDateTime getFechaCierre()            { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fc)     { this.fechaCierre = fc; }

    public LocalDateTime getFechaCreacion()          { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fc)   { this.fechaCreacion = fc; }

    public int getTotalPartidos()                    { return totalPartidos; }
    public void setTotalPartidos(int totalPartidos)  { this.totalPartidos = totalPartidos; }

    public List<PartidoResponse> getPartidos()              { return partidos; }
    public void setPartidos(List<PartidoResponse> partidos) { this.partidos = partidos; }
}

