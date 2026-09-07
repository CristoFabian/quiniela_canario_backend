package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.TipoPronostico;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Proyección para el jugador: tipo de pronóstico con la lista completa
 * de sus opciones activas embebidas. Permite que el frontend construya
 * el formulario de pronóstico sin llamadas adicionales.
 */
public class TipoPronosticoConOpcionesResponse {

    private Long   id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private int    puntos;

    /** Solo se incluyen opciones activas. */
    private List<OpcionPronosticoResponse> opciones;

    public TipoPronosticoConOpcionesResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static TipoPronosticoConOpcionesResponse from(TipoPronostico t) {
        TipoPronosticoConOpcionesResponse r = new TipoPronosticoConOpcionesResponse();
        r.id          = t.getId();
        r.codigo      = t.getCodigo();
        r.nombre      = t.getNombre();
        r.descripcion = t.getDescripcion();
        r.puntos      = t.getPuntos();
        r.opciones    = t.getOpciones()
                         .stream()
                         .filter(o -> o.isActivo())
                         .map(OpcionPronosticoResponse::from)
                         .collect(Collectors.toList());
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                          { return id; }
    public void setId(Long id)                                   { this.id = id; }

    public String getCodigo()                                    { return codigo; }
    public void setCodigo(String codigo)                         { this.codigo = codigo; }

    public String getNombre()                                    { return nombre; }
    public void setNombre(String nombre)                         { this.nombre = nombre; }

    public String getDescripcion()                               { return descripcion; }
    public void setDescripcion(String descripcion)               { this.descripcion = descripcion; }

    public int getPuntos()                                       { return puntos; }
    public void setPuntos(int puntos)                            { this.puntos = puntos; }

    public List<OpcionPronosticoResponse> getOpciones()          { return opciones; }
    public void setOpciones(List<OpcionPronosticoResponse> opts) { this.opciones = opts; }
}

