# G2 — Recursos por ejercicio, guía visual y progreso

## Estado y encargo
Base inspeccionada: fase/f1 en cb7aa08, G1 publicado: 50 ejercicios, 10 grupos, cinco por grupo. Se revisó el catálogo Kotlin, el esquema Room v4 y el informe G1. No se ha vuelto a ejecutar la app en esta entrega de medios. G1 ya conserva catalogId en rutina y sesión además de exerciseKey. No rehacer catálogo ni migración v4.

El usuario aprueba las referencias de design/exercises-v1/references/: catálogo con miniatura izquierda y ficha con Guía / Progreso / Historial, demostración superior y panel Músculos implicados. Las referencias son conceptos estáticos; sus cifras son ejemplos. No copiar cifras ni textos de técnica ciegamente desde un mockup.

## Inventario y ampliación
Los 50 actuales cubren empuje/tirón, rodilla/cadera y core suficientes para completar este lote visual. No es una lista exhaustiva. Propuestas para una ampliación independiente: zancada inversa, sentadilla búlgara (distinta de dividida), hack squat, face pull, remo con barra, press de pecho en máquina y Pallof press dinámico. No añadir variantes sin fichas, IDs y recursos propios ni cambiar el significado de IDs existentes. Plancha/cardio requieren series por tiempo reales; no simularlas con repeticiones. No bloquean G2.

## Archivos reales y origen
Consultar manifest.json, audit.json y README.md de design/exercises-v1. Cada entrada debe tener thumbnail.webp, anatomy.webp y frames/01.webp,02.webp,03.webp. Las planchas sources/<id>.webp conservan el contexto para revisión visual. prompts/ conserva el prompt de generación por ID. Son ilustraciones originales generadas mediante la herramienta integrada de imágenes, no fotografías documentales ni medios extraídos de Hevy. No atribuir licencia CC a un tercero ni afirmar revisión clínica inexistente. Las consultas de ACE son referencia técnica general, no fuente licenciada de estas imágenes.

Los mapas son esquemáticos; principal champagne y secundarios ocre. Las clasificaciones finas se deben leer de la ficha y del manifiesto, no inferirlas por un píxel. No representan porcentajes de activación ni lesiones. Los recursos se revisan visualmente como ilustraciones; no hay validación profesional de técnica ni anatomía. Si una revisión detecta equipo imposible, músculos equivocados, o poses inconsistentes, marcar el recurso afectado y conservar la guía textual. No ocultar esa limitación ni sustituirlo por una foto de otro ejercicio.

## Miniatura del selector
Imagen a la izquierda en recuadro aproximado 80×72dp (adaptar a fuente grande), fondo oscuro, radios 12dp. Nombre con hasta dos/tres líneas según accesibilidad, debajo músculo y equipo, información separada de seleccionar. Mantener orden/selección/filtros de G1. No recortar barra, manos o pies necesarios para identificar el movimiento: preferir Fit/Contain y fondo #121416. No reutilizar la misma miniatura para variantes distintas. Cargar solo miniaturas visibles; sin decodificar todos los frames en la lista.

## Ficha / Guía
Cabecera con nombre y pestañas Guía, Progreso e Historial; navegación de vuelta conserva selección/borrador/sesión. Hero arriba con imagen completa, 3 fases y control manual anterior/siguiente o deslizar. Modo secuencia opcional en bucle 1→2→3→2→1, aproximadamente 1,2 segundos por posición. Botones Pausar/Repetir y estado «Secuencia de 3 posiciones». Sin audio falso ni barra 00:06 de vídeo inexistente. No interpolar articulaciones con zoom o morph para simular que se generó un vídeo real. Movimiento reducido: frame inicial y avance manual. Pausar reproducción al salir de Guía, poner app en segundo plano o abandonar ficha. El descanso del entrenamiento es independiente y debe seguir comportándose como en G1.

IMPORTANTE: los archivos suministrados son frames estáticos, NO MP4 ni GIF de movimiento continuo. Esto permite una guía secuencial funcional ahora; para reproducir exactamente un vídeo fluido como en la referencia se necesita posteriormente un clip real y revisado de cada ejercicio. No etiquetar estos frames como ese vídeo. El manifiesto video=null refleja esta ausencia. No fabricar recursos/rutas de vídeo.

Debajo: Músculos implicados, imagen anatómica completa con principal/secundarios, leyenda en texto accesible, después preparación, pasos, respiración y errores del catálogo. Hero puede usar contenedor ancho, pero Fit sin recortar figuras de los frames cuadrados. No mostrar play automático en fichas no visibles. Conservar lectura offline, contrastes y fuente 200%.

## Progreso — definiciones sin ambigüedad
Consulta las sesiones COMPLETED por catalogId, independientemente de la rutina, preservando exerciseKey para personalizados/legados. No emparejar nombres iguales automáticamente. Históricos sin catalogId conservan su consulta por exerciseKey y nunca se atribuyen silenciosamente a una definición. No reescribir snapshots. Ordenar por instante real y representar fechas locales.

