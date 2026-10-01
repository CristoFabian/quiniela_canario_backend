#!/bin/bash
# ============================================================================
# Script de inicialización de administrador para Oracle Cloud
# ============================================================================
#
# USO:
#   chmod +x setup-admin-oci.sh
#   ./setup-admin-oci.sh
#
# Este script te ayuda a generar y configurar un administrador inicial
# de forma segura en Oracle Cloud Free Always.
#
# ============================================================================

set -e  # Salir si hay error

echo "╔════════════════════════════════════════════════════════╗"
echo "║   Inicializador de Administrador para OCI             ║"
echo "║   Quinielas Deportivas API                            ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Detectar el sistema operativo
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
else
    echo "⚠  Sistema operativo no detectado. Usando configuración para Linux."
    OS="linux"
fi

echo "Sistema detectado: $OS"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 1: Generar contraseña admin
# ─────────────────────────────────────────────────────────────────────────────

echo "📋 PASO 1: Generar contraseña segura"
echo "────────────────────────────────────"
echo ""

# Generar contraseña aleatoria
if command -v openssl &> /dev/null; then
    ADMIN_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
    echo "✓ Contraseña generada con OpenSSL:"
else
    # Fallback si no está openssl
    ADMIN_PASSWORD=$(tr -dc 'A-Za-z0-9!@#$%^&*()_+-=' </dev/urandom | head -c 32)
    echo "✓ Contraseña generada aleatoriamente:"
fi

echo "   $ADMIN_PASSWORD"
echo ""
echo "⚠  GUARDA ESTA CONTRASEÑA EN UN LUGAR SEGURO"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 2: Nombre de usuario
# ─────────────────────────────────────────────────────────────────────────────

echo "📋 PASO 2: Nombre de usuario"
echo "───────────────────────────"
echo ""

read -p "¿Nombre de usuario admin? (default: admin): " ADMIN_USERNAME
ADMIN_USERNAME=${ADMIN_USERNAME:-admin}

if [ ${#ADMIN_USERNAME} -lt 3 ]; then
    echo "✗ El usuario debe tener mínimo 3 caracteres."
    exit 1
fi

echo "   Usuario: $ADMIN_USERNAME"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 3: Email
# ─────────────────────────────────────────────────────────────────────────────

echo "📋 PASO 3: Email del admin"
echo "──────────────────────────"
echo ""

read -p "Email: " ADMIN_EMAIL

if [[ ! "$ADMIN_EMAIL" =~ @.*\. ]]; then
    echo "✗ Email inválido."
    exit 1
fi

echo "   Email: $ADMIN_EMAIL"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# PASO 4: Mostrar opciones de configuración
# ─────────────────────────────────────────────────────────────────────────────

echo "📋 PASO 4: ¿Cómo deseas configurar las variables?"
echo "─────────────────────────────────────────────────"
echo ""
echo "1) Cloud Shell (temporal, solo esta sesión)"
echo "2) Archivo .env (guardar para reutilizar)"
echo "3) Mostrar comandos (copiar manualmente)"
echo ""

read -p "Selecciona opción (1-3): " OPTION

case $OPTION in
    1)
        echo ""
        echo "📍 Configurando para Cloud Shell..."
        echo ""
        export ADMIN_USERNAME="$ADMIN_USERNAME"
        export ADMIN_PASSWORD="$ADMIN_PASSWORD"
        echo "✓ Variables exportadas en esta sesión"
        echo ""
        echo "Ahora ejecuta:"
        echo "  java -jar target/api-0.0.1-SNAPSHOT.jar"
        ;;
    2)
        echo ""
        echo "📍 Creando archivo .env..."
        cat > .env.admin << EOF
# Admin credentials para Quinielas API
ADMIN_USERNAME=$ADMIN_USERNAME
ADMIN_PASSWORD=$ADMIN_PASSWORD
ADMIN_EMAIL=$ADMIN_EMAIL
EOF
        echo "✓ Archivo creado: .env.admin"
        echo ""
        echo "Para usar:"
        echo "  source .env.admin"
        echo "  java -jar target/api-0.0.1-SNAPSHOT.jar"
        ;;
    3)
        echo ""
        echo "📍 Comandos para ejecutar manualmente:"
        echo ""
        if [ "$OS" == "linux" ] || [ "$OS" == "macos" ]; then
            echo "export ADMIN_USERNAME='$ADMIN_USERNAME'"
            echo "export ADMIN_PASSWORD='$ADMIN_PASSWORD'"
            echo "java -jar target/api-0.0.1-SNAPSHOT.jar"
        fi
        echo ""
        ;;
    *)
        echo "✗ Opción inválida"
        exit 1
        ;;
esac

# ─────────────────────────────────────────────────────────────────────────────
# PASO 5: Próximos pasos
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║              ✓ CONFIGURACIÓN COMPLETADA               ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "📝 Próximos pasos:"
echo "   1. Inicia tu aplicación Java"
echo "   2. Verifica en los logs: '✓ Administrador creado'"
echo "   3. Accede a: http://tudominio:8080/api/auth/login"
echo "   4. Usuario: $ADMIN_USERNAME"
echo "   5. Contraseña: ••••••••••••••••"
echo ""
echo "⚠  IMPORTANTE:"
echo "   - Guarda la contraseña en lugar seguro"
echo "   - Cambia la contraseña después del primer acceso"
echo "   - No compartas las credenciales"
echo ""

