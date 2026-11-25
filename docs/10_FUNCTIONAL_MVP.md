# Lote F1 — FitnessApp utilizable después de H2

Autorización de alcance del usuario del 25 de noviembre de 2026: conservar H2, aplicar el PRIMER Liquid Glass elegido y completar un recorrido básico real en cada tarjeta. Este lote se ejecuta de principio a fin con una sola autorización; no detenerse a pedir confirmación entre H3/H4/H5/H6 para las funciones mínimas aquí enumeradas. Los hitos históricos permanecen como guía de evolución avanzada, no como bloqueo del lote F1.

## Fuente de verdad y estado
1. Instrucción actual del usuario y este documento para alcance, secuencia y pruebas.
2. docs/11_LIQUID_GLASS.md para el menú y tacto visual elegido.
3. docs/09_VISUAL_POLISH.md para fotos y dos tarjetas por fila.
4. docs/07_H1_H2_DELIVERY.md y 08_MOTION.md para conservar entrenamiento y contratos útiles.

H2 está en la rama fase/h2, commit inspeccionado 49539280be364d4444909d2744bf166d0076f9e5. Main contiene planificación y la base histórica, no usar su app H0 como punto de partida. Revisar HEAD/local antes de implementar y conservar todo lo que H2 ya hace. El usuario declara H2 finalizada; este cambio documental no reejecuta ni certifica sus pruebas. No volver a generar el proyecto.

## Resultado que debe poder usar el usuario
Inicio con dos tarjetas por fila y fotos claras a la derecha, cinco destinos en menú Liquid Glass, entrenamiento H2 conservado, Salud/Nutrición/Hidratación/Sueño/Progreso con datos reales locales. Abrir una tarjeta ya no termina en «Disponible en una fase futura». Cada apartado permite su acción mínima completa, muestra lo guardado y sobrevive a cerrar/reabrir. No basta con seis rutas que abren una pantalla informativa.
Los refinamientos avanzados de cada módulo se hacen después. No añadir servicios de pago, backend, cuenta, IA, catálogo remoto de alimentos, recetas, escáner, Bluetooth directo, alarmas exactas, social o analítica. Los registros de hábitos/salud son manuales en esta primera versión; integración Health Connect queda separada para no bloquear su uso. Fuente «Manual» visible, sin simular reloj sincronizado ni fabricar valores. Si ya hubiera integración posterior funcionando al empezar, conservarla y su fuente sin duplicar datos.

## Seis tarjetas y Perfil
| Entrada | Función mínima de F1 | Fuera del lote |
|---|---|---|
| Entrenamiento | Reutilizar crear/editar/duplicar/archivar rutina, sesión, series, pausa, descanso, recuperación, resumen e historial H2; Inicio inicia/continúa correctamente | Reescribir repositorio, nuevos tipos de entrenamiento o IA |
| Salud | Registro diario manual de pasos, distancia, minutos y calorías activas como campos opcionales; consultar otro día, editar y eliminar el registro | Sincronización reloj/Health Connect, diagnósticos o gasto estimado |
| Nutrición | Añadir comida con nombre, kcal y macros del propio registro; lista diaria, total, editar/eliminar y metas opcionales | Catálogo externo, escáner, recetas, cálculo dietético automático |
| Hidratación | +150/+250/+500 ml, cantidad propia, meta editable, lista diaria y editar/eliminar/deshacer alta reciente | Recordatorios o notificaciones automáticas |
| Sueño | Inicio/fin manual, duración real, nota o calidad subjetiva opcional, últimas noches y editar/eliminar | Fases inferidas, score clínico o lectura del reloj |
| Progreso | Peso corporal manual con fecha, historial y gráfico sencillo si hay >=2 puntos; sesiones y volumen reales de H2 | Composición estimada, 1RM/PR avanzado o nueva librería de charts |
| Perfil | Conservar nombre, unidades y reducir animaciones; editar metas necesarias de pasos/agua/kcal/macros | Siete paletas no aprobadas, exportación/borrado global H7 |

