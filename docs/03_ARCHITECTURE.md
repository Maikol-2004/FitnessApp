# Arquitectura Android y contrato de datos

## Base y dependencias
Kotlin y Compose con Material 3, MVVM y flujo unidireccional. UI emite acciones a ViewModel; ViewModel expone StateFlow<UiState>; repositorios coordinan DAO o proveedor; UI recoge con collectAsStateWithLifecycle. No acceder a DAO, Context de Activity ni Health Connect desde composables. Usar corrutinas estructuradas, inyectar reloj y dispatcher cuando la lógica lo requiera. Errores tipados en repositorio y mensajes recuperables en UI; CancellationException no se consume como error de negocio.

H0 comienza con un módulo Gradle :app y paquetes core/designsystem, core/navigation, core/database, core/preferences y feature/. Separar módulos cuando aparezca una dependencia o compilación que lo justifique. No crear quince módulos vacíos ni convention plugins complejos. DI manual mediante AppContainer al principio; Hilt solo si se justifica con una decisión documentada. Casos de uso para lógica compartida o transacciones complejas, no una clase por cada getter.

Antes de configurar: inspeccionar repositorio, SDK y JDK reales; verificar compatibilidad AGP/Gradle/Kotlin/JDK en documentación oficial. Fijar versiones estables compatibles en libs.versions.toml, Compose BOM y Wrapper; no usar versiones dinámicas ni copiar Java 25 del informe histórico como requisito. compileSdk y targetSdk se eligen conforme al entorno y requisitos vigentes de distribución; minSdk es 28. Documentar matriz y comandos. No mezclar configuración Kotlin antigua y soporte integrado de AGP sin verificar la versión.

## Persistencia y entidades propuestas
IDs UUID como String, claves foráneas e índices para búsquedas por fecha, rutina y sesión. Campos createdAt/updatedAt como epoch millis UTC cuando correspondan. Instant y conversiones con desugaring compatible con API 28. Esquema Room exportado y versionado. No usar destructiveMigration en producción.

| Entidad | Campos relevantes y reglas |
|---|---|
| Routine | id, name no vacío, notes, archivedAt nullable |
| RoutineExercise | id, routineId FK, position, name, notes; orden único por rutina |
| PlannedSet | id, routineExerciseId FK, position, reps > 0, loadKg >= 0 nullable, restSeconds >= 0, warmup |
| WorkoutSession | id, routineId nullable, routineNameSnapshot, startedAt, endedAt nullable, pausedAt nullable, accumulatedPauseSeconds, status ACTIVE/PAUSED/COMPLETED/DISCARDED |
| SessionExercise | id, sessionId FK, position, nameSnapshot, notes |
| PerformedSet | id, sessionExerciseId FK, position, reps, loadKg nullable, completedAt nullable, warmup |
| Food | id, name, basisGrams > 0, kcal/protein/carbs/fat >= 0, favorite |
| MealEntry | id, foodId nullable, nameSnapshot, dateTime, grams > 0, nutrientSnapshots; editar Food no altera histórico |
| WaterEntry | id, occurredAt, volumeMl > 0 |
| SleepEntry | id, startAt, endAt > startAt, source MANUAL/HEALTH_CONNECT, sourceRecordId nullable, sourcePackage nullable, zoneOffset, subjectiveQuality nullable |
| BodyWeight | id, measuredAt, kg > 0, notes |
| HealthCache | recordType, sourceRecordId, sourcePackage, payload normalizado, start/end, fetchedAt; único por tipo/origen/id |
| SyncState | tipo/intervalo, lastSuccessAt y error recuperable; token de cambios solo si se implementa correctamente |

La tabla es contrato conceptual, no código listo para compilar. Crear entidades cuando la fase las necesita. Borrar una rutina conserva snapshots de sesiones; archived es preferible a borrado para rutinas usadas. Transacción al iniciar sesión con copia de rutina/ejercicios/series; finalizar sesión actualiza estado y deja histórico atómico. Restricción de una sesión activa gestionada en transacción, incluso con doble toque. Cada serie confirmada se persiste antes de confirmar éxito; errores no marcan éxito visual.

