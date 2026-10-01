#!/bin/bash

###############################################################################
# Script de Migración de Comprobantes
#
# Reorganiza los comprobantes existentes en la estructura jerárquica por quiniela.
# Uso: bash migrate-comprobantes.sh [BASE_DIR] [DRY_RUN]
#
# Ejemplos:
#   bash migrate-comprobantes.sh                    # Usa 'uploads' como base
#   bash migrate-comprobantes.sh /path/to/uploads   # Usa ruta personalizada
#   bash migrate-comprobantes.sh /path/to/uploads true # Modo dry-run
###############################################################################

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m' # No Color

# Configuración
BASE_DIR="${1:-.}/uploads"
DRY_RUN="${2:-false}"
COMPROBANTES_DIR="$BASE_DIR/comprobantes"
LOG_FILE="migration_comprobantes_$(date +%Y%m%d_%H%M%S).log"
BACKUP_DIR="$BASE_DIR/comprobantes_backup_$(date +%Y%m%d_%H%M%S)"

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         MIGRACIÓN DE COMPROBANTES DE PAGO                  ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""

# Validar que exista el directorio
if [ ! -d "$COMPROBANTES_DIR" ]; then
    echo -e "${RED}✗ Error: Directorio de comprobantes no encontrado: $COMPROBANTES_DIR${NC}"
    exit 1
fi

echo -e "${BLUE}Configuración:${NC}"
echo "  Base Directory:     $BASE_DIR"
echo "  Comprobantes Dir:   $COMPROBANTES_DIR"
echo "  Dry Run:            $DRY_RUN"
echo "  Log File:           $LOG_FILE"
if [ "$DRY_RUN" = "true" ]; then
    echo -e "  ${YELLOW}⚠ Modo SIMULACIÓN: No se realizarán cambios reales${NC}"
fi
echo ""

# Funciones auxiliares
log() {
    echo "$1" | tee -a "$LOG_FILE"
}

sanitize_quiniela_name() {
    local name="$1"
    # Convertir a minúsculas
    name=$(echo "$name" | tr '[:upper:]' '[:lower:]')
    # Reemplazar espacios múltiples con uno solo
    name=$(echo "$name" | sed 's/[[:space:]]\+/ /g')
    # Eliminar caracteres especiales
    name=$(echo "$name" | sed 's/[\/\\:*?"<>|.]*//g')
    # Reemplazar espacios con guiones bajos
    name=$(echo "$name" | sed 's/ /_/g')
    # Limitar a 100 caracteres
    name=$(echo "$name" | cut -c1-100)
    echo "$name"
}

# Iniciar log
log "═══════════════════════════════════════════════════════════"
log "Migración iniciada: $(date)"
log "═══════════════════════════════════════════════════════════"

# Contar archivos existentes
TOTAL_FILES=$(find "$COMPROBANTES_DIR" -maxdepth 1 -type f | wc -l)
echo -e "${BLUE}Archivos a procesar: ${YELLOW}$TOTAL_FILES${NC}"

if [ "$TOTAL_FILES" -eq 0 ]; then
    echo -e "${GREEN}✓ No hay archivos para migrar${NC}"
    exit 0
fi

# Crear backup
if [ "$DRY_RUN" = "false" ]; then
    echo -e "${YELLOW}Creando backup...${NC}"
    if ! mkdir -p "$BACKUP_DIR"; then
        echo -e "${RED}✗ Error: No se pudo crear directorio de backup${NC}"
        exit 1
    fi
    cp -r "$COMPROBANTES_DIR"/* "$BACKUP_DIR/"
    log "Backup creado en: $BACKUP_DIR"
    echo -e "${GREEN}✓ Backup completado${NC}"
fi

# Procesar archivos
echo -e "${BLUE}Procesando archivos...${NC}"
PROCESSED=0
FAILED=0
SKIPPED=0

while IFS= read -r file; do
    filename=$(basename "$file")

    # Extraer tipo y ID del nombre del archivo
    if [[ $filename =~ ^(pago|premio)_([0-9]+)_ ]]; then
        tipo="${BASH_REMATCH[1]}"
        id="${BASH_REMATCH[2]}"

        # Consultar la base de datos para obtener el nombre de la quiniela
        # Este es un placeholder - en una implementación real, se haría una query SQL
        # Por ahora, usaremos una estructura aproximada basada en el ID

        # Para esta migración, agruparemos por un nombre genérico
        # En producción, se debe hacer una query a la BD
        quiniela_name="quinielas"
        quiniela_dir="$COMPROBANTES_DIR/$(sanitize_quiniela_name "$quiniela_name")"

        # Crear directorio si no existe
        if [ "$DRY_RUN" = "false" ]; then
            mkdir -p "$quiniela_dir"
        fi

        # Mover archivo
        if [ "$DRY_RUN" = "true" ]; then
            log "[DRY-RUN] Mover: $filename → $(basename $quiniela_dir)/$filename"
        else
            if mv "$file" "$quiniela_dir/$filename"; then
                log "[✓] Movido: $filename → $(basename $quiniela_dir)/"
                ((PROCESSED++))
            else
                log "[✗] Error al mover: $filename"
                ((FAILED++))
            fi
        fi
    else
        log "[⊘] Archivo no reconocido (omitido): $filename"
        ((SKIPPED++))
    fi
done < <(find "$COMPROBANTES_DIR" -maxdepth 1 -type f)

echo ""
echo -e "${BLUE}Resumen:${NC}"
echo "  Procesados:  $PROCESSED"
echo "  Omitidos:    $SKIPPED"
echo "  Errores:     $FAILED"

if [ "$DRY_RUN" = "true" ]; then
    echo -e "${YELLOW}⚠ Modo simulación - No se realizaron cambios reales${NC}"
    echo -e "${YELLOW}Ejecute sin 'true' como tercer parámetro para aplicar los cambios${NC}"
fi

log "═══════════════════════════════════════════════════════════"
log "Migración completada: $(date)"
log "Resumen: $PROCESSED procesados, $SKIPPED omitidos, $FAILED errores"
log "═══════════════════════════════════════════════════════════"

echo ""
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}✓ Migración completada exitosamente${NC}"
else
    echo -e "${RED}✗ Migración completada con errores${NC}"
fi

echo -e "${BLUE}Log guardado en: $LOG_FILE${NC}"

exit 0

