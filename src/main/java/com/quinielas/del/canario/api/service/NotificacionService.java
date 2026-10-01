package com.quinielas.del.canario.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quinielas.del.canario.api.dto.NotificacionPageResponse;
import com.quinielas.del.canario.api.dto.NotificacionResponse;
import com.quinielas.del.canario.api.entity.Notificacion;
import com.quinielas.del.canario.api.entity.Role;
import com.quinielas.del.canario.api.entity.TipoNotificacion;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.repository.NotificacionRepository;
import com.quinielas.del.canario.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Único punto de escritura/lectura de {@link Notificacion}. Los controllers de negocio
 * (Pago, Premio, Quiniela, etc.) NUNCA llaman a este servicio directamente: publican un
 * evento de dominio y es {@code NotificacionEventListener} quien lo traduce a una llamada
 * aquí. Esto mantiene el canal "in-app" desacoplado de la lógica de negocio y permite
 * agregar futuros canales (email, push, SMS) como listeners adicionales del mismo evento,
 * sin tocar ni este servicio ni los servicios de negocio.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    /** Máximo de notificaciones devueltas por página, sin importar lo que pida el cliente. */
    private static final int TAMANIO_PAGINA_MAX = 50;

    private final NotificacionRepository notificacionRepo;
    private final UserRepository         userRepo;
    private final ObjectMapper           objectMapper;

    public NotificacionService(NotificacionRepository notificacionRepo,
                               UserRepository userRepo,
                               ObjectMapper objectMapper) {
        this.notificacionRepo = notificacionRepo;
        this.userRepo         = userRepo;
        this.objectMapper     = objectMapper;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Escritura (usada únicamente por NotificacionEventListener)
    // ═════════════════════════════════════════════════════════════════

    /** Crea una notificación para un único usuario. */
    @Transactional
    public void notificar(User destino, TipoNotificacion tipo, String titulo, String mensaje,
                          Map<String, Object> metadata) {
        Notificacion n = new Notificacion();
        n.setUsuarioDestino(destino);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setMetadata(serializarMetadata(metadata));
        notificacionRepo.save(n);
    }

    /** Crea una notificación para todos los administradores activos (broadcast acotado por rol). */
    @Transactional
    public void notificarATodosLosAdmins(TipoNotificacion tipo, String titulo, String mensaje,
                                         Map<String, Object> metadata) {
        List<User> admins = userRepo.findByRoleAndActivoTrue(Role.ADMIN);
        String metadataJson = serializarMetadata(metadata);
        admins.forEach(admin -> {
            Notificacion n = new Notificacion();
            n.setUsuarioDestino(admin);
            n.setTipo(tipo);
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setMetadata(metadataJson);
            notificacionRepo.save(n);
        });
    }

    /** Crea una notificación para todos los jugadores activos (broadcast: nueva quiniela, aviso de cierre). */
    @Transactional
    public void notificarATodosLosJugadoresActivos(TipoNotificacion tipo, String titulo, String mensaje,
                                                    Map<String, Object> metadata) {
        List<User> jugadores = userRepo.findByRoleAndActivoTrue(Role.USER);
        String metadataJson = serializarMetadata(metadata);
        jugadores.forEach(jugador -> {
            Notificacion n = new Notificacion();
            n.setUsuarioDestino(jugador);
            n.setTipo(tipo);
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setMetadata(metadataJson);
            notificacionRepo.save(n);
        });
    }

    private String serializarMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar metadata de notificación: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializarMetadata(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("No se pudo deserializar metadata de notificación: {}", e.getMessage());
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Lectura (usada por el controller — siempre acotada al usuario autenticado)
    // ═════════════════════════════════════════════════════════════════

    /** Historial paginado del usuario autenticado, más reciente primero. */
    @Transactional(readOnly = true)
    public NotificacionPageResponse listar(User usuario, int pagina, int tamanio) {
        Pageable pageable = construirPageable(pagina, tamanio);
        Page<Notificacion> page = notificacionRepo
                .findByUsuarioDestinoIdOrderByFechaCreacionDesc(usuario.getId(), pageable);
        return construirRespuestaPagina(page);
    }

    /** Solo las no leídas del usuario autenticado, paginado. */
    @Transactional(readOnly = true)
    public NotificacionPageResponse listarNoLeidas(User usuario, int pagina, int tamanio) {
        Pageable pageable = construirPageable(pagina, tamanio);
        Page<Notificacion> page = notificacionRepo
                .findByUsuarioDestinoIdAndLeidaFalseOrderByFechaCreacionDesc(usuario.getId(), pageable);
        return construirRespuestaPagina(page);
    }

    /** Conteo de no leídas para el badge de la campana. */
    @Transactional(readOnly = true)
    public long contarNoLeidas(User usuario) {
        return notificacionRepo.countByUsuarioDestinoIdAndLeidaFalse(usuario.getId());
    }

    /**
     * Marca una notificación propia como leída (idempotente).
     * Lanza excepción si la notificación no existe o no pertenece al usuario autenticado
     * (nunca se revela si existe y es de otro usuario; mismo mensaje en ambos casos).
     */
    @Transactional
    public NotificacionResponse marcarLeida(User usuario, String publicId) {
        Notificacion n = notificacionRepo.findByPublicIdAndUsuarioDestinoId(publicId, usuario.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Notificación no encontrada o no pertenece al usuario autenticado."));

        if (!n.isLeida()) {
            n.setLeida(true);
            n.setFechaLectura(LocalDateTime.now(ZoneId.of("America/Mexico_City")));
            notificacionRepo.save(n);
        }
        return aResponse(n);
    }

    /** Marca todas las notificaciones del usuario autenticado como leídas. */
    @Transactional
    public int marcarTodasLeidas(User usuario) {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        return notificacionRepo.marcarTodasLeidas(usuario.getId(), ahora);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Privados
    // ═════════════════════════════════════════════════════════════════

    private Pageable construirPageable(int pagina, int tamanio) {
        int paginaSegura  = Math.max(pagina, 0);
        int tamanioSeguro = tamanio <= 0 ? 20 : Math.min(tamanio, TAMANIO_PAGINA_MAX);
        // El ordenamiento real ya lo define el nombre del método del repositorio;
        // aquí solo se controla el tamaño de página para evitar respuestas gigantes.
        return PageRequest.of(paginaSegura, tamanioSeguro, Sort.unsorted());
    }

    private NotificacionPageResponse construirRespuestaPagina(Page<Notificacion> page) {
        List<NotificacionResponse> contenido = page.getContent().stream()
                .map(this::aResponse)
                .collect(Collectors.toList());
        return new NotificacionPageResponse(
                contenido, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private NotificacionResponse aResponse(Notificacion n) {
        return new NotificacionResponse(
                n.getPublicId(),
                n.getTitulo(),
                n.getMensaje(),
                n.getTipo(),
                n.isLeida(),
                n.getFechaCreacion(),
                n.getFechaLectura(),
                deserializarMetadata(n.getMetadata()));
    }
}