Los botones visibles deben ejecutar una acción real. Omitir opciones futuras en vez de presentar botones que no hacen nada. Un gráfico sin dos registros muestra un estado útil para añadir el primero, no una curva de ejemplo. Los vacíos son normales en instalación limpia, pero siempre hay una acción operativa para empezar.

## Contrato de cada módulo
### Salud y resumen de actividad
Editor por fecha (hoy por defecto): pasos entero >=0, distancia >=0 con unidad elegida, minutos entero >=0, kcal activas >=0; cada campo puede estar vacío y se guarda como null, no cero. Al menos un valor para guardar. Un registro manual diario por fecha, editar sustituye valores, no los suma otra vez. Cada tarjeta muestra fuente Manual y fecha. No añadir frecuencia cardíaca u otros signos vitales si su registro/gráfico no se va a implementar; no mostrar «Sincronizado».
Inicio observa el registro real de hoy. Pasos, distancia, minutos y kcal solo se muestran cuando existan. Aro diario = pasos/meta de pasos si ambos válidos; clamp visual 0..100 %, cifra real aparte. Meta ausente muestra «Configura tu objetivo» en Salud/Perfil y no calcula porcentaje ficticio. «Ver salud» abre Salud. No sumar kcal de entrenamiento a las introducidas manualmente. No llamar a un índice arbitrario «salud del día».

### Nutrición
Formulario mínimo: nombre de comida obligatorio y valores totales de esa entrada en kcal, proteínas, hidratos y grasas. Kcal puede ser 0 válido si introducido explícitamente; macros pueden omitirse (null), y la UI diferencia parcial/desconocido de cero conocido. Al menos energía o un macro para guardar. Fecha/hora editable dentro del día elegido. No pedir gramos/base por 100 g si aún no existe conversión: así se evita mezclar valores totales y densidades. Etiqueta clara «Valores de esta comida». No inferir kcal de macros si el usuario aporta un valor distinto.
Totales diarios suman lo conocido y señalan datos incompletos cuando corresponda. Editar/eliminar recalcula, sin acumulaciones repetidas. Decimal regional permitido; números finitos y no negativos. Metas introducidas por el usuario, no prescripción dietética. Fuente manual. Alimentos reutilizables/favoritos podrán añadirse en el refinamiento posterior.

### Hidratación
Guardar ml como entero positivo y momento de registro. Atajos ejecutan una única alta por toque aceptado; proteger duplicación accidental mientras guarda, permitir múltiples altas deliberadas después. Total diario = suma de entradas. Meta en DataStore o preferencia existente; sin meta configurada mostrar total y CTA configurar. Se puede superar meta: aro limitado, número real visible. Lista con hora, editar/eliminar; deshacer se refiere al ID concreto de alta reciente y no elimina otro registro creado después.
Relleno M11 de 08_MOTION.md se activa ahora con datos reales: número se actualiza tras guardar, nivel interpola 250 ms y onda se apaga; no bucle ni retraso. Motion reducido actualiza nivel estático. No pedir permisos de notificación para registrar agua.

### Sueño
Selector fecha/hora inicio y fin con zona del dispositivo. Requiere fin > inicio, sin duraciones negativas. Guardar instantes y zona/offset de origen; atribuir a fecha del despertar. Cruce de medianoche y cambios de hora usan duración entre instantes, no una resta de horas de reloj. Permitir corregir errores de fecha sin perder el formulario. Mostrar duración y lista últimas noches; editar/eliminar registros propios. Calidad opcional se llama «Cómo has descansado» y no diagnóstico. No pintar fases ni puntuaciones sin fuente.
Si hay intervalos manuales solapados, avisar y pedir corregir/confirmar en lugar de sumar dos veces la misma noche. El total semanal debe evitar doble conteo; solución mínima: no aceptar solapamientos hasta que se definan reglas de combinación.

