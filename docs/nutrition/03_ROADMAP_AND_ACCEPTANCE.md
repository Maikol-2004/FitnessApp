# Nutrición — roadmap y criterios de cierre

## Orden de ejecución

El encargo actual incluye N1–N3. No pedir confirmación entre sus pasos internos, pero cerrar cada bloque de forma verificable. No dejar una pantalla «bonita» con acciones vacías ni desarrollar N4/N5 por anticipación.

| Bloque | Cambio visible y funcional | Dependencias y cierre |
|---|---|---|
| N0 inspección | Identificar consumidores de comidas/metas, esquema y datos previos | No regenerar la app; confirmar base y preservar cambios locales |
| N1A | Diario por comidas, vacíos y resumen claro | Migración de registros antiguos sin reclasificación automática ni duplicación |
| N1B | Alimentos propios, cantidad/ración, favoritos/recientes y registro rápido | Cálculos unitarios y snapshots; registrar/editar/copiar sin red |
| N2 | Buscar productos reales, revisar cantidad, leer código y alternativa manual | Fuente, atribución, límites, permiso/cámara y fallos reales; caché de elegidos |
| N3A | Crear receta, raciones/peso final y registrar porción | Versionado y conservación de históricos |
| N3B | Objetivos coherentes y tendencias7/30d con cobertura | Diario vacío no se transforma en ingesta0; objetivos históricos no inventados |
| Cierre | APK, capturas, pruebas y límites documentados | Una secuencia conjunta; correcciones repiten solo lo afectado |
| N4 posterior | Plan semanal/lista de compra/exportación/recordatorios | Encargo independiente, consumo separado del plan |
| N5 posterior | Foto/voz/OCR/asistente con revisión | Proveedor, presupuesto, privacidad y evaluación definidos; no botones falsos |

## Criterios de aceptación del lote

- N-01: un usuario nuevo puede crear un alimento desde etiqueta y registrar una cantidad en una comida concreta. El resumen se actualiza tras guardar, no antes.
- N-02: se preservan al migrar todos los IDs, fechas, zonas, nombres, ceros, nulls y nutrientes de comidas previas; se muestran en Sin clasificar y siguen editables. Entrenar, sueño, agua, peso y preferencias siguen intactos.
- N-03:150g de la base A del fixture dan247,5kcal,15gP,30gC y7,5gG. Cambiar de g a una ración definida y volver conserva cantidad equivalente; ml sin densidad no se convierte a g.
- N-04: kcal sin macros se guarda como información parcial; cero se mantiene. Un total parcialmente conocido no se presenta como completo. Las cantidades negativas/no finitas y cantidad0 se rechazan sin perder texto.
- N-05: editar/borrar/mover/copiar se refleja una sola vez; copia no modifica origen; doble toque/reintento no duplica. Un fallo Room deja borrador y error, no un toast de éxito.
- N-06: lista local rápida sin red; favoritos/recientes correctos. Crear definición sin consumir no crea entrada. Consulta remota solo por acción; cancelar o buscar otra cosa descarta respuestas antiguas.
- N-07: código con ceros iniciales se conserva; resultado exige confirmación de producto/cantidad. Permiso denegado, sin cámara/modelo, producto ausente,429,503 y sin conexión permiten continuar manualmente. No introducir resultado ficticio.
- N-08: actualizar fuente o editar alimento/receta no reescribe comidas pasadas. No se borran snapshots por archivar definiciones.
- N-09: receta del fixture total365kcal;2raciones→182,5kcal/ración. Peso final200g→182,5kcal/100g. Sin peso final no se permite consumir gramos calculados. Nutrición desconocida se propaga por nutriente.
- N-10: fecha fijada se conserva cruzando medianoche, cambio de zona y recreación del proceso. Copia a otro día usa destino explícito y no cambia origen.
- N-11: Perfil y Nutrición muestran el mismo objetivo. Cambiar objetivo hoy no inventa una meta para días previos ni cambia sus consumos. Sin meta no hay anillo/porcentaje ficticio.
- N-12: una semana con2días de1000/2000kcal registradas y5vacíos muestra2/7días y media1500kcal de días registrados. Los cinco restantes son huecos. Días parciales se marcan y se explican; no mostrar déficit clínico.
- N-13: fuentes/estado de preparación/base visibles; no copiar base/medios de competidores ni poner check verificado por origen público. NOTICES/atribución disponibles según fuente usada.
- N-14: diario, buscar, cantidad, receta y tendencias respetan estilo abierto, teclado,48dp, fuente200%, orientación, navegación atrás y TalkBack en acciones/cifras. Ningún botón tapa última fila.
- N-15: en producción no se precargan los datos del mockup/fixtures, ni calorías consumidas inventadas ni fotos como prueba de nutrición. IA/foto y planificador futuros no aparecen como controles sin función.

## Validación proporcional

Unitarias de escalado/unidades/nulos/cobertura, receta, fechas y selección de objetivos; pruebas del adapter con respuestas válidas, incompletas y error. Instrumentadas de migración con datos v5 reales de prueba, transacción/idempotencia y recorrido diario→cantidad→guardar→editar→receta. Probar fixtures en una base de test identificada; nunca pm clear ni insertar DEMO en el móvil personal.

Una secuencia final: build Debug, unitarias pertinentes, lint, instrumentadas necesarias, recorrido manual con modo avión, permiso cámara denegado, recreación, fuente200% y objetivos. No lanzar suite completa por cada padding. Si API28/TalkBack/móvil no están disponibles, poner NO EJECUTADO y no PASS. La prueba de escáner puede necesitar dispositivo/cámara real: no confundir introducir un código con haber validado la cámara.

Capturas pedidas: diario vacío/real de prueba, comida con ingredientes, búsqueda local/remota, ficha de cantidad, producto incompleto, receta, tendencias con huecos y fuente grande. Rotular datos de prueba. Informe único NUTRITION_REPORT.md con criterios, comandos/resultados, tamaño de APK, requisitos externos y pendientes reales.

No declarar el lote completo si falta N2 o recetas/tendencias: explicar exactamente qué bloque está operativo y por qué falta el resto. No bloquear lo local por una integración externa que requiera configuración.
