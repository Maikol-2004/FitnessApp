# Corrección visual después de H2

> Actualización de alcance: docs/10_FUNCTIONAL_MVP.md y docs/11_LIQUID_GLASS.md prevalecen para funcionalidad, material y secuencia de pruebas. Mantener DOS columnas.

Decisión actual del usuario del 25 de noviembre de 2026. Se mantienen exactamente **dos tarjetas por fila**, seis categorías y orden actual. La propuesta intermedia de una columna se descartó expresamente y NO debe implementarse. La paleta Titanium Champagne sigue intacta. Este documento delimita el ajuste visual. El encargo posterior docs/10_FUNCTIONAL_MVP.md autoriza además completar todos los registros básicos en un lote, sin rehacer H1/H2.

## Qué se ha revisado
Capturas del usuario de Inicio, Entrenar vacío y Nueva rutina. Código de fase/h2 en 49539280be364d4444909d2744bf166d0076f9e5: HomeComponents.kt, HomeScreen.kt, PhotoBackground.kt, FitnessNavHost.kt, PrimaryButton.kt, Motion.kt y TrainingComponents.kt. Main sigue en a0fc8cc; el código H1/H2 está en sus ramas de fase. Comprobar estado local y HEAD al ejecutar: puede haber trabajo posterior.
Problema concreto: CategoryPhotoCard coloca título a ancho completo, descripción al 80 %, fotografía Crop a contenedor completo y scrim inferior 70 %. Eso oscurece el objeto y deja texto sobre él. Cambiar solo el tamaño en píxeles o Alignment.CenterEnd no basta. No aumentar resolución ni instalar librerías por este problema.

## Tarjetas de Inicio manteniendo dos columnas
Conservar los seis destinos, orden y función. En teléfono normal (incluido 320–412 dp), seguir con dos por fila; no pasar a una columna para conseguir el encuadre. Excepción de accesibilidad: si fuente extrema/zoom exige un modo alternativo para poder leer, conservar el fallback accesible existente, sin usarlo como diseño normal ni reducir a texto diminuto. La opción por defecto debe mostrar dos tarjetas.
Usar los nuevos recursos `design/polish-v2/photos/category_*_right_v2.webp`, editados a partir de las fotos previas para reducir el objeto, dejarlo completo en el tercio derecho y reservar fondo oscuro a la izquierda. No borrar los v1 que usan otras pantallas. Copiar únicamente los seis nuevos recursos utilizados a drawable-nodpi y cambiar HomeCategory, no sustituir fotos de gimnasio/resumen/entrenamiento globalmente.

Anatomía de tarjeta a tamaño normal:
- Mantener margen de pantalla 16 dp, gap 12 dp, radio 20 dp. Referencia de altura 140–152 dp; el contenido manda. No fijar una altura que recorte. No hacerlas más altas solo para alojar un primer plano gigantesco.
- Icono de categoría 18–20 dp en esquina superior izquierda, con objetivo táctil de toda la tarjeta >=48 dp. Título 17–18 sp semibold y línea 22–24 sp en una banda superior tranquila, sin el objeto detrás. Puede saltar a dos líneas naturales si hace falta; no bajar a 10–12 sp ni deformar el texto.
- Descripción a la izquierda, aprox. 52–58 % del ancho útil, nunca el 80 % actual. 13–14 sp con interlínea 18–19 sp, 2–4 líneas naturales. Sin ellipsis que oculte la función. Texto principal blanco roto; descripción legible.
- Objeto reconocible a la derecha, unos 34–40 % del ancho total, y preferentemente por debajo de la banda del título. Completo y algo más pequeño: mancuerna con mango/cabezas; reloj con esfera/parte de correa; bowl; botella con tapón y base; almohada/cama; libreta/cinta. El texto de descripción no cubre el objeto.
- La foto continúa como fondo de toda la tarjeta, no un recuadro independiente con borde, no una miniatura cuadrada añadida encima. La zona izquierda es fondo fotográfico oscuro y libre. Evitar cortes verticales duros; fundir con un degradado suave hacia el objeto.
- Ajustar ContentScale y alineación según dimensiones reales y cada sujeto. No FillBounds ni escalado no proporcional. Las nuevas composiciones guardan margen para Crop moderado, pero si este recorta el tapón/reloj/mancuerna, usar ajuste que preserve su silueta o una capa de imagen alineada a la derecha con recorte controlado dentro del mismo fondo. No tratar Alignment.CenterEnd como una garantía de recolocar un objeto ya centrado.
- Scrim localizado al lado del texto; el sector derecho no necesita la capa negra inferior al 70 % actual. Comenzar con degradado horizontal oscuro hasta 55–60 % y transparente después; bajar/eliminar el inferior solo en las categorías. Revisar contraste sobre el resultado real y claridad de cada foto. No cambiar PhotoBackground globalmente si oscurece o rompe heroes/sesiones ya correctos; parametrizar el uso de categorías con la API existente si es suficiente.

Textos compactos para que el área izquierda sea útil sin invadir la imagen:
| Tarjeta | Descripción |
|---|---|
| Entrenamiento | Planifica rutinas y sesiones. |
| Salud | Tu actividad y datos de salud. |
| Nutrición | Comidas, calorías y macros. |
| Hidratación | Agua y objetivo diario. |
| Sueño | Tu descanso y su evolución. |
| Progreso | Tus marcas, peso y evolución. |

No añadir información o funciones que no existan. Conservar descripciones completas en la pantalla de destino si ayudan al usuario. El ajuste es texto/encuadre, no rediseño del dominio.

