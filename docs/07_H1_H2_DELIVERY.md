# Plan maestro de entrega H1 y H2

Actualización del usuario del 24 de noviembre de 2026. Este documento y `08_MOTION.md` actualizan H1 y H2 del paquete inicial. Se aprueban como objetivo colores, estructura y movimiento; la ejecución se autoriza por fase. No se implementa código Android al subir este plan.

## Qué significa terminar una fase
Entregar una parte que pueda usarse y revisarse, con diseño, estados, interacciones, persistencia donde corresponda y movimiento incluidos. No dejar texto provisional, fotos ausentes, acciones falsas o animaciones prometidas para después dentro del alcance de esa fase. No declarar toda la aplicación terminada: los módulos de salud y hábitos conservan su calendario H3–H6.

Cada paso interno produce una captura y una comprobación concreta. Se continúa entre pasos internos de la misma fase sin pedir aprobaciones repetidas. Al cerrar H1 se solicita aprobación para H2. No iniciar H3. Si hay una limitación real de entorno o una decisión pendiente, identificarla; no convertirla silenciosamente en PASS. Un error posterior se corrige sin reabrir arbitrariamente el diseño ya aprobado. Las fases siguientes añaden datos o capacidades por las fronteras previstas, no rehacen componentes finalizados.

## Estado real revisado antes de redactar
Repositorio `Maikol-2004/FitnessApp`, rama main, commit base `6ba4e3944e9264f4a1f90713f6200df799d40a9b`. Existe un módulo :app, DI manual AppContainer, Theme.kt, TitaniumChampagneTokens.kt, FitnessCard/PrimaryButton/StateViews, contrato TopLevelDestination, FoundationRoute, DataStore de unidades y Room v1 con app_metadata. Navigation Compose está instalado; FitnessNavHost solo muestra foundation. No hay código H1/H2 en ese commit.
Los tokens ya coinciden con la paleta aprobada. Reutilizarlos; actualizar su documentación de propuesta a aprobada, no duplicarlos. H0_REPORT.md informa de build, unitarias, lint y pruebas instrumentadas en API 37; son resultados históricos del autor de H0, no pruebas reejecutadas al preparar este plan. API 28, móvil físico y TalkBack manual siguen pendientes. Revisar HEAD/local antes de ejecutar porque puede haber trabajo posterior al commit inspeccionado.

## Jerarquía de referencias
1. Instrucciones actuales del usuario.
2. Este documento, 08_MOTION.md y los valores de `design/titanium-champagne/colors.json`.
3. Captura reciente `design/references/cards-layout-user-approved.webp`: anatomía de tarjetas, texto mayor, icono y descripción.
4. `home-h1-approved.webp`: composición general; el estado vacío es la guía inicial.
5. `training-h2-proposal.webp`: orientación visual nueva para las cuatro vistas de H2; no prueba de implementación ni aprobación individual de cada texto/píxel.
6. Documentos 01–06 para requisitos generales que no contradigan esta actualización.

El texto y los colores numéricos prevalecen sobre pequeñas diferencias en imágenes generadas. No copiar la interfaz como bitmap. Los siete fondos fotográficos adjuntos son nuevos recursos, no las diez imágenes originales ausentes. Las referencias contienen métricas DEMO o ejemplos: nunca insertarlos como datos personales reales.

## Sistema visual cerrado para H1 y H2
| Token | Hex opaco sRGB | Uso |
|---|---|---|
| background | #121416 | Fondo y barras |
| surface | #1D2023 | Paneles y tarjetas de datos |
| surfaceRaised | #282C30 | Campos y capas elevadas |
| primary | #D8C3A5 | CTA, iconos destacados, selección y progreso |
| onPrimary | #17191B | Texto de botón champán |
| onSurface | #F4F0EA | Títulos, cifras y texto principal |
| onSurfaceVariant | #BCB7AE | Explicaciones y unidades |
| outline | #76736D | Límites funcionales |
| error | #FFB4AB | Error con texto e icono |
| success | #A7C8B0 | Confirmación semántica auxiliar, no acento general |

