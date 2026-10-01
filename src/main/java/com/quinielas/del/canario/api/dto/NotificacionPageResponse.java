package com.quinielas.del.canario.api.dto;

import java.util.List;

/**
 * Página de notificaciones con metadatos de paginación propios (no se expone
 * {@code org.springframework.data.domain.Page} directamente para no acoplar el
 * contrato REST a Spring Data).
 */
public class NotificacionPageResponse {

    private List<NotificacionResponse> contenido;
    private int  pagina;
    private int  tamanio;
    private long totalElementos;
    private int  totalPaginas;

    public NotificacionPageResponse() {}

    public NotificacionPageResponse(List<NotificacionResponse> contenido, int pagina, int tamanio,
                                    long totalElementos, int totalPaginas) {
        this.contenido      = contenido;
        this.pagina         = pagina;
        this.tamanio        = tamanio;
        this.totalElementos = totalElementos;
        this.totalPaginas   = totalPaginas;
    }

    public List<NotificacionResponse> getContenido()          { return contenido; }
    public void setContenido(List<NotificacionResponse> c)    { this.contenido = c; }

    public int getPagina()               { return pagina; }
    public void setPagina(int pagina)    { this.pagina = pagina; }

    public int getTamanio()              { return tamanio; }
    public void setTamanio(int tamanio)  { this.tamanio = tamanio; }

    public long getTotalElementos()                 { return totalElementos; }
    public void setTotalElementos(long totalElementos) { this.totalElementos = totalElementos; }

    public int getTotalPaginas()             { return totalPaginas; }
    public void setTotalPaginas(int totalPaginas) { this.totalPaginas = totalPaginas; }
}
