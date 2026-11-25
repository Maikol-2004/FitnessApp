# Biblioteca visual de ejercicios v1

Recursos originales generados para los 50 catalogId existentes en G1 (cb7aa08). Encargo de integración: docs/14_EXERCISE_MEDIA_AND_PROGRESS.md. Las imágenes están generadas y separadas en archivos reales; esta entrega no modifica todavía la interfaz Android.

## Rutas por ejercicio
`<catalogId>/thumbnail.webp`: miniatura320×320 para la lista.
`<catalogId>/anatomy.webp`: mapa muscular esquemático frente/dorso.
`<catalogId>/frames/01.webp`, `02.webp`, `03.webp`: tres posiciones estáticas.
`sources/<catalogId>.webp`: plancha completa para revisión, no incluir en el APK.
`prompts/<catalogId>.json`: prompt exacto utilizado (herramienta integrada de imágenes), no incluir en el APK.

`manifest.json` relaciona los IDs exactos del catálogo con cada recurso. `audit.json` comprueba rutas, decodificación, dimensiones y SHA-256. `review/` contiene hojas de revisión por grupo. `references/` conserva los mockups aprobados por el usuario, con cifras ficticias; nunca se usan como datos del usuario.

## Alcance real
Son ilustraciones generadas, no filmaciones de un deportista ni contenido copiado de Hevy. La anatomía es esquemática, sin validación profesional. La revisión visual comprueba correspondencia general, no certifica una técnica perfecta. Los casos problemáticos quedan descritos en VISUAL_REVIEW.md para no incorporarlos ciegamente.

No se incluyen GIF ni MP4 fluidos. Los tres frames permiten una secuencia de posiciones y control manual/bucle como paso inicial; `video: null` es intencional. No presentar esta secuencia como un vídeo real. El objetivo final de un vídeo fluido por ejercicio requiere clips adicionales reales y revisados.

Guía textual, origen y límites visibles cuando corresponda. Mantener cuadros completos mediante Fit; no recortar manos, barras o pies al forzar una relación ancha. Cargar miniaturas en la lista y los medios grandes al abrir la ficha. No empaquetar sources/, review/, references/, prompts/, audit o documentos dentro del APK.

## Uso por Claude
Leer el encargo G2, conservar G1/Room v4 y los datos. Relacionar medios por catalogId, no por coincidencia del nombre. Implementar Guía/Progreso/Historial con registros reales y una secuencia final de comprobaciones. No hacer push posterior del código integrado sin autorización.
