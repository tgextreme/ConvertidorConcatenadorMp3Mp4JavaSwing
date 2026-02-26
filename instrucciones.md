# Arquitectura de software (Java Swing + FFmpeg)

## Objetivo

Aplicación de escritorio que **unifica** (p. ej. concatenar/mezclar/“merge” según el caso) y **convierte** archivos **de audio y vídeo** usando **FFmpeg**. El usuario puede **arrastrar y soltar** archivos en la UI, elegir **destino y formato de salida**, y ejecutar trabajos con **progreso, logs y cola**.

Requisito UI: **2 JFrames**

* `AudioFrame`: operaciones de audio.
* `VideoFrame`: operaciones de vídeo.

La lógica de negocio debe ser **común** y reutilizable en ambos.

---

## Principios de diseño

* **Clean Architecture / Hexagonal**: UI Swing desacoplada del core.
* **Trabajo asíncrono**: nunca bloquear EDT (Event Dispatch Thread).
* **FFmpeg como dependencia externa**: ejecución por `ProcessBuilder`, parseo de progreso desde `-progress pipe:1`.
* **Jobs reproducibles**: cada conversión es un “Job” serializable con parámetros.
* **Extensible por presets**: perfiles guardados y reusables.

---

## Capas y responsabilidades

### 1) Presentación (Swing)

**Responsable**: interacción usuario, validación superficial, binding de eventos, render de estado.

* `AudioFrame` y `VideoFrame` reutilizan paneles y controladores compartidos.
* Drag&drop: `TransferHandler` / `DropTarget`.
* Visuales: lista de entradas, panel de salida, presets, cola, progreso y logs.

### 2) Aplicación (Use Cases)

**Responsable**: orquestar casos de uso y coordinar servicios.

* `CreateJobUseCase` (crear job desde selección/drag-drop)
* `ValidateInputsUseCase`
* `RunJobUseCase`
* `CancelJobUseCase` / `PauseJobUseCase` (pausa solo si implementas estrategia por segmentación; FFmpeg no “pausa” nativa)
* `QueueManagementUseCase`
* `PresetManagementUseCase`

### 3) Dominio (Modelo)

**Responsable**: entidades y reglas.

* `MediaItem` (archivo + metadatos)
* `MediaType` (AUDIO/VIDEO)
* `Job` (tipo de operación + inputs + output + opciones)
* `JobStatus` (PENDING/RUNNING/SUCCESS/FAILED/CANCELED)
* `Preset` (perfil)
* `Operation` (TRANSCODE, CONCAT, EXTRACT_AUDIO, MUX, REMUX, TRIM, NORMALIZE, etc.)

### 4) Infraestructura

**Responsable**: integración con SO y binarios.

* `FfmpegLocator` (resolver ruta a ffmpeg/ffprobe)
* `FfmpegRunner` (ejecutar procesos)
* `FfprobeService` (lectura metadatos)
* `ProgressParser` (parsear salida `-progress`)
* `ConfigRepository` (guardar JSON/YAML en `~/.appname/`)
* `RecentFilesRepository`
* `Logger` (archivo + UI)

---

## Estructura de paquetes (propuesta)

```
com.tuapp.avtool
  ├─ app
  │   ├─ usecase
  │   ├─ service
  │   └─ di
  ├─ domain
  │   ├─ model
  │   ├─ rules
  │   └─ preset
  ├─ infra
  │   ├─ ffmpeg
  │   ├─ config
  │   ├─ io
  │   └─ logging
  └─ ui
      ├─ frame
      ├─ panel
      ├─ controller
      ├─ dnd
      └─ binding
```

---

## Componentes clave

### A) UI (2 JFrames)

#### `AudioFrame`

Paneles recomendados:

* `DropZonePanel` (arrastrar/soltar)
* `InputListPanel` (lista con reorder, remove)
* `AudioOptionsPanel` (formato, codec, bitrate, sample rate, canales, normalización)
* `OutputPanel` (ruta destino + nombre + overwrite)
* `QueuePanel` (jobs pendientes)
* `ProgressPanel` (barra + ETA)
* `LogPanel` (texto con niveles)

#### `VideoFrame`

Paneles recomendados:

* `DropZonePanel`
* `InputListPanel`
* `VideoOptionsPanel` (container, codec, CRF/bitrate, fps, resolución, preset, audio codec)
* `OutputPanel`
* `QueuePanel`
* `ProgressPanel`
* `LogPanel`

**Nota:** para compartir UI, crea un `BaseMediaFrame` que solo cambia el `OptionsPanel`.

---

### B) Controladores (Presentation → UseCases)

* `AudioController`
* `VideoController`
* `QueueController`
* `PresetController`

Responsabilidades:

* Convertir eventos UI en comandos de aplicación (`RunJobCommand`, `CreateJobCommand`, etc.)
* Suscribirse a eventos/observables de estado (`JobProgressEvent`, `JobStatusEvent`)

---

### C) Core de conversión (FFmpeg)

#### 1) Localización y validación

