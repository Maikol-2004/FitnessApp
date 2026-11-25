# G1 — Biblioteca de ejercicios y entrenamiento guiado

## Problema y objetivo
El editor actual exige conocer y escribir el nombre de cada ejercicio. El usuario necesita una guía para descubrir movimientos por músculo, construir una rutina y seguir series y descansos dentro de FitnessApp. Este es el siguiente trabajo funcional tras el refinamiento visual, no una reconstrucción de Entrenar.

Estado remoto inspeccionado: fase/f1 en 1422ed3 (entrega visual). El usuario afirma que Claude terminó cambios posteriores, que pueden estar solo en local. Antes de integrar nada, conservar y revisar ese trabajo; no reemplazarlo por el código remoto antiguo. La revisión actual confirma RoutineForm/ExerciseForm con nombres libres y PlannedSet con repeticiones, carga y descanso, y un SessionViewModel existente. Reutilizarlos.

## Experiencia principal
Entrenar → Crear rutina → nombre opcional de borrador → Añadir ejercicio → catálogo → grupo muscular → ficha o selección → Añadir N ejercicios → editor con las series editables → guardar → iniciar sesión.

Ejemplo de navegación, no rutina prescrita: el usuario escribe «Hombro», elige Hombros y consulta Press de hombros, Elevaciones laterales y Pájaros. Selecciona los que quiere y vuelve al editor. Una rutina puede mezclar hombros, espalda o cualquier grupo; no forzar un único músculo por rutina ni incluir automáticamente todos los ejercicios del grupo.

## Pantalla del catálogo
Cabecera «Elegir ejercicios», buscador visible y filtros por músculo y equipo. Buscar nombres y alias en español sin distinguir mayúsculas ni tildes; conservar consulta, filtros, scroll y selección al abrir/cerrar una ficha. Filtros combinados con AND entre categorías. Contador de seleccionados y acción fija «Añadir N ejercicios» sin tapar la lista. Volver/cancelar no modifica el borrador ni duplica registros. Carga local sin esperas ficticias; resultado vacío útil con opción de limpiar filtros.

Grupos: Pecho, Espalda, Hombros, Bíceps, Tríceps, Cuádriceps, Isquiotibiales, Glúteos, Gemelos y Abdomen/core. Clasificar músculo principal y secundarios; un movimiento compuesto puede aparecer en varios filtros sin duplicar su identidad. Equipos: Peso corporal, Mancuernas, Barra, Máquina, Polea, Bandas y Banco cuando corresponda; distinguir equipo principal de accesorio.

Cada fila: nombre completo, músculo principal, equipo y miniatura si hay recurso real. La ficha se abre desde un control de información separado de la selección para que tocar información no añada accidentalmente un ejercicio. Selección múltiple clara con check champagne. Conservar orden de selección al insertar. Si ya está en el borrador, mostrarlo y evitar duplicación accidental; permitir duplicarlo solo con acción explícita cuando sea útil.

## Ficha de ejercicio y enseñanza
Nombre, grupo principal/secundarios, equipo, dificultad descriptiva, preparación, 3–5 pasos de ejecución claros, respiración y 2–3 errores comunes redactados específicamente para ese movimiento. No reutilizar consejos genéricos idénticos en todo el catálogo. Texto en español comprensible para principiantes. Botón Añadir o estado Seleccionado. La ficha debe ser accesible desde el editor y durante la sesión sin perder estado ni pausar/reiniciar el temporizador accidentalmente.

Representación visual: no confundir fotos decorativas de las categorías con demostraciones de técnica. Usar únicamente imágenes/ilustraciones/vídeos reales disponibles y con permiso; documentar origen/licencia en un manifiesto. No descargar ni copiar los medios, textos ni base de datos de Hevy. No generar una ilustración anatómicamente dudosa y presentarla como enseñanza validada. Si no hay demostración, mostrar «Guía en texto» con un icono y las instrucciones completas; no dejar una imagen rota ni prometer un vídeo inexistente. La ausencia de medios no bloquea el catálogo funcional offline. Animaciones de demostración e ilustraciones revisadas serán un lote posterior si faltan recursos; declarar esta limitación al cerrar.

