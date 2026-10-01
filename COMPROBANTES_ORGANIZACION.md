# Reorganización de Comprobantes de Pago

## Descripción del Cambio

Los comprobantes de pago han sido reorganizados en una estructura jerárquica mejorada para mejor gestión y consulta.

### Estructura Antigua ❌
```
uploads/
└── comprobantes/
    ├── pago_1_2deb49097d664734b7224cbb85614b36.png
    ├── pago_2_d2322f445d3a44d88737a80351153bd1.jpg
    ├── pago_12_4401ca529619422fb01c007953757e64.png
    ├── premio_5_316c0738a2834f9e923d6b8cdd9c2f2b.jpg
    └── ...
```

### Estructura Nueva ✅
```
uploads/
└── comprobantes/
    ├── quiniela_1_name/
    │   ├── pago_1_2deb49097d664734b7224cbb85614b36.png
    │   ├── pago_2_d2322f445d3a44d88737a80351153bd1.jpg
    │   ├── premio_5_316c0738a2834f9e923d6b8cdd9c2f2b.jpg
    │   └── ...
    ├── quiniela_2_name/
    │   ├── pago_12_4401ca529619422fb01c007953757e64.png
    │   └── ...
    └── ...
```

## Beneficios

1. **Mejor Organización**: Los comprobantes se agrupan por quiniela
2. **Fácil Auditoría**: Es sencillo revisar todos los comprobantes de una quiniela
3. **Escalabilidad**: Soporta fácilmente un gran número de quinielas
4. **Mantenimiento**: Facilita la limpieza y gestión de archivos antiguos
5. **Seguridad**: Mejor control de acceso por quiniela

## Cambios en el Código

### FileStorageService

#### Métodos Nuevos
- `guardarComprobante(file, pagoId, nombreQuiniela)` - Guardar en carpeta organizada
- `guardarComprobantePremio(file, ganadorId, nombreQuiniela)` - Guardar premio en carpeta organizada
- `cargarComprobante(nombre, nombreQuiniela)` - Cargar desde carpeta organizada
- `eliminarComprobante(nombre, nombreQuiniela)` - Eliminar de carpeta organizada

#### Métodos Deprecados (Aún Funcionales)
- `guardarComprobante(file, pagoId)` - Usa carpeta raíz (compatibilidad atrás)
- `guardarComprobantePremio(file, ganadorId)` - Usa carpeta raíz (compatibilidad atrás)

### PagoService

Actualizado para pasar el nombre de la quiniela al guardar/cargar/eliminar comprobantes:
- `crearPago()` - Obtiene nombre de quiniela y lo pasa al guardar
- `reintentarPago()` - Obtiene nombre de quiniela y lo pasa al guardar
- `subirComprobante()` - Obtiene nombre de quiniela y lo pasa al guardar/eliminar
- `subirComprobanteAdmin()` - Obtiene nombre de quiniela y lo pasa al guardar/eliminar
- `obtenerComprobante()` - Intenta cargar desde carpeta organizada; fallback a carpeta raíz

### PremioService

Actualizado para pasar el nombre de la quiniela al guardar/cargar/eliminar comprobantes:
- `subirComprobantePremio()` - Obtiene nombre de quiniela y lo pasa al guardar/eliminar
- `cargarComprobanteOException()` - Intenta cargar desde carpeta organizada; fallback a carpeta raíz

## Sanitización de Nombres

Los nombres de quinielas se sanitizan automáticamente para que sean válidos como nombres de carpeta:
- Se eliminan caracteres especiales: `/`, `\`, `:`, `*`, `?`, `"`, `<`, `>`, `|`, `.`
- Se convierten espacios múltiples a un solo espacio
- Se reemplazan espacios por guiones bajos `_`
- Se convierte a minúsculas
- Se limita a 100 caracteres

Ejemplo:
```
"Quiniela Premier League - 2026" → "quiniela_premier_league__2026"
"Cup/Championship" → "cupchamponship"
```

## Compatibilidad Hacia Atrás

El sistema es totalmente compatible con archivos antiguos:
- Nuevos comprobantes se guardan en la estructura organizada
- Al cargar un comprobante, primero intenta buscar en la carpeta organizada por quiniela
- Si no lo encuentra, busca en la carpeta raíz de comprobantes (archivos antiguos)
- El fallback asegura que los comprobantes antiguos sigan siendo accesibles

## Migración de Archivos Existentes

Se proporciona un script de migración (ver `MIGRATION_COMPROBANTES.md`) para reorganizar los archivos existentes. Este script es **opcional** pero recomendado para:
- Aprovechar la nueva estructura desde el primer momento
- Facilitar futuras auditorías y mantenimiento
- Liberar espacio (eliminar comprobantes huérfanos)

## Storage Backends

La reorganización funciona automáticamente con ambos backends:

### Local Storage (Predeterminado)
```
{base-dir}/comprobantes/{nombre_quiniela}/{nombre_archivo}
```

### OCI Object Storage
```
bucket/comprobantes/{nombre_quiniela}/{nombre_archivo}
```

Ambos backends manejan la creación automática de carpetas según sea necesario.

## FAQ

**P: ¿Se pierden los comprobantes antiguos?**  
R: No, el sistema mantiene compatibilidad hacia atrás. Los archivos nuevos irán a las carpetas organizadas, y los antiguos seguirán siendo accesibles desde la carpeta raíz.

**P: ¿Qué pasa si una quiniela tiene un nombre muy largo?**  
R: El nombre se limita a 100 caracteres y se sanitiza. Si dos quinielas generan el mismo nombre sanitizado, compartirán la misma carpeta (caso muy raro).

**P: ¿Debo ejecutar la migración?**  
R: No es obligatorio, pero es recomendado para aprovechar la nueva estructura al máximo.

**P: ¿Afecta esto al rendimiento?**  
R: No hay impacto negativo. El sistema maneja dinámicamente la creación de carpetas tanto en almacenamiento local como en Object Storage.

