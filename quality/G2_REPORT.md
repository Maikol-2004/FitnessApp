# Informe G2 — imágenes, ficha, progreso e historial por ejercicio

Rama `fase/f1`. Fecha 25-11-2026. Commit local, **sin push**. Se conservan G1, el refinamiento visual, las rutinas, las sesiones y los datos.

## Qué cambia

- **Medios por `catalogId`.**
  - Se empaquetan 250 archivos de `design/exercises-v1`, 5 por ejercicio: miniatura, anatomía y 3 posiciones. Van en `app/src/main/assets/exercises/<id>/` y suman 6,1 MB en disco (5,6 MB dentro del APK).
  - No se empaquetan `sources/`, `references/`, `prompts/`, `review/` ni la auditoría. No hay vídeo (`video = null`).
  - La correspondencia está en `catalog/ExerciseMedia.kt`, derivada del ID y comprobada contra `manifest.json` en una prueba.
  - Las imágenes se decodifican fuera del hilo principal y se guardan en una caché LRU de 24 MB. No se añade ninguna dependencia.
- **Selector.**
  - Cada fila muestra su miniatura: caja de 80×72 dp, Fit, fondo `#121416` y radio 12. Con fuente grande baja a 64×58.
  - Se conservan la selección múltiple, el orden y el botón «i».
- **Ficha con Guía, Progreso e Historial.** Se abre desde el selector, el editor y la sesión.
  - **Guía.**
    - «Secuencia de 3 posiciones» con flechas o deslizamiento, y etiquetas «1 · Inicio / 2 · Intermedia / 3 · Final».
    - Reproducir/Pausar y Repetir ejecutan el bucle 1→2→3→2→1 a 1,2 s por posición, con cambio directo de imagen: sin interpolación, barra de vídeo ni audio.
    - Empieza en pausa. Se pausa al cambiar de pestaña, cerrar la ficha o pasar la app a segundo plano.
    - Con «Reducir animaciones» no aparecen los botones de reproducción: solo controles manuales.
    - Muestra un aviso: «Ilustraciones generadas y orientativas… No es un vídeo».
    - «Músculos implicados» incluye el mapa anatómico y una leyenda accesible (principal en champán, secundarios en ocre). Debajo siguen el equipo, la dificultad, la preparación, los pasos, la respiración y los errores de G1.
  - **Progreso.**
    - Filtro de periodo: Todo el historial (por defecto), 90 días o 30 días.
    - Carga máxima con sus repeticiones y fecha (no es un 1RM). Mejor volumen con su número de series y fecha. Carga media por serie.
    - Gráfica con la carga máxima de cada sesión, y fechas de primera y última práctica.
    - Notas de reglas. Para mancuernas se explica la convención de carga. Sin carga externa se muestra «—», nunca 0.
  - **Historial.** Una tarjeta por sesión con fecha, nombre de la rutina en esa sesión, series (calentamiento marcado como «C»), volumen, distintivo «Carga máxima» y «Ver resumen», que abre el resumen existente. Al volver, la ficha sigue abierta.
- **Cálculo** (`data/ExerciseProgress.kt`, siguiendo `docs/14`).
  - **Qué cuenta.** Solo sesiones `COMPLETED`, agrupadas por `catalogId`; los personalizados o antiguos van por `exerciseKey`. No se empareja por nombre.
  - **Series válidas.** Solo series completadas, sin calentamiento y con repeticiones ≥ 1. Una serie tiene carga si `loadKg > 0`. Nunca se usan objetivos planificados.
  - **Volumen.** Si el ejercicio aparece dos veces en una sesión, su volumen se acumula.
  - **Media.** Es la media de las cargas por serie, sin ponderar por repeticiones.
  - **Empates.** Ante una misma carga máxima, gana la primera vez que se alcanzó.
  - **Dominada asistida.** Muestra «Asistencia» y su evolución (menor asistencia por sesión), sin récord ni volumen.
  - **Consulta en Room.** Dos consultas nuevas en `TrainingDao`, sin migración, porque el esquema no cambia.

## Archivos

