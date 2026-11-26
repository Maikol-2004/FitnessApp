# V1 — Editor de rutinas abierto con Liquid Glass

Fecha: 26-11-2026 · Rama `fase/f1`, sobre 9032674 · Commit local, sin push.

## Hecho

- **Sin contenedores.** El nombre, las notas, cada ejercicio y sus series están directamente sobre el fondo. Se quitaron las tarjetas `FitnessCard` y el borde de las filas apiladas. Los ejercicios se separan con un divisor sutil y 24 dp de margen.
- **Cristal solo en controles.** `EditorGlass.kt` es un modificador local con el mismo lenguaje que `LiquidGlassDock`: grafito, reflejo superior fino y contorno champán. Lo usan:
  - Los campos de nombre, notas, carga, repeticiones y descanso.
  - Los botones de subir, bajar, «Añadir serie» y «Añadir ejercicio».
  - El selector de tipo, que lleva la lente champán.
  - No hay desenfoque por celda. No se modificaron `FitnessCard`, el dock ni la sesión activa.
- **Cabecera de ejercicio.**
  - Ejercicios del catálogo: miniatura real (72 dp) por catalogId, nombre, músculo y equipo, y acceso a la ficha.
  - Personalizados: casilla sin foto, con el nombre editable y acceso a su ficha.
  - Subir, bajar, «+ Notas» y el menú (duplicar o eliminar) aparecen sin barra contenedora.
- **Notas plegables.** «Añadir notas» y «+ Notas»: si ya hay texto empiezan abiertas, plegar nunca borra, y el estado se conserva al rotar y al volver del catálogo o de la ficha.
- **Recuentos reales.** Se muestran «N ejercicios» y «Series planificadas · N de trabajo» (sin contar calentamientos).
- **Tipo de serie.** El selector ofrece «Trabajo» y «Calentamiento (C)». Solo cambia el calentamiento, sin tocar el orden ni los valores.
- **Eliminar serie** pasa al menú de cada serie y respeta el mínimo de una.
- **Descanso.** Se muestra como m:ss (120 → 2:00; 3600 → 60:00) y se guarda en segundos.
  - Al tocarlo se abre «Descanso en segundos», con el valor seleccionado y un rango de 0 a 3600.
  - Un valor no válido muestra el error y deja «Aplicar» desactivado. «Cancelar» no cambia nada.
  - Se omitió el botón global «Descanso · 2 min» del mockup, como permite la especificación.
- **Guardar rutina** flota sobre el fondo con su margen inferior, sin barra detrás, y queda por encima del teclado.
- **Carga 0.** Una carga de 0 escrita en el editor ahora se guarda como 0; antes se convertía en «sin carga». Vacía sigue siendo dato ausente («—»).

## Comprobación conjunta (resultados reales)

| Comprobación | Resultado |
|---|---|
| `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` | PASS. 81/81 unitarias. Lint con 8 avisos previos; se retiraron 5 textos sin uso |
| Instrumentadas en emulador API 37 (`pm clear` solo en el emulador) | PASS. 29/29 |
| V1-01 contenido sobre el fondo, campos independientes | PASS (`comparativa_editor.png`) |
| V1-02 edición de nombre, notas, tipo, peso, reps y descanso; reordenar, duplicar, eliminar, añadir; catálogo y ficha | PASS (manual) |
| V1-03 «—» vacío y 0, calentamiento C, ayuda Máquina, validación de reps, personalizado con ficha | PASS (manual) |
| V1-04 descanso 0, 3600 e inválido; Cancelar; guardar y reabrir mantienen 0:00, 60:00 y 2:00 | PASS (manual y prueba unitaria) |
| V1-05 teclado, fuente al 200 % (dos líneas con etiquetas) y horizontal | PASS (capturas D6, D12 y D11) |
| V1-06 rotación, catálogo y ficha conservan borrador y notas; salir pide confirmación | PASS (manual) |
| TalkBack completo, API 28, móvil físico | NO EJECUTADO |

Pruebas adaptadas o añadidas:
- `RoutineFormTest`: descanso en m:ss y en segundos, y carga 0 conservada.
- `TrainingFlowTest`: usa el diálogo de descanso y las etiquetas accesibles de los campos.
- `ProfilePersistenceTest`: se desplaza a «kg · km» antes de pulsarlo. Fallaba tras reiniciar el emulador porque la opción quedaba fuera de pantalla. Es un ajuste de la prueba, no de la app.

## Límites

- El detalle de rutina muestra una carga planificada de 0 como «Sin carga». Es la pantalla anterior al editor y no se ha cambiado en este lote.
- La miniatura de ejercicio mide 72 dp y los botones de subir y bajar mantienen 48 dp tocables, así que la cabecera queda algo más alta que en el mockup.
- Las capturas D1 y D2 son anteriores al cambio a m:ss, que solo afecta a 3600 (60:00, en D7 y D12). El menú de tipo se verificó en el dispositivo; su captura se descartó porque mostraba el formato anterior.
