# Plan de Pruebas: Reorganización de Comprobantes

## Objetivos

Verificar que la reorganización jerárquica de comprobantes funciona correctamente en todas las operaciones sin afectar la funcionalidad existente.

## Cobertura de Pruebas

### 1. Crear Pago con Comprobante

**Escenario**: Un jugador crea un pago y adjunta un comprobante

**Pasos**:
1. Autenticarse como jugador
2. Crear una jugada en una quiniela
3. Crear un pago con comprobante
4. Verificar que el comprobante se guarda en: `comprobantes/{nombre_quiniela_sanitizado}/pago_{id}_*.ext`

**Resultado esperado**: ✅ Comprobante guardado en la carpeta correcta

**Comando curl de prueba**:
```bash
curl -X POST http://localhost:8080/api/jugador/pagos \
  -H "Authorization: Bearer $TOKEN" \
  -F "jugadaIds=1" \
  -F "monto=100.00" \
  -F "comprobante=@test_receipt.jpg"
```

### 2. Descargar Comprobante (Admin)

**Escenario**: Un admin descarga un comprobante para revisar

**Pasos**:
1. Autenticarse como admin
2. Navegar a un pago con comprobante
3. Descargar el comprobante
4. Verificar que se descarga correctamente

**Resultado esperado**: ✅ Comprobante se descarga sin errores

**Comando curl de prueba**:
```bash
curl -X GET http://localhost:8080/api/admin/pagos/1/comprobante \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -o downloaded_receipt.jpg
```

### 3. Reemplazar Comprobante

**Escenario**: Un jugador reemplaza un comprobante rechazado

**Pasos**:
1. Crear un pago rechazado
2. Subir un nuevo comprobante
3. Verificar que el nuevo comprobante está en la carpeta por quiniela
4. Verificar que el anterior se eliminó

**Resultado esperado**: ✅ Comprobante reemplazado en la ubicación correcta

**Comando curl de prueba**:
```bash
# Primero rechazar el pago
curl -X PATCH http://localhost:8080/api/admin/pagos/1/validar \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"RECHAZADO","observacion":"Monto incorrecto"}'

# Luego subir nuevo comprobante
curl -X PUT http://localhost:8080/api/jugador/pagos/1/comprobante \
  -H "Authorization: Bearer $TOKEN" \
  -F "comprobante=@new_receipt.jpg"
```

### 4. Admin Sube Comprobante

**Escenario**: Admin sube comprobante para un pago sin comprobante

**Pasos**:
1. Crear un pago sin comprobante (comprobanteWhatsapp=true)
2. Admin sube el comprobante
3. Verificar ubicación en: `comprobantes/{nombre_quiniela_sanitizado}/pago_{id}_*.ext`

**Resultado esperado**: ✅ Comprobante guardado en la carpeta correcta

**Comando curl de prueba**:
```bash
curl -X PUT http://localhost:8080/api/admin/pagos/1/comprobante \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "comprobante=@whatsapp_receipt.jpg" \
  -F "comprobanteWhatsapp=true"
```

### 5. Reintentar Pago

**Escenario**: Jugador reintenta un pago rechazado con nuevo comprobante

**Pasos**:
1. Crear pago rechazado
2. Reintentar con nuevo comprobante
3. Verificar que el nuevo comprobante está en carpeta por quiniela
4. Verificar que el anterior se eliminó

**Resultado esperado**: ✅ Nuevo comprobante en ubicación correcta

**Comando curl de prueba**:
```bash
curl -X POST http://localhost:8080/api/jugador/pagos/1/reintentar \
  -H "Authorization: Bearer $TOKEN" \
  -F "monto=100.00" \
  -F "comprobante=@new_receipt.jpg"
```

### 6. Comprobante de Premio

**Escenario**: Admin sube comprobante de premio a ganador

**Pasos**:
1. Cerrar una quiniela con ganadores
2. Admin sube comprobante de transferencia de premio
3. Verificar ubicación en: `comprobantes/{nombre_quiniela_sanitizado}/premio_{id}_*.ext`
4. Jugador descarga comprobante

**Resultado esperado**: ✅ Comprobante de premio en ubicación correcta

**Comando curl de prueba**:
```bash
# Admin sube comprobante
curl -X POST http://localhost:8080/api/admin/premios/1/comprobante \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "comprobante=@transfer_receipt.jpg"

# Jugador descarga
curl -X GET http://localhost:8080/api/jugador/premios/1/comprobante \
  -H "Authorization: Bearer $TOKEN" \
  -o my_prize.jpg
```

### 7. Compatibilidad Hacia Atrás

**Escenario**: Los comprobantes antiguos en la carpeta raíz siguen siendo accesibles

**Pasos**:
1. Poner un archivo de prueba en `uploads/comprobantes/pago_1_oldfile.jpg`
2. Intentar descargarlo con la API
3. Verificar que se descarga correctamente (fallback de carpeta raíz)

**Resultado esperado**: ✅ Archivos antiguos siguen siendo accesibles

### 8. Sanitización de Nombres

