# Convertidor & Unificador de Audio/Vídeo

Aplicación de escritorio en **Java 21 + Swing** para convertir, concatenar, recortar, unir y eliminar silencios de archivos de audio y vídeo, utilizando **FFmpeg** como motor de procesamiento.

---

## Características

- Conversión de audio y vídeo (transcoding, remux, extracción de audio, mux, concatenación, normalización)
- **Unir vídeos** (`JOIN`): recodificación con `filter_complex` + `concat`, selección de pista de audio por archivo
- **Conversión en masa** de audio y vídeo: arrastra N archivos y se encolan como trabajos independientes
- Eliminación automática de silencios en vídeo y audio (detección en dos pasadas con `silencedetect`)
- **Modo rápido de eliminación de silencios** (copia de stream via concat demuxer con `inpoint`/`outpoint`, sin recodificación ni archivos temporales)
- Soporte para múltiples pistas de audio (p. ej. grabaciones de OBS con 7 pistas independientes)
- Recorte de vídeo (modo rápido por copia de stream o preciso por re-codificación)
- Generación de vídeo MP4 a partir de imagen estática + uno o varios archivos de audio
- **Presets de codificación** guardados en disco y cargables desde el panel de opciones
- **Historial de trabajos** persistido en JSON (últimas N ejecuciones configurables)
- Procesamiento por cola (los trabajos se ejecutan de uno en uno)
- Arrastrar y soltar archivos en la interfaz
- Barra de progreso en tiempo real y log con niveles INFO / WARN / ERROR
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
├── App.java                        ← Punto de entrada. Launcher con botones a cada módulo.
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
│       ├── PresetUseCase.java            ← Guardar / cargar / eliminar presets
│       ├── QueueManagementUseCase.java   ← Cola secuencial de trabajos + historial
│       └── VideoJoinUseCase.java         ← Valida y encola trabajos JOIN
├── domain/
│   └── model/
│       ├── MediaItem.java          ← Archivo + metadatos (duración, codecs…)
│       ├── Job.java                ← Trabajo: inputs + output + opciones + estado
│       ├── JobHistoryEntry.java    ← Entrada del historial de trabajos
│       ├── JobStatus.java          ← PENDING / RUNNING / SUCCESS / FAILED / CANCELED
│       ├── Operation.java          ← TRANSCODE, CONCAT, TRIM, JOIN, SILENCE_REMOVE, etc.
│       ├── MediaType.java          ← AUDIO / VIDEO
│       ├── Options.java            ← Interfaz marcadora de opciones
│       ├── AudioOptions.java       ← Parámetros de trabajos de audio
│       ├── VideoOptions.java       ← Parámetros de trabajos de vídeo
│       ├── JoinOptions.java        ← Parámetros JOIN (VideoOptions + pista de audio por input)
│       ├── SilenceRemoveOptions.java  ← fastCopy (modo rápido, default true) + codificación
│       ├── AudioStreamInfo.java    ← Información de una pista de audio (ffprobe)
│       └── Preset.java             ← Preset guardado (nombre + opciones)
├── infra/
│   ├── config/
│   │   ├── ConfigRepository.java      ← Persistencia JSON en %APPDATA%
│   │   ├── JobHistoryRepository.java  ← Historial de trabajos (jobs-history.json)
│   │   └── PresetRepository.java      ← Presets de usuario (presets.json)
│   └── ffmpeg/
│       ├── FfmpegLocator.java         ← Detección de binarios
│       ├── FfprobeService.java        ← Extracción de metadatos
│       ├── FfmpegCommandBuilder.java  ← Construcción de comandos FFmpeg (buildJoin, buildSilenceSegmentExtractCommand…)
│       ├── FfmpegRunner.java          ← Ejecución asíncrona del proceso
│       ├── ProgressParser.java        ← Parseo de -progress pipe:1
│       ├── SilenceDetectParser.java   ← Parseo de silencedetect stderr + computeKeepSegments
│       └── ProgressInfo.java          ← Snapshot de progreso
└── ui/
    ├── frame/
    │   ├── BaseMediaFrame.java     ← JFrame base con drag&drop, cola, log, progreso
    │   ├── AudioFrame.java         ← Módulo de audio (con soporte de presets)
    │   ├── VideoFrame.java         ← Módulo de vídeo (con soporte de presets)
    │   ├── BulkAudioFrame.java     ← Conversión de audio en masa
    │   ├── BulkVideoFrame.java     ← Conversión de vídeo en masa
    │   ├── VideoJoinFrame.java     ← Unificador de vídeos (JOIN)
    │   ├── TrimVideoFrame.java     ← Recortador de vídeo
    │   ├── SilenceVideoFrame.java  ← Eliminador de silencios de vídeo
    │   ├── SilenceAudioFrame.java  ← Eliminador de silencios de audio
    │   ├── FfmpegSetupDialog.java  ← Diálogo de configuración de FFmpeg
    │   └── SettingsDialog.java     ← Ajustes generales (rutas, directorio, historial)
    └── panel/
        ├── DropZonePanel.java          ← Zona de arrastre de archivos
        ├── InputListPanel.java         ← Tabla de archivos de entrada
        ├── AudioOptionsPanel.java      ← Opciones de audio (codec, bitrate, presets…)
        ├── VideoOptionsPanel.java      ← Opciones de vídeo (codec, CRF, presets…)
        ├── BulkAudioOptionsPanel.java  ← Opciones para conversión de audio en masa
        ├── BulkVideoOptionsPanel.java  ← Opciones para conversión de vídeo en masa
        ├── VideoJoinPanel.java         ← Tabla de archivos a unir con selección de pista de audio
        ├── SilenceRemovePanel.java     ← Opciones eliminación de silencios (vídeo)
        ├── SilenceAudioPanel.java      ← Opciones eliminación de silencios (audio)
        ├── TrimVideoPanel.java         ← Opciones de recorte de vídeo
        ├── OutputPanel.java            ← Ruta y nombre de salida
        ├── ProgressPanel.java          ← Barra de progreso y velocidad
        ├── QueuePanel.java             ← Tabla de cola de trabajos
        └── LogPanel.java               ← Visor de log (INFO/WARN/ERROR)
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

