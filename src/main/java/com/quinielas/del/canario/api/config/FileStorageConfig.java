package com.quinielas.del.canario.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Expone el directorio físico de fotos de perfil como recurso estático HTTP.
 *
 * Directorio físico : {proyecto}/uploads/perfiles/
 * URL pública       : GET /perfiles/{nombre-archivo}
 *
 * Angular accede a la foto con:
 *   http://localhost:8080/perfiles/user1_20260402153045.png
 */
@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Solo fotos de perfil son accesibles públicamente vía URL estática.
        // Los comprobantes de pago se sirven exclusivamente a través del endpoint
        // protegido GET /api/admin/pagos/{id}/comprobante (nunca por URL directa).
        Path perfilesPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + perfilesPath + "/");
    }
}
