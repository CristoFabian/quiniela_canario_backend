package com.quinielas.del.canario.api.listener;

import com.quinielas.del.canario.api.entity.EstadoPago;
import com.quinielas.del.canario.api.entity.TipoNotificacion;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.event.*;
import com.quinielas.del.canario.api.repository.UserRepository;
import com.quinielas.del.canario.api.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Traduce cada evento de dominio en una llamada a {@link NotificacionService}.
 *
 * <p>Se usa {@link TransactionalEventListener} con {@code AFTER_COMMIT}: la notificación
 * solo se crea si la transacción de negocio (crear pago, validar pago, cerrar quiniela...)
 * realmente se confirmó. Si esa transacción hace rollback, nunca se genera la notificación
 * correspondiente — evitando notificaciones "fantasma" de operaciones que fallaron.</p>
 *
 * <p><b>Importante:</b> cada método está anotado con {@link Async} (habilitado con
 * {@code @EnableAsync} en {@code ApiApplication}). Con {@code spring.jpa.open-in-view=true}
 * (valor por defecto de Spring Boot), el {@code EntityManager} de la petición HTTP original
 * sigue ligado al hilo cuando se dispara {@code AFTER_COMMIT} (el "unbind" real ocurre
 * después, en {@code afterCompletion}). Si esta lógica corriera de forma síncrona en ese
 * mismo hilo, la nueva transacción de {@code NotificacionService} termina "participando"
 * por error en la transacción original — que ya hizo commit — y sus inserts nunca llegan
 * realmente a la base de datos (sin lanzar ninguna excepción visible: las notificaciones se
 * pierden en silencio). Ejecutar cada listener en un hilo aparte le da un
 * {@code EntityManager}/conexión completamente nuevos y evita este problema.</p>
 *
 * <p>Este es el ÚNICO lugar del código que conoce la existencia del canal "in-app".
 * Agregar un canal futuro (email, push, SMS) significa crear otro listener de estos
 * mismos eventos (p.ej. {@code EmailNotificacionListener}); los servicios de negocio
 * (Pago, Premio, Quiniela, Cierre) no requieren ningún cambio.</p>
 */
