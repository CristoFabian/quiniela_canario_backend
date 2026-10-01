# 🔐 Crear Administrador Inicial - Guía Segura para Oracle Cloud

## Resumen Ejecutivo

Tu aplicación Spring Boot ahora puede crear automáticamente un **usuario administrador seguro** al iniciarse, sin necesidad de hardcodear credenciales. 

**Mejor práctica:** Usar **variables de entorno** para inyectar credenciales en tiempo de ejecución.

---

## 🎯 ¿Qué Necesitas Hacer?

### PASO 1️⃣: Generar Contraseña Segura

En tu terminal Linux/Mac o PowerShell (Windows):

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Maximum 256) }))
```

**Resultado ejemplo:**
```
a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR
```

✅ **Guarda esta contraseña en un lugar seguro** (preferentemente OCI Vault)

### PASO 2️⃣: Configurar Variables de Entorno

**En OCI Cloud Shell:**
```bash
export ADMIN_USERNAME="tuadmin"
export ADMIN_PASSWORD="a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR"
```

**En Windows PowerShell:**
```powershell
$env:ADMIN_USERNAME = "tuadmin"
$env:ADMIN_PASSWORD = "a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR"
```

### PASO 3️⃣: Iniciar la Aplicación

```bash
java -jar target/api-0.0.1-SNAPSHOT.jar
```

### PASO 4️⃣: Verificar en los Logs

Deberías ver:
```
✓ Administrador creado exitosamente:
  Usuario: tuadmin
  Rol: ADMIN
  Estado: Activo
  Perfil: Inicializado
```

✅ **¡Listo!** El admin fue creado automáticamente.

---

## 🔄 Flujo de Funcionamiento

```
1. Aplicación arranca
           │
           ▼
2. AdminDataInitializer se ejecuta
           │
           ▼
3. ¿Existe admin en BD?
       ├─ SÍ → Salta (no sobrescribe)
       └─ NO ▼
4. Lee ADMIN_USERNAME y ADMIN_PASSWORD del entorno
           │
           ▼
5. Valida:
   - Username >= 3 caracteres ✓
   - Password >= 10 caracteres ✓
           │
           ▼
6. Encripta password con BCrypt
           │
           ▼
7. Crea usuario ADMIN en BD
           │
           ▼
✓ Admin disponible para login
```

---

## 🛡️ ¿Por Qué Es Seguro?

| Característica | Beneficio |
|---|---|
| **Variables de entorno** | Las credenciales no están en código ni repositorio Git |
| **BCrypt** | La contraseña se encripta irreversiblemente en BD |
| **Validaciones** | Mínimo 10 caracteres, sin espacios, caracteres especiales permitidos |
| **Idempotencia** | Si reinicia la app, no crea admin duplicado |
| **Logs seguros** | Los logs NO muestran la contraseña |
| **Inyección de dependencias** | PasswordEncoder gestionado por Spring Security |

---

## 📦 Tres Formas de Hacerlo

### ✅ OPCIÓN 1: Script Automático (RECOMENDADO)

**Linux/Mac:**
```bash
chmod +x setup-admin-oci.sh
./setup-admin-oci.sh
# Te guía paso a paso
```

**Windows:**
```powershell
.\setup-admin-oci.ps1
# Interfaz interactiva
```

### ✅ OPCIÓN 2: Terraform (Para Producción)

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edita terraform.tfvars con tus credenciales OCI
terraform init
terraform apply
```

**Resultado:** Instancia VM + MySQL + API + Admin **AUTOMÁTICAMENTE**

### ✅ OPCIÓN 3: Docker Compose (Para Desarrollo)

```bash
docker-compose up -d
# MySQL + API + Nginx en 3 contenedores
# Admin creado automáticamente
```

---

## 🚨 Casos Comunes y Soluciones

### ❌ "No se encontraron variables de entorno"

**Causa:** No estableciste ADMIN_USERNAME o ADMIN_PASSWORD

**Solución:**
```bash
# Establece ANTES de ejecutar
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="contraseña_larga_32_caracteres"
java -jar app.jar
```

---

### ❌ "El usuario ya existe"

**Causa:** El admin fue creado en una ejecución anterior

**Solución:** Normal. Usa las credenciales de ese admin para iniciar sesión.

---

### ❌ "La contraseña debe tener al menos 10 caracteres"

**Causa:** Contraseña muy corta

**Solución:**
```bash
export ADMIN_PASSWORD=$(openssl rand -base64 32)
# Mínimo 32 caracteres aleatorios
```

---

## 📍 Ubicación de Archivos

