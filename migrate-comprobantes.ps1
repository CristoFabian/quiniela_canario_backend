#Requires -Version 5.0

<#
.SYNOPSIS
    Script de Migración de Comprobantes de Pago

.DESCRIPTION
    Reorganiza los comprobantes existentes en la estructura jerárquica por quiniela.

.PARAMETER BaseDir
    Directorio base donde se encuentran los comprobantes. Por defecto: './uploads'

.PARAMETER DryRun
    Si es $true, simula los cambios sin ejecutarlos. Por defecto: $false

.EXAMPLE
    .\migrate-comprobantes.ps1
    .\migrate-comprobantes.ps1 -BaseDir "C:\uploads"
    .\migrate-comprobantes.ps1 -BaseDir "C:\uploads" -DryRun $true
#>

param(
    [string]$BaseDir = ".\uploads",
    [bool]$DryRun = $false
)

# Configuración de colores
$Colors = @{
    Red    = 'Red'
    Green  = 'Green'
    Yellow = 'Yellow'
    Cyan   = 'Cyan'
    Gray   = 'Gray'
}

function Write-Log {
    param(
        [string]$Message,
        [string]$Color = 'Gray'
    )
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = "[$timestamp] $Message"
    Write-Host $logEntry -ForegroundColor $Color
    Add-Content -Path $LogFile -Value $logEntry
}

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host "╔$('═' * ($Text.Length + 2))╗" -ForegroundColor Cyan
    Write-Host "║ $Text ║" -ForegroundColor Cyan
    Write-Host "╚$('═' * ($Text.Length + 2))╝" -ForegroundColor Cyan
    Write-Host ""
}

function Sanitize-QuinielaName {
    param([string]$Name)

    # Convertir a minúsculas
    $result = $Name.ToLower()
    # Reemplazar espacios múltiples con uno solo
    $result = [System.Text.RegularExpressions.Regex]::Replace($result, '\s+', ' ')
    # Eliminar caracteres especiales
    $result = [System.Text.RegularExpressions.Regex]::Replace($result, '[/\\:*?"<>|.]+', '')
    # Reemplazar espacios con guiones bajos
    $result = $result -replace ' ', '_'
    # Limitar a 100 caracteres
    if ($result.Length -gt 100) {
        $result = $result.Substring(0, 100)
    }

    return $result
}

# Inicializar
Write-Header "MIGRACIÓN DE COMPROBANTES DE PAGO"

$ComprobantesDir = Join-Path -Path $BaseDir -ChildPath "comprobantes"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFile = "migration_comprobantes_$Timestamp.log"
$BackupDir = Join-Path -Path $BaseDir -ChildPath "comprobantes_backup_$Timestamp"

# Validar directorio
if (-not (Test-Path -Path $ComprobantesDir)) {
    Write-Host "✗ Error: Directorio no encontrado: $ComprobantesDir" -ForegroundColor $Colors.Red
    exit 1
}

Write-Host "Configuración:" -ForegroundColor $Colors.Cyan
Write-Host "  Base Directory:     $BaseDir"
Write-Host "  Comprobantes Dir:   $ComprobantesDir"
Write-Host "  Dry Run:            $DryRun"
Write-Host "  Log File:           $LogFile"
if ($DryRun) {
    Write-Host "  ⚠ Modo SIMULACIÓN: No se realizarán cambios reales" -ForegroundColor $Colors.Yellow
}
Write-Host ""

Write-Log "═════════════════════════════════════════════════════════════" -Color $Colors.Cyan
Write-Log "Migración iniciada: $(Get-Date)" -Color $Colors.Cyan
Write-Log "═════════════════════════════════════════════════════════════" -Color $Colors.Cyan

# Contar archivos
$Files = Get-ChildItem -Path $ComprobantesDir -File -ErrorAction SilentlyContinue
$TotalFiles = $Files.Count

