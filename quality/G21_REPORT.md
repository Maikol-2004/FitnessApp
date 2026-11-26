# G2.1 — Ajustes sobre G2 (8220773)

Fecha: 26-11-2026 · Rama `fase/f1` · Commit local, sin push.

## Corregido

1. **Ficha más compacta.**
   - La secuencia ocupa como máximo 236 dp o el 30 % de la altura de la ventana, y muestra siempre la imagen completa.
   - Los controles caben en una sola fila: anterior y siguiente, «1/3 · Inicio», reproducir/pausar y repetir.
   - «Músculos implicados» se ve en la primera pantalla. El mapa queda a la izquierda y la leyenda a la derecha.
   - Con fuente a partir de 130 %, el mapa (240 dp, centrado) se coloca encima de la leyenda.
   - Se corrigió un solape a 200 % que apareció durante la propia verificación.
   - Los tres frames siguen siendo posiciones fijas y la ficha indica que no son un vídeo.
2. **Ejercicios personalizados.**
   - Su ficha abre Guía, Progreso e Historial a partir de su `exerciseKey`. Se accede desde el botón «i» del editor y de la sesión.
   - La Guía muestra un estado textual («Sin imágenes ni mapa muscular») y las notas del ejercicio. No se le asignan imágenes de otro ejercicio.
3. **Dominada asistida: máquina o banda.**
   - Nueva columna `assistMode` en la rutina y en la sesión (Room v5, migración automática 4→5).
   - Se elige en el editor y durante la sesión.
   - Progreso muestra una gráfica para cada modalidad. Las sesiones anteriores quedan como «sin indicar»: se ven en Historial y en un aviso, pero no se reclasifican ni aparecen en las gráficas.
   - Una asistencia de 0 se muestra y se conserva como «0 kg». Antes desaparecía como si fuera un dato ausente.
4. **Mapa del press sentado con mancuernas.**
   - Se retiró el resaltado ocre del trapecio y la espalda alta. Quedan deltoides como principales y tríceps como secundarios.
   - Es una edición local de píxeles sobre el mismo archivo, no una regeneración. `audit.json` y `VISUAL_REVIEW.md` están actualizados, y una prueba comprueba el tamaño y el hash.

## Comprobación conjunta (resultados reales)

| Comprobación | Resultado |
|---|---|
| `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` | PASS. 78/78 unitarias. Lint con los 9 avisos previos, ninguno nuevo |
| Instrumentadas en emulador API 37 (tras `pm clear`) | PASS. 28/28, incluida `MigrationV5Test` |
| Persistencia: instalación sobre datos de 8220773 | PASS. Se conservan la rutina y la sesión, que pasa a «sin indicar» |
| Navegación: editor y sesión → ficha → Guía, Progreso e Historial, con ejercicios de catálogo y personalizados | PASS (manual) |
| Fuente al 200 % | PASS, tras corregir el solape |
| Movimiento reducido | PASS. Sin reproducir ni repetir, con avance manual y aviso |
| Horizontal | PASS. Imagen completa |
| TalkBack completo, API 28, móvil físico | NO EJECUTADO |

Capturas en `quality/evidence/g21/`: `antes_*`, `despues_*` y `comparativa_*.png`.

## Pendiente

- El resumen de la sesión y el volumen de la rutina siguen sumando la asistencia como carga. Es un comportamiento anterior a G2.1 y no se tocó en este encargo.
- El mapa corregido es un retoque manual. Conviene regenerar el recurso si se exige revisión profesional.
- No se ejecutaron TalkBack, API 28 ni el móvil físico.
