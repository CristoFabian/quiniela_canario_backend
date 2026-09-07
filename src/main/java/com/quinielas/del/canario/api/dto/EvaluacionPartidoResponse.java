package com.quinielas.del.canario.api.dto;

import java.util.List;

/**
 * Resultado de evaluar los pronósticos de un partido FINALIZADO.
 * Se devuelve al administrador cuando dispara la evaluación manual
 * o se utiliza internamente cuando la evaluación es automática.
 */
public class EvaluacionPartidoResponse {

    private Long    partidoId;
    private String  equipoLocal;
    private String  equipoVisitante;
    /** Pronósticos evaluados (todos los que estaban pendientes). */
    private int     pronosticosEvaluados;
    /** Jugadas ACTIVAS que tenían pronósticos en este partido. */
    private int     jugadasAfectadas;
    /** Detalle por jugada. */
    private List<ResumenJugadaEvaluada> jugadas;

    // ─── Constructor ─────────────────────────────────────────────────
    public EvaluacionPartidoResponse() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long    getPartidoId()                                 { return partidoId; }
    public void    setPartidoId(Long v)                           { this.partidoId = v; }

    public String  getEquipoLocal()                               { return equipoLocal; }
    public void    setEquipoLocal(String v)                       { this.equipoLocal = v; }

    public String  getEquipoVisitante()                           { return equipoVisitante; }
    public void    setEquipoVisitante(String v)                   { this.equipoVisitante = v; }

    public int     getPronosticosEvaluados()                      { return pronosticosEvaluados; }
    public void    setPronosticosEvaluados(int v)                 { this.pronosticosEvaluados = v; }

    public int     getJugadasAfectadas()                          { return jugadasAfectadas; }
    public void    setJugadasAfectadas(int v)                     { this.jugadasAfectadas = v; }

    public List<ResumenJugadaEvaluada> getJugadas()               { return jugadas; }
    public void    setJugadas(List<ResumenJugadaEvaluada> v)      { this.jugadas = v; }

    // ─── Clase interna ────────────────────────────────────────────────
    public static class ResumenJugadaEvaluada {
        private Long    jugadaId;
        private Long    usuarioId;
        private String  username;
        /** Puntos sumados EN ESTE partido para esta jugada. */
        private int     puntosEstePartido;
        /** Puntos acumulados totales de la jugada después de esta evaluación. */
        private int     puntosAcumulados;

        public Long   getJugadaId()            { return jugadaId; }
        public void   setJugadaId(Long v)      { this.jugadaId = v; }

        public Long   getUsuarioId()           { return usuarioId; }
        public void   setUsuarioId(Long v)     { this.usuarioId = v; }

        public String getUsername()            { return username; }
        public void   setUsername(String v)    { this.username = v; }

        public int    getPuntosEstePartido()   { return puntosEstePartido; }
        public void   setPuntosEstePartido(int v) { this.puntosEstePartido = v; }

        public int    getPuntosAcumulados()    { return puntosAcumulados; }
        public void   setPuntosAcumulados(int v) { this.puntosAcumulados = v; }
    }
}

