# V1 — Editor de rutinas abierto con Liquid Glass

## Encargo aprobado y base
El usuario aprobó el mockup design/routine-editor-open-glass/approved-reference.png y pide aplicarlo. Base inspeccionada: fase/f1 e065c51, con G2.1 y corrección de volumen asistido. Este documento autoriza el cambio visual del editor de rutinas, conservando funcionalidades, datos y trabajo local posterior. La subida documental no es la implementación Android.

Objetivo: el ejercicio y las series están directamente sobre el fondo, al aire. Glass solo en campos, selectores y botones. NO tarjeta exterior de ejercicio, NO fondo de sección, NO cápsula alrededor de cada fila, NO borde o sombra que encierre el conjunto. Separar por alineación, tipografía y espacio; entre ejercicios, margen vertical o divisor sutil, nunca otra tarjeta grande.

## Referencia y disposición
- Encabezado: volver y «Nueva rutina» o el título de edición actual.
- Nombre en campo de cristal ahumado. Mantener etiqueta accesible incluso si tiene valor.
- «Añadir notas» abre el editor de notas de rutina. Si ya hay notas, permitir verlas/editarlas sin borrar ni colapsar el campo enfocado; estado de expansión conservado al rotar/abrir catálogo. Mantener errores visibles aunque esté plegado.
- Sección Ejercicios con conteo real singular/plural, no el «1 ejercicio» fijo del mockup.
- Cabecera de ejercicio: miniatura a izquierda, nombre y músculo/equipo al lado, información independiente a derecha. Usar catalogId y ExerciseAssetImage existentes. Personalizados conservan nombre editable y acceso a su ficha; no asignar foto de otro ejercicio.
- Reordenar arriba/abajo, notas y menú se muestran sin barra contenedora. Mantener duplicar/eliminar ejercicio y estado habilitado/deshabilitado de reordenación.
- Notas de ejercicio plegables con el mismo criterio de conservación y accesibilidad.
- «Series planificadas», con conteo real de series de trabajo (excluye calentamientos). La captura no obliga a tres series.
- Cabecera TIPO / KG o LB / REPS / DESCANSO. Mantener unidades seleccionadas.
- Cada fila tiene campos flotantes independientes: tipo/número con chevron, carga, repeticiones, descanso y menú. Nada detrás que englobe la fila. Los números y campos deben alinearse de forma consistente entre series.
- Tipo abre opciones «Trabajo» y «Calentamiento (C)», usando warmup actual. No cambiar orden, identidad, lógica de volumen o valores al seleccionar. El número sigue el criterio actual de la app; mostrar C donde corresponde y texto accesible.
- Menú de la serie contiene eliminar. Respetar mínimo de series y reglas actuales, reducir el riesgo de borrado accidental. No añadir funciones que no existan solo por tener un menú.
- «Añadir serie» y «Añadir ejercicio» tienen cristal ahumado y ejecutan sus acciones existentes.
- Guardar rutina es el botón principal champagne flotante con inset inferior. Sin gran dock ni tarjeta detrás. Con teclado e IME, no tapar campos, errores ni acción necesaria; scroll alcanza todo el contenido.

## Descanso y valores
El descanso se conserva internamente en segundos; mostrar mm:ss (120 → 2:00). No pasar el texto «2:00» al parser actual de segundos sin adaptación. Opción mínima: tocar muestra editor rotulado «Descanso en segundos» con el valor actual y confirmación, y al cerrar se formatea en mm:ss. Respetar rango actual 0–3600s, cero válido, mensajes y borrador inválido. No convertir120 en2 segundos ni reinterpretar un valor al rotar.

El botón adicional «Descanso · 2 min» del mockup NO obliga a crear un control global nuevo. Si se mantiene, debe abrir un diálogo funcional con alcance explícito y confirmación antes de aplicar a todas las series; con descansos diferentes mostrar «Descansos variados». Para conservar alcance visual y evitar una función redundante, se puede omitir ese botón manteniendo el descanso editable en cada fila. No mostrar controles sin acción ni un valor2min ficticio.

Carga vacía continúa siendo dato ausente, con raya/placeholder, nunca carga ficticia0. Conservar LoadText y conversión kg/lb. Repeticiones y valores inválidos muestran validación existente. No cambiar descansos, series, notas ni cargas al aplicar el estilo. El ejemplo «Pecho y tríceps» no se precarga en nuevas rutinas.

