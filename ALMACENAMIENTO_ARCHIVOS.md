# Almacenamiento de Archivos — Quinielas del Canario

Este documento describe la arquitectura de almacenamiento de archivos (fotos de perfil
y comprobantes de pago/premio) del backend, tras la refactorización que introduce una
capa de abstracción (`StorageService`) para soportar tanto **disco local** (desarrollo)
como **Oracle Object Storage** (producción, vía API S3-compatible).

---

## 1. Objetivo de la refactorización

Antes, `FileStorageService` escribía y leía directamente del filesystem con
`java.nio.file.Files`, acoplando toda la lógica de negocio (validaciones, nombres de
archivo) al almacenamiento en disco. Esto es un problema en producción cuando:

- Se despliega en contenedores/PaaS con filesystem **efímero** (los archivos se pierden
  en cada redeploy).
- Hay **múltiples instancias** de la app detrás de un balanceador (un archivo subido en
  una instancia no es visible en las demás).

La solución fue introducir una interfaz `StorageService` que abstrae el backend físico,
de modo que `FileStorageService` (validaciones + nombres de archivo) no cambia sin
importar dónde se guarden los archivos.

## 2. Arquitectura de capas

```
Controller (PlayerController, PagoController, PremioController)
        │
        ▼
FileStorageService   ← validaciones (extensión, tamaño, MIME real con Apache Tika)
        │               y generación de nombres de archivo seguros (UUID)
        ▼
StorageService (interfaz)
   ├── LocalFileStorageService   (app.storage.provider=local, por defecto)
   └── OciS3StorageService       (app.storage.provider=oci)
```

- **`StorageService`** (`service/storage/StorageService.java`): interfaz con 4 métodos,
  todos parametrizados por una "carpeta" lógica (`"perfiles"` o `"comprobantes"`):
  - `guardar(carpeta, nombreArchivo, MultipartFile)`
  - `cargar(carpeta, nombreArchivo)` → `Resource` para descarga/streaming
  - `eliminar(carpeta, nombreArchivo)` → silencioso si no existe
  - `obtenerUrlPublica(carpeta, nombreArchivo)` → URL absoluta si el backend la soporta
    (o `null` si debe servirse de otra forma, p. ej. recurso estático o siempre `null`
    para archivos privados como los comprobantes)

- **`LocalFileStorageService`**: implementación por defecto. Guarda en
  `{app.storage.local.base-dir}/{carpeta}/...` (por defecto `uploads/perfiles/` y
  `uploads/comprobantes/`, exactamente la misma estructura física que antes de la
  refactorización — no requiere migrar archivos existentes).

- **`OciS3StorageService`**: usa el SDK **AWS S3 v2** (`software.amazon.awssdk:s3`)
  apuntando al endpoint S3-compatible de Oracle Object Storage. Los objetos se guardan
  en un único bucket con el prefijo `carpeta/nombreArchivo` (p. ej.
  `perfiles/profile_3_abc123.png`, `comprobantes/pago_45_def456.pdf`).

- **`FileStorageService`**: sin cambios de comportamiento de negocio. Sigue exponiendo
  los mismos métodos (`guardarFoto`, `guardarComprobante`, `guardarComprobantePremio`,
  `cargarComprobante`, `eliminarFoto`, `eliminarComprobante`, `detectarContentType`),
  pero ahora delega el guardado físico en el `StorageService` inyectado. También agrega:
  - `obtenerUrlPublicaFoto(nombre)` → usado por `PublicFileController` en modo OCI.
  - `cargarFotoComoResource(nombre)` → fallback de streaming si el bucket es privado.

## 3. Cómo se sirven las fotos de perfil públicamente

La URL pública `GET /perfiles/{nombre}` se mantiene igual para el frontend en ambos
modos, pero el mecanismo interno cambia según el backend activo:

| Provider | Mecanismo |
|---|---|
| `local` | `FileStorageConfig` (WebMvcConfigurer) registra `/perfiles/**` como recurso estático de Spring, apuntando al directorio físico `uploads/perfiles/`. |
| `oci`   | `PublicFileController` intercepta `GET /perfiles/{nombre}`: si hay `app.storage.oci.public-url-base` configurada, responde con un **redirect 302** a la URL del bucket; si no, hace **streaming** del objeto a través del backend (bucket privado). |

Ambos son mutuamente excluyentes vía `@ConditionalOnProperty(prefix = "app.storage",
name = "provider", ...)`, así que solo uno de los dos beans se registra según
`app.storage.provider`. La ruta `/perfiles/**` ya está declarada como pública en
`SecurityConfig` independientemente del backend.

