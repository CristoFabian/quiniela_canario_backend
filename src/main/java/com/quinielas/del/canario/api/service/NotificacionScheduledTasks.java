package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.entity.EstadoQuiniela;
import com.quinielas.del.canario.api.entity.Quiniela;
import com.quinielas.del.canario.api.event.QuinielaProximaCerrarEvent;
import com.quinielas.del.canario.api.repository.QuinielaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Tareas de fondo que alimentan el sistema de notificaciones con eventos que no
 * se disparan por una acción directa del usuario, sino por el paso del tiempo.
 */
@Service
public class NotificacionScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(NotificacionScheduledTasks.class);
    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");

    private final QuinielaRepository        quinielaRepo;
    private final ApplicationEventPublisher eventPublisher;

    public NotificacionScheduledTasks(QuinielaRepository quinielaRepo,
                                      ApplicationEventPublisher eventPublisher) {
        this.quinielaRepo   = quinielaRepo;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Revisa diariamente (9:00 am hora CDMX) las quinielas ABIERTA cuyo cierre cae
     * dentro de las próximas 24 horas y aún no tienen el aviso enviado.
     * Publica {@link QuinielaProximaCerrarEvent} por cada una y marca la bandera
     * para no volver a avisar (la tarea corre todos los días, no solo una vez).
     */
    @Scheduled(cron = "0 55 18 * * *", zone = "America/Mexico_City")
    @Transactional
    public void avisarQuinielasProximasACerrar() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_MX);
        LocalDateTime hasta = ahora.plusDays(1);

        List<Quiniela> proximas = quinielaRepo.findByEstadoAndAvisoCierreEnviadoFalseAndFechaCierreBetween(
                EstadoQuiniela.ABIERTA, ahora, hasta);

        if (proximas.isEmpty()) return;

        for (Quiniela q : proximas) {
            eventPublisher.publishEvent(
                    new QuinielaProximaCerrarEvent(q.getId(), q.getNombre(), q.getFechaCierre()));
            q.setAvisoCierreEnviado(true);
        }
        quinielaRepo.saveAll(proximas);
        log.info("Aviso de cierre próximo enviado para {} quiniela(s).", proximas.size());
    }
}
