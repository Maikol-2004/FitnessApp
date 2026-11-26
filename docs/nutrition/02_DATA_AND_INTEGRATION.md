# Nutrición — datos, cálculo y fuentes externas

## Compatibilidad con Room v5

Inspeccionar la versión vigente antes de migrar; no fijar un número que colisione con cambios locales posteriores. Exportar esquema y hacer migración aditiva probada con registros existentes. No fallback destructivo, no reset/pm clear del dispositivo del usuario. No alterar tablas ni cálculos de entrenamiento por este lote.

Mantener los IDs y campos históricos de `meal_entries`. Añadir grupo de comida con valor inicial UNSPECIFIED y modo inicial LEGACY_TOTALS. Los valores viejos siguen siendo totales de ese registro; instante/zona/localDate y timestamps se conservan exactamente. No inferir grupo por hora ni cantidades por nombre.

Para registros nuevos con ingredientes, modo FOOD_ITEMS. Los hijos guardan cantidades y snapshot. En un registro solo hay una fuente contable: LEGACY_TOTALS/QUICK_TOTALS o FOOD_ITEMS. Nunca sumar totales manuales y sus hijos a la vez. Edición del modo requiere elección explícita y vista previa, no conversión automática.

## Modelo mínimo, orientativo

- FoodDefinition: ID local estable, nombre/marca/idioma/estado de preparación, fuente (MANUAL/OFF/etc.), sourceId/código como texto, URL/atribución, fecha de consulta y revisión del proveedor. Base con cantidad y unidad (ej.100g/100ml/1ración), nutrientes nullable y versionado. Favorito/archivado y últimas cantidades elegidas son datos del usuario, independientes del contenido externo.
- ServingDefinition: foodId/versión, nombre, magnitud y unidad de masa o volumen o cantidad de base. Una equivalencia se introduce o procede de una fuente; nunca se deduce de «una taza» genérica.
- MealEntry: cabecera compatible, fecha local/instante/zona, grupo, modo y nombre. Los registros antiguos y los rápidos usan nutrientes agregados que ya admiten campos parciales.
- MealItem: UUID, mealId, posición, referencia opcional a alimento/versión o receta/versión y snapshot independiente: nombre/estado, base, cantidad/unidad elegida, conversión, nutrientes para la cantidad, fuente. No cascade desde borrar un alimento/receta hacia un diario.
- Recipe + RecipeIngredient: definición versionada de ingredientes alimentarios, cantidades, raciones, peso final opcional y preparación opcional. Sin recetas anidadas en N3; evita ciclos y no requiere motor genérico de expresiones.
- NutritionGoalHistory: fecha efectiva, valores nullable, origen manual y fecha de creación/cambio. Inicializar un registro desde la fecha real de migración/configuración, no desde el primer día histórico del usuario.

Los nombres de entidades pueden adaptarse a la arquitectura existente. No crear un framework de proveedores abstractos para dos lecturas concretas; una separación pequeña de lectura remota/Room y cálculos puros basta.

## Lectura y escritura

Un repositorio de Nutrición local observa comidas con sus hijos. Construye un agregado de lectura común para modos antiguos y nuevos, que contiene por nutriente valor conocido y estado de cobertura. Adaptar los consumidores de `nutritionTotals`, no mantener dos diarios que divergen.

Guardar cabecera e ingredientes en transacción. Proteger doble envío mediante estado pending y operación de guardado identificable: el reintento del mismo borrador no crea otra comida. Recalcular desde los registros, no incrementar contadores al editar. Copiar crea nuevos IDs pero mantiene el snapshot; un nuevo consumo elegido desde una definición puede usar su versión actual. Un producto que cambia en la API o una receta editada no cambia consumos pasados.

Favorito y reciente se actualizan por el éxito de la operación, no por abrir una ficha. Definiciones en uso se archivan o desvinculan sin borrar snapshots. Eliminar una comida borra únicamente sus hijos y recalcula el día. Si hay deshacer, restaurar exactamente la operación y evitar duplicados.

Borrador: SavedStateHandle para selección/campos simples; persistencia temporal solo si el tamaño de receta exige salir del Bundle, sin duplicar el diario. No meter bitmaps ni respuestas completas de red en SavedStateHandle.

## Cálculos y unidades

Para una base B de la misma dimensión que la cantidad Q, factor=Q/B y nutrienteConsumido=nutrienteBase×factor. Ejemplo: base100g, cantidad150g, factor1,5. Una ración de30g y1,5raciones equivalen45g. No mezclar unidades de masa y volumen sin densidad positiva explícita y procedencia.

Kcal y gramos son unidades de almacenamiento; kJ se convierte mediante kcal=kJ/4,184 y se conserva la procedencia. Si la fuente ya da kcal, usarla sin recalcular desde macros. Alcohol, fibra y redondeo pueden explicar diferencias con4/4/9. No sobreescribir una etiqueta para hacer cuadrar la energía. No inferir macros ausentes desde calorías.

Si se añaden fibra, azúcares, saturadas y sal, son campos independientes opcionales. Sal y sodio NO son el mismo campo: solo derivar sal=sodio×2,5 tras normalizar g/mg y marcar que es derivado. No sumar sal y sodio como dos aportes distintos. No inferir ausencia de alérgenos por un campo vacío.