No color dinámico, neón ni metalizados en superficies o botones. Respetar fondos exactos: no dejar tonal elevation o surfaceTint imprevistos. Fotografías con luz cálida natural no cambian los tokens. Mismo tema en barras de sistema con iconos legibles, como la corrección ya realizada en H0.
Márgenes 16 dp; gaps de tarjetas 12 dp; entre secciones 24 dp; radio tarjeta 20 dp, botón/campo 12 dp; área táctil >=48 dp. Sistema de texto existente, afinado si hace falta: cabecera 28/34 sp, sección 22/28, título de categoría 18/24 semibold (20/26 cuando quepa), descripción 14/19, cuerpo 16/24 y etiquetas 14/20. No resolver cortes reduciendo fuentes esenciales. Font scale 200 %: reflujo y scroll; una columna cuando dos no sean legibles. Dimensiones iniciales propuestas, comprobar en el emulador real.

## Fotografías y tarjetas a fondo completo
Usar `design/assets/photos/` según assets-manifest.json; copiar solo recursos utilizados a drawable-nodpi con los mismos nombres válidos. Estos WebP son versiones optimizadas de los originales generados, sin cambios de contenido. No incluir los PNG de generación duplicados en el APK. Guardar las referencias de pantalla solo en design/, nunca empaquetarlas en runtime.
La foto ocupa todo el rectángulo de la tarjeta, recortada por el radio. Encima: degradado horizontal negro 85 % a transparente tras la zona izquierda y protección inferior hasta 80 % cuando la foto lo requiera; validar contraste real de títulos/descripciones. Icono lineal champán de 20–24 dp arriba a la izquierda, título con padding 12–16 dp, descripción debajo con gap 4–6 dp. Sujeto principal visible a la derecha; ajustar ContentScale.Crop/alineación por imagen y, si hace falta, reforzar el degradado. No separar la imagen en una miniatura o en media tarjeta. No tintar toda la foto de champán ni dejar una caja sólida que oculte el fondo.
Dos columnas a tamaño normal: categoría con altura mínima inicial 140–156 dp en teléfonos 360–412 dp; crecer por texto. Título hasta dos líneas naturales, descripción 2–4 líneas sin ellipsis esencial. No repetir tamaño de texto minúsculo de la captura enviada. A 200 % pasar a una columna si lo exige la legibilidad, respetando orden. El título y descripción son Text reales de Compose. Imagen decorativa sin lectura redundante por TalkBack; el contenedor anuncia categoría y acción. No controles anidados dentro de una tarjeta que ya sea clickable.

| Orden | Tarjeta | Texto visible | Fondo |
|---|---|---|---|
| 1 | Entrenamiento | Planifica rutinas y sigue tus sesiones. | h1_training.webp |
| 2 | Salud | Consulta tu actividad y datos de salud. | h1_health.webp |
| 3 | Nutrición | Registra comidas, calorías y macros. | h1_nutrition.webp |
| 4 | Hidratación | Registra agua y sigue tu objetivo diario. | h1_hydration.webp |
| 5 | Sueño | Consulta tu descanso y su evolución. | h1_sleep.webp |
| 6 | Progreso | Sigue tus marcas, peso y evolución. | h1_progress.webp |

Fondo de hero/lista/detalle/próximo entrenamiento: training_gym_hero.webp. Reutilizar estos recursos sin generar un catálogo de imágenes por ejercicio. No usar ilustraciones de técnica deportiva como si fueran instrucciones anatómicas fiables. Los siete assets permiten continuidad visual en futuras fases sin desarrollar ahora sus funciones.

## H1 Objetivo cerrado
Al abrir, se ve un Inicio reconocible respecto a las referencias, con navegación completa de cinco raíces, fotografías locales, Perfil útil y movimiento definido. Lo que aún no existe tiene un estado final bien diseñado de disponibilidad, no una pantalla técnica vacía o botones que aparenten funcionar.

