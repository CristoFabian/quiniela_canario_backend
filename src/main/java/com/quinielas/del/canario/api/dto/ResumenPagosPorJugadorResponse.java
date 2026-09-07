package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.Pago;
import com.quinielas.del.canario.api.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resumen de pagos pendientes agrupados por jugador.
 * Diseñado para el panel de validación del administrador:
 * permite ver de un vistazo qué jugadores tienen pagos en espera,
 * cuántos tickets cubren y si ya adjuntaron comprobante.
 */
public class ResumenPagosPorJugadorResponse {

    // ─── Datos del jugador ────────────────────────────────────────────
    private Long    usuarioId;
    private String  username;
    private String  email;
    private boolean cuentaActiva;

    // ─── Resumen de pagos pendientes ──────────────────────────────────
    /** Número de pagos en estado PENDIENTE para este jugador. */
    private int        totalPagosPendientes;

    /** Suma de montos de todos los pagos pendientes. */
    private BigDecimal montoPendienteTotal;

    /**
     * {@code true} si TODOS los pagos pendientes tienen comprobante
     * (archivo subido o marcado como enviado por WhatsApp).
     */
    private boolean todosConComprobante;

    /** {@code true} si al menos un pago fue reportado como enviado por WhatsApp. */
    private boolean algunoConWhatsapp;

    /**
     * Fecha de creación del pago pendiente más antiguo.
     * Permite al administrador priorizar los casos más urgentes.
     */
    private LocalDateTime pagoMasAntiguo;

    /** Lista detallada de los pagos pendientes. */
    private List<PagoResponse> pagos;

    // ─── Factory ──────────────────────────────────────────────────────

    /**
     * Construye el resumen a partir del usuario y la lista de sus pagos pendientes.
     *
     * @param usuario    jugador dueño de los pagos
     * @param pagosList  pagos en estado PENDIENTE del jugador (ya filtrados)
     */
    public static ResumenPagosPorJugadorResponse from(User usuario, List<Pago> pagosList) {
        ResumenPagosPorJugadorResponse r = new ResumenPagosPorJugadorResponse();

        r.usuarioId            = usuario.getId();
        r.username             = usuario.getUsername();
        r.email                = usuario.getEmail();
        r.cuentaActiva         = usuario.isActivo();
        r.totalPagosPendientes = pagosList.size();

        r.montoPendienteTotal  = pagosList.stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        r.todosConComprobante  = pagosList.stream().allMatch(p ->
                (p.getComprobanteUrl() != null && !p.getComprobanteUrl().isBlank())
                || p.isComprobanteWhatsapp());

        r.algunoConWhatsapp    = pagosList.stream().anyMatch(Pago::isComprobanteWhatsapp);

        r.pagoMasAntiguo       = pagosList.stream()
                .map(Pago::getFechaCreacion)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        r.pagos                = pagosList.stream()
                .map(PagoResponse::from)
                .collect(Collectors.toList());

        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getUsuarioId()                                      { return usuarioId; }
    public void setUsuarioId(Long usuarioId)                        { this.usuarioId = usuarioId; }

    public String getUsername()                                     { return username; }
    public void setUsername(String username)                        { this.username = username; }

    public String getEmail()                                        { return email; }
    public void setEmail(String email)                              { this.email = email; }

    public boolean isCuentaActiva()                                 { return cuentaActiva; }
    public void setCuentaActiva(boolean cuentaActiva)               { this.cuentaActiva = cuentaActiva; }

    public int getTotalPagosPendientes()                            { return totalPagosPendientes; }
    public void setTotalPagosPendientes(int t)                      { this.totalPagosPendientes = t; }

    public BigDecimal getMontoPendienteTotal()                      { return montoPendienteTotal; }
    public void setMontoPendienteTotal(BigDecimal m)                { this.montoPendienteTotal = m; }

    public boolean isTodosConComprobante()                          { return todosConComprobante; }
    public void setTodosConComprobante(boolean t)                   { this.todosConComprobante = t; }

    public boolean isAlgunoConWhatsapp()                            { return algunoConWhatsapp; }
    public void setAlgunoConWhatsapp(boolean a)                     { this.algunoConWhatsapp = a; }

    public LocalDateTime getPagoMasAntiguo()                        { return pagoMasAntiguo; }
    public void setPagoMasAntiguo(LocalDateTime p)                  { this.pagoMasAntiguo = p; }

    public List<PagoResponse> getPagos()                            { return pagos; }
    public void setPagos(List<PagoResponse> pagos)                  { this.pagos = pagos; }
}

