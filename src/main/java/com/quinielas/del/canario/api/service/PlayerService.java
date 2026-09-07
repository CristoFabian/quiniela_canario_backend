package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.dto.*;
import com.quinielas.del.canario.api.entity.EstadoQuiniela;
import com.quinielas.del.canario.api.entity.Quiniela;
import com.quinielas.del.canario.api.entity.User;
import com.quinielas.del.canario.api.entity.UserProfile;
import com.quinielas.del.canario.api.repository.PartidoRepository;
import com.quinielas.del.canario.api.repository.QuinielaRepository;
import com.quinielas.del.canario.api.repository.TipoPronosticoRepository;
import com.quinielas.del.canario.api.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final UserProfileRepository  userProfileRepository;
    private final FileStorageService     fileStorageService;
    private final QuinielaRepository     quinielaRepository;
    private final PartidoRepository      partidoRepository;
    private final TipoPronosticoRepository tipoPronosticoRepository;
    private final CierreQuinielaService  cierreQuinielaService;

    public PlayerService(UserProfileRepository userProfileRepository,
                         FileStorageService fileStorageService,
                         QuinielaRepository quinielaRepository,
                         PartidoRepository partidoRepository,
                         TipoPronosticoRepository tipoPronosticoRepository,
                         CierreQuinielaService cierreQuinielaService) {
        this.userProfileRepository   = userProfileRepository;
        this.fileStorageService      = fileStorageService;
        this.quinielaRepository      = quinielaRepository;
        this.partidoRepository       = partidoRepository;
        this.tipoPronosticoRepository = tipoPronosticoRepository;
        this.cierreQuinielaService   = cierreQuinielaService;
    }

    // ─── GET perfil completo ──────────────────────────────────────────
    public PerfilResponse getPerfil(User user) {
        UserProfile perfil = obtenerPerfil(user);
        return PerfilResponse.from(user, perfil);
    }

    // ─── PUT actualizar datos de perfil ──────────────────────────────
    public PerfilResponse actualizarPerfil(User user, UpdatePerfilRequest request) {
        UserProfile perfil = obtenerPerfil(user);

        perfil.setNombre(request.getNombre());
        perfil.setApellidoPaterno(request.getApellidoPaterno());
        perfil.setApellidoMaterno(request.getApellidoMaterno());
        perfil.setCiudad(request.getCiudad());
        perfil.setFechaNacimiento(request.getFechaNacimiento());
        perfil.setTelefono(request.getTelefono());

        userProfileRepository.save(perfil);
        return PerfilResponse.from(user, perfil);
    }

    // ─── PUT actualizar foto de perfil ───────────────────────────────
    public PerfilResponse actualizarFoto(User user, MultipartFile foto) throws IOException {
        UserProfile perfil = obtenerPerfil(user);

        // Eliminar foto anterior si existe
        fileStorageService.eliminarFoto(perfil.getFoto());

        // Guardar nueva foto y actualizar el nombre en el perfil
        String nombreArchivo = fileStorageService.guardarFoto(foto, user.getId());
        perfil.setFoto(nombreArchivo);

        userProfileRepository.save(perfil);
        return PerfilResponse.from(user, perfil);
    }

    // ─── Quinielas disponibles (estado ABIERTA) ──────────────────────
    public List<QuinielaPublicaResponse> getQuinielas(User user) {
        return quinielaRepository.findByEstado(EstadoQuiniela.ABIERTA)
                .stream()
                .map(QuinielaPublicaResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Detalle de una quiniela visible al jugador (ABIERTA, EN_JUEGO o FINALIZADA) ─────
    @Transactional(readOnly = true)
    public QuinielaPublicaResponse obtenerDetalleQuiniela(Long quinielaId) {
        Quiniela quiniela = quinielaRepository.findById(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + quinielaId));

        if (quiniela.getEstado() == EstadoQuiniela.CREADA) {
            throw new IllegalArgumentException(
                    "La quiniela no esta disponible " +
                    "(estado actual: " + quiniela.getEstado() + ").");
        }
        return QuinielaPublicaResponse.fromDetalle(quiniela);
    }

    // ─── Partidos de una quiniela visible al jugador (ABIERTA, EN_JUEGO o FINALIZADA) ───
    @Transactional(readOnly = true)
    public List<PartidoResponse> listarPartidosDeQuiniela(Long quinielaId) {
        Quiniela quiniela = quinielaRepository.findById(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + quinielaId));

        if (quiniela.getEstado() == EstadoQuiniela.CREADA) {
            throw new IllegalArgumentException(
                    "La quiniela no esta disponible " +
                    "(estado actual: " + quiniela.getEstado() + ").");
        }
        return partidoRepository.findByQuinielaId(quinielaId)
                .stream()
                .map(PartidoResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Catálogo: tipos de pronóstico activos con opciones activas ──
    @Transactional(readOnly = true)
    public List<TipoPronosticoConOpcionesResponse> listarTiposConOpciones() {
        return tipoPronosticoRepository.findByActivoTrue()
                .stream()
                .map(TipoPronosticoConOpcionesResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Cierre de quiniela (resultado final, público para el jugador) ───
    @Transactional(readOnly = true)
    public CierreQuinielaResponse obtenerCierreQuiniela(Long quinielaId) {
        Quiniela quiniela = quinielaRepository.findById(quinielaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Quiniela no encontrada con id: " + quinielaId));
        if (quiniela.getEstado() != EstadoQuiniela.FINALIZADA) {
            throw new IllegalArgumentException(
                    "La quiniela aun no está finalizada (estado actual: " +
                    quiniela.getEstado() + ").");
        }
        return cierreQuinielaService.obtenerCierre(quinielaId);
    }

    // ─── Compatibilidad con AdminService (UserProfileResponse simple) ─
    public UserProfileResponse getPerfilSimple(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // ─── Privados ─────────────────────────────────────────────────────
    private UserProfile obtenerPerfil(User user) {
        return userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Perfil no encontrado para el usuario: " + user.getUsername()));
    }
}