**Escenario**: Nombres de quinielas con caracteres especiales se sanitizan correctamente

**Pruebas de nombre**:
- `"Quiniela Premier League - 2026"` → `"quiniela_premier_league__2026"`
- `"Copa/Championship"` → `"copachampionship"`
- `"Liga:2025*"` → `"liga2025"`
- `"Very Long Name With Many Words..."` → `"very_long_name_with_many_w"` (limitado a 100)

**Verificación**: En el código de `FileStorageService.sanitizarNombreQuiniela()`

### 9. Manejo de Errores

**Escenario 1**: Cargar comprobante que no existe

```bash
curl -X GET http://localhost:8080/api/admin/pagos/999/comprobante \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Resultado esperado**: ✅ Error 404 o mensaje "comprobante no existe"

**Escenario 2**: Archivo corrupto o inválido

```bash
curl -X POST http://localhost:8080/api/jugador/pagos \
  -H "Authorization: Bearer $TOKEN" \
  -F "jugadaIds=1" \
  -F "monto=100.00" \
  -F "comprobante=@virus.exe"
```

**Resultado esperado**: ✅ Error de validación (extensión/MIME no permitido)

### 10. Rendimiento

**Escenario**: Operaciones con múltiples comprobantes

**Prueba**:
1. Crear 100 pagos con comprobantes
2. Descargar 100 comprobantes
3. Medir tiempo de respuesta

**Resultado esperado**: ✅ Sin degradación de rendimiento notable

## Casos de Uso Completos

### Caso 1: Flujo Completo de Pago

```bash
# 1. Crear jugada
POST /api/jugador/quinielas/{id}/jugadas

# 2. Crear pago con comprobante
POST /api/jugador/pagos
  -F "jugadaIds=1"
  -F "monto=100"
  -F "comprobante=@receipt.jpg"

# 3. Admin revisa pago
GET /api/admin/pagos/{id}
GET /api/admin/pagos/{id}/comprobante

# 4. Admin aprueba
PATCH /api/admin/pagos/{id}/validar
  -d '{"estado":"APROBADO"}'

# 5. Verificar comprobante en carpeta correcta
# uploads/comprobantes/{quiniela_name}/pago_{id}_*.jpg
```

### Caso 2: Rechazo y Reintento

```bash
# 1. Admin rechaza pago
PATCH /api/admin/pagos/{id}/validar
  -d '{"estado":"RECHAZADO","observacion":"Monto incorrecto"}'

# 2. Jugador reintenta con nuevo comprobante
POST /api/jugador/pagos/{id}/reintentar
  -F "monto=100"
  -F "comprobante=@new_receipt.jpg"

# 3. Verificar nuevos comprobantes en carpeta correcta
# uploads/comprobantes/{quiniela_name}/pago_{id}_*.jpg
```

### Caso 3: Comprobante por WhatsApp

```bash
# 1. Jugador crea pago sin comprobante
POST /api/jugador/pagos
  -F "jugadaIds=1"
  -F "monto=100"
  -F "comprobanteWhatsapp=true"

# 2. Admin sube comprobante recibido por WhatsApp
PUT /api/admin/pagos/{id}/comprobante
  -F "comprobante=@whatsapp.jpg"

# 3. Verificar en carpeta correcta
# uploads/comprobantes/{quiniela_name}/pago_{id}_*.jpg
```

## Herramientas de Prueba Recomendadas

1. **Postman/Insomnia**: Para probar endpoints HTTP
2. **curl**: Para pruebas desde terminal
3. **File Explorer/Terminal**: Para verificar estructura de carpetas
4. **Logs de aplicación**: Para diagnosticar problemas

## Matriz de Verificación

| Funcionalidad | Windows | Linux | OCI | Resultado |
|---|---|---|---|---|
| Crear pago con comprobante | ✓ | ✓ | ✓ | |
| Descargar comprobante | ✓ | ✓ | ✓ | |
| Reemplazar comprobante | ✓ | ✓ | ✓ | |
| Admin sube comprobante | ✓ | ✓ | ✓ | |
| Reintentar pago | ✓ | ✓ | ✓ | |
| Comprobante de premio | ✓ | ✓ | ✓ | |
| Compatibilidad hacia atrás | ✓ | ✓ | ✓ | |
| Sanitización de nombres | ✓ | ✓ | ✓ | |
| Manejo de errores | ✓ | ✓ | ✓ | |
| Rendimiento | ✓ | ✓ | ✓ | |

## Criterios de Aceptación

- ✅ Todos los comprobantes nuevos se guardan en carpetas organizadas
- ✅ Los comprobantes antiguos siguen siendo accesibles
- ✅ No hay degradación de rendimiento
- ✅ Manejo correcto de caracteres especiales en nombres
- ✅ Errores manejados gracefully
- ✅ Funciona con Local Storage y OCI
- ✅ Logs claros de auditoría

## Notas

- Realizar pruebas en ambiente de desarrollo/staging antes de producción
- Mantener logs de todas las pruebas para auditoría
- Documentar cualquier comportamiento inesperado

