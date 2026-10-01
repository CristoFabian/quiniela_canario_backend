package com.quinielas.del.canario.api.service;

import com.quinielas.del.canario.api.service.storage.StorageService;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de negocio para guardar y eliminar archivos (fotos y comprobantes).
 * Se encarga de:
 *  - Validar extensión, tamaño y contenido real (Tika) del archivo.
 *  - Generar nombres de archivo seguros y únicos.
 *  - Delegar el almacenamiento físico en {@link StorageService} (disco local
 *    u Oracle Object Storage, según {@code app.storage.provider}).
 *
 *  - Fotos de perfil : carpeta "perfiles"    (jpg/jpeg/png/webp, máx 2 MB)
 *  - Comprobantes    : carpeta "comprobantes" (jpg/jpeg/png/pdf, máx 5 MB, sin URL pública)
 */
@Service
public class FileStorageService {

    private static final String CARPETA_PERFILES     = "perfiles";
    private static final String CARPETA_COMPROBANTES = "comprobantes";

    // ─── Foto de perfil ───────────────────────────────────────────────
    private static final Set<String>        EXT_FOTO  = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String>        MIME_FOTO = Set.of(
            "image/jpeg", "image/png", "image/webp");
    private static final long               MAX_FOTO  = 2L * 1024 * 1024; // 2 MB

    // ─── Comprobante de pago ──────────────────────────────────────────
    /** Extensiones permitidas para comprobantes (sin webp, pero con pdf). */
    private static final Set<String>        EXT_COMPROBANTE = Set.of("jpg", "jpeg", "png", "pdf");

    /** MIME types aceptados; application/octet-stream se tolera como fallback. */
    private static final Set<String>        MIME_COMPROBANTE = Set.of(
            "image/jpeg", "image/png", "application/pdf", "application/octet-stream");

    /** Extensión esperada según MIME declarado. */
    private static final Map<String,String> MIME_A_EXT = Map.of(
            "image/jpeg",        "jpg",
            "image/png",         "png",
            "application/pdf",   "pdf");

    private static final long               MAX_COMPROBANTE = 5L * 1024 * 1024; // 5 MB

    private final Tika tika;
    private final StorageService storageService;

