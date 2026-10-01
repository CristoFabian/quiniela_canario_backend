# Guía de Migración de Comprobantes

## Introducción

Esta guía describe cómo migrar los comprobantes de pago existentes a la nueva estructura jerárquica organizada por quiniela.

## Estado Actual

Los comprobantes están actualmente en la carpeta raíz:
```
uploads/comprobantes/
├── pago_1_*.png
├── pago_2_*.jpg
├── pago_12_*.png
├── premio_5_*.jpg
└── ...
```

## Estado Deseado

Los comprobantes estarán organizados por quiniela:
```
uploads/comprobantes/
├── quiniela_1_name/
│   ├── pago_1_*.png
│   ├── pago_2_*.jpg
│   └── premio_5_*.jpg
├── quiniela_2_name/
│   ├── pago_12_*.png
│   └── ...
└── ...
```

## Pasos de Migración

### Opción 1: Automatizada (Recomendada)

#### Windows (PowerShell)

```powershell
# Modo simulación (recomendado primero)
.\migrate-comprobantes.ps1 -DryRun $true

# Aplicar migración
.\migrate-comprobantes.ps1 -DryRun $false
```

#### Linux/Mac (Bash)

```bash
# Modo simulación (recomendado primero)
bash migrate-comprobantes.sh uploads true

# Aplicar migración
bash migrate-comprobantes.sh uploads false
```

### Opción 2: Manual

Si prefieres hacerlo manualmente:

1. **Crear carpetas por quiniela**:
   ```bash
   mkdir -p uploads/comprobantes/quiniela_name_1
   mkdir -p uploads/comprobantes/quiniela_name_2
   # ... una carpeta por cada quiniela
   ```

2. **Mover archivos**:
   ```bash
   # Para cada comprobante, identificar a qué quiniela pertenece
   # y moverlo a la carpeta correspondiente
   mv uploads/comprobantes/pago_1_*.png uploads/comprobantes/quiniela_name_1/
   ```

3. **Verificar**:
   ```bash
   find uploads/comprobantes -type f | wc -l
   # Debe ser igual al número de comprobantes original
   ```

## Consideraciones Importantes

### 1. **Hacer un Backup Primero**

Es altamente recomendable hacer un backup antes de la migración:

```bash
# Linux/Mac
cp -r uploads/comprobantes uploads/comprobantes_backup_$(date +%Y%m%d_%H%M%S)

# Windows (PowerShell)
Copy-Item -Path "uploads\comprobantes" -Destination "uploads\comprobantes_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')" -Recurse
```

### 2. **Modo Simulación**

Siempre ejecute primero en modo simulación (`DryRun=true`) para ver qué cambios se aplicarían:

```bash
# PowerShell
.\migrate-comprobantes.ps1 -DryRun $true

# Bash
bash migrate-comprobantes.sh uploads true
```

El modo simulación mostrará un preview sin realizar cambios reales.

### 3. **Verificar Resultado**

Después de la migración, verificar que:

- No haya archivos en `uploads/comprobantes/` directamente (excepto subcarpetas)
- Cada subcarpeta contenga solo archivos de su quiniela
- El número total de archivos sea igual al original

```bash
# Contar archivos antes
find uploads/comprobantes -maxdepth 1 -type f | wc -l

# Contar archivos después
find uploads/comprobantes -type f | wc -l

# Ambos números deben coincidir
```

### 4. **Sin Tiempo de Inactividad**

El sistema sigue funcionando durante la migración:

- Los comprobantes antiguos en la carpeta raíz seguirán siendo accesibles
- Los comprobantes nuevos se guardarán automáticamente en las carpetas organizadas
- No es necesario detener el servicio

## Compatibilidad

La implementación mantiene compatibilidad total hacia atrás:

```java
// Código antiguo sigue funcionando
fileStorageService.guardarComprobante(file, pagoId);

// Nuevo código con organización por quiniela
fileStorageService.guardarComprobante(file, pagoId, nombreQuiniela);

// Al cargar, intenta primero las nuevas carpetas, luego fallback a raíz
fileStorageService.cargarComprobante(nombre, nombreQuiniela);
```

## Solución de Problemas

### Error: "Directorio no encontrado"

```bash
# Verificar que la ruta existe
ls -la uploads/comprobantes

# Si no existe, crear la estructura base
mkdir -p uploads/comprobantes
```

### Error: "Permiso denegado"

```bash
# Verificar permisos
ls -la uploads/

# Dar permisos si es necesario (Linux/Mac)
chmod -R 755 uploads/
```

### Algunos archivos no se migraron

Posibles razones:

1. Nombres de archivo que no coinciden con el patrón `pago_*` o `premio_*`
2. Archivos sin extensión
3. Problemas de permisos

Revisar el archivo `migration_comprobantes_*.log` para detalles.

## Rollback

Si algo sale mal, puede restaurar desde el backup:

```bash
# Linux/Mac
rm -rf uploads/comprobantes
mv uploads/comprobantes_backup_YYYYMMDD_HHMMSS uploads/comprobantes

# Windows (PowerShell)
Remove-Item -Path "uploads\comprobantes" -Recurse -Force
Move-Item -Path "uploads\comprobantes_backup_*" -Destination "uploads\comprobantes"
```

## Próximos Pasos

Después de la migración:

1. ✅ Verificar que todo funciona correctamente
2. ✅ Hacer pruebas de carga/descarga de comprobantes
3. ✅ Revisar logs de aplicación para errores
4. ✅ Si todo está bien, eliminar el backup después de 2-4 semanas
5. ✅ Documentar en el equipo sobre la nueva estructura

## Preguntas Frecuentes

**P: ¿Se pierden datos?**  
R: No, la migración es 100% segura. Los archivos se copian a un backup antes de mover.

**P: ¿Afecta el rendimiento?**  
R: No, puede haber una leve mejora en la organización de directorios.

**P: ¿Qué pasa con la Base de Datos?**  
R: La BD no necesita cambios, solo cambia la ruta del archivo en el sistema de archivos.

**P: ¿Puedo detener la aplicación durante la migración?**  
R: Recomendado para evitar conflictos, pero no es estrictamente necesario.

**P: ¿Cómo sé que fue exitosa?**  
R: Verifica:
  - Número de archivos es igual antes/después
  - Log `migration_comprobantes_*.log` muestra 0 errores
  - La aplicación sigue funcionando normalmente

## Contacto y Soporte

Si encuentras problemas durante la migración:

1. Revisar el archivo de log generado
2. Consultar la sección "Solución de Problemas"
3. Restaurar desde el backup si es necesario
4. Contactar al equipo de soporte

