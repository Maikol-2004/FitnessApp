# N — Nutrición: diario profesional y registro de alimentos

Entrega de planificación y diseño solicitada por el usuario. Base revisada: `fase/f1`, `cae2db1c00d16de5123d2b4f9859b4562dd30432`, Room v5. Entrenar se conserva y queda fuera de este encargo. Esta entrega aún no implementa Nutrición en Kotlin.

## Resultado buscado

Abrir Nutrición y entender lo registrado ese día; añadir un alimento con una cantidad real sin calcular macros a mano; reutilizar favoritos y recetas; corregir registros; consultar tendencias sin confundir ausencia de datos con cero. Debe funcionar sin cuenta, con registro local y recuperación de borradores. La búsqueda externa amplía el catálogo, pero no bloquea la consulta y edición del diario guardado.

Guía de producto: registro sencillo y recetas de Fitia; elección de cantidad/comida de MyFitnessPal; identificación de fuentes y datos incompletos de Cronometer. Son patrones de referencia, no autorización para copiar sus recursos, textos, bases o APIs privadas. La identidad visual será Titanium Champagne y el Liquid Glass abierto que ya tiene FitnessApp.

## Lo que ya existe

- `NutritionScreen.kt`: selector de día, resumen de calorías/macros y lista de comidas con formulario manual.
- `NutritionViewModel.kt`: Room observable, día elegido, formulario fijado a su fecha y errores de guardado/borrado.
- `meal_entries`: nombre, instante UTC, zona, fecha local y totales opcionales kcal/proteína/carbohidrato/grasa. Son totales de una comida, NO valores por100g ni ingredientes.
- `TrackingCalc.nutritionTotals`: suma de lo conocido y banderas de información parcial. Null y cero ya tienen semánticas diferentes.
- Metas actuales en preferencias, compartidas con Perfil. Navegación desde la tarjeta Nutrición de Inicio; no hay sexta pestaña global.

Se preservan todos estos datos y contratos. No transformar un registro antiguo de «Comida650kcal» en arroz/pollo ni asignarle gramos inventados.

## Alcance del siguiente lote: N1 + N2 + N3

### N1 — Diario, alimentos propios y cantidades

1. Diario por fecha con Desayuno, Comida, Merienda, Cena y «Sin clasificar». Las comidas antiguas entran en esta última categoría sin adivinar por hora; el usuario puede reclasificarlas.
2. Resumen de kcal registradas, objetivo opcional y diferencia; macros con unidades y estado parcial. Si se supera la meta: «X kcal por encima del objetivo», sin saldo negativo confuso ni juicio moral. Sin meta, mostrar registro y configurar objetivo; no inventar2200kcal.
3. Comidas con alimentos e importes escalados por cantidad; detalle editable, mover a otra comida y borrar con confirmación o deshacer coherente con la persistencia real.
4. Buscador LOCAL de alimentos propios, guardados desde fuentes y recetas; recientes, favoritos y crear alimento. Orden reproducible, consulta sin tildes, marca y estado de preparación visibles. Sin datos iniciales, ofrecer crear alimento/registro rápido o buscar en línea, no una base DEMO.
5. Ficha de alimento: fuente, información por unidad base, cantidad elegida, equivalencia de ración explícita, valores para esa cantidad y fecha/comida de destino.
6. Crear/editar alimento propio desde etiqueta: base100g,100ml o ración definida; energía y macros opcionales, al menos un nutriente informado. Registro rápido de totales sigue disponible para quien no sabe los ingredientes.
7. Copiar una comida o selección a otra fecha/comida con vista previa y confirmación; nuevos IDs y cero cambios en el original. Guardar una plantilla no debe registrar consumo.
8. Mantener borradores al rotar, volver del detalle y cerrar por el sistema cuando sea recuperable. Doble toque en Añadir/Guardar no duplica entradas.

### N2 — Productos reales y códigos de barras