Los **comprobantes** (pago y premio) nunca se exponen por URL pública en ningún modo:
`obtenerUrlPublica("comprobantes", ...)` siempre retorna `null`, y solo se acceden vía
endpoints protegidos (`GET /api/admin/pagos/{id}/comprobante`, etc.) que hacen streaming
autenticado a través de `cargarComprobante()`.

## 4. Configuración

### 4.1 Desarrollo / almacenamiento local (por defecto)

```properties
app.storage.provider=local
app.storage.local.base-dir=uploads
app.upload.url-prefix=/perfiles
```

No requiere ninguna otra configuración ni dependencia externa.

### 4.2 Producción / Oracle Object Storage

```properties
app.storage.provider=oci
app.storage.oci.endpoint=https://<namespace>.compat.objectstorage.<region>.oraclecloud.com
app.storage.oci.region=us-ashburn-1
app.storage.oci.access-key=${OCI_ACCESS_KEY}
app.storage.oci.secret-key=${OCI_SECRET_KEY}
app.storage.oci.bucket=quinielas-uploads

# Solo si el bucket/prefijo "perfiles/" es publico:
app.storage.oci.public-url-base=https://<namespace>.objectstorage.<region>.oraclecloud.com/n/<namespace>/b/quinielas-uploads/o/perfiles/
```

**Pasos para habilitarlo:**

1. Crear un bucket en OCI Object Storage (uno solo; `perfiles/` y `comprobantes/` son
   prefijos dentro del mismo bucket).
2. Generar una **Customer Secret Key** (Identity → Users → Customer Secret Keys en la
   consola de OCI). **No** son las API Signing Keys del SDK nativo de OCI; son
   credenciales estilo AWS Access Key/Secret Key usadas por la API S3-compatible.
3. Definir las propiedades anteriores como **variables de entorno** en el ambiente de
   producción (nunca hardcodeadas ni versionadas).
4. Si se desea que las fotos de perfil carguen directo desde el bucket (sin pasar por
   el backend), marcar el bucket/prefijo `perfiles/` como público y definir
   `app.storage.oci.public-url-base`. Si se deja vacío, el backend hace streaming.
5. No se requiere ningún cambio de código adicional: controladores y servicios
   (`PlayerService`, `PagoService`, `PremioService`) siguen usando `FileStorageService`
   exactamente igual, sin conocer el backend concreto.

## 5. Dependencias añadidas

```xml
<!-- pom.xml -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.29.29</version>
</dependency>
```

Se eligió el SDK de AWS S3 (en vez del SDK nativo de OCI) porque Oracle Object Storage
expone una API **compatible con S3**, lo que evita añadir una dependencia adicional
específica de OCI y facilita portar el mismo código a otros proveedores S3-compatibles
si fuera necesario en el futuro (MinIO, Backblaze B2, etc.) simplemente cambiando el
`endpoint`.

## 6. Archivos relevantes

| Archivo | Rol |
|---|---|
| `service/storage/StorageService.java` | Interfaz de abstracción del backend de almacenamiento. |
| `service/storage/LocalFileStorageService.java` | Implementación en disco local (por defecto). |
| `service/storage/OciS3StorageService.java` | Implementación Oracle Object Storage (API S3-compatible). |
| `service/FileStorageService.java` | Validaciones (Tika, tamaño, extensión) y nombres de archivo; delega en `StorageService`. |
| `config/FileStorageConfig.java` | Expone `/perfiles/**` como recurso estático (solo modo `local`). |
| `controller/PublicFileController.java` | Sirve `/perfiles/{nombre}` en modo `oci` (redirect o streaming). |
| `application.properties` | Propiedades `app.storage.*`. |

## 7. Compatibilidad y migración

- La estructura de carpetas locales (`uploads/perfiles/`, `uploads/comprobantes/`) **no
  cambió**, por lo que los archivos ya subidos siguen siendo válidos sin migración si se
  continúa en modo `local`.
- Para migrar de `local` a `oci` en un ambiente ya en producción, es necesario subir
  manualmente (o con un script) los archivos existentes de `uploads/perfiles/` y
  `uploads/comprobantes/` al bucket, respetando los mismos nombres de archivo (que ya
  están referenciados en la base de datos: `UserProfile.foto`, `Pago.comprobanteUrl`,
  `GanadorQuiniela.comprobantePremioUrl`).

