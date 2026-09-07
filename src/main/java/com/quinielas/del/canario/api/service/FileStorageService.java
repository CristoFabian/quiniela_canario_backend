package com.quinielas.del.canario.api.service;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio responsable de guardar y eliminar archivos en disco.
 *  - Fotos de perfil : uploads/perfiles    (jpg/jpeg/png/webp, máx 2 MB)
 *  - Comprobantes    : uploads/comprobantes (jpg/jpeg/png/pdf, máx 5 MB, sin URL pública)
 */
@Service
public class FileStorageService {

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

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.dir.comprobantes}")
    private String comprobanteDir;

    private final Tika tika;

    public FileStorageService(Tika tika) {
        this.tika = tika;
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
        Path destino = resolverDirectorio(uploadDir);
        String nombre = generarNombreFoto(userId, file.getOriginalFilename());
        Files.copy(file.getInputStream(), destino.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        return nombre;
    }

    /** Elimina una foto de perfil del disco (silencioso si no existe). */
    public void eliminarFoto(String nombre) {
        eliminarArchivo(uploadDir, nombre);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Comprobante de pago
    // ═════════════════════════════════════════════════════════════════

    /**
     * Guarda el comprobante de pago y devuelve el nombre generado.
     * Nombre seguro: pago_{pagoId}_{UUID}.{ext}
     *
     * Validaciones:
     *  - Extensión: jpg, jpeg, png, pdf
     *  - MIME type: image/jpeg, image/png, application/pdf (application/octet-stream permitido)
     *  - Tamaño máximo: 5 MB
     */
    public String guardarComprobante(MultipartFile file, Long pagoId) throws IOException {
        validarComprobante(file);
        Path destino = resolverDirectorio(comprobanteDir);
        String nombre = generarNombreComprobante(pagoId, file.getOriginalFilename());
        Files.copy(file.getInputStream(), destino.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        return nombre;
    }

    /**
     * Carga un comprobante como {@code Resource} para enviarlo al cliente (solo admin).
     *
     * @throws IOException si el archivo no existe o no es legible
     */
    public Resource cargarComprobante(String nombre) throws IOException {
        Path ruta = Paths.get(comprobanteDir).toAbsolutePath().normalize().resolve(nombre);
        Resource resource = new UrlResource(ruta.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Comprobante no encontrado: " + nombre);
        }
        return resource;
    }

    /** Elimina un comprobante del disco (silencioso si no existe). */
    public void eliminarComprobante(String nombre) {
        eliminarArchivo(comprobanteDir, nombre);
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

    // ─── Privados ─────────────────────────────────────────────────────

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

    private Path resolverDirectorio(String dir) throws IOException {
        Path path = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }

    private void eliminarArchivo(String dir, String nombre) {
        if (nombre == null || nombre.isBlank()) return;
        try {
            Files.deleteIfExists(
                Paths.get(dir).toAbsolutePath().normalize().resolve(nombre));
        } catch (IOException ignored) {}
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
