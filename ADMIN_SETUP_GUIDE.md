# 🔐 Guía: Crear Administrador Inicial en Oracle Cloud Free Always

## 📋 Resumen Rápido

Tu aplicación Spring Boot ahora tiene **3 formas seguras** de crear el primer administrador:

1. **✅ RECOMENDADO**: Variables de Entorno (AdminDataInitializer)
2. **Alternativa**: CLI Interactiva (CreateAdminCLI)
3. **Fallback**: Script SQL Manual

---

## 🌐 OPCIÓN 1: Variables de Entorno (RECOMENDADO PARA PRODUCCIÓN)

### ¿Por qué es segura?
- No requiere código hardcodeado
- Las credenciales se almacenan en OCI Vault o Secrets Manager
- Se ejecuta automáticamente al arrancar
- Los logs NO muestran la contraseña

### 📍 EN ORACLE CLOUD FREE ALWAYS

#### Paso 1: Generar la contraseña admin

Usa contraseña larga y aleatoria (mínimo 10 caracteres, recomendado 32+):

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[System.Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Maximum 256) }))
```

Ejemplo de salida:
```
a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR
```

#### Paso 2: Crear las variables de entorno en OCI

**OPCIÓN A: Cloud Shell (Más rápido)**

```bash
# 1. Abre Cloud Shell en la consola de OCI
# 2. Ejecuta:
export ADMIN_USERNAME="mi_admin_usuario"
export ADMIN_PASSWORD="a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR"

# 3. Inicia tu aplicación
java -Dspring.profiles.active=prod -jar target/api-0.0.1-SNAPSHOT.jar
```

**OPCIÓN B: Compute Instance (Recomendado)**

1. Ve a **Compute > Instances** en OCI Console
2. Selecciona tu instancia
3. Haz clic en los 3 puntitos (⋮) > **Edit instance**
4. Baja a **Metadata** y agrega:

```bash
#!/bin/bash
export ADMIN_USERNAME="mi_admin_usuario"
export ADMIN_PASSWORD="a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR"
```

**OPCIÓN C: OCI Vault (MÁXIMA SEGURIDAD)**

```bash
# 1. Crear un secreto en OCI Vault
oci secrets secret create \
  --compartment-id <COMPARTMENT_ID> \
  --secret-name admin-password \
  --secret-content-type "text/plain" \
  --secret-content "a7k2L9mP3qR5s8T1vW4xY6zB0cD2eF4gH6iJ8kL0mN2oP4qR"

# 2. Crear un volcado del secreto (Bastion Host)
oci secrets secret-bundle get \
  --secret-id <SECRET_OCID> \
  --raw-output \
  --query data.secret-bundle-content.content | jq -r . | base64 -d

# 3. Usar en tu script de inicio
export ADMIN_PASSWORD=$(oci secrets secret-bundle get \
  --secret-id <SECRET_OCID> \
  --raw-output \
  --query data.secret-bundle-content.content)
```

#### Paso 3: Verificar que funcionó

Al arrancar la aplicación, deberías ver en los logs:

```
✓ Administrador creado exitosamente:
  Usuario: mi_admin_usuario
  Rol: ADMIN
  Estado: Activo
  Perfil: Inicializado
```

Si ya existe un admin:
```
✓ Administrador ya existe en la base de datos. Se omite creación.
```

---

## 💻 OPCIÓN 2: CLI Interactiva (Para emergencias)

Si las variables de entorno no están disponibles, puedes crear admin manualmente.

### Preparar el archivo CreateAdminCLI

El archivo `CreateAdminCLI.java` está comentado por defecto. Para usarlo:

```bash
# 1. Descomenta la línea @Component en CreateAdminCLI.java
# 2. Recompila
mvn clean package

# 3. Ejecuta con bandera especial
java -jar target/api-0.0.1-SNAPSHOT.jar --create-admin

# 4. Sigue el asistente interactivo
```

Ejemplo de sesión:
```
=== CREAR ADMINISTRADOR ===

Nombre de usuario: admin
Email: admin@miempresa.com
Contraseña (mínimo 10 caracteres): •••••••••••••
Confirmar contraseña: •••••••••••••

✓ Administrador creado exitosamente!
  Usuario: admin
  Email: admin@miempresa.com
  Rol: ADMIN
```

---

## 🗄️ OPCIÓN 3: Script SQL Manual (Fallback)

Si ninguna de las anteriores funciona, ejecuta SQL directamente.

### Paso 1: Generar hash bcrypt de la contraseña

```bash
# Con Python
python3 << 'EOF'
import bcrypt
password = "tucontraseña"
hash = bcrypt.hashpw(password.encode(), bcrypt.gensalt(12))
print(hash.decode())
EOF

# Con Node.js
node -e "const bcrypt = require('bcrypt'); bcrypt.hash('tucontraseña', 12, (err, hash) => console.log(hash));"

# Resultado: $2a$12$R9h7cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jKMm.
```

### Paso 2: Ejecutar en MySQL

```bash
# Conectarse a la BD en OCI
mysql -h <IP_BASE_DATOS> -u admin -p quinielas_deportivas

