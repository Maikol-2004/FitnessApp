# Informe H2 — Rutinas, sesión, resumen e historial

**Fecha:** 2026-11-25.

**Rama:** `fase/h2`, creada desde `main` en af52a2a. Sin push.

**Entorno:** Windows 11, JBR de Android Studio y emulador `Medium_Phone_API_37.0` (API 37, 1080×2400, 420 dpi).

## Implementado
- **Datos.** Room pasa de v1 a v2 con `AutoMigration` 1→2, sin fallback destructivo. Se añaden estas tablas:
  - rutinas, ejercicios y series planificadas;
  - sesiones, con estado ACTIVE, PAUSED, COMPLETED o DISCARDED;
  - ejercicios y series de sesión, que son una copia de la rutina al iniciar.

  El esquema 2 está en `app/schemas`. Todas las escrituras se hacen en transacción:
  - Un índice parcial garantiza una sola sesión abierta.
  - Completar una serie es idempotente.
  - Finalizar es idempotente.
- **Tiempo.** El cronómetro y el descanso se calculan con `SessionTiming` a partir de marcas persistidas (pared + `elapsedRealtime`). No dependen de un temporizador en memoria. Por eso sobreviven a la muerte del proceso, nunca dan negativos y no avanzan durante la pausa.
- **Entrenar.** Muestra:
  - las rutinas propias, con su foto;
  - la sesión en curso;
  - el historial reciente;
  - las rutinas archivadas.

  El menú de cada rutina permite editar, duplicar, archivar/restaurar y elegirla como próxima.
- **Editor.** Permite:
  - añadir, reordenar y eliminar ejercicios y series;
  - marcar series de calentamiento;
  - usar decimales con coma (por ejemplo 22,5).

  Los errores aparecen junto a cada campo y el resto del formulario no se pierde. El teclado no tapa la acción de guardar.
- **Detalle.** Muestra un resumen de la rutina y el botón «Iniciar entrenamiento», protegido contra el doble toque.
- **Sesión.**
  - Cronómetro y anillo de progreso (M07).
  - Tabla de series editable, con check animado y háptico (M06).
  - Dock de descanso (M08) con −15/+15 s y «Saltar».
  - Pausar/reanudar y navegación entre ejercicios.
  - Finalizar o descartar, siempre con confirmación.
  - En pantallas bajas o en horizontal, el dock se integra en el contenido.
- **Resumen.** Es la pantalla de logro:
  - foto a sangre del gimnasio;
  - medalla con halo champán (M09, una sola vez);
  - título grande y «rutina · fecha»;
  - duración, volumen (destacado en champán), ejercicios y series;
  - el detalle de cada serie.
- **Historial.** Lista de sesiones terminadas. Conserva los nombres originales aunque la rutina se edite o archive después.
- **Inicio.** La tarjeta «Tu próximo entrenamiento» tiene tres estados reales:
  - sin rutina;
  - rutina planificada, con botón Iniciar;
  - sesión en curso o en pausa, con botón Continuar.
- **Diálogos.** Son propios, con el estilo del tema. La acción destructiva va en color de error.
- **Movimiento.** M06–M09 siguen el doc 08. Con movimiento reducido o escala 0 llegan al estado final sin bloquear la interacción.
- **Unidades.** Los valores se guardan siempre en kg y solo se convierten al mostrarlos o editarlos. Si el valor no cambia, no se reescribe, así que no hay deriva.

## Comandos y resultados reales
Ejecutados sobre el código final:

