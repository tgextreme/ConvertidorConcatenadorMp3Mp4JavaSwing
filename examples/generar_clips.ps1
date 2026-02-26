# Script para generar clips de video con ffmpeg
# Asegurate de tener ffmpeg instalado y en el PATH

# Configuracion
$videoInput = "Aventura Gentoo en directo — Resumen práctico (se hizo lo que se pudo).mp4"
$outputDir = "clips_generados"

# Crear directorio de salida si no existe
if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
    Write-Host "Directorio $outputDir creado" -ForegroundColor Green
}

# Funcion para generar clips
function Generate-Clip {
    param(
        [string]$InputFile,
        [string]$StartTime,
        [string]$EndTime,
        [string]$OutputName,
        [string]$Description
    )
    
    $outputPath = Join-Path $outputDir $OutputName
    Write-Host "`nGenerando: $Description" -ForegroundColor Cyan
    Write-Host "Desde $StartTime hasta $EndTime" -ForegroundColor Yellow
    
    ffmpeg -ss $StartTime -to $EndTime -i $InputFile -c copy $outputPath -y
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Clip generado: $OutputName" -ForegroundColor Green
    } else {
        Write-Host "Error al generar: $OutputName" -ForegroundColor Red
    }
}

Write-Host "=====================================" -ForegroundColor Magenta
Write-Host "   GENERADOR DE CLIPS CON FFMPEG    " -ForegroundColor Magenta
Write-Host "=====================================" -ForegroundColor Magenta
Write-Host ""

# Verificar que existe el video de entrada
if (!(Test-Path $videoInput)) {
    Write-Host "ERROR: No se encuentra el archivo $videoInput" -ForegroundColor Red
    Write-Host "Por favor renombra tu video o actualiza la variable videoInput en el script" -ForegroundColor Yellow
    exit 1
}

Write-Host "Video de entrada: $videoInput`n" -ForegroundColor Green

# CLIPS 1-3 minutos
Write-Host "`n=== GENERANDO CLIPS 1-3 minutos ===" -ForegroundColor Magenta

Generate-Clip -InputFile $videoInput -StartTime "00:01:25" -EndTime "00:03:15" -OutputName "clip01_kde_arreglado.mp4" -Description "Clip 1 - Como arregle KDE y arranco por fin"

Generate-Clip -InputFile $videoInput -StartTime "00:06:30" -EndTime "00:09:00" -OutputName "clip02_18h_gentoo_drivers.mp4" -Description "Clip 2 - 18 horas peleando con Gentoo y los drivers"

Generate-Clip -InputFile $videoInput -StartTime "00:08:40" -EndTime "00:11:20" -OutputName "clip03_gentoo_vs_arch.mp4" -Description "Clip 3 - Gentoo vs Arch diferencias y filosofia"

Generate-Clip -InputFile $videoInput -StartTime "00:25:10" -EndTime "00:28:10" -OutputName "clip04_problema_linux.mp4" -Description "Clip 4 - Que problema tiene Linux mi rant"

Generate-Clip -InputFile $videoInput -StartTime "00:35:30" -EndTime "00:38:10" -OutputName "clip05_especial_gentoo.mp4" -Description "Clip 5 - Que hace especial a Gentoo compilar para tu CPU"

Generate-Clip -InputFile $videoInput -StartTime "00:37:40" -EndTime "00:40:10" -OutputName "clip06_trayectoria_profesional.mp4" -Description "Clip 6 - Mi trayectoria profesional en el mundo TIC"

Generate-Clip -InputFile $videoInput -StartTime "00:49:25" -EndTime "00:52:10" -OutputName "clip07_ia_futuro_trabajo.mp4" -Description "Clip 7 - La IA general y el futuro del trabajo"

Generate-Clip -InputFile $videoInput -StartTime "00:53:20" -EndTime "00:56:40" -OutputName "clip08_cierre_directo.mp4" -Description "Clip 8 - Cierre del directo pendientes Firefox y proximos pasos"

# SHORTS 15-60 segundos
Write-Host "`n=== GENERANDO SHORTS 15-60 segundos ===" -ForegroundColor Magenta

Generate-Clip -InputFile $videoInput -StartTime "00:00:00" -EndTime "00:00:45" -OutputName "short01_intro_dramatica.mp4" -Description "Intro dramatica vamos a volver a esta pesadilla"

Generate-Clip -InputFile $videoInput -StartTime "00:01:32" -EndTime "00:02:15" -OutputName "short02_funciona_kde.mp4" -Description "Celebracion por fin funciona KDE en Gentoo"

Generate-Clip -InputFile $videoInput -StartTime "00:02:25" -EndTime "00:02:56" -OutputName "short03_saludo_luis.mp4" -Description "Clip social saludo a Luis en directo"

Generate-Clip -InputFile $videoInput -StartTime "00:06:50" -EndTime "00:07:10" -OutputName "short04_18h_gentoo.mp4" -Description "18 horas instalando Gentoo"

Generate-Clip -InputFile $videoInput -StartTime "00:06:00" -EndTime "00:06:41" -OutputName "short05_autotroleo.mp4" -Description "Autotroleo Soy un perdio con Gentoo"

Generate-Clip -InputFile $videoInput -StartTime "00:08:05" -EndTime "00:08:36" -OutputName "short06_conseguido_gentoo.mp4" -Description "Lo consegui Gentoo Gintu instalado"

Generate-Clip -InputFile $videoInput -StartTime "00:10:25" -EndTime "00:11:06" -OutputName "short07_especial_gentoo.mp4" -Description "Que hace especial a Gentoo paquetes binarios vs compilados"

Generate-Clip -InputFile $videoInput -StartTime "00:25:30" -EndTime "00:26:11" -OutputName "short08_problema_linux.mp4" -Description "Uno de los grandes problemas de Linux"

Generate-Clip -InputFile $videoInput -StartTime "00:35:30" -EndTime "00:36:09" -OutputName "short09_diferencia_gentoo.mp4" -Description "Diferencia clave de Gentoo frente a otras distros"

Generate-Clip -InputFile $videoInput -StartTime "00:37:40" -EndTime "00:38:31" -OutputName "short10_28_anos_tic.mp4" -Description "Mi experiencia 28 anos trabajando en el mundo TIC"

Generate-Clip -InputFile $videoInput -StartTime "00:49:30" -EndTime "00:50:21" -OutputName "short11_ia_general.mp4" -Description "Reflexion rapida la Inteligencia Artificial general"

Generate-Clip -InputFile $videoInput -StartTime "00:51:10" -EndTime "00:51:56" -OutputName "short12_resumen_directo.mp4" -Description "Comentando el resumen del directo"

# RESUMEN FINAL
Write-Host "`n=====================================" -ForegroundColor Magenta
Write-Host "         PROCESO COMPLETADO         " -ForegroundColor Magenta
Write-Host "=====================================" -ForegroundColor Magenta
Write-Host "`nTodos los clips se han generado en la carpeta: $outputDir" -ForegroundColor Green
Write-Host "`nEstadisticas:" -ForegroundColor Cyan
Write-Host "  - Clips largos 1-3 min: 8" -ForegroundColor Yellow
Write-Host "  - Shorts 15-60 seg: 12" -ForegroundColor Yellow
Write-Host "  - Total: 20 clips" -ForegroundColor Yellow
Write-Host ""
