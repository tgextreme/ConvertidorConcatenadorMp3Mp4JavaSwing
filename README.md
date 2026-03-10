# Convertidor & Unificador de Audio/Vídeo

Aplicación de escritorio en **Java 21 + Swing** para convertir, concatenar, recortar y eliminar silencios de archivos de audio y vídeo, utilizando **FFmpeg** como motor de procesamiento.

---

## Características

- Conversión de audio y vídeo (transcoding, remux, extracción de audio, mux, concatenación, normalización)
- Eliminación automática de silencios en vídeo y audio (detección en dos pasadas con `silencedetect`)
- **Modo rápido de eliminación de silencios** (copia de stream via concat demuxer con `inpoint`/`outpoint`, sin recodificación ni archivos temporales)
- Soporte para múltiples pistas de audio (p. ej. grabaciones de OBS con 7 pistas independientes)
- Recorte de vídeo (modo rápido por copia de stream o preciso por re-codificación)
- Generación de vídeo MP4 a partir de imagen estática + uno o varios archivos de audio
- Procesamiento por cola (los trabajos se ejecutan de uno en uno)
- Arrastrar y soltar archivos en la interfaz
- Barra de progreso en tiempo real y log con niveles INFO / WARN / ERROR
- Presets guardados en disco
- Empaquetado como JAR ejecutable autocontenido (fat JAR con Maven Shade)

---

## Requisitos

| Componente | Versión mínima |
|---|---|
| Java JDK | 21 |
| Maven | 3.9 (solo para compilar) |
| FFmpeg | 5.x o superior |
| FFprobe | incluido con FFmpeg |

> La aplicación detecta automáticamente `ffmpeg` y `ffprobe` en `./bin/`, en el `PATH` del sistema o en los valores guardados en la configuración. Si no los encuentra, muestra un diálogo de configuración.

---

## Compilar y ejecutar

```bash
# Compilar y empaquetar
mvn package -DskipTests

# Ejecutar el fat JAR
java -jar target/ConvertidorUnificadorJavaSwing-0.0.1-SNAPSHOT.jar
```

También se puede lanzar desde el IDE ejecutando `App.main()`.

### Binarios de FFmpeg junto al JAR

Si colocas `ffmpeg.exe` y `ffprobe.exe` en la carpeta `bin/`, la aplicación los usará automáticamente sin necesidad de que estén en el `PATH`.

---

## Estructura del proyecto

```
src/main/java/tomas/gonzalez/ConvertidorUnificadorJavaSwing/
├── App.java                        ← Punto de entrada. Lanzador de módulos.
├── app/
│   ├── event/                      ← Eventos de dominio (EventBus pub/sub)
│   │   ├── AppEvent.java           ← Interfaz marcadora
│   │   ├── EventBus.java           ← Bus singleton (despacho en EDT)
│   │   ├── JobLogEvent.java        ← Línea de log de FFmpeg
│   │   ├── JobProgressEvent.java   ← Actualización de progreso
│   │   ├── JobQueuedEvent.java     ← Trabajo añadido a la cola
│   │   ├── JobStatusChangedEvent.java
│   │   └── MediaInspectedEvent.java
│   └── usecase/
│       ├── InspectMediaUseCase.java      ← Ejecuta ffprobe en background
│       └── QueueManagementUseCase.java   ← Cola secuencial de trabajos
├── domain/
│   └── model/
│       ├── MediaItem.java          ← Archivo + metadatos (duración, codecs…)
│       ├── Job.java                ← Trabajo: inputs + output + opciones + estado
│       ├── JobStatus.java          ← PENDING / RUNNING / SUCCESS / FAILED / CANCELED
│       ├── Operation.java          ← TRANSCODE, CONCAT, TRIM, SILENCE_REMOVE, etc.
│       ├── MediaType.java          ← AUDIO / VIDEO
│       ├── Options.java            ← Interfaz marcadora de opciones
│       ├── AudioOptions.java       ← Parámetros de trabajos de audio
│       ├── VideoOptions.java       ← Parámetros de trabajos de vídeo
│       ├── SilenceRemoveOptions.java  ← fastCopy (modo rápido, default true) + parámetros de codificación
│       ├── AudioStreamInfo.java    ← Información de una pista de audio (ffprobe)
│       └── Preset.java             ← Preset guardado (nombre + opciones)
├── infra/
│   ├── config/
│   │   └── ConfigRepository.java  ← Persistencia JSON en %APPDATA%
│   └── ffmpeg/
│       ├── FfmpegLocator.java      ← Detección de binarios
│       ├── FfprobeService.java     ← Extracción de metadatos
        ├── FfmpegCommandBuilder.java  ← Construcción de comandos FFmpeg (incluye buildSilenceSegmentExtractCommand)
        ├── FfmpegRunner.java           ← Ejecución asíncrona del proceso
        ├── ProgressParser.java         ← Parseo de -progress pipe:1
        ├── SilenceDetectParser.java    ← Parseo de silencedetect stderr + computeKeepSegments (sin solapamiento)
        └── ProgressInfo.java           ← Snapshot de progreso
└── ui/
    ├── frame/
    │   ├── BaseMediaFrame.java     ← JFrame base con drag&drop, cola, log, progreso
    │   ├── AudioFrame.java         ← Módulo de audio
    │   ├── VideoFrame.java         ← Módulo de vídeo
    │   ├── TrimVideoFrame.java     ← Recortador de vídeo
    │   ├── SilenceVideoFrame.java  ← Eliminador de silencios de vídeo
    │   ├── SilenceAudioFrame.java  ← Eliminador de silencios de audio
    │   └── FfmpegSetupDialog.java  ← Diálogo de configuración de FFmpeg
    └── panel/
        ├── DropZonePanel.java      ← Zona de arrastre de archivos
        ├── InputListPanel.java     ← Tabla de archivos de entrada
        ├── AudioOptionsPanel.java  ← Opciones de audio (codec, bitrate…)
        ├── VideoOptionsPanel.java  ← Opciones de vídeo (codec, CRF…)
        ├── SilenceRemovePanel.java ← Opciones de eliminación de silencios (vídeo)
        ├── SilenceAudioPanel.java  ← Opciones de eliminación de silencios (audio)
        ├── TrimVideoPanel.java     ← Opciones de recorte de vídeo
        ├── OutputPanel.java        ← Ruta y nombre de salida
        ├── ProgressPanel.java      ← Barra de progreso y velocidad
        ├── QueuePanel.java         ← Tabla de cola de trabajos
        └── LogPanel.java           ← Visor de log (INFO/WARN/ERROR)
```