| Comando | Resultado |
|---|---|
| `gradlew clean :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug` | BUILD SUCCESSFUL |
| Pruebas unitarias | 44/44 OK (detalle debajo) |
| Lint | 0 errores, 8 avisos (detalle debajo) |
| `gradlew :app:connectedDebugAndroidTest` | BUILD SUCCESSFUL, 21/21 OK (detalle debajo) |
| `adb shell am instrument -w -r …AndroidJUnitRunner` tras `pm clear` | `OK (21 tests)` |
| Release | `app-release-unsigned.apk`, 28 MB. El único permiso es el interno de AndroidX `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. No pide permisos de salud, ni alarmas exactas, ni wake lock, ni servicio en primer plano |

**Pruebas unitarias (44):**
- LoadingPhase: 4
- contraste: 5
- destinos: 2
- preferencias: 5
- categorías: 1
- ProfileViewModel: 7
- SessionTiming: 7
- TrainingFormat: 8
- RoutineForm: 5

**Pruebas instrumentadas (21):**
- DB: 2
- MigrationTest: 1
- LoadingRegion: 2
- Navegación: 4
- persistencia de Perfil: 1
- SessionWriteFailure: 1
- TrainingFlow: 1
- TrainingRepository: 9

**Avisos de lint:**
- Versiones más nuevas de Gradle, del BOM y de coroutines-test, que se descartan a propósito.
- Plurales.
- `screenHeightDp`.
- Tipografía.

**Defecto encontrado y corregido durante la verificación:** lint marcó `StateFlow.value` leído dentro de la composición en `SessionScreen`. Ahora se usa el estado ya recogido.

## Criterios
| ID | Estado | Evidencia |
|---|---|---|
| H2-01 Crear, ordenar, guardar y reabrir | PASS | `saveAndReopen_returnsIdenticalRoutine_andReorderIsKept`. En el dispositivo, la rutina reabierta y reordenada es idéntica (capturas 02, 04, 05 y 14) |
| H2-02 Vacío, decimal regional e inválido | PASS | `RoutineFormTest` y `TrainingFlowTest` (errores junto al campo, sin navegar). «22,5» aceptado en el dispositivo (captura 03) |
| H2-03 Duplicar y archivar una rutina usada | PASS | `duplicate_createsNewIds_andArchiveKeepsHistory`. «(copia)» en el dispositivo, y el historial sigue intacto (captura 15) |
| H2-04 Próxima, iniciar desde Inicio y doble toque | PASS | `concurrentStarts_createOnlyOneOpenSession` (5 inicios concurrentes dan una sesión). Doble toque en `TrainingFlowTest`. Inicio muestra «Sesión en curso / En pausa / Continuar sesión» (captura 09) |
| H2-05 400 → 360 kg | PASS | `volume_400_thenEditToEightReps_360` y dispositivo (360 kg). El segundo toque no reinicia el descanso (`completeTwice_isIdempotent_andRestStartsOnce`) |
| H2-06 Check con fallo de escritura | PASS | `SessionWriteFailureTest`: un repositorio que lanza `IOException` hace que se muestre el error en la fila, sin check, sin progreso y con reintento posible |
| H2-07 Pausa, fondo y forzar cierre | PASS | `pauseFreezesTime_resumeContinues`. En el dispositivo, el tiempo en pausa sigue en 0:30 tras `force-stop` (captura 08) |
| H2-08 Rotar, teclado y cambiar de ejercicio | PASS | «22,5» se conserva al rotar, al cambiar de ejercicio y al reinstalar (capturas 17 y 18). En horizontal se corrigió un dock que tapaba la tabla |
| H2-09 Atrás y descarte | PASS | Atrás conserva la sesión, y cancelar también. Confirmar el descarte la excluye: `discarded_isExcludedFromHistory_andFreesStart` (captura 16) |
| H2-10 Finalizar dos veces | PASS | `finishTwice_givesOneHistoryEntry` (dos finalizaciones concurrentes dan una entrada de 200 kg y 60 s). Doble toque en el dispositivo y en `TrainingFlowTest` (una única entrada) |
| H2-11 Editar la rutina tras terminar | PASS | `editingRoutineAfterwards_leavesHistoryUnchanged`. En el dispositivo, el historial sigue mostrando «Remo con barra» después de renombrar el ejercicio |
| H2-12 Migración v1 con `app_metadata` | PASS | `MigrationTest`: base v1 real con el identity hash `eb0bbe18…`, abierta por Room v2. Conserva `app_metadata`, queda en versión 2 y las tablas nuevas funcionan |
| H2-13 M06–M09 con escala normal y reducida | PASS | Capturas 07 (M06/M08), 12 (fotograma intermedio de M09) y 19 (escala reducida). Con `animator_duration_scale 0` el estado final es inmediato. La escala se restauró después |
| H2-14 kg ↔ lb | PASS | `TrainingFormatTest`. En el dispositivo, 360 kg se muestran como 794 lb y, al volver a kg, quedan exactamente 20 y 22,5 |

## Capturas (`quality/evidence/h2/`)
Todas usan datos de prueba («Prueba», «Prueba torso»). No contienen datos personales.

| Archivo | Contenido |
|---|---|
| 01 | Entrenar vacío |
| 02, 03 | Editor nuevo y editor con errores |
| 04 | Reordenar ejercicios |
| 05 | Detalle de rutina |
| 06 | Inicio de sesión |
| 07 | Serie completada y descanso |
| 08 | Sesión en pausa |
| 09 | Inicio con sesión en curso |
| 10 | Diálogo de finalizar |
| 11 | Resumen, con la cabecera fotográfica final |
| 12 | Fotograma intermedio de M09 |
| 13 | Historial |
| 14 | Entrenar con datos |
| 15 | Duplicar y archivar |
| 16 | Diálogo de descartar |
| 17, 18 | Rotación |
| 19 | Movimiento reducido |

## Pendiente y limitaciones
- **API 28: NO EJECUTADO.** No hay imagen del sistema API 28 instalada.
- **TalkBack: NO EJECUTADO.** Las semánticas se revisaron en el código: encabezados, descripciones de check y botones, la medalla como decorativa, «Serie N, kg». No se ha probado con el lector de pantalla.
- **La prueba de UI completa usa descanso 0.** El anillo M08 se redibuja en cada fotograma y Compose no llega a quedar en reposo. El descanso se cubre en `TrainingRepositoryTest` y en el dispositivo.
- **`connectedDebugAndroidTest` es sensible a la carga del emulador.** Antes falló una vez con «Process crashed» (carga ≈26) y otra por un tiempo de espera de 5 s en `TrainingFlowTest`. Se ampliaron las esperas de navegación a 15 s y la última ejecución completa pasó 21/21.
- **PR (récords) y M10 quedan para H6**, como dice el roadmap. No hay botones ficticios.
- **Notificación de descanso en segundo plano: no existe**, porque H2 excluye alarmas exactas y servicio en primer plano. El descanso se recalcula al volver a la app.
- **APK de release:** sin firmar y sin R8, igual que en H1.