* `FfmpegLocator.detect()`

  * Busca en: PATH, carpeta app (`./bin`), config guardada, variables.
  * Valida con `ffmpeg -version`.
* `FfprobeService.inspect(file)`

  * Extrae streams, duración, codecs, resolución.

#### 2) Generación de comandos

* `FfmpegCommandBuilder`

  * Entrada(s)
  * Operación
  * Opciones
  * Salida
  * Flags estándar: `-y` (overwrite), `-hide_banner`, `-loglevel`, `-progress pipe:1`

#### 3) Ejecución y progreso

* `FfmpegRunner.run(command, listeners)`

  * Ejecuta en background (ExecutorService)
  * Lee stdout/stderr
  * Progreso: parsea líneas tipo `out_time_ms=...`, `progress=continue/end`
  * Calcula porcentaje con duración total (de ffprobe)

#### 4) Cancelación

* `JobHandle.cancel()`

  * `Process.destroy()` y, si no responde, `destroyForcibly()`
  * Limpia temporales

---

## Modelo de Jobs

### `Job`

Campos mínimos:

* `UUID id`
* `MediaType mediaType` (AUDIO/VIDEO)
* `Operation operation`
* `List<Path> inputs`
* `Path output`
* `JobOptions options`
* `JobStatus status`
* `Instant createdAt`

### `JobOptions`

Separar por tipo:

* `AudioOptions` y `VideoOptions` implementan una interfaz `Options`.

Ejemplo audio:

* `container`: mp3/aac/m4a/flac/wav/ogg/opus
* `codec`: libmp3lame/aac/libopus/flac/pcm_s16le
* `bitrateKbps`, `sampleRateHz`, `channels`
* `normalize`: true/false (EBU R128 o loudnorm)

Ejemplo vídeo:

* `container`: mp4/mkv/webm/mov
* `videoCodec`: libx264/libx265/libvpx-vp9/libaom-av1
* `mode`: CRF o bitrate
* `crf`, `bitrateKbps`
* `preset`: ultrafast…veryslow
* `scale`: ancho/alto
* `fps`
* `audioCodec`, `audioBitrateKbps`

---

## Operaciones soportadas (MVP → Pro → “Nice-to-have”)

### MVP (imprescindible)

1. **Transcodificación simple** (1 input → 1 output)
2. **Remux** (cambiar contenedor sin recomprimir cuando sea posible)
3. **Extracción de audio** desde vídeo (`EXTRACT_AUDIO`)
4. **Cola de trabajos** (secuencial)
5. **Progreso y logs**
6. **Drag & drop** en ambos frames
7. **Reordenar inputs** (para operaciones que lo requieran)
8. **Validaciones** (extensiones soportadas, destino, overwrite)

### Pro (muy viable)

9. **Concatenar** (varios clips → uno)

   * Vídeo: concat demuxer (mismos codecs) o concat filter (reencode).
   * Audio: concat filter.
10. **Recortar** (trim por hh:mm:ss)
11. **Normalización de audio** (loudnorm)
12. **Presets guardados** (perfiles)
13. **Arrastrar salida** (drag-out del archivo generado: opcional)
14. **Historial** de jobs
15. **Paralelismo controlado** (N workers)

### Nice-to-have (si te da tiempo)

16. **Multipista / selección de streams** (elegir pista audio/subs)
17. **Subtítulos**: burn-in / mux
18. **Watermark** (overlay)
19. **Capturas/thumbnails**
20. **Watch folder** (carpeta vigilada que crea jobs automáticamente)
21. **Plugins** (operaciones custom)

---

## Flujo de ejecución (end-to-end)

1. Usuario arrastra archivos al `DropZonePanel`.
2. UI crea `MediaItem` y lanza `FfprobeService.inspect()` (async) para metadatos.
3. Usuario configura operación + preset + destino.
4. `CreateJobUseCase` valida y crea `Job`.
5. `QueueManagementUseCase.enqueue(job)`.
6. `RunJobUseCase` toma el siguiente job, genera comando con `FfmpegCommandBuilder`.
7. `FfmpegRunner` ejecuta y emite eventos:

   * `JobStarted`
   * `JobProgress(percent, speed, outTime)`
   * `JobLog(line)`
   * `JobCompleted` / `JobFailed`
8. UI actualiza barras, tabla de jobs e historial.

---

## Concurrencia y threading (Swing)

* **EDT**: solo pintar UI.
* **ExecutorService**:

  * `metadataExecutor` (ffprobe)
  * `jobExecutor` (ffmpeg)
* Comunicación UI: `SwingUtilities.invokeLater()` al recibir eventos.

---

## Eventos (bus interno simple)

Implementación simple y efectiva:

* `EventBus` (listeners registrados)
* Eventos:

  * `MediaInspectedEvent(mediaItem)`
  * `JobQueuedEvent(job)`
  * `JobStatusChangedEvent(jobId, status)`
  * `JobProgressEvent(jobId, percent, speed, eta)`
  * `JobLogEvent(jobId, level, message)`

Esto te permite desacoplar UI de infra.

---

## Persistencia de configuración

Guardar en JSON (fácil para vibe coding):