## Menú elegido posteriormente
El usuario eligió el PRIMER Liquid Glass. Aplicar docs/11_LIQUID_GLASS.md: cápsula de cristal ahumado, lente champán que sigue la selección y cinco etiquetas visibles. La propuesta de cápsula opaca mate anterior queda descartada. Para los botones prevalece también el documento11; el resto de este documento mantiene el encuadre de dos columnas.

## Botones superiores y acciones
Refinar PrimaryButton y SecondaryButton existentes, sin una tercera jerarquía de botones ni nuevo framework. Acción principal champán mate, texto oscuro, icono 18 dp cuando aporte significado (más para Crear, check para Guardar, flecha para Ver salud). Altura 48–52 dp, radio 14–16 dp y espaciado 12–16 dp; consistente en Inicio/Entrenar/editor. Evitar cambiar ancho al cargar. Acción secundaria carbón elevado y texto claro; destructiva solo rojo/etiqueta explícita.
Conservar pressScale, gating busy, guardado real y retraso de loader ya implementados. Feedback al pulsar en 120 ms y retorno 150 ms, sin animación por cada tecla. No añadir una segunda escala a un Button ya animado. Icono puede trasladarse máximo 2 dp al soltar durante <=150 ms si no distrae; es opcional, no motivo para una clase nueva. Nada de sonido, confeti ni progreso falso al navegar.
Control Perfil de cabecera: cuadrado redondeado 48 dp, radio 16 dp, superficie elevada, icono/initiales champán y borde tenue; mantener acción y etiqueta accesible. Atrás en editor/detalle sigue el patrón nativo dentro de hit target 48 dp; no sustituirlo por un icono sin semántica. No rediseñar validación, tiempos ni navegación de sesiones para cambiar su apariencia.

## Inicio, Entrenar y editor vistos en las capturas
Inicio: respetar resumen de actividad y sus estados reales, sin inventar datos ni conectar salud antes de H3. Puede compactarse el espacio del aro vacío si hace falta, pero no crear otra tarjeta o nueva métrica. Prioridad absoluta: seis tarjetas legibles y nuevo dock. El resumen no debe consumir otro bloque de refactorización.
Entrenar: conservar hero, rutinas e historial. Mejorar el CTA Crear rutina con icono y proporciones comunes. No rellenar vacío con rutinas de muestra en producción ni cambiar lógica de repositorio. No es necesario rehacer el hero para este encargo.
Editor: aplicar únicamente el mismo estilo al botón Guardar, volver y menús; conservar tabla, campos, orden y validación funcional H2. Eliminar solo duplicación visual claramente local si se detecta. No añadir secciones plegables, pasos de wizard, animaciones de cada fila o autosave distinto por iniciativa.

## Trabajo acotado y consumo razonable
Primero abrir los componentes indicados, inspeccionar las imágenes nuevas y editar allí. No releer y reconstruir toda la arquitectura del proyecto para cambiar dos componentes visuales. No nuevos módulos Gradle, tablas Room, migraciones, interfaces/repositorios, casos de uso, servidores, analítica ni dependencias de UI/animación.
Si el trabajo se limita al aspecto, no cambiar ViewModels, cálculos, transacciones o SessionTiming. No borrar pruebas o protecciones existentes bajo la excusa de ahorrar tokens. Una función compartida o un pequeño composable usado de verdad es razonable; scaffolds vacíos y abstracciones especulativas no.
Orden: corregir una tarjeta de Entrenamiento y otra Salud y mostrar captura temprana con DOS COLUMNAS; aplicar a las seis; afinar dock/botones; compilar y revisar. Continuar sin pedir autorización por cada paso interno. No generar seis alternativas de menú, narraciones largas, informes repetidos ni decenas de tests de estilo. Si dos intentos de encuadre no mejoran, comparar con la composición nueva y corregir la causa en lugar de lanzar refactorizaciones generales.
Pruebas proporcionales: una compilación Debug sobre el resultado final; comprobar visualmente seis fotos, scroll, cinco destinos, selección, Atrás, CTA Crear y Guardar existentes, teclado, texto grande y movimiento reducido. Ejecutar tests existentes de navegación/perfil afectados si el cambio toca esa conducta. Una prueba de humo crear/guardar/abrir rutina confirma que el estilo no rompió el CTA. Suite completa de repositorio/migración/release solo si se cambió lógica/esquema o aparece una regresión que la justifique; no clean ni builds Release repetidos sin necesidad. No crear tests que solo comparen un hex o una medida con la misma constante.

## Aceptación de esta corrección
1. Teléfono normal mantiene dos columnas y tres filas, mismo orden y cinco destinos.
2. Descripciones quedan a la izquierda; los seis sujetos se reconocen a la derecha sin taparlos con texto o un scrim negro excesivo.
3. La imagen no se deforma ni se corta de forma que pierda su significado. El reloj ya no queda debajo de Salud/descripción.
4. Dock distingue claramente la selección sin mover las otras celdas, respeta insets y deja ver el final del scroll.
5. Botones consistentes con estados pressed/disabled/busy reales y sin disparos dobles o espera artificial.
6. Misma funcionalidad/datos de H2, sin cambios de DB ni funciones H3.
7. Movimiento reducido y fuente grande no rompen interacción ni eliminan información esencial.
8. Evidencia breve: antes/después de Inicio en el mismo emulador, Entrenar y editor con el botón nuevo; comando de build y pendientes reales. No afirmar ejecución si no se pudo abrir el emulador.

## Límites de la entrega de especificaciones
Este commit añade recursos y un encargo concreto; no modifica el código Kotlin ni ejecuta la app. Claude aplicará la corrección sobre la rama que contiene H2. La lámina intermedia de una columna fue descartada y no se incluye como referencia activa. Las nuevas fotos son variantes de encuadre, no imágenes originales históricas recuperadas.
