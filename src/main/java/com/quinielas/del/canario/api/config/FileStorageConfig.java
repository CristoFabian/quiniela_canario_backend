package com.quinielas.del.canario.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Expone el directorio físico de fotos de perfil como recurso estático HTTP.
 *
 * Directorio físico : {app.storage.local.base-dir}/perfiles/
 * URL pública       : GET /perfiles/{nombre-archivo}
 *
 * Angular accede a la foto con:
 *   http://localhost:8080/perfiles/user1_20260402153045.png
 *
 * Solo se activa cuando el almacenamiento es local ({@code app.storage.provider=local}
 * o si no se define). En producción con Oracle Object Storage
 * ({@code app.storage.provider=oci}) las fotos se sirven mediante el
 * controlador {@code PublicFileController}, que redirige a la URL pública del bucket.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class FileStorageConfig implements WebMvcConfigurer {

    @Value("${app.storage.local.base-dir:uploads}")
    private String baseDir;

    @Value("${app.upload.url-prefix:/perfiles}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Solo fotos de perfil son accesibles públicamente vía URL estática.
        // Los comprobantes de pago se sirven exclusivamente a través del endpoint
        // protegido GET /api/admin/pagos/{id}/comprobante (nunca por URL directa).
        Path perfilesPath = Paths.get(baseDir, "perfiles").toAbsolutePath().normalize();
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + perfilesPath + "/");
    }
}