### H1 A Inicio visual y primera captura
Sustituir foundation como startDestination por home. Reutilizar tema y componentes de H0. Mostrar una primera captura compilada tras construir Inicio con fotos y texto; no esperar al último informe para enseñar el resultado. Esta captura temprana no obliga a detener la implementación de H1.
Orden vertical: saludo «Hola» o nombre local; «Tu resumen de hoy»; avatar genérico o iniciales, sin rostro ficticio; resumen de actividad; seis categorías en orden; próximo entrenamiento fotográfico; barra inferior fuera del scroll.
Resumen sin fuente real en H1: título «Actividad de hoy», aro neutro con guion/«Sin datos», explicación «Tu actividad aparecerá aquí» y botón «Ver salud». No usar 0, 72 % ni 6.240 como hechos reales. Las cuatro métricas futuras (pasos, calorías activas, distancia, minutos) pueden verse en preview DEMO; en producción su estado sin datos debe ser legible, sin presión por rellenarlo.
Próximo entrenamiento H1: foto gym, icono calendario, «Tu próximo entrenamiento», «Crea tu primera rutina en Entrenar», botón «Ver entrenamiento». Lleva al estado de Entrenar. No hay fecha, rutina ni botón iniciar falso. H2 conecta esta misma tarjeta a la rutina elegida y cambia su contenido sin rehacerla. Si hay sesión activa en H2, la prioridad es «Continuar sesión».

### H1 B Navegación y Perfil funcional
Raíces exactas: Inicio, Entrenar, Progreso, Salud, Perfil. Nutrición, Hidratación, Sueño son secundarios desde Inicio con Atrás al origen. Entrenamiento abre Entrenar. No pestaña Bienestar, no sexta pestaña. Reutilizar TopLevelDestination/NavHost; no instalar otro sistema.
Barra fija con insets correctos, icono y etiqueta siempre visibles, selección champán. SingleTop/restoreState o equivalente actual para no duplicar pilas; volver a Inicio conserva scroll. Atrás del sistema y gesto predictivo si soportado no deben perder datos ni disparar acciones. No exigir un morph de tarjeta para acabar H1: la transición simple coherente queda finalizada según Motion.
Cada destino futuro tiene cabecera, imagen correspondiente, explicación concreta de disponibilidad y regreso; una pantalla común reutilizable parametrizada basta. Salud explica integración opcional H3, sin pedir permisos. Entrenar explica creación de rutinas en H2; botón solo para volver a Inicio en H1. Nutrición e Hidratación H4, Sueño H5, Progreso avanzado H6. No exponer acciones Guardar/Registrar/Crear que no estén conectadas.
Perfil H1 es útil: nombre opcional editable, guardar con feedback real, unidades persistentes H0 y ajuste «Reducir animaciones». Guardar nombre vacío restaura «Hola». Extender el repositorio DataStore existente con nombre y preferencia reduceMotion (valor inicial false, sin desactivar límites del sistema). Tema Titanium Champagne identificado; siete paletas futuras no aparecen como activas. No pedir avatar remoto ni acceso a galería. Controles de exportación/borrado futuros se omiten hasta H7.

### H1 C Movimiento, estados y cierre
Implementar los contratos M01–M05 de 08_MOTION.md: pulsación, entrada, cambio entre raíces, mancuerna de carga y valores/errores. Perfil conserva su estado y los controles no se mueven al escribir. Mancuerna solo durante operación real suficientemente larga; preview para demostrarla sin añadir retrasos al runtime.
Previews/fixtures: Inicio vacío, Inicio DEMO con datos conocidos, error recuperable, carga de mancuerna y fuente 200 %. Sin toggle Demo en release ni DB sembrada. Decodificar fotos fuera del camino crítico o recursos optimizados sin red; no añadir una dependencia por seis imágenes locales.
Cerrar H1 solo cuando pase la aceptación y haya capturas normal/grande y vídeo corto o evidencia temporal del movimiento. Si no hay grabación posible, describir la prueba realizada y su limitación; no afirmar que se vio un efecto solo por tener código.

