# Convertidor & Unificador de Audio/Vídeo

---

## Título (itch.io display title)

**Convertidor & Unificador de Audio/Vídeo**

---

## Descripción corta *(tagline — máx. ~160 caracteres)*

Herramienta de escritorio para convertir, recortar, unir y eliminar silencios de audio y vídeo usando FFmpeg como motor. Sin límites, sin suscripción.

---

## Descripción larga

¿Tienes grabaciones de OBS con silencios eternos? ¿Necesitas unir clips, cambiar el contenedor, extraer el audio o convertir en masa sin tocar la línea de comandos?

**Convertidor & Unificador de Audio/Vídeo** es una aplicación de escritorio gratuita que pone todos los comandos de FFmpeg al alcance de un clic. Interfaz moderna en Java/Swing con tema oscuro, cola de trabajos, barra de progreso en tiempo real y log detallado por cada tarea.

### ¿Qué puedes hacer?

#### 🎵 Audio
- Transcoding (MP3, AAC, FLAC, WAV, OGG, Opus…)
- Remux (cambio de contenedor sin recodificación)
- Extraer audio de un vídeo
- Concatenar varios archivos de audio
- Mux (combinar audio + vídeo)
- Recortar por tiempo de inicio/fin
- Normalización de volumen (`loudnorm`)
- Imagen estática + audio → MP4
- Conversión en masa: arrastra N archivos y se encolan como trabajos independientes

#### 🎬 Vídeo
- Transcoding y remux
- Concatenar con concat demuxer o `filter_complex`
- Mux de audio y vídeo
- Formato vertical 9:16 con fondo desenfocado
- Recorte rápido (copia de stream) o preciso (recodificación)
- Extraer la pista de audio
- Conversión en masa con sufijo personalizable

#### 🔗 Unificador de vídeos (JOIN)
Une N vídeos en uno solo con recodificación completa. Cada clip puede tener una pista de audio diferente seleccionada individualmente — ideal para grabaciones OBS multitrack.

#### 🔇 Eliminador de silencios (vídeo y audio)
Detecta y elimina automáticamente los segmentos silenciosos. Soporta grabaciones con hasta 7 pistas de audio independientes (OBS).

**Modo rápido** *(activo por defecto)*: copia de stream via concat demuxer con `inpoint`/`outpoint`. Sin recodificación, sin archivos temporales. Velocidad máxima.

**Modo preciso**: recodificación completa con `filter_complex trim/concat`. Cortes exactos a nivel de frame.

Parámetros configurables: umbral de silencio (dB), duración mínima (s), relleno/padding, pistas de detección, codec de salida, CRF, bitrate.

> Si tienes una grabación de 1 hora con 150 silencios, el modo rápido la procesa en segundos. El modo preciso tarda minutos pero evita artefactos de keyframe.

#### ⚙ Ajustes y calidad de vida
- **Presets de codificación**: guarda y carga configuraciones completas con un clic
- **Historial de trabajos** persistido en JSON (últimas N ejecuciones)
- Cola secuencial: los trabajos se ejecutan de uno en uno sin bloquear la UI
- Arrastrar y soltar archivos en todos los módulos
- Detección automática de `ffmpeg`/`ffprobe` en `./bin/`, en el PATH o en la configuración guardada
- Configuración persistida en `%APPDATA%\ConvertidorAVTool\`
- Multimonitor: cada módulo abre en la misma pantalla que el launcher

---

## Instalación

### Opción A — Portable (ZIP, sin instalador)

1. Descarga `ConvertidorUnificadorJavaSwing-portable.zip`
2. Extrae la carpeta donde quieras
3. Dentro encontrarás:
   - `ConvertidorUnificadorJavaSwing.exe` — ejecutable
   - `jre/` — Java 21 embebido (no necesitas instalar Java)
   - `bin/` — coloca aquí `ffmpeg.exe` y `ffprobe.exe` si quieres usarlos localmente
4. Ejecuta el `.exe`

### Opción B — Instalador (`.exe`)

1. Descarga `ConvertidorUnificadorJavaSwing-setup.exe`
2. Ejecuta el instalador y sigue los pasos (elige carpeta de instalación, acceso directo, etc.)
3. El instalador incluye el JRE embebido — no necesitas Java instalado

### FFmpeg (requerido para usar la aplicación)

La aplicación necesita `ffmpeg` y `ffprobe` para funcionar. Los puedes obtener de:
- [https://ffmpeg.org/download.html](https://ffmpeg.org/download.html) (builds oficiales)
- [https://www.gyan.dev/ffmpeg/builds/](https://www.gyan.dev/ffmpeg/builds/) (builds para Windows)

Coloca `ffmpeg.exe` y `ffprobe.exe` en la carpeta `bin/` junto al ejecutable, o en cualquier carpeta del PATH del sistema. Si la aplicación no los encuentra al arrancar, abre automáticamente el diálogo de configuración para indicar las rutas manualmente.

### Opción C — Ejecutar desde el JAR (requiere Java 21+)

```bash
java -jar ConvertidorUnificadorJavaSwing-0.0.1-SNAPSHOT.jar
```

---

## Requisitos del sistema

| Componente | Requisito |
|---|---|
| Sistema operativo | Windows 10/11 (64-bit) |
| Java | 21 (incluido en las versiones portable/instalador) |
| FFmpeg | 5.x o superior (no incluido) |
| RAM | 256 MB o más |
| Espacio en disco | ~150 MB (incluye JRE embebido) |

---

## Capturas de pantalla *(sugerencia de orden)*

1. Launcher principal con los botones de módulos
2. Módulo de Audio en operación Transcoding con presets
3. Módulo de Eliminación de silencios de vídeo (modo rápido activo)
4. Unificador de vídeos (JOIN) con tabla de pistas de audio por clip
5. Cola de trabajos con barra de progreso y log en tiempo real
6. Diálogo de Ajustes

---

## Tags sugeridos para itch.io

`tools` · `video` · `audio` · `ffmpeg` · `converter` · `silence-removal` · `windows` · `java` · `desktop` · `free`

---

## Categoría itch.io

**Tools** → *Other tools*

---

## Licencia / Precio

- Precio: **Gratis**
- Código fuente disponible en GitHub

---

## Notas de versión (changelog resumido)

### v0.0.1-SNAPSHOT (2026-03-11)
- Corrección crítica: la detección de silencios no funcionaba en vídeo (`-loglevel error` ocultaba los eventos `silence_start`/`silence_end`)
- Fallback automático de modo rápido a modo preciso si el concat demuxer falla en vídeo
- Corrección de spinners que no aplicaban el valor tecleado si el campo no perdía el foco
- Ventanas secundarias se abren en el mismo monitor que el launcher principal
- Mejoras de microcopia en paneles (botones, tooltips, etiquetas)