* Ruta ffmpeg/ffprobe
* Carpeta de salida por defecto
* Último preset seleccionado
* Lista de presets
* Preferencias UI (tema, tamaños, columnas)

Ubicación:

* Windows: `%APPDATA%/TuApp/` (o `user.home` + `.TuApp`)
* Linux/macOS: `~/.tuapp/`

---

## Manejo de errores (imprescindible)

* FFmpeg no encontrado → pantalla de “Configurar FFmpeg”.
* Archivo corrupto/no soportado → marca item con icono/tooltip.
* Output existente → diálogo overwrite / auto-rename.
* Fallo de job → guardar log + comando completo para reproducibilidad.

---

## Telemetría local (sin nube)

* `logs/app.log`
* `jobs-history.json`
* “Copiar comando” (botón) para depurar en terminal.

---

## Diseño de UI (simple y productivo)

### DropZone

* Área grande con texto “Suelta aquí…”
* Acepta multiarchivo
* Detecta tipo por ffprobe + extensión

### Lista de inputs

* Tabla con: nombre, duración, codec, tamaño, estado
* Botones: arriba/abajo, quitar

### Salida

* Selector carpeta + nombre
* Extensión autogenerada según preset

### Ejecución

* Botones: **Añadir a cola**, **Iniciar**, **Cancelar**, **Limpiar**
* Progreso por job + progreso total (cola)

---

## Reglas de “unificar” (definición práctica)

Para evitar sorpresas, define “unificar” como un conjunto de operaciones:

1. **Concatenar** (inputs en orden → un único output)
2. **Mux** (un vídeo + un audio → un único contenedor)
3. **Remux** (cambiar contenedor sin recodificar)

Cada una debe ser un `Operation` distinto.

---

## Backlog listo para “vibe coding” (tareas pequeñas)

### Sprint 0 – Base

* [ ] Crear proyecto Maven/Gradle
* [ ] `FfmpegLocator` + pantalla de configuración
* [ ] `FfprobeService` (inspección básica)
* [ ] `FfmpegRunner` (exec + logs)
* [ ] Modelo `Job` + cola simple

### Sprint 1 – VideoFrame MVP

* [ ] `VideoFrame` + `DropZonePanel`
* [ ] Lista de inputs + metadatos
* [ ] Transcode mp4 (libx264 + aac) y Remux
* [ ] Progreso con `-progress pipe:1`

### Sprint 2 – AudioFrame MVP

* [ ] `AudioFrame` + drop
* [ ] Convertir a mp3/opus/flac
* [ ] Normalización (loudnorm) opcional

### Sprint 3 – Unificar

* [ ] Concatenar audio
* [ ] Concatenar vídeo (dos modos: remux si compatible / reencode)
* [ ] Mux (vídeo+audio)

### Sprint 4 – Calidad de vida

* [ ] Presets guardados
* [ ] Historial y “copiar comando”
* [ ] Múltiples workers configurables

---

## Prompts concretos para vibe coding (Claude/ChatGPT)

Usa prompts por piezas (para evitar “monolitos”):

1. **Modelo de dominio**

* “Genera las clases Java (POJOs) para Job, MediaItem, Preset, Options (AudioOptions/VideoOptions) con validaciones básicas y enums; incluye builder o records si usas Java 17.”

2. **Infra FFmpeg**

* “Implementa FfmpegLocator (PATH + config) y FfmpegRunner con ProcessBuilder, lectura de stdout/stderr, cancelación y callbacks.”

3. **Progreso**

* “Implementa ProgressParser para salida `-progress pipe:1` (key=value) y calcula % usando duración (ms) de ffprobe.”

4. **UI DnD**

* “Crea DropZonePanel Swing con TransferHandler que acepte List<File>, emita callback onFilesDropped.”

5. **Frames**

* “Crea BaseMediaFrame con layout: DropZone arriba, tabla inputs centro, opciones derecha, salida abajo, controles y log.”

6. **Use cases + EventBus**

* “Crea un EventBus simple y los casos de uso CreateJob/RunJob/Queue con eventos de progreso y status.”

---

## Criterios de aceptación (para saber que está bien)

* Arrastrar 1+ archivos y se listan con metadatos.
* Elegir salida y formato, ejecutar sin bloquear UI.
* Se ve progreso real (no solo spinner).
* Se puede cancelar un job.
* VideoFrame y AudioFrame comparten el mismo core.
* Logs guardados y visibles.

---

## Notas técnicas importantes

* **FFmpeg concat**: para remux/concat sin reencode, los inputs deben ser compatibles (mismo codec/parámetros). Si no, usa filtro concat y reencode.
* **Pausa**: FFmpeg no ofrece pausa/resume genérico; si lo necesitas, se implementa por **segmentación** (HLS/segment) o guardando progreso y re-lanzando con `-ss` y `-t` (no perfecto). Mejor ofrecer “cancelar” + “reanudar desde punto aproximado” como mejora.
* **Seguridad**: escapar correctamente rutas/espacios; nunca construir comandos como string plano (usar lista de args).
