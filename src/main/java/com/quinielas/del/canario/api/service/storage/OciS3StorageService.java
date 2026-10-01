package com.quinielas.del.canario.api.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Almacenamiento en Oracle Cloud Infrastructure (OCI) Object Storage, usando
 * su API compatible con S3. Se activa con {@code app.storage.provider=oci}.
 *
 * Configuración necesaria (ver application.properties / variables de entorno):
 *   app.storage.oci.endpoint          → https://<namespace>.compat.objectstorage.<region>.oraclecloud.com
 *   app.storage.oci.region            → p.ej. us-ashburn-1 (valor libre, S3Client lo requiere pero OCI lo ignora)
 *   app.storage.oci.access-key        → Customer Secret Key (Access Key) generada en la consola OCI
 *   app.storage.oci.secret-key        → Customer Secret Key (Secret Key)
 *   app.storage.oci.bucket            → nombre del bucket (uno solo; se usan prefijos "perfiles/" y "comprobantes/")
 *   app.storage.oci.public-url-base   → URL base pública del bucket (solo si es público), p.ej.
 *                                        https://<namespace>.objectstorage.<region>.oraclecloud.com/n/<namespace>/b/<bucket>/o/
 *
 * Notas:
 *  - Las credenciales son "Customer Secret Keys" (Identity → Users → Customer Secret Keys
 *    en la consola de OCI), NO las API Signing Keys usadas por el SDK nativo de OCI.
 *  - Para exponer fotos de perfil públicamente sin credenciales, el bucket "perfiles"
 *    debe tener visibilidad "Public" o generarse Pre-Authenticated Requests (PAR).
 *  - Los comprobantes se guardan en el mismo bucket bajo el prefijo "comprobantes/"
 *    y NUNCA se exponen vía obtenerUrlPublica (siempre null), igual que en local.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "oci")
public class OciS3StorageService implements StorageService {

    @Value("${app.storage.oci.endpoint}")
    private String endpoint;

    @Value("${app.storage.oci.region:us-ashburn-1}")
    private String region;

    @Value("${app.storage.oci.access-key}")
    private String accessKey;

    @Value("${app.storage.oci.secret-key}")
    private String secretKey;

    @Value("${app.storage.oci.bucket}")
    private String bucket;

    /** Solo se usa para exponer URLs públicas (carpeta "perfiles"); null/blank si no aplica. */
    @Value("${app.storage.oci.public-url-base:}")
    private String publicUrlBase;

    private S3Client s3;

    @PostConstruct
    void init() {
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                // OCI (como muchos proveedores S3-compatibles) requiere path-style:
                // https://endpoint/bucket/key  en lugar de https://bucket.endpoint/key
                .forcePathStyle(true)
                .build();
    }

    @PreDestroy
    void close() {
        if (s3 != null) s3.close();
    }

    @Override
    public void guardar(String carpeta, String nombreArchivo, MultipartFile file) throws IOException {
        String key = objectKey(carpeta, nombreArchivo);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
        s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    @Override
    public Resource cargar(String carpeta, String nombreArchivo) throws IOException {
        String key = objectKey(carpeta, nombreArchivo);
        try {
            var responseStream = s3.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return new InputStreamResource(responseStream);
        } catch (NoSuchKeyException e) {
            throw new IOException("Archivo no encontrado en Object Storage: " + key, e);
        }
    }

    @Override
    public void eliminar(String carpeta, String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey(carpeta, nombreArchivo))
                    .build());
        } catch (Exception ignored) {
            // Silencioso: igual que en almacenamiento local, no debe romper el flujo de negocio.
        }
    }

    @Override
    public String obtenerUrlPublica(String carpeta, String nombreArchivo) {
        // Solo se exponen públicamente las fotos de perfil; los comprobantes son privados.
        if (!"perfiles".equals(carpeta) || publicUrlBase == null || publicUrlBase.isBlank()) {
            return null;
        }
        String base = publicUrlBase.endsWith("/") ? publicUrlBase : publicUrlBase + "/";
        return base + URLEncoder.encode(nombreArchivo, StandardCharsets.UTF_8);
    }

    private String objectKey(String carpeta, String nombreArchivo) {
        return carpeta + "/" + nombreArchivo;
    }
}