Incluye barra de **presets**: guarda y restaura configuraciones de codificación completas con un clic.

### 🎬 Módulo de Vídeo (`VideoFrame`)

Operaciones disponibles: Transcoding · Remux · Concatenar (demuxer o filter_complex) · Mux · Vertical 9:16 (con fondo desenfocado) · Recortar · Extraer audio

Incluye barra de **presets**: guarda y restaura configuraciones de codificación completas con un clic.

### 🔗 Unificador de Vídeos (`VideoJoinFrame`)

Une N vídeos en uno solo usando `filter_complex` con `concat`. Cada archivo puede tener una pista de audio diferente seleccionada individualmente. El panel `VideoJoinPanel` muestra una tabla con columnas de archivo, duración, info y un combo de pista de audio por fila.

- Recodificación completa con los parámetros de `VideoOptions` (codec, CRF, preset, bitrate de audio)
- Relleno automático de pistas de audio no especificadas con índice 0
- Si el codec es `copy`, hace fallback a `libx264` (la operación JOIN requiere recodificación)

### 📦 Conversión en Masa de Audio (`BulkAudioFrame`)

Arrastra N archivos de audio (mp3, aac, flac, wav, ogg, opus…); cada uno se encola como un trabajo independiente con las mismas opciones de codificación. La ruta de salida puede ser la misma carpeta del origen o un directorio fijo.

### 📦 Conversión en Masa de Vídeo (`BulkVideoFrame`)

Igual que el anterior pero para archivos de vídeo (mp4, mkv, webm, mov, avi…). Soporta sufijo personalizable para el nombre de salida.

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