## Catálogo inicial concreto
Entregar al menos 50 ejercicios diferenciados, con identificadores estables y fichas completas, cubriendo todos los grupos siguientes. Esta lista es cobertura de biblioteca, no una sesión ni recomendación de volumen:

- Pecho: press banca con barra, press banca con mancuernas, press inclinado con mancuernas, aperturas en máquina, flexiones.
- Espalda: jalón al pecho, remo sentado en polea, remo con mancuerna, dominada asistida, remo con apoyo de pecho.
- Hombros: press sentado con mancuernas, elevaciones laterales, elevaciones laterales en polea, pájaros con mancuernas, aperturas inversas en máquina.
- Bíceps: curl con mancuernas, curl con barra EZ, curl martillo, curl en polea, curl predicador.
- Tríceps: extensión en polea con cuerda, extensión en polea con barra, extensión por encima de la cabeza en polea, extensión tumbado con barra EZ, extensión unilateral con mancuerna.
- Cuádriceps: sentadilla goblet, sentadilla con barra, prensa de piernas, extensión de rodilla en máquina, sentadilla dividida.
- Isquiotibiales: curl femoral sentado, curl femoral tumbado, peso muerto rumano con barra, peso muerto rumano con mancuernas, curl femoral con deslizadores.
- Glúteos: hip thrust con barra, puente de glúteos, patada en polea, abducción en máquina, subida al banco.
- Gemelos: elevaciones de talón de pie, elevaciones sentado en máquina, elevaciones en prensa, elevación unilateral con peso corporal, elevaciones de pie en máquina.
- Abdomen/core: crunch en colchoneta, crunch en polea, crunch en máquina, crunch inverso, crunch en fitball.

Algunos movimientos requieren más aprendizaje; la biblioteca no debe calificarlos todos como principiantes ni recomendar automáticamente los más técnicos. No asignar cargas calculadas ni prescribir programas personales a partir del grupo seleccionado.

## Editor de rutinas
Añadir ejercicio abre el catálogo como camino principal. Mantener «Crear ejercicio personalizado» como opción secundaria para rutinas antiguas o movimientos ausentes. Al elegir un ejercicio mostrar nombre e información, no volver a pedir el mismo nombre. Reordenar, quitar y editar notas/series sigue funcionando. Se permite cambiar un ejercicio con confirmación si afecta la información programada.

Reutilizar el valor de descanso y el mecanismo de series existentes; mostrar valores iniciales editables y diferenciados de datos realizados. No rellenar cargas ficticias ni historial. Las sugerencias iniciales son plantillas configurables, no registros completados ni una recomendación personalizada. No ampliar en G1 el modelo a tiempo/distancia si el motor actual solo admite repeticiones; los 50 ejercicios de este lote son de repeticiones. Plancha, cardio y ejercicios por duración se implementarán después con soporte real de series temporizadas, no simulados como repeticiones.

## Sesión guiada
Mostrar ejercicio actual, acceso a su guía, serie actual y total, repeticiones planificadas y carga editable, progreso real de series y siguiente ejercicio. Aclarar planificado frente a realizado. Al confirmar una serie con guardado satisfactorio, usar el descanso real configurado; nunca lanzar un descanso por una escritura fallida. Mantener ajustar/saltar descanso si existe, y añadirlo de forma mínima si falta. Al terminar descanso avisar visualmente y respetar preferencia de sonido/vibración y permisos existentes; no solicitar permisos ajenos al alcance.