## H2 Objetivo cerrado
Un usuario puede crear una rutina propia, ejecutarla, pausar, recuperar la sesión tras cerrar el proceso, finalizar y ver su historial. Diseño y movimiento forman parte de cada recorrido. No hace falta esperar a H6 para comprobar una sesión real: resumen e historial llegan aquí.

### H2 A Lista de rutinas y editor
Entrenar: hero de gimnasio de 160–200 dp (adaptable), título «Tus rutinas», CTA «Crear rutina», acceso a sesión activa y sección «Historial». Vacío con explicación y CTA real. Card de rutina con foto gym tenue, nombre, nº ejercicios y series; menú editar/duplicar/archivar y acción seleccionar como próxima. Archivo reversible sin borrar sesiones. No límites comerciales artificiales.
Editor (pantalla o formulario nativo consistente): nombre obligatorio, notas opcionales, ejercicios manuales con nombre, notas, series planificadas con reps positivas, carga opcional >=0, descanso entero >=0 y bandera calentamiento. Permitir añadir/eliminar, duplicar y ordenar ejercicios con acciones subir/bajar accesibles (drag no obligatorio). Una rutina ejecutable necesita >=1 ejercicio y cada ejercicio >=1 serie válida. Permitir guardar borrador solo si existe estado DRAFT explícito; la solución mínima es guardar únicamente válido y mantener formulario incompleto como borrador de UI, nunca rutina rota.
Errores junto al campo, conservar entradas, parsear decimales regionales, sin crash por vacío/NaN/infinito. Guardar en transacción y confirmar éxito únicamente después de persistencia; fallo deja formulario recuperable. Confirmar salir con cambios sin guardar. Mantener IDs/estado en ViewModel/SavedStateHandle; no perder formulario al rotar. Archivado conserva rutina y sesiones; duplicación genera IDs nuevos. Unidades visibles y normalización kg, conversión solo presentación; cambiar preferencia nunca reinterpreta un valor ya guardado.

### H2 B Detalle de rutina e Inicio conectado
Detalle: hero gym con degradado, Atrás, nombre, notas, conteos, ejercicios ordenados con objetivos, descanso y CTA «Iniciar entrenamiento». Editar desde menú. No grandes fotos por fila que desplacen los datos útiles. Mostrar selección «Próxima rutina» si corresponde.
Seleccionar rutina como próxima guarda su ID local (preferencia o campo de repositorio coherente); sin inventar planificación semanal. Home muestra «Próximo entrenamiento», nombre real y «Rutina seleccionada», sin fecha falsa; iniciar funciona. Si se archiva/elimina selección, limpiar o mostrar CTA elegir otra, no seguir con ID inexistente. Si no se seleccionó ninguna, elegir la primera rutina activa solo con etiqueta «Rutina disponible» o invitar a elegir; no atribuir un calendario.