Preferencias DataStore: nombre, sistema de unidades, themeId titanium_champagne, metas, onboarding y configuración de recordatorios. Crear una sola instancia por archivo; repositorio expone Flow. IDs de siete temas futuros no equivalen a temas implementados. No guardar historial relacional ni cachés grandes en DataStore. DataStore no constituye cifrado.

## Sesión y temporizador
Room es fuente persistente; SavedStateHandle conserva IDs y formulario transitorio. Temporizador no cuenta recomposiciones: mientras proceso vive medir tiempo transcurrido con reloj monotónico; persistir inicio, pausas y deadline de descanso para restaurar tras reinicio. Definir reconciliación si cambia reloj del sistema; no mostrar duraciones negativas. No garantizar exactitud si se altera manualmente la hora. Probar muerte de proceso y doble finalización idempotente. Servicio en primer plano solo si un requisito real lo exige, con tipos y permisos verificados entonces.

## Health Connect H3
Integración opcional de lectura, aislada tras HealthRepository/HealthConnectDataSource. Consultar getSdkStatus y permisos efectivos, no inferir disponibilidad por versión. Android 14+ integra Health Connect en el sistema; en versiones anteriores compatibles puede requerirse proveedor instalable. Revalidar capacidades y documentación al implementar. No se promete que Xiaomi S4 o Mi Fitness publique todos los tipos: solo mostrar registros efectivamente disponibles en Health Connect. Sin emparejamiento directo.
Solicitar los mínimos permisos de lectura por tipo cuando el usuario conecta: pasos, distancia, calorías activas, ejercicio/actividad, frecuencia cardíaca y sueño según funcionalidades habilitadas. No pedir escritura, lectura histórica extendida o segundo plano de oficio. Declaraciones manifest, pantalla de justificación y configuración de distribución deben corresponder a tipos realmente usados.
Disponibilidad: no soportado, proveedor requerido/actualización, permisos ausentes/parciales, conectado sin datos, conectado con datos, error temporal. Revocación se maneja sin crash y retira datos no autorizados de UI/caché conforme a la política acordada. Nunca modificar registros de otras apps.
Lectura inicial acotada, paginación hasta token nulo o vacío, refresco manual y al volver al primer plano con caducidad razonable. Mostrar límite de cobertura; no prometer historial completo. Importación por upsert de ID/origen/tipo, eliminaciones reconciliadas al releer el intervalo o con cambios soportados. Agregaciones de pasos/actividad por API agregada cuando aplique; no sumar registros crudos superpuestos de proveedores. Frecuencia cardíaca no suma muestras. Conservar fuente y distinguir kcal activas de totales. No sumar sueño manual e importado superpuestos: preferir importado y ofrecer elegir fuente, sin borrar el manual. Sin sincronización en segundo plano en MVP.

## Privacidad, exportación y errores
Datos en sandbox privado Android; no registrar valores personales en logs. No afirmar cifrado de Room por defecto. En H0 excluir DB y preferencias de backups automáticos hasta definir política explícita; documentar efecto de desinstalar. Health Connect tiene controles propios, separados del borrado local.
H7 exporta JSON versionado mediante Storage Access Framework con unidades, zona, fecha de exportación y snapshots; advertir que es un archivo personal sin cifrado por defecto. Exportar datos propios; caché importada de salud excluida inicialmente. Importación/restauración fuera de alcance hasta especificar validación y conflictos. Borrado local transaccional limpia DB, preferencias y tareas; no toca Health Connect. Cancelar selector o confirmación no cambia datos.

## Validación técnica
Unitarias para cálculos, intervalos, conversión y lógica de sesión; pruebas DAO/transacción y migración con bases anteriores; ViewModel para loading/error/denegación; Compose para navegación y acciones principales. Health Connect detrás de fake para pruebas, además de dispositivo real compatible. API 28 y Android 14+ para compatibilidad. Comandos típicos una vez creado :app: .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug; :app:connectedDebugAndroidTest necesita emulador/dispositivo. Registrar comandos reales y resultado; no afirmar éxito si faltó SDK o dispositivo.