Integrar lectura de Open Food Facts con origen visible, normalización y caché de lo elegido. Buscar en línea mediante acción explícita, sin pedir un servidor por cada letra. Escáner real para códigos alimentarios y entrada manual del código como alternativa. Permiso de cámara solo al usarlo; sin permiso, sin red, sin modelo o producto ausente ofrecen una salida funcional. Véanse fuentes y contrato en `nutrition/02_DATA_AND_INTEGRATION.md`.

Un código identifica un producto, no pesa una ración ni demuestra su exactitud. Presentar el producto, cantidad y nutrientes para revisión antes de registrar. Nunca sustituir un código desconocido por un alimento parecido sin elección del usuario. No publicar correcciones ni fotos a terceros en este lote.

No se promete un catálogo completo offline recién instalado. Se puede registrar manualmente sin red; los productos guardados y recetas quedan disponibles offline. Un catálogo genérico precargado solo se incorpora si existe un archivo real con procedencia y permiso verificables: no rellenarlo con cifras generadas por IA ni extraer la base de Fitia/Cronometer. USDA es ampliación posible con sus requisitos, no dependencia ficticia ya disponible.

### N3 — Recetas, objetivos y tendencias

Recetas propias: ingredientes con cantidades, total calculado, raciones y peso final opcional. Registrar una ración o gramos cuando exista una equivalencia válida; no convertir el peso crudo en cocinado por suposición. Editar una receta crea una versión nueva y no cambia diarios anteriores.

Objetivos manuales de kcal/macros conservando Perfil como entrada válida; mostrar cuándo se aplican. Historial de objetivos desde que se registra realmente un cambio, sin fabricar objetivos históricos. No prescribir déficits ni metas a partir de una foto o de un peso aislado. No descontar automáticamente calorías de entrenamiento ni sumar agua a Nutrición.

Tendencias7/30d: días con registros, kcal y macros conocidos por fecha, días incompletos diferenciados y medias con denominador visible. Un día vacío se muestra como hueco, no cero ingerido. La media es de días con ese nutriente registrado; no una media de toda la alimentación del usuario. Acceso del gráfico al diario de la fecha.

## Futuro planificado, fuera de esta implementación

N4: planificación semanal separada de consumo, lista de compra con unidades compatibles y marcado manual, importación/exportación con control del usuario, catálogo genérico curado, más micronutrientes cuando haya datos, recordatorios opt-in. Una comida planificada nunca suma al consumo hasta confirmarla.

N5: foto/voz/OCR o sugerencias automáticas solo con proveedor real, coste/configuración y privacidad definidos, evaluación y revisión de cantidades por el usuario. Las imágenes no revelan por sí solas aceite, ingredientes ocultos o peso exacto. No crear botones muertos de IA, puntuaciones de salud inventadas, recomendaciones clínicas ni planes para alergias/enfermedades. La base actual es seguimiento general; los objetivos individuales pueden proceder de un profesional.

No implementar N4/N5 por anticipación ni construir un backend genérico para justificarlos. Tampoco detener N1→N2→N3 para pedir permiso entre bloques internos: el usuario solicita este lote completo. Una limitación externa real se documenta y se continúa con lo independiente, sin simular éxito.

## Cierre esperado

Leer los documentos enlazados; ejecutar cambios por bloques completos; una secuencia final de comprobaciones proporcionales. Entregar APK si se genera, capturas reales del diario, búsqueda, cantidad, receta y tendencia, resultados de migración/cálculos y límites. No repetir toda la suite tras cada cambio visual. No borrar pruebas útiles para ahorrar tiempo. No tocar datos del móvil para probar migraciones con fixtures.

No publicar ni hacer push posterior del código sin autorización del usuario. Esta entrega documental sí está autorizada para subirse al repositorio.

## Lectura breve para Claude

1. Este documento: alcance y prioridades.
2. [UX y diseño](nutrition/01_UX_AND_DESIGN.md).
3. [Datos, cálculo e integración](nutrition/02_DATA_AND_INTEGRATION.md).
4. [Roadmap y aceptación](nutrition/03_ROADMAP_AND_ACCEPTANCE.md).
5. [Fuentes](nutrition/04_RESEARCH_AND_SOURCES.md).
6. `design/nutrition-v1/README.md` y su manifiesto: imágenes realmente disponibles, con datos de ejemplo.