Solo series completadas de trabajo, no calentamientos, con repeticiones válidas y carga registrada. No usar targetLoadKg/targetReps como realizado. Activas/descartadas/series pendientes no cuentan. Primera/última práctica: sesiones terminadas con al menos una serie de trabajo completada del ejercicio, aunque no hubiera carga externa.

- Carga máxima: máximo loadKg de una serie válida con carga externa, junto a fecha y reps de esa marca. No es un 1RM estimado.
- Volumen por sesión: suma(loadKg × reps) de las series elegibles de ESE ejercicio en la sesión; acumular si aparece dos veces. Mejor volumen: máximo de esos totales, fecha y número de series.
- Carga media por serie: suma de las cargas válidas / número de series válidas, sin ponderar por repeticiones. Mostrar periodo/filtro (por defecto Todo el historial) y número de series. No confundir con volumen medio ni con carga ponderada por repetición.
- Gráfica: máximo de carga por sesión en orden de fecha. No inventar días intermedios; un punto no necesita línea ficticia. El filtro de periodo afecta gráfica y métricas de forma explícita; primera práctica histórica puede seguir siendo global si se etiqueta.
- Cero frente a ausencia: null no es cero. Sin registros: mensaje claro y CTA a entrenar, no 0 kg como récord. Cero externo en peso corporal no estima el peso del usuario ni toneladas desplazadas.
- Dominada asistida: la carga es asistencia, menor ayuda puede indicar progreso; mostrar «Asistencia» y su evolución, no récord de mayor contrapeso como fuerza. Mantener series/reps e historial; no sumar asistencia como volumen levantado. El catálogo actual permite máquina o banda: si el dato no permite distinguir modalidad, no inferir un récord numérico comparable hasta guardar el contexto adecuado.
- Mancuernas/unilateral: documentar y mostrar la convención de carga que usa la app (por mano o total) antes de comparar; no duplicar ni dividir históricos silenciosamente. No comparar modalidades distintas como si fueran equivalentes.

Ejemplo verificable de cálculo, solo fixture: 40kg×10 y 50kg×8 → carga máxima50kg, volumen800kg, media por serie45kg. Una serie de calentamiento, pendiente o de sesión descartada no modifica esos tres resultados. No cargar el fixture en datos reales.

## Historial del ejercicio
Lista por sesión con fecha, nombre de rutina snapshot, series realizadas (peso×reps), marcas y enlace a resumen. Mostrar todas las apariciones del ejercicio sin duplicar sesión en totales. Eliminar/corregir un registro según capacidades existentes debe actualizar métricas. No introducir borrado masivo por este encargo.

## Implementación y rendimiento
Asignación por ID estable y manifiesto; recursos locales con carga diferida. No incluir planchas sources/ ni referencias/prompts en APK; solo los archivos runtime usados. Revisar tamaño del APK al empaquetar medios, sin dependencia de red para abrir ficha. Aprovechar repositorio/DAO/Compose existentes. Una consulta agregada o estado de lectura proporcional es suficiente; no crear servicios ni capas genéricas. Sin cambio Room si los campos actuales bastan; si una semántica de carga necesita nuevo campo, migración segura y documentada en vez de reinterpretar históricos.

## Cierre y aceptación
G2-01 todos los IDs actuales están en manifest; cada ruta indicada existe y decodifica; video ausente explícito.
G2-02 cada tarjeta tiene su miniatura correcta; variantes no se confunden; selección múltiple/info conservan comportamiento.
G2-03 ficha muestra frames reales, secuencia manual/bucle/pause/replay y músculos con leyenda; no promete vídeo.
G2-04 Progreso e Historial se calculan con datos reales; fixture800/45/50, calentamientos, nulos, peso corporal, asistencia y dos rutinas con mismo catalogId comprobados.
G2-05 vacíos, una sesión y varias; unidades, periodo, actualización tras cambios, y legado sin catalogId funcionan.
G2-06 abrir guía no reinicia/pausa descanso; fondo/cambio de tab detiene solo animación de guía; no múltiples jobs.
G2-07 fuente200%, TalkBack, teclado, orientación e insets; controles48dp y movimiento reducido.
G2-08 build, unitarias relevantes, lint y recorrido final del selector/ficha/sesión; registrar tamaño del APK y capturas. Una pasada conjunta; repetir solo lo afectado por fallos. No tests visuales triviales ni reescritura de infraestructura.

Reportar qué medios necesitan corrección; no declarar equivalencia exacta al mockup si hay limitaciones. No push posterior de implementación sin autorización del usuario.

## Fuentes de contraste y límites
- https://www.acefitness.org/resources/everyone/exercise-library/ (biblioteca general de técnica y regiones; consulta, sin copiar medios).
- Catálogo real: app/src/main/java/com/maikol/fitnessapp/feature/training/catalog/ExerciseCatalog.kt en cb7aa08.
- Esquema real: core/database/TrainingEntities.kt, campos catalogId/exerciseKey y performed_sets.
Las imágenes generadas no equivalen a una revisión profesional de cada ejercicio.
