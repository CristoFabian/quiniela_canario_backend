package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.service.FileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;

/**
 * Sirve las fotos de perfil de forma pública cuando el almacenamiento activo
 * es Oracle Object Storage ({@code app.storage.provider=oci}).
 *
 * Reemplaza al resource handler estático de {@code FileStorageConfig} (que solo
 * aplica para almacenamiento local), manteniendo la misma URL pública para el
 * frontend: GET /perfiles/{nombre-archivo}
 *
 * Comportamiento:
 *  - Si el bucket es público y hay una URL base configurada
 *    ({@code app.storage.oci.public-url-base}), redirige (302) directamente
 *    al objeto en Object Storage (sin pasar el binario por este servidor).
 *  - Si no hay URL pública configurada (bucket privado), hace streaming del
 *    archivo a través de este servidor.
 *
 * Ruta ya declarada como pública en SecurityConfig ("/perfiles/**").
 */
@RestController
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "oci")
public class PublicFileController {

    private final FileStorageService fileStorageService;

    public PublicFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/perfiles/{nombre}")
    public ResponseEntity<Resource> verFoto(@PathVariable String nombre) throws IOException {
        String urlPublica = fileStorageService.obtenerUrlPublicaFoto(nombre);
        if (urlPublica != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(urlPublica))
                    .build();
        }

        // Bucket privado: se hace streaming del archivo a través del backend.
        Resource resource = fileStorageService.cargarFotoComoResource(nombre);
        String contentType = fileStorageService.detectarContentType(nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}