### H2 C Sesión activa y descanso
Iniciar crea en una sola transacción sesión con snapshots de nombre, ejercicios, objetivos y series. Bloquear doble alta incluso con doble toque/segunda ruta. Si hay sesión ACTIVE/PAUSED, ofrecer continuar; no crear otra silenciosamente. ID por navegación, nunca objeto completo mutable. Cambiar rutina después no cambia el histórico.
Pantalla de sesión centrada en datos: cabecera con tiempo efectivo y progreso «Series 2 de 9»; ejercicio actual y notas; filas Serie / Carga (kg o lb) / Reps / completar; añadir serie; siguiente/anterior ejercicio; Pausar/Reanudar y Finalizar. Teclado numérico apropiado, contenido por encima de IME y desplazamiento, campos accesibles con ejercicio/serie/unidad. Las acciones frecuentes no mueven el layout mientras se escribe.
Completar valida y persiste valores. Tras éxito: check y relleno de estado M06, actualización del progreso M07, descanso M08 según la serie. Se puede desmarcar/editar: recalcular métricas, no crear evento duplicado. Completar repetido no dispara otro descanso. Si falla la escritura, no pintar check de éxito. Añadir serie actualiza denominador sin animar un progreso ficticio. Guardar el resto del formulario al cambiar de ejercicio cuando sea válido o señalar el campo pendiente.
Descanso es panel integrado, nunca overlay que bloquee edición. Tiempo restante, aro descendente, «Omitir» y opción ajustar segundos. Se calcula desde deadline, no con contador de recomposiciones; al llegar a cero cambia a «Descanso terminado». Pausar congela tiempo efectivo de sesión y descanso; reanudar restaura ambos. Salir a otra pestaña no pausa automáticamente; fondo continúa lógicamente, sin promesa de alarma ni ejecución exacta del proceso. No alarmas exactas, wake lock ni servicio en primer plano en H2.
Room es fuente de registros confirmados. Restaurar sesión ACTIVE/PAUSED, series, ejercicio y descanso tras recreación/muerte de proceso; mientras proceso vive medir elapsedRealtime para temporización. Persistir anclajes UTC y pausa para restauración tras reinicio; si hay cambio de reloj/reboot no reconciliable, pedir confirmar el tiempo en vez de inventar exactitud. Duración nunca negativa. La prueba incluye force-stop/reabrir y rotación; no confundir con solo recrear Activity.
Atrás navega dejando sesión recuperable. Descartar pide confirmación, marca DISCARDED y excluye de progreso; no equivale a finalizar. Cancelar diálogo conserva datos. No borrar sesión por cerrar pantalla.

### H2 D Finalizar, resumen e historial
Finalizar muestra confirmación con series hechas y pendientes: «Finalizar con lo registrado» o continuar; 0 series permite descartar, no crear logro vacío. Edición pendiente debe validarse o descartarse explícitamente antes de finalizar. Persistencia transaccional/idempotente cambia a COMPLETED y fija tiempo final; no cerrar pantalla antes de confirmar éxito. Error conserva sesión activa/reintentable. Éxito anima medallón/aro M09 y abre resumen real.
Resumen: duración efectiva, nº ejercicios realizados, nº series completadas y volumen de carga externa; lista por ejercicio con reps/cargas reales y notas. Acciones «Ver historial» y «Volver a Inicio». No calorías estimadas, puntuación de salud ni diagnóstico. Compartir imagen/exportación social fuera de alcance H2.
Historial: sesiones completas por fecha, abrir resumen persistido. Descartadas no contribuyen. No hace falta edición de sesiones finalizadas en H2; no mostrar menú de editar sin implementación. Rutinas modificadas/archivadas no alteran sesiones. Mantener acceso después de reiniciar app.
PR opcional dentro del alcance H2 definido: mayor carga externa completada por ejercicio identificado, comparada con sesiones completas anteriores; primera medición es «Primera marca registrada», no «Récord superado». No comparar por texto libre sin identidad estable: añadir exerciseKey estable a ejercicios y conservarlo al duplicar; ejercicios nuevos con mismo nombre no se fusionan automáticamente. No 1RM. Si se incluye PR se prueba y aplica M10; si no se implementa, M10 queda para H6 y no se presenta como parte de H2 terminada. Requisito obligatorio H2 es registro/volumen/tiempo/historial, no PR avanzado.