# Ejecutar
INSERT INTO users (username, email, password, role, activo, fecha_creacion) 
VALUES (
    'admin',
    'admin@local.com',
    '$2a$12$R9h7cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jKMm.',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
);

# Crear perfil
INSERT INTO user_profiles (user_id, nombre_completo, cedula, telefono, foto_perfil, fecha_nacimiento, estado_perfil, fecha_actualizacion) 
SELECT id, NULL, NULL, NULL, NULL, NULL, 'INCOMPLETO', CURRENT_TIMESTAMP FROM users WHERE username = 'admin' LIMIT 1;

# Verificar
SELECT id, username, email, role, activo FROM users WHERE role = 'ADMIN';
```

---

## 🔒 Mejores Prácticas de Seguridad

### ✅ HAZLO
- ✅ Usa contraseñas de **mínimo 32 caracteres** en producción
- ✅ Incluye números, mayúsculas, minúsculas, símbolos: `Ab9!Xy$Pq2@mK#Lv3&Nm4*Ro5%St6`
- ✅ Almacena credenciales en **OCI Vault** o **Secrets Manager**
- ✅ Usa **HTTPS** en todas las comunicaciones
- ✅ Cambia la contraseña admin después de la instalación
- ✅ Crea usuarios admin adicionales para diferentes departamentos
- ✅ Revisa los logs regularmente: `tail -f nohup.out | grep -i "admin\|error"`

### ❌ NO LO HAGAS
- ❌ NO hardcodees credenciales en `application.properties`
- ❌ NO commits credenciales a Git (aunque sea en rama privada)
- ❌ NO uses contraseñas débiles (`admin123`, `password`)
- ❌ NO mantengas la contraseña por defecto
- ❌ NO compartas credenciales por email/Slack
- ❌ NO reutilices contraseñas de otros servicios
- ❌ NO dejes debug=true en producción

---

## 🚀 Flujo Completo para Oracle Cloud Free Always

### Esquema Recomendado

```
┌─────────────────────────────────────┐
│   OCI Console (Cloud Shell)         │
│  ┌─────────────────────────────────┐│
│  │ export ADMIN_USERNAME=admin    ││
│  │ export ADMIN_PASSWORD=xxxxx    ││
│  └─────────────────────────────────┘│
└────────────────┬────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│   Compute Instance (VM.Standard)    │
│  ┌─────────────────────────────────┐│
│  │ java -jar api-0.0.1.jar        ││
│  │ ↓ AdminDataInitializer          ││
│  │ ✓ Admin creado en BD            ││
│  └─────────────────────────���───────┘│
└────────────────┬────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│   MySQL DB (Oracle Cloud)           │
│  ┌─────────────────────────────────┐│
│  │ users table:                    ││
│  │ ├─ id: 1                        ││
│  │ ├─ username: admin              ││
│  │ ├─ role: ADMIN                  ││
│  │ └─ password: <bcrypt hash>      ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

---

## 📝 Archivo de Configuración por Perfil

### `application-dev.properties` (Local)
```properties
admin.username=admin
admin.password=Admin123!@#Segura
spring.jpa.hibernate.ddl-auto=update
```

### `application-prod.properties` (Producción)
```properties
# NO incluyas credenciales aquí
# Usa variables de entorno en OCI
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.quinielas.del.canario.api.config.AdminDataInitializer=INFO
```

---

## 🆘 Troubleshooting

### Problema: "No se encontraron variables de entorno ADMIN_USERNAME..."

**Solución 1:** Establecer variables antes de iniciar
```bash
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="password123456789"
java -jar api-0.0.1-SNAPSHOT.jar
```

**Solución 2:** Pasar como argumentos JVM
```bash
java -D "admin.username=admin" \
     -D "admin.password=password123456789" \
     -jar api-0.0.1-SNAPSHOT.jar
```

### Problema: "El usuario 'admin' ya existe"

**Solución:** El admin ya fue creado. Solo intenta cambiar contraseña vía endpoint:
```bash
curl -X PUT http://localhost:8080/api/admin/usuarios/1/rol \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"role": "USER"}'
```

### Problema: "La contraseña debe tener al menos 10 caracteres"

**Solución:** Usa contraseña más larga
```bash
export ADMIN_PASSWORD="contraseña_larga_de_minimo_10_caracteres"
```

---

## 📚 Referencias

- Spring Security: https://spring.io/projects/spring-security
- BCrypt: https://en.wikipedia.org/wiki/Bcrypt
- OCI Vault: https://docs.oracle.com/en-us/iaas/Content/KeyManagement/Concepts/keyoverview.htm
- MySQL en OCI: https://docs.oracle.com/en-us/iaas/mysql-database/

---

## ✨ Resumen

| Método | Seguridad | Facilidad | Automatización |
|--------|-----------|-----------|-----------------|
| Variables Entorno | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| CLI Interactiva | ⭐⭐⭐⭐ | ⭐⭐ | ⭐ |
| Script SQL | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |

**Para producción en Oracle Cloud: OPCIÓN 1 (Variables de Entorno)**

