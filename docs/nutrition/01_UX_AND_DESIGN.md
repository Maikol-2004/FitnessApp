# Nutrición — estructura visual e interacción

## Lenguaje compartido con la app

Fondo #121416, superficies/tintes de controles #1D2023 y #282C30, champagne #D8C3A5, texto #F4F0EA y secundario #BCB7AE. Usar los tokens existentes, no aplicar colores distintos a todo el tema. Conservar el menú Liquid Glass de cinco destinos y dos tarjetas por fila en Inicio.

Nutrición es un apartado anidado: volver lleva al lugar de origen; no añadir una pestaña global. Dentro: Diario, Recetas y Tendencias. Biblioteca de alimentos y objetivos se abren por acciones contextuales. No una segunda barra flotante que compita con la global.

Sustituir la cabecera fotográfica alta de NutritionRoute por esta cabecera compacta solo en Nutrición. No cambiar ModuleScreen global ni las otras categorías para conseguirlo.

Contenido al aire sobre fondo oscuro. Cristal ahumado para campos, filtros y acciones; sin grandes tarjetas alrededor de cada comida ni paneles anidados. Separar mediante espacio, tipografía e iconos de línea. Reutilizar el material del editor sin copiar su lógica de entrenamiento. No blur por cada fila, animaciones perpetuas ni fotos enormes que desplacen los datos.

## Diario

Cabecera compacta «Nutrición» y acceso a objetivos; fecha con anterior/siguiente y volver a Hoy. Selector por calendario accesible que conserva la fecha elegida. Los registros consumidos siguen permitiendo hoy y pasado; planificar fechas futuras pertenece a N4.

Resumen: aro de progreso solo con objetivo y valor real; texto «kcal registradas», objetivo y diferencia. El aro se limita visualmente al100%, pero el texto conserva exceso real. Tres macros con nombre, gramos conocidos, objetivo si existe y barra. Las cifras siempre llevan unidades. No usar solo rojo/verde para éxito/fracaso.

Luego Desayuno/Comida/Merienda/Cena/Sin clasificar, icono y subtotal real, botón Añadir contextual. Cada comida se expande para mostrar nombre, cantidad y unidad, energía conocida o raya, y menú editar/mover/copiar/eliminar. Si hay calorías de algunos alimentos pero no de todos, subtotal rotulado «Parcial». Una sección sin registros dice «Sin registros» y no prueba que se haya consumido0kcal.

Acción principal «Registrar alimento» disponible sin tapar lista ni teclado. El usuario puede registrar en una sección vacía desde su botón, sin rodeos. Al guardar, confirmar mediante actualización real del diario y feedback breve. No cerrar el formulario hasta confirmar escritura; en error mantener valores y opción reintentar.

## Búsqueda y registro

Arriba fecha y comida de destino, conservadas al cambiar filtros. Campo con etiqueta «Buscar alimento o marca», historial local/recientes, favoritos y mis alimentos. Resultados locales pueden filtrar al escribir; resultados remotos requieren Buscar en línea/Enter y manejo de carga/error independiente. No presentar «buscando» infinito si está sin conexión.

Fila: nombre, marca opcional, crudo/cocinado o estado desconocido, fuente y base nutricional; miniatura opcional. La marca «Fuente» identifica procedencia, no revisión profesional. No usar un check «Verificado» por venir de una API pública. Tocar abre detalle; añadir rápido solo si el usuario ya tiene una cantidad elegida y visible.

Detalle: etiqueta por100g/100ml/ración, cantidad numérica con coma o punto, unidad y equivalencia explícita. Antes de añadir, mostrar «Para tu cantidad», kcal/macros conocidos y destino. Carga instantánea de valores al cambiar cantidad, con cálculos puros; el cálculo visual no guarda hasta confirmar. Cero no es una ración válida; cantidades positivas y límites técnicos razonables con mensajes por campo.

Raciones: no ofrecer «cucharada», «taza» o «unidad» sin masa/volumen de ese alimento. Cuando una ración tiene30g,1,5raciones equivalen a45g y se indica. Para líquidos no convertir ml↔g sin densidad conocida. Nombre de alimento crudo/cocinado siempre visible en la confirmación.

Producto no encontrado: «No encontramos ese código», introducir manualmente o crear alimento con su código. Falta de nutrientes: mostrar cuáles faltan y permitir registro parcial informado. Nunca ocultar ni borrar campos introducidos por el usuario al llegar tarde una respuesta de red.

## Crear alimento y registro rápido

Crear alimento guarda una definición reutilizable, no consumo. Nombre obligatorio, marca/estado opcionales, base de la etiqueta y nutrientes. Kcal y macros pueden faltar; informar al menos uno. Mostrar una confirmación separada «Guardar alimento y registrar» solo si realiza ambas operaciones de forma atómica o recuperable.

Registro rápido añade un total de comida o alimento sin desglose. Conserva el comportamiento anterior y se identifica «Totales manuales». No pedir un peso inventado. Fecha/hora y grupo editables.

«Guardar como receta» solo puede usar ingredientes reales de un registro FOOD_ITEMS con cantidades válidas. Para una comida antigua o rápida que solo tiene totales, ofrecer crear receta manualmente; nunca convertir sus calorías en ingredientes imaginados.

Favoritos y recientes pertenecen al usuario; nunca sobreescribir una entrada propia con una actualización remota. Si el usuario corrige un alimento externo, crear una versión/copia privada con origen claro, no modificar el proveedor públicamente.

## Recetas y tendencias

Receta: nombre, ingredientes, cantidades y fuente, total/ración/100g si existe peso final. Raciones positivas; peso final opcional >0. Guardar no añade al diario. Al registrar, elegir cantidad y destino; seguir disponible aunque después se archive la definición.

Tendencias:7/30d, periodo visible, barra por día y huecos de días sin información. Listado textual equivalente al gráfico y acceso a fecha. Mostrar cobertura («5días con calorías registradas de7») y marcar parciales. No asignar «comiste poco» a una agenda incompleta ni diagnosticar carencias por campos ausentes.

Objetivos: valores opcionales en kcal/g, editor claro con fecha efectiva. No autocompletar un objetivo de pérdida de peso. Si el reparto energético de macros no coincide exactamente con kcal, aviso informativo y editable, no bloqueo: etiquetas y preferencias pueden usar otros criterios.

## Movimiento y accesibilidad

Transiciones cortas de selección/expansión, feedback al guardar y progreso ligado a datos. Respetar movimiento reducido. Sin animación artificial de cocción o carga si los datos ya están disponibles.

48dp mínimo por acción, foco y etiquetas persistentes; error junto al campo, teclado decimal, selección de valor al editar y unidades pronunciables por TalkBack. Hasta200% fuente y horizontal: macros/campos se apilan; no scroll horizontal obligatorio. CTA por encima del teclado y padding suficiente para alcanzar la última fila. Pestañas con rol/estado y gráficos con alternativa textual.

## Uso de las imágenes de referencia

Las láminas contienen ejemplos, no datos nutricionales curados ni instrucciones para prescribir una dieta. No copiar fechas, alimentos ni objetivos del mockup a producción. Las fotos pequeñas son ilustrativas del concepto; en runtime usar imágenes del usuario/proveedor con procedencia y atribución cuando corresponda, o un icono neutro por categoría. No generar una imagen de producto de marca para aparentar que coincide con un código.

La búsqueda local del recurso de pautas Compose no devolvió coincidencias verificadas; se aplican como orientación general los criterios anteriores y los patrones existentes del proyecto, sin introducir un nuevo sistema de diseño.
