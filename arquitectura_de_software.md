# Arquitectura de software — Multimedia Processing Suite

> **Proyecto:** ConvertidorUnificadorJavaSwing  
> **Stack:** Java 21 · Swing · FFmpeg/ffprobe externos  
> **Rama activa:** `ConvertidorUnificadorMp3Mp4Extra`

---

## 1. Visión del producto

Aplicación de escritorio en Java Swing que actúa como **suite multimedia modular**: convierte, une y limpia archivos de audio y vídeo usando FFmpeg como motor externo. El usuario arrastra archivos, elige opciones y destino, y la aplicación gestiona una cola de trabajos con progreso en tiempo real, logs y cancelación.

**Principio central:** Swing solo pinta la interfaz. Toda la lógica vive en use cases e infraestructura.

---

## 2. Estado actual del código

### ✅ Implementado

#### Dominio — `domain/model/`
`MediaItem`, `MediaType`, `Job`, `JobStatus`, `Operation`, `Options`, `AudioOptions`, `VideoOptions`, `SilenceRemoveOptions`, `Preset`, `AudioStreamInfo`

#### Infraestructura — `infra/`
`FfmpegLocator`, `FfmpegCommandBuilder`, `FfmpegRunner`, `FfprobeService`, `ProgressParser`, `ProgressInfo`, `SilenceDetectParser`, `ConfigRepository`

#### Aplicación — `app/`
`InspectMediaUseCase`, `QueueManagementUseCase`, `EventBus`, eventos base (5)

#### Interfaz — `ui/`
`App`, `BaseMediaFrame`, `AudioFrame`, `VideoFrame`, `SilenceAudioFrame`, `SilenceVideoFrame`, `TrimVideoFrame`, `FfmpegSetupDialog`  
`BulkAudioFrame`, `BulkVideoFrame`, `BulkAudioOptionsPanel`, `BulkVideoOptionsPanel`  
Panels: `DropZonePanel`, `InputListPanel`, `AudioOptionsPanel`, `VideoOptionsPanel`, `OutputPanel`, `QueuePanel`, `ProgressPanel`, `LogPanel`, `SilenceAudioPanel`, `SilenceRemovePanel`, `TrimVideoPanel`

---

## 3. Arquitectura objetivo (pragmática)

### Capas

```
┌─────────────────────────────────────────┐
│          Presentación (Swing)           │  ← Solo UI, nunca lógica FFmpeg
├─────────────────────────────────────────┤
│        Aplicación (Use Cases)           │  ← Orquesta, decide qué y en qué orden
├─────────────────────────────────────────┤
│          Dominio (Modelo)               │  ← Entidades, reglas de negocio, enums
├─────────────────────────────────────────┤
│        Infraestructura (Adapters)       │  ← FFprobe, FFmpeg, disco, config
└─────────────────────────────────────────┘
```

### Estructura de paquetes objetivo

```
tomas.gonzalez.ConvertidorUnificadorJavaSwing
├── App.java
├── app/
│   ├── event/          ✅ EventBus + eventos
│   └── usecase/        ✅ InspectMediaUseCase, QueueManagementUseCase
│                       + PresetUseCase (fase 1)
│                       + VideoJoinUseCase (fase 2)
├── domain/
│   └── model/          ✅ Job, MediaItem, Options, Preset, Enums, AudioStreamInfo
│                       + VideoStreamInfo (fase 2)
│                       + JoinPlan, JoinItem (fase 2)
├── infra/
│   ├── config/         ✅ ConfigRepository
│   └── ffmpeg/         ✅ Locator, Runner, Builder, Ffprobe, Progress, Silence
│                       + PresetRepository (fase 1)
│                       + JobHistoryRepository (fase 3)
└── ui/
    ├── frame/          ✅ todos los actuales
    │                   + VideoJoinFrame (fase 2)
    └── panel/          ✅ todos los actuales
                        + VideoJoinPanel (fase 2)
                        + SettingsPanel (fase 3)
```

---

## 4. Backlog real por fases

### Fase 1 — Presets de usuario