Write-Host "Archivos a procesar: " -ForegroundColor $Colors.Cyan -NoNewline
Write-Host "$TotalFiles" -ForegroundColor $Colors.Yellow

if ($TotalFiles -eq 0) {
    Write-Host "✓ No hay archivos para migrar" -ForegroundColor $Colors.Green
    exit 0
}

# Crear backup
if (-not $DryRun) {
    Write-Host "Creando backup..." -ForegroundColor $Colors.Yellow
    try {
        if (-not (Test-Path -Path $BackupDir)) {
            New-Item -ItemType Directory -Path $BackupDir | Out-Null
        }
        Copy-Item -Path "$ComprobantesDir\*" -Destination $BackupDir -Recurse -Force
        Write-Log "Backup creado en: $BackupDir" -Color $Colors.Green
        Write-Host "✓ Backup completado" -ForegroundColor $Colors.Green
    }
    catch {
        Write-Host "✗ Error al crear backup: $_" -ForegroundColor $Colors.Red
        exit 1
    }
}

# Procesar archivos
Write-Host "Procesando archivos..." -ForegroundColor $Colors.Cyan
$Processed = 0
$Failed = 0
$Skipped = 0

foreach ($File in $Files) {
    $FileName = $File.Name

    # Extraer tipo e ID del nombre del archivo usando regex
    if ($FileName -match '^(pago|premio)_(\d+)_') {
        $Tipo = $matches[1]
        $Id = $matches[2]

        # Nombre genérico para esta migración (en producción, consultar BD)
        $QuinielaName = "quinielas"
        $QuinielaDir = Join-Path -Path $ComprobantesDir -ChildPath (Sanitize-QuinielaName -Name $QuinielaName)

        # Crear directorio si no existe
        if (-not $DryRun) {
            if (-not (Test-Path -Path $QuinielaDir)) {
                New-Item -ItemType Directory -Path $QuinielaDir | Out-Null
            }
        }

        # Mover archivo
        if ($DryRun) {
            Write-Log "[DRY-RUN] Mover: $FileName → $(Split-Path $QuinielaDir -Leaf)/$FileName" -Color $Colors.Yellow
        }
        else {
            try {
                Move-Item -Path $File.FullName -Destination $QuinielaDir -Force
                Write-Log "[✓] Movido: $FileName → $(Split-Path $QuinielaDir -Leaf)/" -Color $Colors.Green
                $Processed++
            }
            catch {
                Write-Log "[✗] Error al mover: $FileName - $_" -Color $Colors.Red
                $Failed++
            }
        }
    }
    else {
        Write-Log "[⊘] Archivo no reconocido (omitido): $FileName" -Color $Colors.Yellow
        $Skipped++
    }
}

Write-Host ""
Write-Host "Resumen:" -ForegroundColor $Colors.Cyan
Write-Host "  Procesados:  $Processed"
Write-Host "  Omitidos:    $Skipped"
Write-Host "  Errores:     $Failed"

if ($DryRun) {
    Write-Host "⚠ Modo simulación - No se realizaron cambios reales" -ForegroundColor $Colors.Yellow
    Write-Host "Ejecute sin '-DryRun' o con '-DryRun `$false' para aplicar los cambios" -ForegroundColor $Colors.Yellow
}

Write-Log "═════════════════════════════════════════════════════════════" -Color $Colors.Cyan
Write-Log "Migración completada: $(Get-Date)" -Color $Colors.Cyan
Write-Log "Resumen: $Processed procesados, $Skipped omitidos, $Failed errores" -Color $Colors.Cyan
Write-Log "═════════════════════════════════════════════════════════════" -Color $Colors.Cyan

Write-Host ""
if ($Failed -eq 0) {
    Write-Host "✓ Migración completada exitosamente" -ForegroundColor $Colors.Green
}
else {
    Write-Host "✗ Migración completada con errores" -ForegroundColor $Colors.Red
}

Write-Host "Log guardado en: $LogFile" -ForegroundColor $Colors.Cyan

