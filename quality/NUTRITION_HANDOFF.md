# Nutrición N1–N3 — punto de parada (27-11-2026, 01:00)

Estado: **implementación completa, verificación a medias**. Commit local WIP en `fase/f1` (sin push). Instalado en el móvil
(9LX495KRHMXO99QS) con `install -r`, la app arranca sin cierre. Copia de seguridad previa de los datos del móvil fuera del repo:
`C:\Users\giral\Documents\FitnessApp\backup-movil-2026-11-27\app-data.tar` (databases/ + files/datastore).

## Hecho
- Room v6 (AutoMigration 5→6): `meal_entries.mealGroup` (UNSPECIFIED) y `mode` (LEGACY_TOTALS); tablas meal_items, foods,
  recipes, recipe_ingredients, nutrition_goals. Esquema `app/schemas/.../6.json`.
- Código en `feature/nutrition/data/` (cálculo, modelos, repositorio, Open Food Facts) y `feature/nutrition/ui/` (diario por
  grupos, detalle, búsqueda local/remota, ración, alimento propio, registro rápido, recetas, objetivos con historial,
  tendencias 7/30 días, escáner CameraX + ML Kit). Entrenar sin cambios.
- `./gradlew assembleDebug` OK. `testDebugUnitTest` OK: **100 tests, 0 fallos** (incluye `NutritionCalcTest` con los fixtures).
- Corregido hoy: la unidad de ración de OFF ya no se adivina (se lee de `serving_size` o no hay equivalencia).

## Pendiente, en este orden
1. **lintDebug**: tenía 1 error (`UnsafeOptInUsageError` en `ScannerScreen.kt:125`). Ya añadido `@OptIn(ExperimentalGetImage::class)`
   pero **sin volver a ejecutar lint**. También un aviso `when` no exhaustivo en `OpenFoodFacts.kt:228` (añadir `else`).
   Los 8 avisos restantes son versiones/antiguos, no de Nutrición.
2. **Compilar y ejecutar instrumentados solo en emulator-5554** (escritos, aún sin compilar ni ejecutar):
   `core/database/MigrationV6Test.kt`, `feature/nutrition/NutritionRepositoryTest.kt`, `feature/nutrition/OpenFoodFactsParseTest.kt`.
   Más toda la suite existente (regresión de Entrenar). `adb` está en `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe`.
3. **Comprobar en el móvil** que las comidas antiguas aparecen en «Sin asignar» con sus valores (la migración real corrió al abrir).
4. Capturas reales en el emulador → `quality/evidence/nutrition/`: diario vacío y con «Prueba», comida con ingredientes,
   búsqueda local y remota (pocas peticiones OFF), hoja de cantidad, producto incompleto, receta, tendencias con huecos,
   fuente 200 %, modo avión, cámara denegada, recreación, objetivos.
5. Escribir `quality/NUTRITION_REPORT.md`: N-01…N-15 PASS/FAIL/NO EJECUTADO, comandos y resultados, tamaño APK
   (debug 80 MB; ~20 MB son `libbarhopper_v3.so` de ML Kit en 4 ABI), requisitos externos (sin correo de contacto en el
   User-Agent de OFF; escáner no validado con cámara física; TalkBack y API 28 NO EJECUTADO).
6. Commit local final (sin push) y esperar revisión del usuario.