- **Nuevos**
  - Código: `catalog/ExerciseMedia.kt`, `data/ExerciseProgress.kt`, `ui/AssetImage.kt`, `ui/ExerciseDetailUi.kt`
  - Recursos: `strings_g2.xml`, `ic_replay.xml`, `assets/exercises/**`
  - Pruebas: `ExerciseMediaTest`, `ExerciseProgressTest`, `ExerciseProgressDbTest`
- **Modificados**
  - Datos: `TrainingDao`, `TrainingEntities` (`ExerciseSetRow`), `TrainingRepository`
  - Catálogo: `ExerciseCatalog` (sin campo de medios)
  - Interfaz: `ExerciseLibraryUi` (fila con miniatura; la ficha antigua pasa a `ExerciseDetailUi`), `RoutineEditorScreen`, `SessionScreen`, `FitnessNavHost` (enlace al resumen)
  - Recursos: `strings_g1.xml` (textos de la guía antigua eliminados)
  - Pruebas: `ExerciseCatalogTest`, `ProfilePersistenceTest` (ver más abajo)
- **Eliminado:** `ic_article.xml` («Guía en texto» ya no se usa)

## Verificación final (resultados reales)

| Paso | Resultado |
|---|---|
| `gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Unitarias | **74/74 OK** |
| Lint | 0 errores. Los mismos 8 avisos previos; se corrigieron los nuevos: recursos sin uso, plurales y `screenHeightDp` |
| Instrumentadas, suite completa en el emulador Medium_Phone_API_37.0 con datos limpios | **OK (27 tests)** |
| APK de depuración | 44,4 MB (44 373 297 bytes); los medios aportan 5,6 MB |

- **Pruebas unitarias.**
  - `ExerciseMediaTest`: los 50 IDs del catálogo coinciden con el manifiesto; las rutas son idénticas y cada archivo es idéntico byte a byte al de diseño, con cabecera WEBP; `video` es null y `autoplay` es false; las miniaturas son distintas entre variantes; solo hay archivos de runtime (50 × 5); la secuencia es la indicada.
  - `ExerciseProgressTest`: fixture 40×10 + 50×8 con calentamiento → **50 / 800 / 45**; dos rutinas y repetición en una misma sesión; null frente a 0; sin datos; solo calentamiento; asistencia; periodo, con primera práctica global; empates.
- **Prueba instrumentada nueva.** `ExerciseProgressDbTest` usa la consulta real de Room con dos rutinas del mismo `catalogId`, una sesión descartada, un personalizado homónimo y una serie pendiente.
- **Dos fallos en la primera pasada, ajenos a G2.**
  - `NavigationTest` dependía del orden de la suite y de los datos que dejaban otras pruebas. Por separado pasaba.
  - `ProfilePersistenceTest` pulsaba «lb · mi» fuera de pantalla, porque Perfil quedó más largo tras el refinamiento visual previo. Se añadió `performScrollTo()`.
  - Además, la prueba no es repetible si el nombre ya está guardado. Por eso se limpiaron los datos de la app **solo en el emulador**.
  - Tras eso, la suite completa dio 27/27.
  - El móvil no estaba conectado y no se tocó.

## Recorrido manual (emulador, capturas en `quality/evidence/g2/`)

1. Selector con miniaturas (01). Ficha: posición 1 (02), posición 2 (03), músculos con leyenda (04) y bucle en marcha (05). Al pasar a segundo plano la secuencia queda en pausa.
2. Progreso vacío antes de entrenar (06).
3. Rutina «Prueba pecho» con Press banca con barra.
   - Sesión 1: C 20×10, 40×10 y 50×8. Resumen: 800 kg.
   - Sesión 2: 55×6, con una serie pendiente.
   - Con la ficha abierta durante el descanso, el descanso siguió (1:57 → 1:36) y la sesión activa no contó (08, 09).
4. Progreso (13):
   - 2 sesiones · 3 series de trabajo.
   - Carga máxima **55 kg × 6**, mejor volumen **800 kg** (2 series) y carga media **48,33 kg** (3 series). La serie pendiente y el calentamiento no cuentan.
   - Gráfica de 50 a 55, con primera y última práctica.
5. Periodo de 30 días (14). Historial con «Carga máxima», chips «C ·» y volúmenes 330/800 (16). «Ver resumen» abre el resumen de esa sesión (15) y «atrás» vuelve a la ficha en Historial.
6. En lb·mi (23, 24): 121,3 lb, 1.764 lb y 106,6 lb. Las conversiones son correctas.
7. Con movimiento reducido solo hay controles manuales y aparece el texto explicativo (22).
8. Pájaros con mancuernas, registrado sin carga en G1 (26): «—» y «Sin carga externa» en lugar de 0, junto a la regla de mancuernas.
9. Fuente al 200 %: Progreso (17), Guía (19) y selector (20).
10. Giro de pantalla (28): la figura completa cabe y se mantiene la posición. Vertical (29).

**Corregido durante el recorrido:**

- La tercera marca bajaba a otra línea: ahora las tres tienen el mismo ancho.
- La gráfica de un solo punto mostraba la etiqueta duplicada.
- «48,33 kg» se partía en dos líneas: ahora el tamaño se ajusta.
- «Reproducir» se partía al 200 %: con fuente grande los botones se apilan.
- En horizontal, la imagen ocupaba más que la pantalla: ahora su alto se limita al 55 % de la ventana.

## Criterios

| ID | Estado |
|---|---|
| G2-01 IDs en el manifiesto, rutas existentes y decodificables, sin vídeo | PASS (`ExerciseMediaTest`; además las imágenes se decodifican en la app) |
| G2-02 Miniatura correcta por tarjeta; selección e «i» sin cambios | PASS (01; `ExercisePickerFlowTest`) |
| G2-03 Posiciones reales, manual, bucle, pausa y repetir, músculos con leyenda, sin prometer vídeo | PASS (02–05, 22) |
| G2-04 Cálculo con datos reales; fixture, calentamiento, nulos, peso corporal, asistencia y dos rutinas | PASS (unitarias y `ExerciseProgressDbTest`; recorrido 13). La asistencia solo está probada en unitarias: no había datos reales de dominada asistida |
| G2-05 Vacío, una y varias sesiones; unidades, periodo, actualización y legado sin `catalogId` | PASS parcial. El legado sin `catalogId` se consulta por `exerciseKey` y está probado en `ExerciseProgressDbTest`, pero los ejercicios personalizados no tienen ficha en la interfaz |
| G2-06 La guía no reinicia ni pausa el descanso; segundo plano y cambio de pestaña paran solo la animación | PASS (recorrido 3; un único `LaunchedEffect` por secuencia) |
| G2-07 Fuente 200 %, orientación, movimiento reducido y controles de 48 dp | PASS parcial. TalkBack y teclado físico: NO EJECUTADO (hay descripciones, roles de pestaña y una leyenda en texto) |
| G2-08 Build, unitarias, lint, recorrido; tamaño del APK y capturas | PASS |

## Medios que conviene corregir

- **`shoulders_seated_dumbbell_press` (anatomía).** El mapa parece resaltar trapecio y espalda alta, que no están en los secundarios del catálogo. Se mantiene el texto del catálogo como referencia.
- **Posiciones muy parecidas.** Como avisa `VISUAL_REVIEW.md`, algunas posiciones intermedias y finales se parecen mucho. Por eso el bucle no arranca solo.
- **Alcance de la revisión.** No se revisaron una a una las 50 fichas en el dispositivo, solo las del recorrido y las hojas de `review/`. No es una validación profesional de anatomía ni de técnica.

## Limitaciones

- **No hay vídeo.** Son tres ilustraciones generadas y orientativas, avisadas como tales en la ficha.
- **Ejercicios personalizados.** El cálculo funciona por `exerciseKey`, pero no tienen botón de ficha ni pestaña de progreso propia.
- **Dominada asistida.** No se guarda si la ayuda es de máquina o de banda, así que solo se muestra la evolución, sin récord ni volumen.
- **Periodo.** Los días son móviles desde ahora (30 o 90 × 24 h), no meses de calendario.
- **No ejecutado.** TalkBack, teclado físico y API 28.