Lo más útil a corto plazo. El usuario guarda sus configuraciones favoritas con un nombre y las reutiliza.

- [ ] **`PresetRepository`** — guarda/carga `List<Preset>` como JSON en `%APPDATA%`. Usa `ConfigRepository` como base (ya tenemos la infraestructura de lectura/escritura JSON).
- [ ] **`PresetUseCase`** — save, load, delete, list. Muy simple, sin complejidad.
- [ ] **`PresetsCombo`** en `AudioOptionsPanel` y `VideoOptionsPanel` — combo "Cargar preset" + botón "Guardar como preset".
- [ ] **Presets por defecto** cargados en primer arranque (ver tabla sección 6).
- [ ] **Tests:** `PresetRepositoryTest` (lectura/escritura en `@TempDir`), `PresetUseCaseTest`.

**Por qué es viable:** `ConfigRepository` ya lee/escribe JSON. Solo hay que añadir un fichero `presets.json` separado. Sin dependencias nuevas.

---

### Fase 2 — Video Joiner

El módulo más valioso que falta. Une vídeos con selección de pista de audio por archivo.

#### Modelo

- [ ] **`VideoStreamInfo`** — resolución, codec, fps, duración. Análogo a `AudioStreamInfo` que ya existe.
- [ ] **`JoinItem`** — wraps `MediaItem` + índice de pista de audio seleccionada (int, default 0).
- [ ] **`JoinPlan`** — `List<JoinItem>` + `VideoOptions` de salida. Validable (`isValid()`): sin lista vacía, todas las pistas existen.

#### Infraestructura

- [ ] **Ampliar `FfprobeService`** — añadir `getVideoStreams(Path)` parseando los campos `width`, `height`, `r_frame_rate`, `codec_name` del stream de vídeo (misma llamada ffprobe que ya hace).
- [ ] **Ampliar `FfmpegCommandBuilder`** — método `buildJoin(JoinPlan)`: usa concat demuxer si todos los streams son `copy`, o `filter_complex concat` si hay reencode. Lógica análoga a `buildConcat` que ya existe.

#### Aplicación

- [ ] **`VideoJoinUseCase`** — recibe un `JoinPlan`, lo valida, construye el comando vía `FfmpegCommandBuilder.buildJoin()`, crea el `Job` y lo encola en `QueueManagementUseCase`. Sin lógica de ejecución propia.

#### Interfaz

- [ ] **`VideoJoinPanel`** — tabla con columnas: nombre de archivo, duración, resolución, codec, combo "Pista de audio" (poblado con los streams detectados por ffprobe). Drag & drop para reordenar filas.
- [ ] **`VideoJoinFrame`** — extiende `BaseMediaFrame`, usa `VideoJoinPanel` + `VideoOptionsPanel` + `OutputPanel` ya existentes.

**Ejemplo de fila en VideoJoinPanel:**
```
[clip01.mkv] [00:03:20] [1920×1080] [H.264] [Pista: #0 – Stereo ▼]
```

**Reglas de negocio simples:**
- Si todos los archivos tienen mismo codec, resolución y fps → ofrecer "concat directo sin recomprimir".
- Si no lo son → forzar reencode con `VideoOptions`.
- No encolar si algún archivo no tiene la pista de audio seleccionada.

**Por qué es viable:** `FfmpegCommandBuilder.buildConcat` ya tiene las dos estrategias. `FfprobeService` ya parsea streams de audio. Solo hay que extender lo que existe.

---

### Fase 3 — Calidad de vida y persistencia

Pequeñas mejoras de alto impacto, sin reestructurar nada.