```
proyecto/api/
├── QUICK_START_ADMIN.md               ← Empieza aquí (5 min)
├── ADMIN_SETUP_GUIDE.md               ← Guía completa
├── ADMIN_INITIALIZATION.md            ← Referencia técnica
├── setup-admin-oci.sh                 ← Script Linux/Mac
├── setup-admin-oci.ps1                ← Script Windows
├── Dockerfile                         ← Para Docker
├── docker-compose.yml                 ← Stack local
│
├── src/main/java/.../config/
│   └── AdminDataInitializer.java      ← Componente principal
│
├── src/main/java/.../util/
│   └── CreateAdminCLI.java            ← CLI opcional
│
├── terraform/
│   ├── main.tf                        ← Infraestructura OCI
│   ├── cloud-init.sh                  ← Script boot
│   └── terraform.tfvars.example       ← Plantilla variables
│
└── src/main/resources/db/migration/
    └── V1_1__Init_Admin_User.sql      ← Script SQL fallback
```

---

## ✅ Checklist Rápido

- [ ] ¿Generé contraseña segura con openssl?
- [ ] ¿Configuré ADMIN_USERNAME y ADMIN_PASSWORD?
- [ ] ¿Ejecuté la aplicación?
- [ ] ¿Verifico en logs "✓ Administrador creado"?
- [ ] ¿Probé login en `/api/auth/login`?
- [ ] ¿Guardé credenciales en lugar seguro (OCI Vault)?

---

## 🎬 Ejemplo Completo (5 minutos)

### Linux/Mac:

```bash
# 1. Generar contraseña
openssl rand -base64 32
# Copiar resultado

# 2. En OCI Cloud Shell
export ADMIN_USERNAME="admin_produccion"
export ADMIN_PASSWORD="<pegue_aquí_la_contraseña_anterior>"

# 3. Ejecutar
java -jar target/api-0.0.1-SNAPSHOT.jar

# 4. En otro terminal, verificar
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_produccion","password":"<contraseña>"}'

# 5. Respuesta (token JWT):
# {"token":"eyJhbGc...","username":"admin_produccion","role":"ADMIN"}
```

### Windows:

```powershell
# 1. Script automático
.\setup-admin-oci.ps1

# 2. Seguir instrucciones interactivas

# 3. Ejecutar (aparecerá en el script)
$env:ADMIN_USERNAME = "admin_produccion"
$env:ADMIN_PASSWORD = "..."
java -jar target\api-0.0.1-SNAPSHOT.jar
```

---

## 🔒 Después del Primer Acceso

1. **Cambiar contraseña del admin:**
   ```bash
   PUT /api/admin/usuarios/1/password
   Content-Type: application/json
   Authorization: Bearer <token>
   
   {"nuevaPassword": "nueva_contraseña_aún_más_segura"}
   ```

2. **Crear usuarios adicionales:**
   ```bash
   POST /api/auth/register
   {"username": "usuario2", "email": "user@example.com", "password": "..."}
   ```

3. **Promover usuario a admin (si es necesario):**
   ```bash
   PUT /api/admin/usuarios/{id}/rol
   {"role": "ADMIN"}
   ```

---

## 💡 Consejos de Seguridad

### ✅ HAZLO
- ✅ Contraseña >= 32 caracteres
- ✅ Números, mayúsculas, minúsculas, símbolos
- ✅ Guarda en OCI Vault (no en Git)
- ✅ Usa HTTPS en producción
- ✅ Cambia contraseña regularmente
- ✅ Revisa logs diariamente

### ❌ NO LO HAGAS
- ❌ Hardcodear en properties
- ❌ Commitear credenciales a Git
- ❌ Usar contraseñas genéricas (`admin123`)
- ❌ Compartir credenciales por email
- ❌ Reutilizar en otros servicios
- ❌ Dejar debug=true en producción

---

## 📚 Documentación Completa

Para más detalles, consulta:

- **Configuración detallada**: `ADMIN_SETUP_GUIDE.md`
- **Inicio rápido**: `QUICK_START_ADMIN.md`
- **Referencia técnica**: `ADMIN_INITIALIZATION.md`

---

## ❓ Preguntas Finales

**P: ¿Es necesario hacer esto para producción?**
R: Sí. Sin un admin, no puedes acceder al panel. Es el primer paso.

**P: ¿Se puede automatizar completamente?**
R: Sí. Con Terraform o Docker se automatiza al 100%.

**P: ¿Qué pasa si pierdo la contraseña?**
R: Puedes resetearla:
- Accediendo a la BD directamente (MySQL)
- O generando una nueva contraseña y redeployando

**P: ¿Funciona en Oracle Cloud Free Tier?**
R: ✅ Sí. Totalmente compatible. Incluye recursos Always Free.

---

## 🎯 Próximo Paso

1. Lee **QUICK_START_ADMIN.md** (elige tu opción)
2. Ejecuta el script o comando
3. ¡Listo para producción!

---

**¿Preguntas? Consulta ADMIN_SETUP_GUIDE.md para documentación exhaustiva.**

**¡Tu aplicación está lista para producción! 🚀**