Null=desconocido;0=dato registrado. Por nutriente: todos desconocidos→null; algunos conocidos→suma conocida con PARTIAL; todos conocidos→KNOWN. El estado parcial de un ingrediente se propaga a receta, comida, día y tendencia. Una comida sin ningún registro tiene estado EMPTY, no KNOWN0. No usar el total de kcal para decidir cobertura de proteínas.

Precisión: almacenar sin redondeos prematuros; presentación kcal sin decimales y gramos con1decimal de forma coherente, valores detallados cuando se edita. Validar finitos, cantidades>0 y raciones>0; rechazar negativos/NaN/Infinity y overflow. Los límites son técnicos, no recomendaciones de ingesta. Aceptar coma/punto en español, mostrar error junto al campo y conservar el borrador inválido al rotar. Unidades kg/lb de entrenamiento no cambian silenciosamente las bases g/ml de etiquetas; si se permite onza alimentaria, su equivalencia debe estar definida y etiquetada.

## Recetas

Sumar los nutrientes de ingredientes con sus factores. Por ración=total/raciones. Por100g solo si hay peso final válido: total×100/pesoFinal. El agua que cambia con la cocción modifica el peso final, no la suma de nutrientes; no estimar pérdidas por cocción ni aceite absorbido. El usuario debe seleccionar ingredientes crudos/cocinados conforme a cómo pesó. Consumir gramos de una receta sin peso final no tiene conversión válida: ofrecer raciones o pedir peso, nunca inventarlo.

Una receta guarda snapshots/versiones de ingredientes, no una referencia mutable que reescriba todo al editar un alimento. Registrar receta guarda snapshot de cantidad y nutrientes; cambiar raciones o ingredientes posteriormente no reescribe el diario.

## Fechas y objetivos

Conservar política existente: instanteUTC + zona de origen + localDate fijada al guardar. Cambiar zona del móvil no mueve comidas históricas. Formulario abierto fija fecha/comida de destino aunque pase medianoche; Hoy se actualiza cuando no hay borrador fijado. Una copia requiere fecha/hora de destino explícitas. Aplicar resolución de cambios de hora de la app, sin calcular duraciones restando horas locales.

Metas de Perfil y Nutrición comparten un escritor/repositorio, no dos preferencias incompatibles. Guardar el cambio y su fecha efectiva de modo recuperable y consistente (si Room/DataStore no comparten transacción, definir reconciliación y no fingir atomicidad). Preferir metas nutricionales con historia en Room como autoridad y adaptar la lectura/escritura de Perfil, conservando valores existentes. Los días previos sin historial se comparan con «Objetivo actual, sin historial», o sin comparación; no inventar metas pasadas.

## Búsqueda externa y código de barras

Proveedor propuesto N2: Open Food Facts, lectura. Su documentación consultada recomienda v3 para integraciones nuevas; revalidar contrato al implementar. Actualmente documenta15consultas de producto/min/IP y10búsquedas/min/IP: la búsqueda externa se confirma con botón, no search-as-you-type. Requiere User-Agent identificable. La base es ODbL; contenidos DbCL e imágenes CC-BY-SA. Consultar términos y documentar atribución/caché separada antes de distribuir datos derivados. Fuente: https://openfoodfacts.github.io/openfoodfacts-server/api/ .

No hay permiso para extraer Fitia ni usar una API privada de competidores. USDA FoodData Central es alternativa pública, pero su API necesita clave; no tratarla como disponible ni incluir una clave en el APK. Fuente: https://fdc.nal.usda.gov/api-guide/ .

OFF adapter: conservar código como string (ceros iniciales), nombre/marca/estado y unidades; elegir nutrientes normalizados de base documentada, no confundir datos del producto preparado con como se vende. Si la base por100ml/100g no es inequívoca, pedir confirmación o rechazar el registro convertido. Ausentes quedan null. No hacer match automático entre alimento genérico y producto de marca. Identidad externa=proveedor+sourceId, no nombre.

Solicitudes con timeout/cancelación, caché limitada y actualización explícita de definición; resultados antiguos no reemplazan la consulta actual. Estado separado: local encontrado, remoto cargando, vacío, error/red,429/503 y producto incompleto. Reintentos acotados según respuesta, no tormentas al cambiar pestaña. Tests con fixtures, no cientos de peticiones reales. Lectura no debe enviar diario, objetivos, peso, fotos ni credenciales a OFF; no habilitar endpoints de escritura.

Completar la identificación/requisitos operativos del proveedor con datos de proyecto autorizados. No rellenar formularios externos, crear cuentas ni publicar información por cuenta del usuario sin su instrucción. Si falta configuración operativa, documentarlo y mantener local/manual utilizables, sin mostrar una integración simulada como lista.

Escaneo Android: elegir una implementación pequeña compatible. ML Kit permite modelo empaquetado inmediatamente disponible o descargable; preferir empaquetado si se promete decodificar offline. Consultar https://developers.google.com/ml-kit/vision/barcode-scanning/android . El escaneo local no hace que el producto desconocido exista offline. Alternativa manual de código siempre accesible. No solicitar micrófono/galería por un simple código. Liberar cámara al salir y pausar con lifecycle; limitar a formatos alimentarios necesarios y evitar múltiples búsquedas del mismo frame.

## Fuera de alcance técnico

Backend propio, cuentas, sincronización cloud, servicios de IA, Health Connect y planes clínicos no son necesarios para N1–N3. No copiar capas de entrenamiento ni crear infraestructura que solo usaría N5.
