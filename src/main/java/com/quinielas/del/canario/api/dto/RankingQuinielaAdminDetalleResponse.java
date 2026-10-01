package com.quinielas.del.canario.api.dto;

import java.util.List;

/**
 * Ranking de una quiniela con detalle completo para el administrador: por cada
 * jugada incluye todos sus pronósticos (elegidos y evaluados), para poder ver
 * exactamente qué seleccionó el jugador y el resultado real que explica su puntaje.
 */
public class RankingQuinielaAdminDetalleResponse {

    private Long   quinielaId;
    private String nombreQuiniela;
    private String estadoQuiniela;
    private int    totalParticipantes;
    private String criteriosAplicados;
    private List<PosicionRankingDetalle> ranking;

    public RankingQuinielaAdminDetalleResponse() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long   getQuinielaId()                                  { return quinielaId; }
    public void   setQuinielaId(Long v)                            { this.quinielaId = v; }

    public String getNombreQuiniela()                              { return nombreQuiniela; }
    public void   setNombreQuiniela(String v)                      { this.nombreQuiniela = v; }

    public String getEstadoQuiniela()                              { return estadoQuiniela; }
    public void   setEstadoQuiniela(String v)                      { this.estadoQuiniela = v; }

    public int    getTotalParticipantes()                          { return totalParticipantes; }
    public void   setTotalParticipantes(int v)                     { this.totalParticipantes = v; }

    public String getCriteriosAplicados()                          { return criteriosAplicados; }
    public void   setCriteriosAplicados(String v)                  { this.criteriosAplicados = v; }

    public List<PosicionRankingDetalle> getRanking()               { return ranking; }
    public void   setRanking(List<PosicionRankingDetalle> v)       { this.ranking = v; }

    // ─── Clase interna: una fila del ranking con pronósticos completos ──
    public static class PosicionRankingDetalle {

        private Long    jugadaId;
        private int     posicion;
        private String  nombreJugador;
        private Integer puntosObtenidos;
        private boolean esGanador;
        private List<PronosticoDetalleAdminResponse> pronosticos;

        public PosicionRankingDetalle() {}

        public Long    getJugadaId()                               { return jugadaId; }
        public void    setJugadaId(Long v)                          { this.jugadaId = v; }

        public int     getPosicion()                                { return posicion; }
        public void    setPosicion(int v)                           { this.posicion = v; }

        public String  getNombreJugador()                           { return nombreJugador; }
        public void    setNombreJugador(String v)                   { this.nombreJugador = v; }

        public Integer getPuntosObtenidos()                         { return puntosObtenidos; }
        public void    setPuntosObtenidos(Integer v)                { this.puntosObtenidos = v; }

        public boolean isEsGanador()                                { return esGanador; }
        public void    setEsGanador(boolean v)                      { this.esGanador = v; }

        public List<PronosticoDetalleAdminResponse> getPronosticos()            { return pronosticos; }
        public void  setPronosticos(List<PronosticoDetalleAdminResponse> v)     { this.pronosticos = v; }
    }
}
