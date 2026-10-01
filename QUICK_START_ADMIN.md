# 🚀 QUICK START: Crear Administrador Inicial

## 📌 Resumen (2 minutos)

Tu aplicación ahora puede crear automáticamente un administrador inicial usando variables de entorno. Sin código hardcodeado. Seguro.

---

## ⚡ OPCIÓN RÁPIDA: Linux/Mac + Cloud Shell OCI

### Paso 1: Generar contraseña segura
```bash
openssl rand -base64 32
```
Salida ejemplo: `a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR`

### Paso 2: Configurar variables y ejecutar
```bash
# En OCI Cloud Shell o tu servidor
export ADMIN_USERNAME="admin_mi_empresa"
export ADMIN_PASSWORD="a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR"

# Iniciar la aplicación
java -jar target/api-0.0.1-SNAPSHOT.jar
```

### Paso 3: Verificar en los logs
```
✓ Administrador creado exitosamente:
  Usuario: admin_mi_empresa
  Rol: ADMIN
  Estado: Activo
```

**¡Listo!** Accede a `http://tudominio:8080/api/auth/login`

---

## 💻 Windows PowerShell

```powershell
# Script automático
.\setup-admin-oci.ps1

# O manual:
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "tucontraseña_larga_32_caracteres"
java -jar target\api-0.0.1-SNAPSHOT.jar
```

---

## 🐧 Linux Bash

```bash
# Script automático
chmod +x setup-admin-oci.sh
./setup-admin-oci.sh

# O manual:
bash setup-admin-oci.sh
```

---

## 🌐 Oracle Cloud Free Always (Terraform)

```bash
# 1. Navegar a carpeta terraform
cd terraform

# 2. Copiar archivo de ejemplo
cp terraform.tfvars.example terraform.tfvars

# 3. Editar con tus credenciales OCI
nano terraform.tfvars
# Completar: tenancy_ocid, user_ocid, fingerprint, etc.

# 4. Desplegar
terraform init
terraform plan
terraform apply

# 5. Ver resultados
terraform output

# La salida mostrará:
# app_url = "http://1.2.3.4:8080"
# Acceder en 2-3 minutos después del despliegue
```

---

## 🔐 Cómo Funciona (Seguro)

### Componente: `AdminDataInitializer.java`
- ✅ Se ejecuta **SOLO al arrancar** la aplicación
- ✅ **Verifica** si ya existe un admin (no sobrescribe)
- ✅ Busca variables de entorno: `ADMIN_USERNAME`, `ADMIN_PASSWORD`
- ✅ Valida: mín 3 caracteres usuario, mín 10 caracteres contraseña
- ✅ **Encripta** la contraseña con BCrypt (no se almacena en texto plano)
- ✅ Crea automáticamente el perfil del usuario
- ✅ Los logs **NO muestran** la contraseña

---

## ❌ Problemas Comunes

### "No se encontraron variables de entorno ADMIN_USERNAME..."
```bash
# Solución: Establecer antes de ejecutar
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="password_segura"
java -jar app.jar
```

### "La contraseña debe tener al menos 10 caracteres"
```bash
# La contraseña era muy corta
# Reintenta con: openssl rand -base64 32
export ADMIN_PASSWORD=$(openssl rand -base64 32)
```

### "El usuario 'admin' ya existe"
```
✓ Significa que el admin se creó exitosamente en la primera ejecución
Ahora puedes:
1. Iniciar sesión con esas credenciales
2. Crear más usuarios desde el panel admin
3. Si olvidaste la contraseña, usa:
   PUT /api/admin/usuarios/{id}/cambiar-password
   (requiere token de otro admin o acceso a BD)
```

---

## 🔍 Verificar que Funciona

### Desde la BD
```sql
-- Conectar a MySQL en OCI
mysql -h <endpoint> -u admin -p quinielas_deportivas

-- Verificar admin
SELECT id, username, email, role, activo FROM users WHERE role = 'ADMIN';

-- Resultado:
-- 1 | admin_mi_empresa | admin@admin_mi_empresa.local | ADMIN | 1
```

### Desde la API
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_mi_empresa","password":"..."}'

# Respuesta:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "username": "admin_mi_empresa",
#   "role": "ADMIN"
# }
```

### Acceder al panel admin
```bash
# URL: http://tudominio:8080/api/admin/usuarios
# (requiere token del admin anterior en header Authorization)
```

---

## 🛡️ Seguridad: Checklist Post-Despliegue

- [ ] ✅ Cambié la contraseña admin desde el panel (o CLI)
- [ ] ✅ Eliminé variables de entorno después del arranque inicial
- [ ] ✅ Las credenciales está en OCI Vault, no en código
- [ ] ✅ HTTPS configurado en nginx (Let's Encrypt)
- [ ] ✅ Firewall solo permite puertos 80, 443, 22 (SSH)
- [ ] ✅ Backups automáticos de MySQL habilitados
- [ ] ✅ Logs monitoreados en tiempo real
- [ ] ✅ Usuarios adicionales creados para otros admins

---

## 📚 Documentación Completa

Ver: `ADMIN_SETUP_GUIDE.md` para:
- ✅ Todas las opciones de configuración
- ✅ OCI Vault y Secrets Manager
- ✅ Docker en OCI
- ✅ Mejores prácticas de seguridad
- ✅ Troubleshooting detallado

---

## ❓ Preguntas Frecuentes

**P: ¿Dónde se almacena la contraseña?**
R: Se encripta con BCrypt (algoritmo de hash irreversible) en la columna `password` de la tabla `users`. No se guarda en texto plano.

**P: ¿Se puede cambiar la contraseña del admin inicial?**
R: Sí, el usuario admin puede cambiarla desde el panel, o manualmente en MySQL.

**P: ¿Qué pasa si reinicio la aplicación?**
R: El AdminDataInitializer verifica si ya existe un admin. Si existe, no crea uno nuevo. Es seguro reiniciar.

**P: ¿Puedo crear múltiples admins?**
R: Sí. El primer admin se crea automáticamente. Luego puedes crear más desde el endpoint `/api/admin/usuarios` o desde el panel.

**P: ¿Es seguro para producción?**
R: ✅ Sí. Las credenciales se inyectan como variables de entorno (mejor práctica), no hardcodeadas.

---

## 🎯 Próximos Pasos

1. **Desplegar en OCI:**
   ```bash
   cd terraform
   terraform apply
   ```

2. **Cambiar contraseña admin:**
   ```bash
   # Después del primer login
   PUT /api/admin/usuarios/1/password
   ```

3. **Crear usuarios normales:**
   ```bash
   POST /api/auth/register
   {
     "username": "usuario1",
     "email": "user@example.com",
     "password": "password123"
   }
   ```

4. **Promover a admin si es necesario:**
   ```bash
   PUT /api/admin/usuarios/{id}/rol
   {"role": "ADMIN"}
   ```

---

**¡Listo para producción! 🚀**