- [ ] **Botón "Copiar comando FFmpeg"** en `LogPanel` — copia al portapapeles el comando ejecutado. Útil para depurar o reproducir en terminal.
- [ ] **Carpeta de salida por defecto** persistida — `ConfigRepository` ya tiene `AppConfig`, solo añadir un campo `defaultOutputDir`.
- [ ] **Archivos recientes** — lista de las últimas rutas usadas como sugerencia en `DropZonePanel` o `InputListPanel`.
- [ ] **`JobHistoryRepository`** — guarda en JSON las últimas N conversiones: archivo de entrada, salida, duración, resultado (OK/ERROR). Se muestra en la cola como historial.
- [ ] **`SettingsPanel`** — ventana de ajustes: rutas ffmpeg/ffprobe (movidas desde `FfmpegSetupDialog`), carpeta de salida por defecto, tamaño del historial.
- [ ] **Tests:** `JobHistoryRepositoryTest`.

---

### Fase 4 — Ventana principal unificada (opcional)

Actualmente la app lanza ventanas flotantes desde un menú. Si crece mucho, un `MainFrame` con pestañas tendría sentido, pero **no es prioritario** — el modelo actual de ventanas independientes funciona bien para una herramienta de uso puntual.

- [ ] `MainFrame` con `JTabbedPane` o panel de navegación lateral.
- [ ] Cards para: Audio / Vídeo / Conversión en masa / Silencios / Unir vídeos / Ajustes.
- [ ] Mover el lanzador actual (`App.java`) a este frame.

---

### Fase 5 — Nice-to-have (no prioritario)

- [ ] Thumbnails de vídeo en `InputListPanel` (ffmpeg -ss 0 -frames:v 1 → imagen temporal).
- [ ] Paralelismo ajustable (N workers simultáneos en cola).
- [ ] Watch folder (carpeta vigilada → jobs automáticos al detectar archivos nuevos).
- [ ] Watermark / overlay de texto o imagen sobre vídeo.
- [ ] Módulo PDF (separado, usa PDFBox — sin relación con el core multimedia).

---

## 5. Notas técnicas críticas

### FFmpeg concat
Para remux sin reencode, todos los inputs deben ser compatibles (mismo codec, resolución y fps). Si no lo son, usar `filter_complex concat` con reencode. Las dos estrategias ya están en `buildConcatDemuxer` y `buildConcatFilterComplex`.

### Construcción de comandos
Nunca construir comandos FFmpeg como `String`. Siempre usar `FfmpegCommandBuilder`, que genera `List<String>` para `ProcessBuilder`. Evita inyección de argumentos por rutas con espacios o caracteres especiales.

### Selección de pista de audio en Video Joiner
Usar siempre el `stream_index` real devuelto por ffprobe, no el índice relativo de pista de audio. Un archivo puede tener streams de subtítulos o datos entre las pistas de audio.

### Threading
- **EDT**: solo UI.
- **`metadataExecutor`**: llamadas a ffprobe (sin bloquear la UI).
- **`jobExecutor`**: procesos FFmpeg (ya gestionado por `QueueManagementUseCase`).
- Toda actualización de UI desde hilos externos: `SwingUtilities.invokeLater()`.

### Logs
El `LogPanel` actual ya muestra stdout/stderr en tiempo real. Para el botón "Copiar comando", `FfmpegRunner` tiene acceso al comando — solo hay que exponerlo en el `JobLogEvent` o en el `Job`.

---

## 6. Presets por defecto

### Audio

| Nombre | Codec | Bitrate |
|---|---|---|
| MP3 128 kbps | libmp3lame | 128 kbps |
| MP3 192 kbps | libmp3lame | 192 kbps |
| MP3 320 kbps | libmp3lame | 320 kbps |
| OGG calidad media | libvorbis | q5 |
| Opus alta fidelidad | libopus | 192 kbps |
| FLAC sin pérdida | flac | — |

### Vídeo

| Nombre | Contenedor | Codec vídeo | Codec audio |
|---|---|---|---|
| MP4 H.264 estándar | mp4 | libx264 CRF 23 | aac 192k |
| MP4 H.265 compacto | mp4 | libx265 CRF 28 | aac 192k |
| WEBM VP9 web | webm | libvpx-vp9 CRF 30 | libopus 128k |
| MKV copia de audio | mkv | libx264 CRF 23 | copy |
| Perfil móvil 720p | mp4 | libx264 CRF 25 | aac 128k |
| Remux sin recomprimir | mkv | copy | copy |