### Progreso
Entrada de peso >0, finito, fecha y nota opcional; kg normalizado, lb solo presentación/conversión. Lista con editar/eliminar, unidad visible. Tendencia simple Canvas/Compose, periodo fijo reciente, sin dependencia nueva ni animación de dígitos que oculte el valor. Accesibilidad: valores textuales y fechas equivalentes al gráfico. Conservar fechas de medida seleccionadas.
Resumen de entrenamiento consume sesiones COMPLETED del repositorio H2: nº sesiones y volumen de los últimos siete días, con etiqueta del periodo y enlace al historial ya existente. No contar DISCARDED ni introducir un segundo cálculo incompatible. Peso no se confunde con carga levantada. No inventar composición corporal.

### Perfil, Inicio y coherencia entre ventanas
Preferencias existentes conservadas. Metas editables opcionales con unidad y validación; configurarlas en Perfil y ofrecer acceso desde cada módulo, sin formularios duplicados con reglas distintas. Nombre/unidades/reducir movimiento siguen funcionando.
Todas las pantallas muestran loading/error/empty/content donde proceda, error de escritura no simula éxito, y Atrás regresa sin bucles. Formulario con cambios pide confirmar descartar cuando corresponde; guardados no se pierden al navegar. Estados confirmados en Room/DataStore; remember solo para UI. Fuente Demo restringida a previews/tests.
Inicio mantiene las seis categorías y la tarjeta de próximo entrenamiento ya conectada a H2. Cabecera/resumen de Hoy se alimentan de datos reales; no hacer ahora otro rediseño de ese panel ni un calendario complejo. «Hoy», cambio de día o historial visibles deben funcionar; no agregar controles nuevos solo por decorar.

## Arquitectura mínima suficiente
Un solo módulo :app y AppContainer existente. Añadir las entidades/DAO/repositorio realmente usados; no un módulo Gradle, interfaz o caso de uso por cada campo/botón. Se permiten pequeños componentes compartidos para formularios, selector de día o estado que se repiten de verdad. No framework genérico de formularios ni motor de sincronización vacío.
Modelo orientativo mínimo: DailyActivity(date, optional steps/distanceMeters/activeMinutes/activeKcal, source MANUAL), MealEntry(id, name, occurredAt, kcal?/protein?/carbs?/fat?), WaterEntry(id, occurredAt, ml), SleepEntry(id,start,end,zone,subjectiveQuality?,note?), BodyWeightEntry(id,date,kg,note?). IDs y fechas coherentes con H2; las tablas de rutina/sesión se conservan intactas. Metas y nombre/unidades se guardan en la misma DataStore existente, sin segunda instancia para el mismo archivo.
Migración desde versión REAL instalada (H2 usa v2) a una sola versión final del lote que añada las tablas necesarias. Conservar app_metadata y todas las rutinas, sesiones y preferencias. Sin fallback destructivo, clear, uninstall de la app personal o reinicialización de datos para «arreglar» el esquema. Exportar esquema final y verificar la migración en la secuencia final. Planificar el esquema antes de editar para evitar una migración por pantalla durante el mismo lote.
Para registros diarios de actividad, la fecha elegida se conserva como fecha local del registro; no cambia al viajar de zona. Para eventos con hora, conservar UTC y zona y agrupar según política documentada, coherente entre lista y total. No almacenar números ya formateados. No añadir APIs que requieran actualizar SDK/JDK/Gradle si las versiones H2 pueden resolver el lote.

## Secuencia de trabajo sin pausas de fase
F1 A: incorporar assets v2, dos columnas y primer Liquid Glass; conservar entrenamiento. Una captura temprana de Inicio confirma el encuadre, sin abrir una aprobación por cada ajuste.
F1 B: definir esquema aditivo final y repositorios mínimos; implementar Salud manual y alimentar resumen diario.
F1 C: Nutrición e Hidratación completas con alta/lista/edición/borrado y metas; activar relleno de agua real.
F1 D: Sueño y Progreso básicos con sus registros, historial y agregados reales; Perfil coherente.
F1 E: una secuencia global de verificación, corregir fallos, repetir solo comprobaciones afectadas y entregar una versión utilizable.
Continuar entre A–E sin autorización adicional. Si hay una duda reversible, elegir lo mínimo documentado. Preguntar solo ante pérdida de datos, conflicto real o ampliación fuera de este alcance. No solicitar permisos innecesarios ni hacer push de código sin autorización posterior.

