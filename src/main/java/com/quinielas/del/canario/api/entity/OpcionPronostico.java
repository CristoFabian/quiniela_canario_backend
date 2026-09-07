package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "opcion_pronostico")
public class OpcionPronostico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo al que pertenece esta opción (FK → tipo_pronostico.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_pronostico_id", nullable = false)
    private TipoPronostico tipoPronostico;

    /** Código único dentro del tipo: LOCAL_WIN, G_0_1, BTTS_SI, C_0_5, etc. */
    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descripcion;

    /**
     * Valor mínimo del rango (aplica a GOLES y CORNERS).
     * null para opciones sin rango (RESULTADO, BTTS).
     */
    @Column(name = "valor_min")
    private Integer valorMin;

    /**
     * Valor máximo del rango (aplica a GOLES y CORNERS).
     * null indica "o más" (p.ej. 6+ goles, 10+ corners).
     */
    @Column(name = "valor_max")
    private Integer valorMax;

    @Column(nullable = false)
    private boolean activo = true;

    // ─── Constructores ────────────────────────────────────────────────
    public OpcionPronostico() {}

    public OpcionPronostico(TipoPronostico tipo, String codigo, String descripcion,
                            Integer valorMin, Integer valorMax, boolean activo) {
        this.tipoPronostico = tipo;
        this.codigo         = codigo;
        this.descripcion    = descripcion;
        this.valorMin       = valorMin;
        this.valorMax       = valorMax;
        this.activo         = activo;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public TipoPronostico getTipoPronostico()              { return tipoPronostico; }
    public void setTipoPronostico(TipoPronostico tp)       { this.tipoPronostico = tp; }

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