No avanzar ni completar automáticamente series. Reutilizar recuperación de sesión y temporizador frente a rotación, bloqueo/segundo plano y reentrada. No crear un servicio/background framework sin comprobar primero qué hace falta y qué cubre la implementación existente. No prometer alarmas con la app cerrada si no están implementadas y probadas.

## Modelo e integración mínimos
Preferir catálogo local versionado (Kotlin o JSON ya compatible con el proyecto), sin API externa, login ni red obligatoria. Campos mínimos: catalogId, nombre, alias, principal, secundarios, equipos, dificultad, pasos, errores, recurso visual opcional. IDs inmutables, no derivarlos de traducciones. Conservar identidad de instancia de ejercicio dentro de la rutina; catalogId identifica la definición, no la posición de una rutina.

Examinar el key existente antes de reutilizarlo: puede representar identidad de historial; no cambiar su semántica para insertar catalogId. Si hace falta una referencia nueva, hacerla opcional y migrar sin pérdida. Mantener nombres/notas de rutinas y sesiones antiguas, incluso si no tienen correspondencia en catálogo. No emparejar automáticamente por nombre ni fusionar historiales diferentes. Las sesiones deben conservar su snapshot al modificar una rutina o el catálogo. No permitir que una actualización del catálogo cambie el entrenamiento ya registrado.

## Estilo y movimiento
Mantener Titanium Champagne y Liquid Glass existentes. Buscador amplio, iconos musculares claros, filas legibles, contraste y controles de al menos 48dp. Respetar inset del teclado y barra inferior. Transición corta al abrir ficha, check suave de selección y feedback de pulsación; reutilizar componentes, sin framework nuevo. El movimiento reducido desactiva desplazamientos decorativos. Ninguna animación retrasa la selección o la guardada.

## Criterios de aceptación y una validación final
G1-01: crear rutina sin escribir un solo nombre de ejercicio manualmente, filtrando Hombros y seleccionando varios ejercicios.
G1-02: al menos 50 definiciones completas, IDs únicos y todos los grupos cubiertos; alias y búsqueda sin tildes funcionan; filtros combinados y vacío funcionan.
G1-03: abrir ficha, volver, cancelar selector o rotar conserva borrador y selección sin duplicados. La guía es específica, accesible y disponible offline.
G1-04: rutina con grupos mezclados, orden, series/notas y descansos editables; guardar y reabrir conserva todo.
G1-05: rutinas y sesiones previas siguen disponibles; personalizados siguen funcionando; cambios de catálogo no reescriben historial. Si hay migración, verificar con datos previos.
G1-06: iniciar rutina nueva, consultar guía, completar serie, descanso, saltarlo/ajustarlo y finalizar; resumen/historial reales. Fallo de guardado no muestra éxito ni arranca descanso.
G1-07: reentrada tras segundo plano conserva la sesión/descanso; navegación no crea dos temporizadores.
G1-08: fuente 200%, TalkBack en selección y ficha, teclado, scroll e insets sin controles ocultos; preferencias de unidades y movimiento reducido conservadas.
G1-09: todos los medios existentes o ausencia explícita; ningún recurso de Hevy copiado ni animación/vídeo falso.

Trabajar por bloques útiles, no lanzar build/tests por cada fila. Una secuencia final de build, unitarias relevantes, lint, recorrido funcional y migración solo si existe. Pruebas centradas en identidad/catálogo/búsqueda, borrador, persistencia y temporizador, evitando tests que solo repitan la implementación visual. Repetir únicamente lo afectado al corregir. Entregar capturas del selector, ficha, rutina y sesión, APK si se genera e informe breve con resultados reales y límites. No afirmar guía visual demostrativa si solo se entrega texto. No hacer push posterior de implementación sin autorización.

## Referencias de producto consultadas
Inspiración de flujo, sin copiar interfaz, textos ni recursos:
- https://www.hevyapp.com/features/exercise-library/
- https://www.hevyapp.com/features/exercise-programming-options/
- https://www.hevyapp.com/features/workout-rest-timer/
