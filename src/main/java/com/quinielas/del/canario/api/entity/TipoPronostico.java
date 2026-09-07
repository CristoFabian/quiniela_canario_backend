package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tipo_pronostico")
public class TipoPronostico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código interno único: RESULTADO, GOLES, BTTS, CORNERS */
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    /** Nombre legible: "Resultado final", "Total de goles", etc. */
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** Puntos que otorga acertar este tipo de pronóstico */
    @Column(nullable = false)
    private int puntos;

    @Column(nullable = false)
    private boolean activo = true;

    // ─── Relación 1:N con OpcionPronostico ───────────────────────────
    @OneToMany(mappedBy = "tipoPronostico", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OpcionPronostico> opciones = new ArrayList<>();

    // ─── Constructores ────────────────────────────────────────────────
    public TipoPronostico() {}

    public TipoPronostico(String codigo, String nombre, String descripcion,
                          int puntos, boolean activo) {
        this.codigo      = codigo;
        this.nombre      = nombre;
        this.descripcion = descripcion;
        this.puntos      = puntos;
        this.activo      = activo;
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

    public List<OpcionPronostico> getOpciones()               { return opciones; }
    public void setOpciones(List<OpcionPronostico> opciones)  { this.opciones = opciones; }
}

