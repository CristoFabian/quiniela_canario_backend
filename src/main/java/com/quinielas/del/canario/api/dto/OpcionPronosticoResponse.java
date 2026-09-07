package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.OpcionPronostico;

public class OpcionPronosticoResponse {

    private Long    id;
    private Long    tipoPronosticoId;
    private String  tipoPronosticoCodigo;
    private String  codigo;
    private String  descripcion;
    private Integer valorMin;
    private Integer valorMax;
    private boolean activo;

    public OpcionPronosticoResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static OpcionPronosticoResponse from(OpcionPronostico o) {
        OpcionPronosticoResponse r = new OpcionPronosticoResponse();
        r.id                  = o.getId();
        r.tipoPronosticoId    = o.getTipoPronostico().getId();
        r.tipoPronosticoCodigo = o.getTipoPronostico().getCodigo();
        r.codigo              = o.getCodigo();
        r.descripcion         = o.getDescripcion();
        r.valorMin            = o.getValorMin();
        r.valorMax            = o.getValorMax();
        r.activo              = o.isActivo();
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public Long getTipoPronosticoId()                      { return tipoPronosticoId; }
    public void setTipoPronosticoId(Long tid)              { this.tipoPronosticoId = tid; }

    public String getTipoPronosticoCodigo()                { return tipoPronosticoCodigo; }
    public void setTipoPronosticoCodigo(String c)          { this.tipoPronosticoCodigo = c; }

    public String getCodigo()                              { return codigo; }
    public void setCodigo(String codigo)                   { this.codigo = codigo; }

    public String getDescripcion()                         { return descripcion; }
    public void setDescripcion(String d)                   { this.descripcion = d; }

    public Integer getValorMin()                           { return valorMin; }
    public void setValorMin(Integer valorMin)              { this.valorMin = valorMin; }

    public Integer getValorMax()                           { return valorMax; }
    public void setValorMax(Integer valorMax)              { this.valorMax = valorMax; }

    public boolean isActivo()                              { return activo; }
    public void setActivo(boolean activo)                  { this.activo = activo; }
}

