# Script para convertir videos 16:9 a 9:16 (formato vertical/shorts)
# Rellena con blur del video original
# Requiere: ffmpeg

param(
    [Parameter(Mandatory=$false)]
    [string]$VideoInput = "",
    
    [Parameter(Mandatory=$false)]
    [string]$InputDir = "videos_a_convertir_shorts",
    
    [Parameter(Mandatory=$false)]
    [string]$OutputDir = "shorts_verticales"
)

# Colores para mensajes
$ColorInfo = "Cyan"
$ColorExito = "Green"
$ColorError = "Red"
$ColorAdvertencia = "Yellow"

Write-Host "=========================================" -ForegroundColor Magenta
Write-Host "  CONVERTIDOR 16:9 a 9:16 VERTICAL     " -ForegroundColor Magenta
Write-Host "=========================================" -ForegroundColor Magenta
Write-Host ""

# Funcion para verificar si ffmpeg esta instalado
function Test-FFmpeg {
    try {
        $null = ffmpeg -version 2>&1
        return $true
    }
    catch {
        return $false
    }
}

# Verificar dependencias
Write-Host "Verificando dependencias..." -ForegroundColor $ColorInfo

if (-not (Test-FFmpeg)) {
    Write-Host "ERROR: ffmpeg no esta instalado o no esta en el PATH" -ForegroundColor $ColorError
    Write-Host "Instala ffmpeg desde: https://ffmpeg.org/download.html" -ForegroundColor $ColorAdvertencia
    exit 1
}
Write-Host "[OK] ffmpeg encontrado" -ForegroundColor $ColorExito
Write-Host ""

# Solicitar video de entrada si no se proporciono
if ([string]::IsNullOrWhiteSpace($VideoInput)) {
    # Verificar si existe el directorio de entrada
    if (Test-Path $InputDir) {
        Write-Host "Videos disponibles en '$InputDir':" -ForegroundColor $ColorInfo
        $videos = Get-ChildItem -Path $InputDir -Filter *.mp4 -File
        if ($videos.Count -eq 0) {
            $videos = Get-ChildItem -Path $InputDir -File | Where-Object { $_.Extension -match '\.(mp4|mkv|avi|mov)$' }
        }
    }
    else {
        Write-Host "Videos disponibles en el directorio actual:" -ForegroundColor $ColorInfo
        $videos = Get-ChildItem -Path . -Filter *.mp4 -File
        if ($videos.Count -eq 0) {
            $videos = Get-ChildItem -Path . -File | Where-Object { $_.Extension -match '\.(mp4|mkv|avi|mov)$' }
        }
    }
    
    if ($videos.Count -eq 0) {
        Write-Host "No se encontraron videos" -ForegroundColor $ColorError
        Write-Host "Crea la carpeta '$InputDir' y coloca tus videos ahi" -ForegroundColor $ColorAdvertencia
        exit 1
    }
    
    for ($i = 0; $i -lt $videos.Count; $i++) {
        Write-Host "  [$($i+1)] $($videos[$i].Name)" -ForegroundColor $ColorAdvertencia
    }
    Write-Host "  [0] TODOS LOS VIDEOS" -ForegroundColor Green
    
    $seleccion = Read-Host "`nSelecciona el numero del video, 0 para todos, o escribe la ruta completa"
    
    if ($seleccion -eq "0") {
        # Procesar todos los videos
        Write-Host "`nProcesando TODOS los videos ($($videos.Count) archivos)..." -ForegroundColor $ColorExito
        
        foreach ($video in $videos) {
            Write-Host "`n=========================================" -ForegroundColor Cyan
            Write-Host "Procesando: $($video.Name)" -ForegroundColor Cyan
            Write-Host "=========================================

" -ForegroundColor Cyan
            
            & $PSCommandPath -VideoInput $video.FullName -InputDir $InputDir -OutputDir $OutputDir
        }
        
        Write-Host "`n=========================================" -ForegroundColor Magenta
        Write-Host "TODOS LOS VIDEOS PROCESADOS" -ForegroundColor Magenta
        Write-Host "=========================================" -ForegroundColor Magenta
        exit 0
    }
    elseif ($seleccion -match '^\d+$' -and [int]$seleccion -le $videos.Count -and [int]$seleccion -gt 0) {
        $VideoInput = $videos[[int]$seleccion - 1].FullName
    }
    else {
        $VideoInput = $seleccion
    }
}

