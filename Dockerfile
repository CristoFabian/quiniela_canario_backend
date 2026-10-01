# ============================================================================
# Dockerfile para Quinielas Deportivas API
# ============================================================================
#
# BUILD:
#   docker build -t quinielas-api:latest .
#
# RUN en LOCAL:
#   docker run -e ADMIN_USERNAME=admin \
#              -e ADMIN_PASSWORD=password123 \
#              -p 8080:8080 \
#              quinielas-api:latest
#
# PUSH a OCI Container Registry:
#   docker tag quinielas-api:latest \
#     us-central1-docker.pkg.dev/PROJECT_ID/quinielas/api:latest
#   docker push us-central1-docker.pkg.dev/PROJECT_ID/quinielas/api:latest
#
# ============================================================================

# Stage 1: Build
FROM eclipse-temurin:17-jdk as builder

WORKDIR /build

# Copiar archivos de compilación
COPY pom.xml .
COPY src ./src

# Compilar con Maven
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Instalar curl para health checks
RUN apk add --no-cache curl

# Copiar JAR compilado desde stage anterior
COPY --from=builder /build/target/api-0.0.1-SNAPSHOT.jar ./app.jar

# Crear usuario no-root por seguridad
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/test/publico || exit 1

# Punto de entrada: pasar ADMIN_USERNAME y ADMIN_PASSWORD como variables de entorno
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []

