package com.quinielas.del.canario.api.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Almacenamiento en el disco local del servidor. Es el backend por defecto
 * (adecuado para desarrollo o un único servidor con disco persistente).
 *
 * Estructura física:
 *   {app.storage.local.base-dir}/perfiles/...
 *   {app.storage.local.base-dir}/comprobantes/...
 *
 * Se activa cuando {@code app.storage.provider=local} o cuando la propiedad
 * no está definida (valor por defecto).
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements StorageService {

    @Value("${app.storage.local.base-dir:uploads}")
    private String baseDir;

    @Override
    public void guardar(String carpeta, String nombreArchivo, MultipartFile file) throws IOException {
        Path destino = resolverDirectorio(carpeta);
        Files.copy(file.getInputStream(), destino.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Resource cargar(String carpeta, String nombreArchivo) throws IOException {
        Path ruta = resolverDirectorio(carpeta).resolve(nombreArchivo);
        Resource resource = new UrlResource(ruta.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Archivo no encontrado: " + carpeta + "/" + nombreArchivo);
        }
        return resource;
    }

    @Override
    public void eliminar(String carpeta, String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return;
        try {
            Files.deleteIfExists(resolverDirectorio(carpeta).resolve(nombreArchivo));
        } catch (IOException ignored) {
            // Silencioso: si no se puede borrar, no debe romper el flujo de negocio.
        }
    }

    @Override
    public String obtenerUrlPublica(String carpeta, String nombreArchivo) {
        // Las URLs públicas locales las resuelve FileStorageConfig como recurso
        // estático (/perfiles/**); aquí no se expone una URL absoluta.
        return null;
    }

    private Path resolverDirectorio(String carpeta) throws IOException {
        Path path = Paths.get(baseDir, carpeta).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }
}