> **Nota técnica — por qué se usaba antes `filter_complex` y por qué afectaba al progreso:**  
> El enfoque original construía un único grafo de filtros con `trim+atrim+setpts+concat` para todos los segmentos. Con 100–200 silencios típicos de una grabación de 1 h, FFmpeg tardaba varios minutos analizando ese grafo antes de emitir la primera línea de `-progress pipe:1`, lo que hacía que la barra de progreso pareciera congelada. El modo rápido elimina ese problema completamente.

### 🔇 Recortador de Silencios de Audio (`SilenceAudioFrame`)

Igual que el anterior pero para archivos de audio puro (MP3, AAC, FLAC, WAV, OGG, Opus). Siempre produce un archivo de audio; no contiene opciones de vídeo.

### ⚙ Ajustes (`SettingsDialog`)

Diálogo modal accesible desde el launcher. Permite configurar:
- Rutas a los binarios `ffmpeg` y `ffprobe`
- Directorio de salida por defecto
- Límite de entradas del historial de trabajos

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
| `JOIN` | Une N vídeos con recodificación vía `filter_complex` + `concat`, con pista de audio por input |

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
| `maxJobHistory` | Número máximo de entradas en el historial de trabajos (default 100) |

Archivos adicionales en el mismo directorio:

| Archivo | Descripción |
|---|---|
| `presets.json` | Presets de codificación definidos por el usuario |
| `jobs-history.json` | Historial de los últimos N trabajos ejecutados |

---

## Tests

```bash
mvn test
```

Actualmente **348 tests** (JUnit 5) cubren:

- Modelo de dominio (`Job`, `MediaItem`, `AudioOptions`, `VideoOptions`, `JoinOptions`, `AudioStreamInfo`, `Preset`, `SilenceRemoveOptions`, enums)
- Parseo de progreso (`ProgressParser`)
- Parseo de silencios (`SilenceDetectParser` — segmentos, notación científica, padding, edge cases, computeKeepSegments sin solapamiento)
- Construcción de comandos (`FfmpegCommandBuilder` — todas las operaciones incluyendo `buildJoin` y `buildSilenceSegmentExtractCommand`)
- Localización de FFmpeg (`FfmpegLocator`)
- Ejecución de FFmpeg (`FfmpegRunner` — callbacks, cancelación)
- Inspección de medios (`InspectMediaUseCase`, `FfprobeService`)
- Gestión de cola (`QueueManagementUseCase`)
- Gestión de presets (`PresetUseCase`)
- Unión de vídeos (`VideoJoinUseCase`)
- Persistencia de configuración (`ConfigRepository`, `JobHistoryRepository`, `PresetRepository`)
- Frames de la UI — smoke tests (`AudioFrame`, `VideoFrame`, `TrimVideoFrame`, `SilenceVideoFrame`, `SilenceAudioFrame`)
- Panels de la UI (`AudioOptionsPanel`, `VideoOptionsPanel`, `BulkAudioOptionsPanel`, `BulkVideoOptionsPanel`, `SilenceAudioPanel`, y otros)
- Eventos del bus (`EventBus`, `AppEventsRecords`)

---

## Changelog

### 2026-03-11 — Correcciones de bugs y cobertura de tests

#### Correcciones recientes en recorte de silencios (vídeo)

**Detección de silencios que no recortaba**

La fase 1 de `silencedetect` se estaba ejecutando con `-loglevel error`. Eso ocultaba las líneas `silence_start` / `silence_end` (se emiten a nivel `info`), provocando que no se detectaran silencios y que en muchos casos la salida quedase igual.

Solución: en `buildSilenceDetectCommand` se usa `-loglevel info` para capturar eventos de detección.

**Salida con pistas no seleccionadas**

En el camino de “sin silencio detectado” se hacía copia completa del archivo. Ahora se mapean explícitamente sólo vídeo + pistas seleccionadas (o sólo pistas seleccionadas en audio-only).

**Robustez del modo rápido en vídeo**

Si el modo rápido falla en vídeo, se hace fallback automático al modo preciso (recodificación) para no abortar el trabajo.

