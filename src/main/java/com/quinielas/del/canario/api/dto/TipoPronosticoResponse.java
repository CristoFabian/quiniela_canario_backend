package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.TipoPronostico;

public class TipoPronosticoResponse {

    private Long   id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private int    puntos;
    private boolean activo;
    private long   totalOpciones;

    public TipoPronosticoResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static TipoPronosticoResponse from(TipoPronostico t) {
        TipoPronosticoResponse r = new TipoPronosticoResponse();
        r.id           = t.getId();
        r.codigo       = t.getCodigo();
        r.nombre       = t.getNombre();
        r.descripcion  = t.getDescripcion();
        r.puntos       = t.getPuntos();
        r.activo       = t.isActivo();
        r.totalOpciones = t.getOpciones().stream().filter(o -> o.isActivo()).count();
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getCodigo()                        { return codigo; }
    public void setCodigo(String codigo)             { this.codigo = codigo; }

    public String getNombre()                        { return nombre; }
    public void setNombre(String nombre)             { this.nombre = nombre; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String d)             { this.descripcion = d; }

    public int getPuntos()                           { return puntos; }
    public void setPuntos(int puntos)                { this.puntos = puntos; }

    public boolean isActivo()                        { return activo; }
    public void setActivo(boolean activo)            { this.activo = activo; }

    public long getTotalOpciones()                   { return totalOpciones; }
    public void setTotalOpciones(long totalOpciones) { this.totalOpciones = totalOpciones; }
}