### H2 E Consistencia y cierre
Inicio muestra continuar sesión mientras esté activa; tras finalizar vuelve a rutina elegida. Progreso H2 puede mostrar un bloque ya funcional de últimas sesiones, nº sesiones y volumen semanal con enlace al historial usando el mismo repositorio; es obligatorio el acceso al historial desde Entrenar/Resumen y suficiente en esta fase. Gráficos de fuerza/peso, composición y filtros avanzados siguen en H6: no declararlos terminados.
Cerrar H2 incluye movimiento M06–M09, recuperación y migración. No dejar «añadiremos persistencia más adelante». Añadir entidades de entrenamiento a la Room existente, migración 1→2 (o desde versión real al implementar), preservando app_metadata y DataStore H0. No fallback destructivo. Exportar esquema nuevo y probar migración con base previa. No introducir Room3 por ver snippets actuales si la app usa Room 2; mantener familia compatible instalada salvo bloqueo real.

## Datos y cálculos de H2
Modelo mínimo: Routine(id, name, notes, archivedAt); RoutineExercise(id, exerciseKey, routineId, order, name, notes); PlannedSet(id, exerciseId, order, reps, loadKg?, restSeconds, warmup); Session(id, routineId?, routineNameSnapshot, status, start/end UTC, accumulatedPause, timingAnchors); SessionExercise(id, exerciseKeySnapshot, sessionId, order, nameSnapshot, notes); PerformedSet(id, sessionExerciseId, order, targetSnapshot, actualReps, actualLoadKg?, completedAt?, warmup). Descanso/exercise actual persistidos de forma que restauración sea verificable. Transacciones y claves foráneas/índices acordes a consultas; la elección exacta del esquema se documenta brevemente en el PR/commit, no con una clase por cada campo.
Volumen = Σ kg externos × reps de series completadas no marcadas calentamiento; carga null/cero o peso corporal sin carga no aporta kg, pero sí cuenta serie y reps válidas. No añadir peso corporal al volumen. Redondear solo al presentar. Meta del aro de sesión = completadas / series totales actuales, clamp 0..1; cifra real y número de series aparte. No sumar pasos o calorías de Health Connect todavía. Los ajustes sobre series eliminadas o cambiadas deben ser transaccionales y consistentes.

## Criterios de aceptación H1
| ID | Prueba | Esperado |
|---|---|---|
| H1-01 | Abrir instalación sin datos | Inicio con fotos, descripciones, resumen vacío, próximo vacío y cinco pestañas |
| H1-02 | Comparar tema | Tokens aprobados reutilizados; sin verde, color dinámico o tintes inesperados |
| H1-03 | Recorrer seis tarjetas/cinco raíces/avatar | Destinos correctos, Atrás y selección consistentes, sin duplicados |
| H1-04 | Nombre y unidades en Perfil; matar proceso | Valores guardados se conservan y saludo cambia; fallo de escritura se informa |
| H1-05 | Fotos sin red | Se ven las siete necesarias por uso local; sin placeholders finales ni descarga |
| H1-06 | Fuente 200 %, ancho 320/360 dp, teclado | Reflujo sin recortes esenciales ni barra tapada; controles >=48 dp |
| H1-07 | Carga 50 ms, 800 ms, error y cancelación con fake | No loader en rápida, mancuerna en lenta, salida inmediata al resolver, sin éxito falso |
| H1-08 | Navegar rápido/repetir pestaña/volver de background | Sin animaciones duplicadas, entrada de cada card al scroll ni bloqueo |
| H1-09 | Reducir animaciones y escala sistema 0 | Sin montaje/desplazamientos/bucles; contenido y controles disponibles |
| H1-10 | Producción limpia | Sin métricas DEMO, avatar ficticio, permisos de salud ni botones falsos |

