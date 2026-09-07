package com.quinielas.del.canario.api.dto;

import java.util.List;

/**
 * Ranking de jugadores para una quiniela específica.
 * Muestra posición, nombre y puntos de cada jugada ACTIVA o FINALIZADA.
 *
 * <p>Las posiciones son densas: si dos jugadas empatan en el 1er lugar,
 * ambas muestran posición 1 y la siguiente es posición 2 (no 3).
 */
public class RankingQuinielaResponse {

    private Long   quinielaId;
    private String nombreQuiniela;
    private String estadoQuiniela;
    private int    totalParticipantes;
    /**
     * Criterios aplicados para determinar el ganador (solo presente cuando la quiniela
     * está FINALIZADA). Ejemplos: "MAYOR_PUNTAJE", "DESEMPATE_TIPO_DIFICIL", etc.
     * null mientras la quiniela sigue EN_JUEGO.
     */
    private String criteriosAplicados;
    private List<PosicionRanking> ranking;

    public RankingQuinielaResponse() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long   getQuinielaId()                          { return quinielaId; }
    public void   setQuinielaId(Long v)                    { this.quinielaId = v; }

    public String getNombreQuiniela()                      { return nombreQuiniela; }
    public void   setNombreQuiniela(String v)              { this.nombreQuiniela = v; }

    public String getEstadoQuiniela()                      { return estadoQuiniela; }
    public void   setEstadoQuiniela(String v)              { this.estadoQuiniela = v; }

    public int    getTotalParticipantes()                  { return totalParticipantes; }
    public void   setTotalParticipantes(int v)             { this.totalParticipantes = v; }

    public String getCriteriosAplicados()                  { return criteriosAplicados; }
    public void   setCriteriosAplicados(String v)          { this.criteriosAplicados = v; }

    public List<PosicionRanking> getRanking()              { return ranking; }
    public void   setRanking(List<PosicionRanking> v)      { this.ranking = v; }

    // ─── Clase interna: una fila del ranking ─────────────────────────
    public static class PosicionRanking {

        /** Id de la jugada (ticket). Permite al front identificar la fila propia del jugador. */
        private Long   jugadaId;

        /** Posición en el ranking (1 = mejor). Dos jugadas con mismo puntaje comparten posición. */
        private int    posicion;

        /** Primer nombre del jugador obtenido de su perfil. */
        private String nombreJugador;

        /** Puntos acumulados por esta jugada. null si aún no se ha evaluado ningún partido. */
        private Integer puntosObtenidos;

        /** true si esta jugada fue declarada ganadora en el cierre. */
        private boolean esGanador;

        public PosicionRanking() {}

        // ─── Getters / Setters ────────────────────────────────────────
        public Long    getJugadaId()                       { return jugadaId; }
        public void    setJugadaId(Long v)                 { this.jugadaId = v; }

        public int     getPosicion()                       { return posicion; }
        public void    setPosicion(int v)                  { this.posicion = v; }

        public String  getNombreJugador()                  { return nombreJugador; }
        public void    setNombreJugador(String v)          { this.nombreJugador = v; }

        public Integer getPuntosObtenidos()                { return puntosObtenidos; }
        public void    setPuntosObtenidos(Integer v)       { this.puntosObtenidos = v; }

        public boolean isEsGanador()                       { return esGanador; }
        public void    setEsGanador(boolean v)             { this.esGanador = v; }
    }
}