---

## Arquitectura

El proyecto sigue una arquitectura en capas inspirada en **Clean Architecture / Hexagonal**:

```
App
 └── ui (Swing)          → presenta la información y captura eventos de usuario
      └── app            → casos de uso y eventos de dominio
           ├── domain    → modelo puro (Job, MediaItem, Options, enums…)
           └── infra     → integración con FFmpeg y persistencia de config
```

### Comunicación entre capas

- La UI **no llama directamente** a la infraestructura: delega en los casos de uso.
- Los resultados se propagan mediante el **EventBus** (publicación/suscripción sincrónica, despacho en EDT).
- La UI solo escucha eventos: `JobStatusChangedEvent`, `JobProgressEvent`, `JobLogEvent`, `MediaInspectedEvent`.

### Concurrencia

| Componente | Pool de hilos |
|---|---|
| `InspectMediaUseCase` | Pool fijo de 3 hilos (`ffprobe-worker`) |
| `QueueManagementUseCase` | Un solo hilo secuencial (`queue-dispatcher`) |
| `FfmpegRunner` | Pool caché para drenado de stdout/stderr |

El EDT de Swing nunca queda bloqueado.

---

## Módulos de la interfaz

### 🎵 Módulo de Audio (`AudioFrame`)

Operaciones disponibles: Transcoding · Remux · Extraer audio · Concatenar · Mux (audio+vídeo) · Recortar · Normalizar (loudnorm) · Audio + Imagen → MP4

### 🎬 Módulo de Vídeo (`VideoFrame`)

Operaciones disponibles: Transcoding · Remux · Concatenar (demuxer o filter_complex) · Mux · Vertical 9:16 (con fondo desenfocado) · Recortar · Extraer audio

### ✂ Recortador de Vídeo (`TrimVideoFrame`)

Recorte por tiempo de inicio y fin. Modo **rápido** (copia de stream sin recodificación) o **preciso** (recodificación con parámetros configurables).

### 🔇 Recortador de Silencios de Vídeo (`SilenceVideoFrame`)

Eliminación automática de segmentos silenciosos en vídeo. Compatible con grabaciones OBS multitrack (hasta 7 pistas). Proceso en dos pasadas:

1. **Fase 1 — detección** (`silencedetect`): analiza el audio de la(s) pista(s) seleccionadas (con mezcla `amix` si hay varias) y extrae los timestamps de silencio. Solo se procesa la pista elegida; las demás no se tocan.
2. **Fase 2 — recorte y unión**: dos estrategias seleccionables en la UI:

   | Modo | Descripción | Velocidad |
   |---|---|---|
   | **Modo rápido** _(activo por defecto)_ | Un solo proceso FFmpeg con el concat demuxer y directivas `inpoint`/`outpoint`. Copia de stream sin recodificación. Cortes exactos a nivel de demuxer. | ⚡ muy rápido |
   | **Modo preciso** | `filter_complex` con `trim`/`atrim`/`concat`. Recodificación completa, cortes a nivel de frame. Más lento pero sin artefactos de keyframe. | 🐢 lento |

