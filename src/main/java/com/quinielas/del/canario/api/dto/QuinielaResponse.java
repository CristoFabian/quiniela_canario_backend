package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.EstadoQuiniela;
import com.quinielas.del.canario.api.entity.Quiniela;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class QuinielaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal costo;
    private EstadoQuiniela estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;
    private String creadoPorUsername;
    private LocalDateTime fechaCreacion;
    private int totalPartidos;
    /**
     * Bolsa acumulada: suma de los montos de los pagos APROBADOS
     * asociados a las jugadas de esta quiniela.
     */
    private BigDecimal bolsaAcumulada;
    /**
     * Mensaje de advertencia informativo (no bloquea la operación).
     * Se devuelve, por ejemplo, cuando hay pagos pendientes de validar
     * al pasar la quiniela a EN_JUEGO. null si no hay advertencia.
     */
    private String advertencia;

    /** Partidos incluidos solo en la vista de detalle; null en listados. */
    private List<PartidoResponse> partidos;

    /**
     * Número de participantes (jugadas con pago confirmado: ACTIVA o FINALIZADA).
     * Se calcula solo en la vista de detalle; 0 en listados.
     */
    private int totalParticipantes;

    public QuinielaResponse() {}

    // ─── Factory: solo cabecera (listar) ─────────────────────────────
    public static QuinielaResponse from(Quiniela q) {
        QuinielaResponse r = new QuinielaResponse();
        r.id                 = q.getId();
        r.nombre             = q.getNombre();
        r.descripcion        = q.getDescripcion();
        r.costo              = q.getCosto();
        r.estado             = q.getEstado();
        r.fechaInicio        = q.getFechaInicio();
        r.fechaCierre        = q.getFechaCierre();
        r.creadoPorUsername  = q.getCreadoPor() != null ? q.getCreadoPor().getUsername() : null;
        r.fechaCreacion      = q.getFechaCreacion();
        r.totalPartidos      = q.getPartidos().size();
        r.bolsaAcumulada     = q.getBolsaAcumulada();
        return r;
    }

    // ─── Factory: cabecera + partidos (detalle) ───────────────────────
    public static QuinielaResponse fromDetalle(Quiniela q) {
        QuinielaResponse r = from(q);
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

    public String getCreadoPorUsername()                       { return creadoPorUsername; }
    public void setCreadoPorUsername(String creadoPorUsername) { this.creadoPorUsername = creadoPorUsername; }

    public LocalDateTime getFechaCreacion()          { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fc)   { this.fechaCreacion = fc; }

    public int getTotalPartidos()                    { return totalPartidos; }
    public void setTotalPartidos(int totalPartidos)  { this.totalPartidos = totalPartidos; }

    public BigDecimal getBolsaAcumulada()                     { return bolsaAcumulada; }
    public void setBolsaAcumulada(BigDecimal bolsaAcumulada)  { this.bolsaAcumulada = bolsaAcumulada; }

    public String getAdvertencia()                   { return advertencia; }
    public void setAdvertencia(String advertencia)   { this.advertencia = advertencia; }

    public List<PartidoResponse> getPartidos()              { return partidos; }
    public void setPartidos(List<PartidoResponse> partidos) { this.partidos = partidos; }

    public int getTotalParticipantes()                        { return totalParticipantes; }
    public void setTotalParticipantes(int totalParticipantes) { this.totalParticipantes = totalParticipantes; }
}