    public FileStorageService(Tika tika, StorageService storageService) {
        this.tika = tika;
        this.storageService = storageService;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Foto de perfil
    // ═════════════════════════════════════════════════════════════════

    /**
     * Guarda la foto de perfil y devuelve el nombre generado.
     * Nombre: profile_{userId}_{uuid}.{ext}
     *
     * Validaciones (3 capas):
     *  1. Extensión: jpg, jpeg, png, webp
     *  2. MIME real (Tika magic bytes): image/jpeg, image/png, image/webp
     *  3. Coherencia extensión ↔ MIME detectado
     */
    public String guardarFoto(MultipartFile file, Long userId) throws IOException {
        validarFoto(file);
        String nombre = generarNombreFoto(userId, file.getOriginalFilename());
        storageService.guardar(CARPETA_PERFILES, nombre, file);
        return nombre;
    }

    /** Elimina una foto de perfil del almacenamiento (silencioso si no existe). */
    public void eliminarFoto(String nombre) {
        storageService.eliminar(CARPETA_PERFILES, nombre);
    }

    /**
     * URL pública y directa de la foto de perfil, o {@code null} si el backend
     * activo no soporta URLs públicas absolutas (en ese caso, se sirve por el
     * recurso estático /perfiles/** de Spring, según app.upload.url-prefix).
     */
    public String obtenerUrlPublicaFoto(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        return storageService.obtenerUrlPublica(CARPETA_PERFILES, nombre);
    }

    /**
     * Carga una foto de perfil como {@code Resource} (usado por {@code PublicFileController}
     * como fallback cuando el bucket de Object Storage es privado y no hay URL pública).
     */
    public Resource cargarFotoComoResource(String nombre) throws IOException {
        return storageService.cargar(CARPETA_PERFILES, nombre);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Comprobante de pago
    // ═════════════════════════════════════════════════════════════════

    /**
     * Guarda el comprobante de pago y devuelve el nombre generado.
     * Nombre seguro: pago_{pagoId}_{UUID}.{ext}
     * Se organiza por quiniela: comprobantes/{nombreQuiniela}/pago_{pagoId}_{UUID}.{ext}
     *
     * Validaciones:
     *  - Extensión: jpg, jpeg, png, pdf
     *  - MIME type: image/jpeg, image/png, application/pdf (application/octet-stream permitido)
     *  - Tamaño máximo: 5 MB
     */
    public String guardarComprobante(MultipartFile file, Long pagoId, String nombreQuiniela) throws IOException {
        validarComprobante(file);
        String nombre = generarNombreComprobante(pagoId, file.getOriginalFilename());
        String carpetaConQuiniela = construirCarpetaComprobantes(nombreQuiniela);
        storageService.guardar(carpetaConQuiniela, nombre, file);
        return nombre;
    }

    /**
     * Sobrecarga para compatibilidad con código existente (sin organización por quiniela).
     * @deprecated Usar guardarComprobante(file, pagoId, nombreQuiniela) en su lugar.
     */
    @Deprecated
    public String guardarComprobante(MultipartFile file, Long pagoId) throws IOException {
        validarComprobante(file);
        String nombre = generarNombreComprobante(pagoId, file.getOriginalFilename());
        storageService.guardar(CARPETA_COMPROBANTES, nombre, file);
        return nombre;
    }

    /**
     * Guarda el comprobante de pago de un premio (transferencia realizada por el
     * administrador a un ganador) y devuelve el nombre generado.
     * Nombre seguro: premio_{ganadorId}_{UUID}.{ext}
     * Se organiza por quiniela: comprobantes/{nombreQuiniela}/premio_{ganadorId}_{UUID}.{ext}
     * Reutiliza las mismas validaciones y estructura que los comprobantes de pago.
     */
    public String guardarComprobantePremio(MultipartFile file, Long ganadorId, String nombreQuiniela) throws IOException {
        validarComprobante(file);
        String nombre = generarNombreComprobantePremio(ganadorId, file.getOriginalFilename());
        String carpetaConQuiniela = construirCarpetaComprobantes(nombreQuiniela);
        storageService.guardar(carpetaConQuiniela, nombre, file);
        return nombre;
    }

    /**
     * Guarda un comprobante adicional (visible para otros jugadores) y devuelve el nombre generado.
     * Nombre seguro: premio_otros_{ganadorId}_{UUID}.{ext}
     * Se organiza por quiniela: comprobantes/{nombreQuiniela}/premio_otros_{ganadorId}_{UUID}.{ext}
     */
    public String guardarComprobantePremioOtros(MultipartFile file, Long ganadorId, String nombreQuiniela) throws IOException {
        validarComprobante(file);
        String nombre = generarNombreComprobantePremioOtros(ganadorId, file.getOriginalFilename());
        String carpetaConQuiniela = construirCarpetaComprobantes(nombreQuiniela);
        storageService.guardar(carpetaConQuiniela, nombre, file);
        return nombre;
    }

    /**
     * Sobrecarga para compatibilidad con código existente (sin organización por quiniela).
     * @deprecated Usar guardarComprobantePremio(file, ganadorId, nombreQuiniela) en su lugar.
     */
    @Deprecated
    public String guardarComprobantePremio(MultipartFile file, Long ganadorId) throws IOException {
        validarComprobante(file);
        String nombre = generarNombreComprobantePremio(ganadorId, file.getOriginalFilename());
        storageService.guardar(CARPETA_COMPROBANTES, nombre, file);
        return nombre;
    }

    /**
     * Carga un comprobante como {@code Resource} para enviarlo al cliente (solo admin).
     *
     * @throws IOException si el archivo no existe o no es legible
     */
    public Resource cargarComprobante(String nombre) throws IOException {
        return storageService.cargar(CARPETA_COMPROBANTES, nombre);
    }

    /**
     * Carga un comprobante de una quiniela específica como {@code Resource}.
     *
     * @throws IOException si el archivo no existe o no es legible
     */
    public Resource cargarComprobante(String nombre, String nombreQuiniela) throws IOException {
        String carpetaConQuiniela = construirCarpetaComprobantes(nombreQuiniela);
        return storageService.cargar(carpetaConQuiniela, nombre);
    }

    /** Elimina un comprobante del almacenamiento (silencioso si no existe). */
    public void eliminarComprobante(String nombre) {
        storageService.eliminar(CARPETA_COMPROBANTES, nombre);
    }

    /**
     * Elimina un comprobante de una quiniela específica (silencioso si no existe).
     */
    public void eliminarComprobante(String nombre, String nombreQuiniela) {
        String carpetaConQuiniela = construirCarpetaComprobantes(nombreQuiniela);
        storageService.eliminar(carpetaConQuiniela, nombre);
    }

    /**
     * Detecta el Content-Type HTTP a partir del nombre del archivo guardado.
     * Usado para construir la cabecera Content-Type al servir el comprobante.
     */
    public String detectarContentType(String nombre) {
        if (nombre == null) return "application/octet-stream";
        String ext = obtenerExtension(nombre);
        return switch (ext) {
            case "pdf"  -> "application/pdf";
            case "png"  -> "image/png";
            default     -> "image/jpeg";
        };
    }

    // ─── Privados ─────────────────────────────────────────────────

    /**
     * Construye el path completo de la carpeta de comprobantes para una quiniela.
     * Sanitiza el nombre de la quiniela para evitar inyección de caracteres especiales.
     */
    private String construirCarpetaComprobantes(String nombreQuiniela) {
        if (nombreQuiniela == null || nombreQuiniela.isBlank()) {
            return CARPETA_COMPROBANTES;
        }
        String sanitizado = sanitizarNombreQuiniela(nombreQuiniela);
        return CARPETA_COMPROBANTES + "/" + sanitizado;
    }

    /**
     * Sanitiza el nombre de la quiniela para uso como parte de un path de archivo.
     * Elimina caracteres especiales, barras, puntos y espacios múltiples.
     * Reemplaza espacios simples con guiones bajos.
     */
    private String sanitizarNombreQuiniela(String nombre) {
        return nombre
                // Reemplazar espacios múltiples con uno solo
                .replaceAll("\\s+", " ")
                // Eliminar caracteres especiales peligrosos (slashes, backslashes, puntos, etc.)
                .replaceAll("[/\\\\:*?\"<>|.]+", "")
                // Reemplazar espacios simples con guiones bajos
                .replaceAll("\\s+", "_")
                // Convertir a minúsculas
                .toLowerCase()
                // Limitar a 100 caracteres
                .substring(0, Math.min(100, nombre.length()));
    }

    // ─── Validación privada ────────────────────────────────────────


    private void validarFoto(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("El archivo no puede estar vacio.");

        if (file.getSize() > MAX_FOTO)
            throw new IllegalArgumentException("La foto no puede superar los 2 MB.");

        // 1. Validar extensión declarada
        String ext = obtenerExtension(file.getOriginalFilename());
        if (!EXT_FOTO.contains(ext))
            throw new IllegalArgumentException(
                    "Extension no permitida (" + ext + "). Use: jpg, jpeg, png, webp.");

        // 2. Detectar MIME real mediante Apache Tika (magic bytes)
        String mimeReal;
        try (InputStream is = file.getInputStream()) {
            mimeReal = tika.detect(is, file.getOriginalFilename());
        }

        if (!MIME_FOTO.contains(mimeReal))
            throw new IllegalArgumentException(
                    "El contenido real del archivo no corresponde a una imagen valida " +
                    "(tipo detectado: " + mimeReal + "). Solo se aceptan JPEG, PNG y WebP.");

        // 3. Coherencia entre extensión y MIME detectado
        String extNorm      = ext.equals("jpeg") ? "jpg" : ext;          // jpeg→jpg
        String mimeEsperado = switch (extNorm) {
            case "jpg"  -> "image/jpeg";
            case "png"  -> "image/png";
            case "webp" -> "image/webp";
            default     -> null;
        };

        if (mimeEsperado != null && !mimeReal.equals(mimeEsperado))
            throw new IllegalArgumentException(
                    "El contenido del archivo no coincide con la extension '" + ext + "' " +
                    "(tipo detectado: " + mimeReal + ").");
    }

    private void validarComprobante(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("El comprobante no puede estar vacio.");

        if (file.getSize() > MAX_COMPROBANTE)
            throw new IllegalArgumentException("El comprobante no puede superar los 5 MB.");

        // 1. Validar extensión declarada en el nombre del archivo
        String ext = obtenerExtension(file.getOriginalFilename());
        if (!EXT_COMPROBANTE.contains(ext))
            throw new IllegalArgumentException(
                    "Extension no permitida (" + ext + "). Use: jpg, jpeg, png, pdf.");

        // 2. Detectar MIME real mediante Apache Tika (magic bytes)
        //    Esto previene ataques donde se cambia la extensión del archivo.
        String mimeReal;
        try (InputStream is = file.getInputStream()) {
            mimeReal = tika.detect(is, file.getOriginalFilename());
        }

        if (!MIME_COMPROBANTE.contains(mimeReal)) {
            throw new IllegalArgumentException(
                    "El contenido real del archivo no es permitido " +
                    "(tipo detectado: " + mimeReal + "). " +
                    "Solo se aceptan JPEG, PNG y PDF.");
        }

        // 3. Coherencia entre extensión y MIME detectado
        //    Evita por ejemplo un PDF con extensión .jpg
        String mimeEsperado = MIME_A_EXT.entrySet().stream()
                .filter(e -> e.getValue().equals(ext.equals("jpeg") ? "jpg" : ext))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (mimeEsperado != null && !mimeReal.equals(mimeEsperado)) {
            throw new IllegalArgumentException(
                    "El contenido del archivo no coincide con la extension '" + ext + "' " +
                    "(tipo detectado: " + mimeReal + ").");
        }
    }

    private String generarNombreFoto(Long userId, String originalFilename) {
        String ext  = obtenerExtension(originalFilename);
        // Normalizar: .jpeg → .jpg
        if (ext.equals("jpeg")) ext = "jpg";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "profile_" + userId + "_" + uuid + "." + ext;
    }

    private String generarNombreComprobante(Long pagoId, String originalFilename) {
        String ext  = obtenerExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "pago_" + pagoId + "_" + uuid + "." + ext;
    }

    private String generarNombreComprobantePremio(Long ganadorId, String originalFilename) {
        String ext  = obtenerExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "premio_" + ganadorId + "_" + uuid + "." + ext;
    }

    private String generarNombreComprobantePremioOtros(Long ganadorId, String originalFilename) {
        String ext  = obtenerExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "premio_otros_" + ganadorId + "_" + uuid + "." + ext;
    }


    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