@Component
public class NotificacionEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEventListener.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionService notificacionService;
    private final UserRepository      userRepo;

    public NotificacionEventListener(NotificacionService notificacionService, UserRepository userRepo) {
        this.notificacionService = notificacionService;
        this.userRepo            = userRepo;
    }

    // ─── Administrador ─────────────────────────────────────────────

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPagoPendienteValidacion(PagoPendienteValidacionEvent event) {
        try {
            notificacionService.notificarATodosLosAdmins(
                    TipoNotificacion.PAGO_PENDIENTE_VALIDACION,
                    "Nuevo pago pendiente de validación",
                    event.getUsuarioNombre() + " registró un pago de $" + event.getMonto() +
                            " que requiere tu validación.",
                    Map.of("pagoId", event.getPagoId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de PagoPendienteValidacionEvent (pagoId={})",
                    event.getPagoId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPremioConfirmado(PremioConfirmadoEvent event) {
        try {
            notificacionService.notificarATodosLosAdmins(
                    TipoNotificacion.PREMIO_CONFIRMADO_JUGADOR,
                    "Premio confirmado por el jugador",
                    event.getUsuarioNombre() + " confirmó la recepción de su premio de la quiniela \"" +
                            event.getQuinielaNombre() + "\".",
                    Map.of("ganadorId", event.getGanadorId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de PremioConfirmadoEvent (ganadorId={})",
                    event.getGanadorId(), e);
        }
    }

    // ─── Jugador ────────────────────────────────────────────────────

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPagoValidado(PagoValidadoEvent event) {
        try {
            User destino = userRepo.findById(event.getUsuarioId()).orElse(null);
            if (destino == null) {
                log.warn("PagoValidadoEvent: usuario id={} no encontrado, se omite notificación.", event.getUsuarioId());
                return;
            }
            boolean aprobado = event.getNuevoEstado() == EstadoPago.APROBADO;
            notificacionService.notificar(
                    destino,
                    aprobado ? TipoNotificacion.PAGO_APROBADO : TipoNotificacion.PAGO_RECHAZADO,
                    aprobado ? "Tu pago fue aprobado" : "Tu pago fue rechazado",
                    aprobado
                            ? "Tu pago para la quiniela \"" + event.getQuinielaNombre() + "\" fue aprobado. ¡Ya puedes participar!"
                            : "Tu pago para la quiniela \"" + event.getQuinielaNombre() + "\" fue rechazado. Revisa el detalle para reintentar.",
                    Map.of("pagoId", event.getPagoId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de PagoValidadoEvent (pagoId={})", event.getPagoId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPremioPagado(PremioPagadoEvent event) {
        try {
            User destino = userRepo.findById(event.getUsuarioId()).orElse(null);
            if (destino == null) {
                log.warn("PremioPagadoEvent: usuario id={} no encontrado, se omite notificación.", event.getUsuarioId());
                return;
            }
            notificacionService.notificar(
                    destino,
                    TipoNotificacion.PREMIO_PAGADO,
                    "Tu premio fue pagado",
                    "El administrador subió el comprobante de pago de tu premio de la quiniela \"" +
                            event.getQuinielaNombre() + "\". Verifícalo y confirma su recepción.",
                    Map.of("ganadorId", event.getGanadorId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de PremioPagadoEvent (ganadorId={})", event.getGanadorId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJugadaGanadora(JugadaGanadoraEvent event) {
        try {
            User destino = userRepo.findById(event.getUsuarioId()).orElse(null);
            if (destino == null) {
                log.warn("JugadaGanadoraEvent: usuario id={} no encontrado, se omite notificación.", event.getUsuarioId());
                return;
            }
            notificacionService.notificar(
                    destino,
                    TipoNotificacion.JUGADA_GANADORA,
                    "¡Ganaste!",
                    "Tu jugada resultó ganadora en la quiniela \"" + event.getQuinielaNombre() +
                            "\". Premio: $" + event.getMontoPremio() + ".",
                    Map.of("ganadorId", event.getGanadorId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de JugadaGanadoraEvent (ganadorId={})", event.getGanadorId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuinielaAbierta(QuinielaAbiertaEvent event) {
        try {
            notificacionService.notificarATodosLosJugadoresActivos(
                    TipoNotificacion.QUINIELA_DISPONIBLE,
                    "Nueva quiniela disponible",
                    "La quiniela \"" + event.getNombreQuiniela() + "\" ya está abierta. ¡Regístrate!",
                    Map.of("quinielaId", event.getQuinielaId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de QuinielaAbiertaEvent (quinielaId={})", event.getQuinielaId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuinielaProximaCerrar(QuinielaProximaCerrarEvent event) {
        try {
            notificacionService.notificarATodosLosJugadoresActivos(
                    TipoNotificacion.QUINIELA_PROXIMA_CERRAR,
                    "Una quiniela está por cerrar",
                    "La quiniela \"" + event.getNombreQuiniela() + "\" cierra el " +
                            event.getFechaCierre().format(FORMATO_FECHA) + ". ¡No te quedes fuera!",
                    Map.of("quinielaId", event.getQuinielaId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de QuinielaProximaCerrarEvent (quinielaId={})", event.getQuinielaId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReglaJuego(ReglaJuegoEvent event) {
        try {
            String titulo = switch (event.getTipo()) {
                case REGLA_AGREGADA -> "Nueva regla del juego";
                case REGLA_EDITADA -> "Regla del juego actualizada";
                case REGLA_ELIMINADA -> "Regla del juego eliminada";
                case REGLA_REACTIVADA -> "Regla del juego reactivada";
                default -> "Regla del juego";
            };
            String mensaje = switch (event.getTipo()) {
                case REGLA_AGREGADA -> "Se agregó la regla \"" + event.getTitulo() + "\". Revísala en Reglas del Juego.";
                case REGLA_EDITADA -> "La regla \"" + event.getTitulo() + "\" fue editada. Revisa los cambios.";
                case REGLA_ELIMINADA -> "La regla \"" + event.getTitulo() + "\" fue eliminada/desactivada.";
                case REGLA_REACTIVADA -> "La regla \"" + event.getTitulo() + "\" volvió a estar activa. Revísala en Reglas del Juego.";
                default -> event.getTitulo();
            };
            notificacionService.notificarATodosLosJugadoresActivos(
                    event.getTipo(), titulo, mensaje, Map.of("reglaId", event.getReglaId()));
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de ReglaJuegoEvent (reglaId={})", event.getReglaId(), e);
        }
    }
}
