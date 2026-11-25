# Informe G1 — biblioteca de ejercicios y entrenamiento guiado

Rama `fase/f1`. Fecha 25-11-2026. Commit local, **sin push**. El refinamiento visual previo (`405aa25`) y los datos se conservan.

## Qué cambia

- **Catálogo local.** 50 ejercicios de repeticiones, 5 por grupo: Pecho, Espalda, Hombros, Bíceps, Tríceps, Cuádriceps, Isquiotibiales, Glúteos, Gemelos y Abdomen/core. Están en `feature/training/catalog/ExerciseCatalog.kt` (versión 1).
  - IDs inmutables en inglés técnico, independientes del texto.
  - Cada ficha incluye alias, músculo principal y secundarios, equipo principal y de apoyo, dificultad (principiante, intermedio o avanzado), preparación, de 3 a 5 pasos, respiración y 2–3 errores redactados para ese movimiento.
  - Todo el texto es original en español. No se ha copiado nada de Hevy.
- **«Añadir ejercicio» abre «Elegir ejercicios».**
  - Búsqueda sin distinguir mayúsculas ni tildes, por nombre, alias o músculo.
  - Filtros de músculo y equipo, que se combinan entre sí (AND). El filtro muscular incluye los secundarios, con los de músculo principal primero.
  - Si no hay resultados, se ofrece «Limpiar filtros».
  - Selección múltiple con check champán numerado según el orden de selección. Contador y botón fijo «Añadir N ejercicios».
  - El botón «i» de cada fila abre la ficha y nunca selecciona el ejercicio.
  - Lo que ya está en la rutina aparece como «Ya en la rutina» y no se puede volver a añadir. Para duplicarlo hay que usar «Duplicar» en el menú del ejercicio.
  - Cancelar o volver no cambia el borrador.
- **Ficha.**
  - Muestra datos, dificultad descrita, preparación, pasos, respiración y errores comunes.
  - Muestra «Guía en texto» con un icono, porque no hay medios con licencia. No hay imágenes ni vídeos inventados.
  - Desde el selector tiene un botón «Añadir a la selección» / «Seleccionado · quitar».
- **Editor.**
  - Una rutina nueva empieza sin ejercicios.
  - Los ejercicios del catálogo muestran su nombre, músculo y equipo, y el botón «i». No piden el nombre.
  - La plantilla inicial es editable y la carga queda vacía:
    - Multiarticulares: 3 × 8 con 120 s de descanso.
    - Aislamiento: 3 × 12 con 60 s de descanso.
  - Siguen funcionando reordenar, duplicar, quitar, notas, series y calentamiento.
  - «Crear ejercicio personalizado» se mantiene como opción secundaria.
- **Sesión.**
  - Botón «i» junto al nombre del ejercicio y la línea de músculo y equipo.
  - La ficha se abre como una capa encima de la sesión: no pausa ni reinicia el tiempo ni el descanso.
  - Se reutilizan la serie, el descanso, ±15 s, «Omitir», la recuperación y el guardado de H2. Una serie solo se completa tras una escritura correcta (comportamiento de H2 sin cambios).
- **Modelo.**
  - Nueva columna opcional `catalogId` en `routine_exercises` y `session_exercises`, con Room v4 y `AutoMigration(3, 4)`.
  - `exerciseKey` mantiene su significado: identidad de historial.
  - No se empareja por nombre. Los ejercicios antiguos quedan como personalizados (`catalogId` null).
  - La sesión copia `catalogId` en su copia (snapshot) de la rutina.
- **Selector y ficha.** Son capas del editor, no rutas nuevas. Su estado (consulta, filtros, selección ordenada y ficha abierta) se guarda en `SavedStateHandle`. «Atrás» va de la ficha al selector y del selector al editor.

## Archivos

- **Nuevos**
  - Catálogo: `catalog/ExerciseCatalog.kt`
  - Interfaz: `ui/ExerciseLibraryUi.kt`
  - Recursos: `strings_g1.xml`, `ic_info.xml`, `ic_search.xml`, `ic_article.xml`
  - Esquema: `schemas/…/4.json`
  - Pruebas: `ExerciseCatalogTest`, `MigrationV4Test`, `ExercisePickerFlowTest`
- **Modificados**
  - Modelo y datos: `TrainingEntities`, `FitnessDatabase` (v4), `TrainingRepository`
  - Editor y sesión: `RoutineForm`, `RoutineEditorViewModel`, `RoutineEditorScreen`, `SessionScreen`
  - Pruebas: `MigrationTest` y `MigrationV3Test` (ahora esperan la versión 4), `TrainingFlowTest` (el borrador nuevo ya no trae un ejercicio vacío, así que usa «Crear ejercicio personalizado»)

## Verificación final (resultados reales)

