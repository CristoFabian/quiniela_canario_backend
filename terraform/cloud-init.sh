#!/bin/bash
# ============================================================================
# Cloud-init script para Oracle Cloud - Inicializar Quinielas API
# ============================================================================
#
# Este script se ejecuta automáticamente cuando la instancia se inicia.
# Instala Java, descarga la aplicación, configura las variables de entorno
# y inicia la aplicación Spring Boot.
#
# ============================================================================

set -e

# Configurar variables
ADMIN_USERNAME="${admin_username}"
ADMIN_PASSWORD="${admin_password}"
APP_HOME="/opt/quinielas-api"
APP_USER="appuser"
LOG_FILE="/var/log/quinielas-api/startup.log"

# ─────────────────────────────────────────────────────────────────────────────
# Logging
# ─────────────────────────────────────────────────────────────────────────────

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

log "════════════════════════════════════════════════════════════"
log "Inicializando Quinielas API en Oracle Cloud"
log "════════════════════════════════════════════════════════════"

# ─────────────────────────────────────────────────────────────────────────────
# Actualizar sistema
# ─────────────────────────────────────────────────────────────────────────────

log "1️⃣  Actualizando paquetes del sistema..."
apt-get update
apt-get upgrade -y

# ─────────────────────────────────────────────────────────────────────────────
# Instalar Java 17
# ─────────────────────────────────────────────────────────────────────────────

log "2️⃣  Instalando Java 17..."
apt-get install -y openjdk-17-jre-headless
java -version

# ─────────────────────────────────────────────────────────────────────────────
# Instalar MySQL Client (para conectarse a MySQL en OCI)
# ─────────────────────────────────────────────────────────────────────────────

log "3️⃣  Instalando MySQL Client..."
apt-get install -y mysql-client

# ─────────────────────────────────────────────────────────────────────────────
# Instalar Nginx como reverse proxy
# ─────────────────────────────────────────────────────────────────────────────

log "4️⃣  Instalando Nginx como reverse proxy..."
apt-get install -y nginx

# Configurar Nginx
cat > /etc/nginx/sites-available/default << 'EOF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;

    server_name _;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 90;
    }
}
EOF

systemctl restart nginx
log "✓ Nginx configurado"

# ─────────────────────────────────────────────────────────────────────────────
# Crear usuario de aplicación
# ─────────────────────────────────────────────────────────────────────────────

log "5️⃣  Creando usuario de aplicación..."
useradd -m -s /bin/false $APP_USER || true
mkdir -p $APP_HOME
mkdir -p /var/log/quinielas-api
chown -R $APP_USER:$APP_USER $APP_HOME /var/log/quinielas-api

# ─────────────────────────────────────────────────────────────────────────────
# Descargar y desplegar la aplicación JAR
# ─────────────────────────────────────────────────────────────────────────────

log "6️⃣  Descargando aplicación..."

# OPCIÓN A: Desde GitHub Releases (si está disponible)
# wget -O $APP_HOME/app.jar https://github.com/tu-usuario/quinielas-api/releases/download/v1.0.0/api-0.0.1-SNAPSHOT.jar

# OPCIÓN B: Construir localmente (requiere git y maven)
log "   Clonando repositorio..."
cd /tmp
git clone https://github.com/tu-usuario/quinielas-api.git || true
cd quinielas-api
log "   Compilando con Maven..."
mvn clean package -DskipTests

cp target/api-0.0.1-SNAPSHOT.jar $APP_HOME/app.jar
chown $APP_USER:$APP_USER $APP_HOME/app.jar

# ─────────────────────────────────────────────────────────────────────────────
# Crear archivo de configuración de systemd
# ─────────────────────────────────────────────────────────────────────────────

log "7️⃣  Configurando servicio systemd..."

cat > /etc/systemd/system/quinielas-api.service << EOF
[Unit]
Description=Quinielas Deportivas API
After=network.target

[Service]
User=$APP_USER
WorkingDirectory=$APP_HOME
Environment="ADMIN_USERNAME=$ADMIN_USERNAME"
Environment="ADMIN_PASSWORD=$ADMIN_PASSWORD"
Environment="SPRING_PROFILES_ACTIVE=prod"
ExecStart=/usr/bin/java -jar $APP_HOME/app.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:$LOG_FILE
StandardError=append:$LOG_FILE

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable quinielas-api
log "✓ Servicio creado y habilitado"

# ─────────────────────────────────────────────────────────────────────────────
# Crear archivo application-prod.properties
# ───────────────────────────────��─────────────────────────────────────────────

log "8️⃣  Configurando aplicación para producción..."

cat > $APP_HOME/application-prod.properties << 'EOF'
spring.application.name=api

# Base de datos MySQL en OCI (reemplaza con tu endpoint)
spring.datasource.url=jdbc:mysql://mysql-db-endpoint:3306/quinielas_deportivas?useSSL=true&serverTimezone=UTC
spring.datasource.username=admin
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT
jwt.secret=tu_clave_jwt_secreta_aqui_con_256_bits_minimo
jwt.expiration=86400000

# CORS - Configurar con tu dominio en producción
cors.allowed-origins=https://tudominio.com

# Almacenamiento de archivos (OCI Object Storage)
app.storage.provider=oci
app.storage.oci.endpoint=https://namespace.compat.objectstorage.region.oraclecloud.com
app.storage.oci.region=us-ashburn-1
app.storage.oci.access-key=${OCI_ACCESS_KEY}
app.storage.oci.secret-key=${OCI_SECRET_KEY}
app.storage.oci.bucket=quinielas-uploads
app.storage.oci.public-url-base=https://namespace.objectstorage.region.oraclecloud.com/n/namespace/b/quinielas-uploads/o/perfiles/

# Límites de carga
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# Logging
logging.level.root=INFO
logging.level.com.quinielas.del.canario.api=DEBUG
logging.file.name=/var/log/quinielas-api/app.log
logging.file.max-size=10MB
logging.file.max-history=10
EOF

chown $APP_USER:$APP_USER $APP_HOME/application-prod.properties
log "✓ Archivo de configuración creado"

# ─────────────────────────────────────────────────────────────────────────────
# Iniciar la aplicación
# ─────────────────────────────────────────────────────────────────────────────

log "9️⃣  Iniciando servicio Quinielas API..."
systemctl start quinielas-api

# Esperar a que la aplicación inicie
sleep 15

if systemctl is-active --quiet quinielas-api; then
    log "✓ Quinielas API está ejecutándose"
else
    log "✗ Error: Quinielas API no está ejecutándose"
    journalctl -u quinielas-api -n 50
    exit 1
fi

# ─────────────────────────────────────────────────────────────────────────────
# Resumen final
# ─────────────────────────────────────────────────────────────────────────────

log ""
log "════════════════════════════════════════════════════════════"
log "✓ INICIALIZACIÓN COMPLETADA"
log "════════════════════════════════════════════════════════════"
log ""
log "📍 Información de acceso:"
log "   URL: http://$(hostname -I | awk '{print $1}'):80"
log "   API: http://$(hostname -I | awk '{print $1}'):8080"
log "   Admin: $ADMIN_USERNAME"
log ""
log "📝 Logs:"
log "   tail -f $LOG_FILE"
log "   journalctl -u quinielas-api -f"
log ""
log "════════════════════════════════════════════════════════════"

