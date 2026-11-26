# V2 — Sesión activa abierta con Liquid Glass

Fecha: 26-11-2026 · Rama `fase/f1`, sobre 285d81d · Commit local, sin push.

## Hecho

- **Sin contenedores.** Se quitaron la tarjeta de cabecera y el fondo de la tabla de series. El cristal (`editorGlass` de V1) solo está en:
  - Los campos kg/reps y el botón de completar.
  - Anterior/siguiente, guía, ±15 s, «Omitir», «Añadir serie», «Siguiente» y «Pausar/Reanudar».
  - «Finalizar» sigue siendo el botón champán.
- **Cabecera compacta.**
  - Tiempo efectivo, «N de M series» reales y aro pequeño.
  - «Ejercicio X de Y» con flechas de cristal.
  - Miniatura real por catalogId (o casilla sin foto si es personalizado), nombre, músculo · equipo y acceso a la guía.
- **Estados con texto, no solo color.** Cada fila lleva una línea de estado: «Serie actual» (además con barra lateral y número grande), «Pendiente», «Hecha» (check relleno) y «Calentamiento» (C).
- **Edición.**
  - Al tocar kg o reps se selecciona el valor y escribir lo sustituye. Antes el cursor caía donde se tocaba y «8» + «10» daba «108».
  - El botón de completar es independiente, de 48 dp, y muestra siempre el check.
  - Con el teclado abierto se ocultan Pausar/Finalizar y el descanso pasa al contenido desplazable, para que las series tengan sitio.
  - Con fuente grande, cada serie se reparte en dos líneas con etiquetas, como el editor.
- **Descanso integrado** entre el contenido y las acciones, sin tapar series.
  - Mismo temporizador persistido (`rememberTicker` y el deadline en Room).
  - Controles −15 s, +15 s, «Omitir» y «Cerrar» al terminar.
  - En horizontal y con el teclado abierto va dentro del scroll.
  - Con la fuente grande, el aro crece para que la cuenta atrás no se salga.
- **«Anterior».**
  - Muestra la última sesión terminada con datos comparables del mismo ejercicio: por catalogId o, si es personalizado, por exerciseKey.
  - Arriba dice «Anterior: sesión del …», y cada fila «Anterior 40 kg × 10» o «Anterior 12 reps».
  - El n-ésimo calentamiento se compara con el n-ésimo calentamiento, y la n-ésima serie de trabajo con la n-ésima de trabajo. Si no hay pareja: «Anterior: sin serie equivalente».
  - Sin datos: «Sin registro anterior».
  - En la dominada asistida solo compara la misma modalidad: máquina con máquina y banda con banda. Sin modalidad indicada no hay referencia, y se pide indicarla.
  - No cambia los objetivos: los valores de la rutina siguen como texto guía en las celdas.
- **Guardado.**
  - Si `completeSet` no cambia nada en Room (devuelve false), ahora se trata como fallo: no hay check ni descanso, sale el mensaje de error y lo escrito se conserva.
  - El check, el descanso y la vibración siguen saliendo solo de lo confirmado en Room.
- **Detalle de rutina.** Distingue una carga 0 escrita («0 kg») de una carga ausente («Sin carga»). Lo hace `formatExplicitLoad`. `formatLoad` no cambia en el resto de pantallas.
- **Se conserva** todo lo anterior:
  - Validaciones, pausa y el aviso de tiempo no verificado.
  - Diálogos de finalizar, descartar y valores no válidos.
  - Unidades, modalidad de asistencia y exclusión de la asistencia del volumen (la sesión de prueba suma 920 kg, sin los 30 kg de ayuda).
  - Movimiento M06/M07/M08 y la ficha superpuesta.

## Comprobación (resultados reales)

| Comprobación | Resultado |
|---|---|
| `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` | PASS. 87/87 unitarias (6 nuevas). Lint con 7 avisos previos y ninguno nuevo |
| Instrumentadas en emulador API 37 (`pm clear` solo en el emulador) | PASS. 30/30 (1 nueva) |
| Navegación: Entrenar → Continuar, ejercicio anterior/siguiente, guía y volver | PASS (manual) |
| Persistencia: valores editados y series hechas sobreviven a cerrar la app | PASS (manual; además la sesión sobrevivió a un cierre del emulador) |
| Temporizador: +15, −15, Omitir, pausa congelada (1:09 → 1:09), recuperación tras cerrar el proceso (2:19 → 1:46 a los ~33 s) | PASS (D2, D8, D9) |
| Teclado: selección del valor al tocar; las series quedan visibles | PASS (D3) |
| Fuente al 200 % y horizontal | PASS (D10, D11) |
| Movimiento reducido: completa la serie y el descanso avanza por segundos | PASS (D12) |
| «Anterior»: calentamiento y trabajo por separado, modalidades de asistencia sin mezclar, «Sin registro anterior» | PASS (D5–D7 y prueba unitaria) |
| Detalle: «0 kg» frente a «Sin carga» | PASS (D4 y prueba unitaria) |
| TalkBack completo, API 28, móvil físico | NO EJECUTADO |

Pruebas añadidas o adaptadas:
- `PreviousSetsTest`: sesión más reciente, emparejado de calentamiento y trabajo, primera aparición del ejercicio, modalidades de asistencia y carga explícita.
- `SessionWriteFailureTest.unchangedWrite_isFailure_keepsDraft_noRest`: si `completeSet` devuelve false, no hay check ni descanso y se conserva lo escrito.
- `TrainingFlowTest` espera el progreso por su descripción accesible («Progreso: 1 de 2 series completadas»). El texto visible ahora es «1 de 2 series».

## Límites

- La fila de serie ya no se tiñe de champán al completarse, porque las series van sobre el fondo. La confirmación es el check relleno, la palabra «Hecha» y el número atenuado.
- El emulador se cerró una vez por un fallo del controlador gráfico del PC. No tiene relación con la app.
- Algunas capturas (D1, D5–D7) son anteriores a dos retoques finales: el aro de descanso que crece con la fuente y el plural «1 de 1 serie». Con fuente normal se ven igual.
- La referencia «Anterior» compara por orden de series y no cambia los objetivos. Si la rutina cambió mucho entre sesiones, la pareja por posición puede no ser la que el usuario esperaba.
