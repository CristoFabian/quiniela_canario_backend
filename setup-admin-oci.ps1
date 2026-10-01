# ============================================================================
# Script de inicialización de administrador para Windows
# ============================================================================
#
# USO:
#   Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
#   .\setup-admin-oci.ps1
#
# Este script te ayuda a generar y configurar un administrador inicial
# de forma segura en Windows.
#
# ============================================================================

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Inicializador de Administrador para OCI             ║" -ForegroundColor Cyan
Write-Host "║   Quinielas Deportivas API                            ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 1: Generar contraseña admin
# ─────────────────────────────────────────────────────────────────────────────

Write-Host "📋 PASO 1: Generar contraseña segura" -ForegroundColor Yellow
Write-Host "────────────────────────────────────" -ForegroundColor Yellow
Write-Host ""

# Generar 32 bytes aleatorios y convertir a base64
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
$rng.GetBytes($bytes)
$ADMIN_PASSWORD = [Convert]::ToBase64String($bytes)

Write-Host "✓ Contraseña generada:" -ForegroundColor Green
Write-Host "   $ADMIN_PASSWORD"
Write-Host ""
Write-Host "⚠  GUARDA ESTA CONTRASEÑA EN UN LUGAR SEGURO" -ForegroundColor Red
Write-Host ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 2: Nombre de usuario
# ─────────────────────────────────────────────────────────────────────────────

Write-Host "📋 PASO 2: Nombre de usuario" -ForegroundColor Yellow
Write-Host "───────────────────────────" -ForegroundColor Yellow
Write-Host ""

$ADMIN_USERNAME = Read-Host "¿Nombre de usuario admin? (default: admin)"
if ([string]::IsNullOrWhiteSpace($ADMIN_USERNAME)) {
    $ADMIN_USERNAME = "admin"
}

if ($ADMIN_USERNAME.Length -lt 3) {
    Write-Host "✗ El usuario debe tener mínimo 3 caracteres." -ForegroundColor Red
    exit 1
}

Write-Host "   Usuario: $ADMIN_USERNAME" -ForegroundColor Cyan
Write-Host ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 3: Email
# ─────────────────────────────────────────────────────────────────────────────

Write-Host "📋 PASO 3: Email del admin" -ForegroundColor Yellow
Write-Host "──────────────────────────" -ForegroundColor Yellow
Write-Host ""

$ADMIN_EMAIL = Read-Host "Email"

if ($ADMIN_EMAIL -notmatch "@.*\.") {
    Write-Host "✗ Email inválido." -ForegroundColor Red
    exit 1
}

Write-Host "   Email: $ADMIN_EMAIL" -ForegroundColor Cyan
Write-Host ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 4: Mostrar opciones de configuración
# ─────────────────────────────────────────────────────────────────────────────

Write-Host "📋 PASO 4: ¿Cómo deseas configurar las variables?" -ForegroundColor Yellow
Write-Host "─────────────────────────────────────────────────" -ForegroundColor Yellow
Write-Host ""
Write-Host "1) PowerShell (temporal, solo esta sesión)"
Write-Host "2) Archivo .env (guardar para reutilizar)"
Write-Host "3) Mostrar comandos (copiar manualmente)"
Write-Host ""

$OPTION = Read-Host "Selecciona opción (1-3)"

switch ($OPTION) {
    "1" {
        Write-Host ""
        Write-Host "📍 Configurando para PowerShell..." -ForegroundColor Cyan
        Write-Host ""
        [System.Environment]::SetEnvironmentVariable("ADMIN_USERNAME", $ADMIN_USERNAME, "Process")
        [System.Environment]::SetEnvironmentVariable("ADMIN_PASSWORD", $ADMIN_PASSWORD, "Process")
        Write-Host "✓ Variables exportadas en esta sesión" -ForegroundColor Green
        Write-Host ""
        Write-Host "Ahora ejecuta:" -ForegroundColor Yellow
        Write-Host "  java -jar target/api-0.0.1-SNAPSHOT.jar"
    }
    "2" {
        Write-Host ""
        Write-Host "📍 Creando archivo .env.admin..." -ForegroundColor Cyan
        $envContent = @"
# Admin credentials para Quinielas API
ADMIN_USERNAME=$ADMIN_USERNAME
ADMIN_PASSWORD=$ADMIN_PASSWORD
ADMIN_EMAIL=$ADMIN_EMAIL
"@
        $envContent | Out-File -FilePath ".env.admin" -Encoding UTF8
        Write-Host "✓ Archivo creado: .env.admin" -ForegroundColor Green
        Write-Host ""
        Write-Host "Para usar (en PowerShell):" -ForegroundColor Yellow
        Write-Host "  `$env:ADMIN_USERNAME = `"$ADMIN_USERNAME`""
        Write-Host "  `$env:ADMIN_PASSWORD = `"$ADMIN_PASSWORD`""
        Write-Host "  java -jar target/api-0.0.1-SNAPSHOT.jar"
    }
    "3" {
        Write-Host ""
        Write-Host "📍 Comandos para ejecutar en PowerShell:" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "`$env:ADMIN_USERNAME = '$ADMIN_USERNAME'" -ForegroundColor White
        Write-Host "`$env:ADMIN_PASSWORD = '$ADMIN_PASSWORD'" -ForegroundColor White
        Write-Host "java -jar target/api-0.0.1-SNAPSHOT.jar" -ForegroundColor White
        Write-Host ""
    }
    default {
        Write-Host "✗ Opción inválida" -ForegroundColor Red
        exit 1
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# PASO 5: Próximos pasos
# ─────────────────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║              ✓ CONFIGURACIÓN COMPLETADA               ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Próximos pasos:" -ForegroundColor Yellow
Write-Host "   1. Inicia tu aplicación Java"
Write-Host "   2. Verifica en los logs: '✓ Administrador creado'"
Write-Host "   3. Accede a: http://tudominio:8080/api/auth/login"
Write-Host "   4. Usuario: $ADMIN_USERNAME"
Write-Host "   5. Contraseña: ••••••••••••••••"
Write-Host ""
Write-Host "⚠  IMPORTANTE:" -ForegroundColor Red
Write-Host "   - Guarda la contraseña en lugar seguro"
Write-Host "   - Cambia la contraseña después del primer acceso"
Write-Host "   - No compartas las credenciales"
Write-Host ""