## Política de pruebas y gasto de tiempo
No ejecutar suite, lint, compilación completa o release después de cada archivo/botón. Implementar bloques coherentes y reservar la secuencia global para el final. Comprobaciones rápidas de sintaxis/compilación local están permitidas si desbloquean un error real; no se elimina la necesidad de verificar guardado/migración por ahorrar tokens.
Reutilizar pruebas existentes. Crear solo pruebas para cálculos, integridad o recorridos nuevos reales; no tests que comparan constantes de estilo consigo mismas ni test vacío por pantalla. No clean, build Release, actualización de dependencias o benchmarks repetidos sin motivo. No borrar ni saltar tests que fallan para declarar éxito. Mantener un único informe corto en quality/F1_REPORT.md y evidencias suficientes, no un documento por cada botón.

## Secuencia final de pruebas del lote
1. Compilar Debug con Wrapper, ejecutar unitarias relevantes y lint una vez sobre el conjunto. Registrar errores/avisos reales. Compilar Release no es necesario para revisar esta versión.
2. Migración de DB de H2 con datos conocidos a esquema F1, verificando que rutinas/sesiones/app_metadata y preferencias sobreviven. Probar en base/dispositivo de prueba aislados; no borrar registros del usuario ni hacer pm clear en su app personal.
3. Recorrido global: abrir Inicio, recorrer seis tarjetas y cinco pestañas, guardar un registro de cada módulo, volver a Inicio/Progreso, editar y eliminar una entrada, comprobar totales y volver atrás.
4. Entrenamiento: abrir rutina existente, crear una de prueba, iniciar/continuar, completar serie, pausar/forzar cierre/reabrir, finalizar e historial. No rediseñar H2 para probarla. Ejecutar tests existentes de esa ruta en el entorno de prueba cuando estén disponibles.
5. Casos de cálculo: 250+500 ml=750, editar 250→150 da650, borrar500 deja150; nutrición suma y resta exactamente; actividad vacía vs0; peso kg/lb sin deriva; sueño medianoche/DST; progreso excluye descartadas.
6. Inspección visual final: dos columnas normales, sujetos completos a la derecha, Liquid Glass con iconos nítidos, scroll sin CTA tapado, teclado, fuente200 %, movimiento reducido y escala sistema0. No simular blur real si solo se implementó fallback; anotarlo.
7. Cerrar/reabrir y modo avión: datos confirmados persisten y todos los módulos manuales funcionan offline. Error de escritura conserva datos/formulario; permiso ausente de salud no bloquea nada porque F1 es manual.
8. Pruebas instrumentadas pertinentes una vez al final si hay dispositivo. Si falta API28, TalkBack o móvil real, NO EJECUTADO con motivo. No repetir toda la secuencia al corregir una etiqueta: repetir solo lo afectado; si cambia esquema/repositorio, repetir las pruebas de integridad pertinentes.

## Aceptación mínima
F1-01 dos columnas y recursos v2; F1-02 menú Liquid Glass y cinco rutas; F1-03 H2 conservada con historial/recuperación; F1-04 Salud manual guarda/edita/borra y actualiza Hoy; F1-05 Nutrición real y totales; F1-06 Agua real y meta/relleno/deshacer; F1-07 Sueño con duración/fechas correctas; F1-08 Peso e historial/tendencia; F1-09 Progreso de entrenamiento real; F1-10 Perfil/metas/unidades persistentes; F1-11 todo offline tras reinicio; F1-12 migración sin pérdida; F1-13 sin demos/botones falsos; F1-14 accesibilidad y movimiento; F1-15 pruebas finales registradas y fallos resueltos o limitaciones explícitas.
Resultado final: qué puede usar el usuario ya, capturas por módulo con datos de prueba claramente identificados, comandos/resultados, campos pendientes avanzados y APK Debug si se construyó. No afirmar «todo probado» sin evidencia. No continuar a integración reloj, personalización avanzada o publicación sin nuevo encargo.