Con el modo rápido **activo**, las opciones de codec/CRF/preset/bitrate se ocultan (no aplican). Al desactivarlo se muestran.

Parámetros configurables: pistas de detección, umbral de silencio (dB), duración mínima (s), relleno/padding, modo rápido/preciso, codec de vídeo y audio, CRF, preset, bitrate de audio, contenedor de salida.

**Valores por defecto para grabaciones OBS:** FPS 60, bitrate 3000 kbps, preset `medium`, resolución automática.

> **Nota técnica — por qué se usaba antes `filter_complex` y por qué afectaba al progreso:**  
> El enfoque original construía un único grafo de filtros con `trim+atrim+setpts+concat` para todos los segmentos. Con 100–200 silencios típicos de una grabación de 1 h, FFmpeg tardaba varios minutos analizando ese grafo antes de emitir la primera línea de `-progress pipe:1`, lo que hacía que la barra de progreso pareciera congelada. El modo rápido elimina ese problema completamente.

### 🔇 Recortador de Silencios de Audio (`SilenceAudioFrame`)

Igual que el anterior pero para archivos de audio puro (MP3, AAC, FLAC, WAV, OGG, Opus). Siempre produce un archivo de audio; no contiene opciones de vídeo.

---

## Operaciones FFmpeg soportadas

| Operación | Descripción |
|---|---|
| `TRANSCODE` | Recodificación completa de audio o vídeo |
| `REMUX` | Cambio de contenedor sin recodificación (`-c copy`) |
| `EXTRACT_AUDIO` | Extrae la pista de audio de un vídeo |
| `CONCAT` | Concatena N archivos (demuxer para copy; filter_complex para recodificación) |
| `MUX` | Combina un vídeo y un audio en un único contenedor |
| `TRIM` | Recorte por rango de tiempo |
| `NORMALIZE` | Normalización de volumen con `loudnorm` |
| `AUDIO_TO_VIDEO` | Imagen estática + audio → MP4 |
| `SILENCE_REMOVE` | Eliminación de silencios en dos pasadas |

---

## Configuración

La configuración se persiste en JSON en:

- **Windows**: `%APPDATA%\ConvertidorAVTool\config.json`
- **Linux/macOS**: `~/.ConvertidorAVTool/config.json`

Campos relevantes:

| Campo | Descripción |
|---|---|
| `ffmpegPath` | Ruta al binario `ffmpeg` |
| `ffprobePath` | Ruta al binario `ffprobe` |
| `defaultOutputDir` | Directorio de salida por defecto |
| `overwriteByDefault` | Si sobreescribir archivos existentes sin preguntar |
| `lastAudioContainer` | Último formato de audio usado |
| `lastVideoContainer` | Último formato de vídeo usado |

---

## Tests

```bash
mvn test
```

Actualmente **251 tests** (JUnit 5) cubren:

- Modelo de dominio (`Job`, `MediaItem`, `AudioOptions`, `VideoOptions`, `Preset`, enums)
- Parseo de progreso (`ProgressParser`)
- Parseo de silencios (`SilenceDetectParser` — segmentos, padding, edge cases, no-solapamiento de keep segments)
- Construcción de comandos (`FfmpegCommandBuilder` — todas las operaciones, incluyendo `buildSilenceSegmentExtractCommand`)
- Localización de FFmpeg (`FfmpegLocator`)
- Ejecución de FFmpeg (`FfmpegRunner` — callbacks, cancelación)
- Inspección de medios (`InspectMediaUseCase`, `FfprobeService`)
- Gestión de cola (`QueueManagementUseCase`)
- Persistencia de configuración (`ConfigRepository`)
- Frames de la UI — smoke tests (`AudioFrame`, `VideoFrame`, `TrimVideoFrame`, `SilenceVideoFrame`, `SilenceAudioFrame`)
- Eventos del bus (`EventBus`, `AppEventsRecords`)

---

## Dependencias

| Librería | Versión | Uso |
|---|---|---|
| `com.formdev:flatlaf` | 3.4 | Tema oscuro FlatDarkLaf para Swing |
| `com.fasterxml.jackson.core:jackson-databind` | 2.16.1 | Serialización JSON de la configuración |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | Tests unitarios (scope `test`) |