| Paso | Resultado |
|---|---|
| `gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Unitarias | 62/62 OK. Las 5 nuevas de `ExerciseCatalogTest` cubren: ≥50 ejercicios, IDs y nombres únicos, todos los grupos, 3–5 pasos, 2–3 errores, errores no repetidos, sin medios, búsqueda sin tildes, alias, filtros AND, resultado vacío y plantilla |
| Lint | 0 errores. Los mismos 8 avisos anteriores; 3 recursos sin uso introducidos se eliminaron |
| Instrumentadas en el emulador Medium_Phone_API_37.0 | **OK (8)**: `MigrationV4Test`, `MigrationV3Test`, `MigrationTest`, `ExercisePickerFlowTest`, `NavigationTest` (4) |
| `TrainingFlowTest` (destructiva; solo en el emulador, el móvil no estaba conectado) | OK (1) |

- **`MigrationV4Test`.** Parte de una base v3 real (esquema y hash de `3.json`) con rutina, sesión y agua «Prueba».
  - Tras migrar se conservan nombre, clave, notas, historial y agua, y `catalogId` queda null.
  - Una rutina mixta (catálogo y personalizado) guarda la referencia, y la sesión la copia.
  - Editar la rutina después no reescribe la sesión.
- **`ExercisePickerFlowTest`.** Cubre:
  - Cancelar sin cambios.
  - Orden de selección.
  - Recrear el ViewModel conserva consulta, filtros, selección y ficha.
  - Sin duplicados.
  - Grupos mezclados.
  - Personalizado.
  - Mover, notas y descanso.
  - Guardar y reabrir.

## Recorrido manual (emulador, capturas en `quality/evidence/g1/`)

1. Rutina nueva vacía (01). Búsqueda «Hombro» con el filtro Hombros: 5 resultados (02). Se seleccionan Pájaros, Elevaciones laterales y Press sentado en ese orden (03).
2. Se abre la ficha de Elevaciones laterales (04) y se gira el dispositivo (05). Al volver, la consulta, el filtro y la selección 1-2-3 siguen intactos (06).
3. Se añaden los tres y luego Remo sentado en polea, con Espalda + Polea, en una segunda tanda. Se guarda la rutina: 4 ejercicios y 12 series en orden de selección, «Sin carga» y descansos de 60/120 s (08). **No se escribió ningún nombre de ejercicio.**
4. En la sesión (09) se completa la serie 1 y empieza el descanso de 60 s (10). Se abre la guía (11). Al cerrarla, el tiempo efectivo y el descanso han seguido corriendo (12).
5. +15 s. La app pasa a segundo plano 5 s y, al volver, el descanso sigue en marcha. Se usa «Omitir», se completa la serie 2 y se finaliza. El resumen (13) y el historial (14) muestran «2 series».
6. Con fuente al 200 %, el pie del selector partía el contador palabra a palabra. **Corregido:** a partir de 1,5× el contador va encima y el botón ocupa todo el ancho (15). La ficha a 200 % se ve bien (16).
7. Cancelar el selector con «atrás» deja el borrador sin cambios y sin diálogo de descartar.

## Criterios

| ID | Estado |
|---|---|
| G1-01 Rutina sin escribir nombres, filtrando Hombros | PASS |
| G1-02 50 fichas, IDs únicos, grupos, búsqueda, filtros y vacío | PASS |
| G1-03 Ficha, volver, cancelar y rotar sin perder nada; guía offline | PASS |
| G1-04 Grupos mezclados, orden, series, notas, descansos; guardar y reabrir | PASS |
| G1-05 Rutinas y sesiones previas, personalizados y snapshot | PASS (`MigrationV4Test`; además la app del emulador migró en sitio de v3 a v4 al instalarse y arrancó con sus datos) |
| G1-06 Sesión guiada, descanso ±15/omitir, finalizar, resumen e historial | PASS. El fallo de escritura sin descanso está cubierto por la lógica de H2, que no cambia; en G1 no se volvió a forzar un fallo |
| G1-07 Segundo plano y reentrada sin un segundo temporizador | PASS (reentrada manual; recuperación de H2) |
| G1-08 Fuente 200 %, teclado, scroll e insets | PASS parcial. TalkBack: NO EJECUTADO (hay descripciones, estado «Seleccionado» y casilla con rol) |
| G1-09 Medios reales o ausencia explícita; nada de Hevy | PASS. Solo «Guía en texto» |

## Limitaciones

- **Guía solo en texto.** No hay imágenes, ilustraciones ni vídeos de técnica. Si se añaden, debe ser en un lote posterior con recursos con licencia y un manifiesto.
- **Iconos.** No hay iconos musculares específicos; se usan filtros de texto. No se dibujaron iconos anatómicos sin validar.
- **Tipo de ejercicio.** Solo hay ejercicios de repeticiones. Plancha, cardio y ejercicios por tiempo quedan para cuando existan series temporizadas.
- **Idioma.** El texto del catálogo está en Kotlin (la app solo está en español) y no en recursos traducibles.
- **Cambiar ejercicio.** No existe una acción para cambiar un ejercicio por otro. Se puede quitar y añadir otro.
- **No ejecutado.** TalkBack y API 28.

APK de depuración: `app/build/outputs/apk/debug/app-debug.apk`.