**Spinners de la UI que no aplicaban el valor recién tecleado**

Se añadió `commitEdit()` en `SilenceRemovePanel` y `SilenceAudioPanel` antes de construir opciones, para que umbral/minDur/padding se apliquen aunque el usuario no cambie foco.

#### Bugs corregidos

**Constructor chain bug en `AudioFrame` y `VideoFrame`**

El constructor de 3 argumentos `(config, queueUseCase, presetUseCase)` llama internamente a `this(config, queueUseCase)`, que a su vez invoca `super()` → `buildLayout()` → `createOptionsPanel()`. Esto significa que el `optionsPanel` se crea antes de que `presetUseCase` haya sido asignado, por lo que los presets nunca se conectaban al panel.

Solución: tras la llamada delegante `this(...)`, se llama explícitamente a `optionsPanel.setPresetUseCase(presetUseCase)` para cablear el caso de uso al panel ya creado.

```java
public AudioFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase,
                  PresetUseCase presetUseCase) {
    this(config, queueUseCase);           // crea optionsPanel
    this.presetUseCase = presetUseCase;
    if (presetUseCase != null && optionsPanel != null) {
        optionsPanel.setPresetUseCase(presetUseCase);  // ← wire explícito
    }
}
```

Mismo fix aplicado en `VideoFrame`.

**Renderer nulo en `presetCombo` de `AudioOptionsPanel` y `VideoOptionsPanel`**

El combo de presets incluye un ítem `null` como placeholder. FlatLaf llama al renderer de celda con `value = null` al pintar ese ítem, lo que producía una `NullPointerException` en el renderer por defecto.

Solución: `DefaultListCellRenderer` personalizado que muestra `"-- Preset --"` cuando `value == null`.

```java
presetCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
    @Override
    public java.awt.Component getListCellRendererComponent(
            javax.swing.JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(value == null ? "-- Preset --" : value.toString());
        return this;
    }
});
```

#### Tests añadidos

| Clase de test | Tests | Descripción |
|---|---|---|
| `SilenceDetectParserTest` | 14 | `feedLine()` con null, líneas no relacionadas, inicio-sin-fin, fin-sin-inicio, pares completos, notación científica, fin < inicio descartado; `computeKeepSegments()` con null/vacío, silencio en el medio/al principio/al final, segmentos de voz < 50 ms descartados |
| `JoinOptionsTest` | 6 | Constructor por defecto crea `VideoOptions`, `audioTrackPerInput` null por defecto, constructor con parámetros, setters, `getMediaType()` devuelve VIDEO, lista vacía aceptada |
| `AudioStreamInfoTest` | 9 | Constructor por defecto, constructor con 6 campos, setters, `toString()` con prefijo "Pista N" (base 1), codec/sampleRate, idioma en corchetes, sin corchetes cuando null/blank |
| `FfmpegCommandBuilderTest` | +9 (total 38) | Operación JOIN: 2 entradas → 2 flags `-i`; 3 entradas → `concat=n=3:v=1:a=1`; maps `[vout]` y `[aout]`; codec de `JoinOptions`; fallback de `copy` a `libx264`; track null → índice 0; output como último argumento; escala por defecto `1280:720`; índice de track de audio personalizado |
| `SilenceAudioPanelTest` | +1 (total 10) | Regresión: confirma que `buildOptions()` aplica valores tecleados en spinners sin necesidad de perder foco (`commitEdit`) |
| `SilenceRemovePanelTest` | 3 (nuevo) | Regresión UI: fallback de pista por defecto, commit de spinners y lectura correcta del estado de modo rápido |

---

## Dependencias

| Librería | Versión | Uso |
|---|---|---|
| `com.formdev:flatlaf` | 3.4 | Tema oscuro FlatDarkLaf para Swing |
| `com.fasterxml.jackson.core:jackson-databind` | 2.16.1 | Serialización JSON de la configuración |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | Tests unitarios (scope `test`) |