## Material y medidas orientativas
Paleta ya aprobada: fondo #121416; base ahumada #1D2023/#282C30; champagne #D8C3A5; texto #F4F0EA; secundario #BCB7AE. Tomar tokens actuales donde existan. Reflejo superior fino champagne/blanco con alpha baja, gradiente leve y relleno oscuro suficiente. No brillo continuo, neón, arcoíris ni transparencias que perjudiquen cifras.

La lente champagne del selector de tipo y el botón Guardar se relacionan con core/navigation/LiquidGlassDock.kt. No copiar navegación ni lógica del dock a cada campo. Usar un modificador/composable local sencillo para el material si hace falta; no cambiar FitnessCard ni formularios globales para conseguir este editor. No recapturar/blurear el fondo por cada celda: degradados, bordes y tintes ligeros bastan para un resultado coherente y fluido.

Padding lateral aproximado16–20dp, espacio entre filas8–12dp, separación entre ejercicios24–32dp, radios campos16–20dp. Controles mínimo48dp tocables. Miniatura64–80dp con Fit y esquinas suaves. Valores numéricos18–20sp orientativos, títulos20–22sp, ajustar al espacio y escalado.

En pantallas estrechas o fuente grande, repartir una serie en dos líneas con etiquetas propias sobre el fondo; NO añadir tarjeta por ese motivo. No comprimir cinco controles por debajo48dp para imitar el mockup. Mantener foco/teclado numérico y acciones de edición. Etiquetas TalkBack: ejercicio, serie, campo, valor y acción; no depender solo del color para calentamiento.

## Movimiento
Feedback suave de pulsación y foco con el estilo existente, lente/contorno champagne del campo enfocado. Abrir notas con transición breve opcional; no retrasar entrada de teclado ni guardar. Respetar movimiento reducido. No aplicar estados de serie completada o descanso activo en el editor: aquí se planifica, no se entrena. La pantalla de sesión activa no forma parte de este lote; se podrá adaptar por separado tras revisar el editor.

## Archivos y límites
Partir de feature/training/ui/RoutineEditorScreen.kt: reemplazar el wrapper FitnessCard del bloque de nombre/notas y ExerciseEditorCard por disposición abierta; conservar callbacks/ViewModel, claves de LazyColumn, selector y fichas. Revisar SetEditorRow, WarmupToggle y NumberCell. Reutilizar media, preferencias y diseño existentes. RoutineEditorViewModel/RoutineForm solo se ajustan si es imprescindible para conservar borrador o adaptar la edición del descanso.

No migración de base de datos ni cambios de arquitectura/dependencias para un ajuste visual. No reconstruir G1/G2, no tocar cálculo de volumen corregido, no cambiar el menú inferior ni las dos columnas de Inicio. No rehacer 50 imágenes.

## Aceptación y comprobación final proporcional
V1-01 captura del editor muestra contenido sobre fondo y campos independientes: sin tarjeta exterior de ejercicio ni contenedor por fila.
V1-02 nombre, notas plegadas/con valor, tipo de serie, peso, reps y descanso son editables; duplicar/reordenar/eliminar/añadir y catálogo/ficha siguen funcionando.
V1-03 carga vacía y0, calentamiento, unidades, ayuda máquina/banda/sin indicar y validaciones previas se conservan. Personalizados siguen editables y abren su ficha.
V1-04 descanso120 se ve2:00 y se guarda120; probar0 y3600, error de entrada y retorno del diálogo. El estilo no altera datos al guardar/reabrir.
V1-05 teclado, fuente200%, horizontal y varios ejercicios con muchas series: scroll/foco estables, guardar alcanzable y controles no solapados.
V1-06 navegación a catálogo/ficha y rotación conservan borrador, notas y orden; salir con cambios mantiene confirmación actual.
V1-07 una pasada final build, lint y pruebas relevantes existentes; añadir prueba solo para nueva conversión de descanso si procede. Recorrido manual y capturas antes/después en móvil normal y fuente grande. No ejecutar toda la suite tras cada ajuste de padding.

Entregar capturas reales y breve informe con límites. El acabado debe parecerse al mockup, implementado con controles reales, no una imagen pegada. No hacer push posterior del código sin autorización del usuario.