## Criterios de aceptación H2
| ID | Prueba | Esperado |
|---|---|---|
| H2-01 | Crear 2 ejercicios, ordenar, guardar/reabrir | Rutina y objetivos idénticos, lista/detalle coherentes |
| H2-02 | Guardar vacío, decimal regional, valor inválido | Error junto al campo, no pierde resto ni almacena inválidos |
| H2-03 | Duplicar/archivar rutina usada | IDs nuevos; archivo no borra sesión ni altera snapshots |
| H2-04 | Seleccionar próxima, iniciar desde Inicio, doble toque | Rutina correcta; una sola sesión activa |
| H2-05 | Dos series 20 kg ×10, cambiar una a 8 | Volumen 400→360 kg, no duplica series ni descanso |
| H2-06 | Check durante error de escritura | No éxito falso ni incremento de progreso; reintento posible |
| H2-07 | Pausar/reanudar con descanso, fondo y fuerza cierre | Tiempo y estado restaurados; sin negativos; no decrementa mientras pausado |
| H2-08 | Rotar/teclado/cambiar ejercicio | No pierde confirmados ni oculta campo; borradores según contrato |
| H2-09 | Atrás, cancelar descarte, confirmar descarte | Sesión conservada al volver/cancelar; DISCARDED excluida del volumen |
| H2-10 | Finalizar dos veces y reiniciar | Un resumen consistente y una sesión histórica; no éxito antes de commit |
| H2-11 | Editar rutina original tras terminar | Histórico conserva nombres, series y valores originales |
| H2-12 | Migrar DB v1 con app_metadata | Datos H0 conservados y esquema nuevo validado sin recrear base |
| H2-13 | Check, relleno, descanso, final con escala normal/reducida | M06–M09 probados sin cambiar los datos o bloquear interacción |
| H2-14 | Cambiar kg/lb y volver | Valor normalizado estable sin deriva de redondeo |

## Evidencia y puertas de salida
H1: capturas Inicio normal y 200 %, Perfil, destino secundario; vídeo 10–20 s de transición/carga con fake de prueba, si hay medios. H2: lista vacía y llena, editor, detalle, sesión, descanso/pausa, resumen e historial; prueba de muerte de proceso y migración. Capturas de pruebas pobladas llevan DEMO o se identifican claramente como datos de prueba en el informe. No poner datos ficticios en release para obtener una captura atractiva.
Ejecutar assembleDebug, testDebugUnitTest y lintDebug con el Wrapper y matriz existentes, más pruebas instrumentadas pertinentes si hay dispositivo. Reutilizar tests H0, no borrarlos. Registrar comando, resultado, commit, API/dispositivo y limitaciones; API 28 y TalkBack manual pendientes siguen visibles. Un único informe breve por fase en quality/H1_REPORT.md o H2_REPORT.md, con PASS/FAIL/NO EJECUTADO y capturas en quality/evidence/. No generar decenas de documentos por pantalla.

## Trabajo mínimo útil
No hay presupuesto de número de archivos artificial: cada archivo nuevo debe participar en una función, prueba relevante o recurso utilizado. Reutilizar tema, botones, tarjetas, navegación y repositorios H0. Un composable compartido para las seis categorías y otro para estados futuros basta. Crear entidades/repositorio de entrenamiento en H2 cuando se usan. No interfaces de repositorios vacíos, módulos Gradle de futuro, capa de caso de uso por getter, frameworks DI/animación nuevos ni actualizaciones del SDK sin necesidad demostrada.
Preferir APIs Compose actuales del proyecto (Image/painterResource, Canvas, Animatable/animate*AsState/AnimatedVisibility/NavHost) sobre JavaScript, WebView, Lottie o GIF. No probar cada constante trivial ni reejecutar pruebas largas idénticas sin cambios relevantes. La prueba de integridad/persistencia/migración sí es necesaria. Medir scroll/sesión con imágenes y dispositivo reales antes de prometer rendimiento.

## Después de H2 sin rehacer la base
H3 conecta salud con permisos y fuentes reales al resumen ya diseñado. H4 activa nutrición/hidratación con el relleno de agua M11. H5 activa sueño; H6 añade tendencias/PR avanzado/peso y temas cuando se aprueben; H7 gestiona exportación/borrado y auditoría global. Cada módulo se cierra con su propio diseño, estados y movimiento, no deja esa parte para una fase genérica de pulido. Revisión transversal posterior solo para problemas descubiertos, consistencia y dispositivos, sin cambiar caprichosamente lo aprobado.