# Verificar que el archivo existe
if (-not (Test-Path $VideoInput)) {
    Write-Host "ERROR: No se encuentra el archivo: $VideoInput" -ForegroundColor $ColorError
    exit 1
}

Write-Host "`nProcesando: $VideoInput" -ForegroundColor $ColorExito

# Crear directorio de salida
if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
    Write-Host "Directorio creado: $OutputDir" -ForegroundColor $ColorInfo
}

# Obtener nombre base del archivo
$nombreBase = [System.IO.Path]::GetFileNameWithoutExtension($VideoInput)
$videoFinal = Join-Path $OutputDir "${nombreBase}_vertical.mp4"

# Conversion a formato vertical 9:16
Write-Host ""
Write-Host "=========================================" -ForegroundColor Magenta
Write-Host "Conversion a 9:16 vertical" -ForegroundColor Magenta
Write-Host "=========================================" -ForegroundColor Magenta

Write-Host "Aplicando transformacion vertical..." -ForegroundColor $ColorExito
Write-Host "  - Fondo: blur del video original" -ForegroundColor $ColorInfo
Write-Host "  - Centro: video escalado" -ForegroundColor $ColorInfo
Write-Host "  - Formato: 1080x1920 (9:16)" -ForegroundColor $ColorInfo

# Filtro complejo para formato vertical
$filtroComplejo = "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black[v_centrado];[0:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,gblur=sigma=50[v_blur];[v_blur][v_centrado]overlay=(W-w)/2:(H-h)/2[v_out]"

# Detectar codec y generar video final
$codecsDisponibles = & ffmpeg -codecs 2>$null | Select-String "h264"
if ($codecsDisponibles -match "libx264") {
    Write-Host "  - Codec: H.264 (libx264)" -ForegroundColor $ColorExito
    ffmpeg -i "$VideoInput" -filter_complex "$filtroComplejo" -map "[v_out]" -map 0:a? -c:v libx264 -preset medium -crf 23 -c:a aac -b:a 128k -y "$videoFinal" 2>$null
}
elseif ($codecsDisponibles -match "h264_nvenc") {
    Write-Host "  - Codec: NVIDIA (h264_nvenc)" -ForegroundColor $ColorExito
    ffmpeg -i "$VideoInput" -filter_complex "$filtroComplejo" -map "[v_out]" -map 0:a? -c:v h264_nvenc -cq 23 -c:a aac -b:a 128k -y "$videoFinal" 2>$null
}
else {
    Write-Host "  - Codec: MPEG4" -ForegroundColor $ColorAdvertencia
    ffmpeg -i "$VideoInput" -filter_complex "$filtroComplejo" -map "[v_out]" -map 0:a? -c:v mpeg4 -q:v 3 -c:a aac -b:a 128k -y "$videoFinal" 2>$null
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Fallo la conversion" -ForegroundColor $ColorError
    exit 1
}

Write-Host "[OK] Video generado exitosamente" -ForegroundColor $ColorExito

# RESUMEN FINAL
Write-Host ""
Write-Host "=========================================" -ForegroundColor Magenta
Write-Host "         PROCESO COMPLETADO             " -ForegroundColor Magenta
Write-Host "=========================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "Archivo generado:" -ForegroundColor $ColorExito
Write-Host "  $videoFinal" -ForegroundColor White
Write-Host ""
Write-Host "Caracteristicas:" -ForegroundColor $ColorInfo
Write-Host "  - Formato: 1080x1920 (9:16 vertical)" -ForegroundColor White
Write-Host "  - Fondo: Blur del video original" -ForegroundColor White
Write-Host "  - Video: Centrado" -ForegroundColor White
Write-Host ""
