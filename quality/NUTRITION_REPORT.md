# Nutrición N1–N3 — informe de verificación (27-11-2026)

Rama `fase/f1`, publicada en origin (sin merge a `main`): `a0d9937` (implementación), `8df985d` (pruebas instrumentadas al día
con v6), `994543c` (verificación, capturas e informe) y la actualización de documentación posterior. Entrenar no se ha modificado.

## Comandos y resultados reales
| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug` | OK. APK debug 81,2 MB (≈20 MB son `libbarhopper_v3.so` de ML Kit en 4 ABI) |
| `./gradlew testDebugUnitTest` | **100 tests, 0 fallos** (incluye `NutritionCalcTest` con los fixtures del paquete) |
| `./gradlew lintDebug` | **0 errores**, 5 avisos, 1 nota (versiones de dependencias / código previo; corregido el `UnsafeOptInUsageError` del escáner) |
| `./gradlew assembleDebugAndroidTest` + `am instrument` en **emulator-5554** (API 37) | **OK (45 tests)**: `MigrationV6Test`, `NutritionRepositoryTest` (8), `OpenFoodFactsParseTest` (6) y toda la suite previa (regresión de Entrenar, navegación, migraciones v1→v6) |

Primera ejecución instrumentada: 5 fallos, todos de expectativas antiguas (cuatro tests de migración esperaban versión 5 y
`NavigationTest` el subtítulo previo de Nutrición). Se actualizaron a v6 / «Tu diario por comidas» y la suite pasó entera.

## Capturas reales (emulador, datos «Prueba») — `quality/evidence/nutrition/`
01 diario vacío · 02 añadir alimento vacío · 03 crear alimento desde etiqueta · 04 hoja de cantidad (150 g = 247,5 kcal) ·
05 búsqueda local/recientes · 06 registro rápido · 07 diario con «Prueba» (proteína «Parcial») · 08 búsqueda remota OFF ·
09–10 producto OFF y su información/fuente/licencia · 11 objetivos con historial · 12 diario con objetivo · 13 receta ·
14 lista de recetas · 15 tendencias 7 días con huecos · 16 modo avión: búsqueda en línea · 17 modo avión: registro manual ·
18 cámara denegada · 19–20 fuente 200 % · 21–22 recreación (proceso matado: cantidad y destino conservados) ·
23 Perfil con el mismo objetivo.

Peticiones reales a Open Food Facts en esta sesión: 2 (una búsqueda «yogur» y una ficha de producto).

## Criterios
| ID | Estado | Evidencia |
|---|---|---|
| N-01 | PASS | Capturas 03→04→07: alimento desde etiqueta, 150 g a Comida; el resumen cambia solo al guardar |
| N-02 | PASS | `MigrationV6Test` (emulador): base v5 con 0 y null → «Sin asignar», LEGACY_TOTALS, sin ingredientes, editable; entreno y agua intactos. Móvil real: migración v5→v6 hecha; la copia previa no tenía comidas (0), así que no hay comidas antiguas que mostrar; 2 rutinas y 2 sesiones conservadas |
| N-03 | PASS | Unit `scaling_fixtureA_150g`, `serving_30g_roundTrip_andNoMassVolumeConversion`; captura 04 (248 kcal, 15/30/7,5 g) |
| N-04 | PASS | Unit `kcalWithoutMacros_isPartial…`, `quantity_rejectsZeroNegativeNonFinite`, `coverage_fixtures`; captura 07 «Parcial» |
| N-05 | PASS | Instrumentado: idempotencia, borrar/deshacer, copiar con reintento, mover. Fallo de Room con borrador: cubierto por código, NO EJECUTADO como prueba |
| N-06 | PASS | Captura 05 (local), 08 (remota solo al pulsar), 16–17 (sin red); unit `localSearch_…` |
| N-07 | PASS parcial | Ceros iniciales (unit), permiso denegado (18), sin conexión (16), 404/429/503 (instrumentado). Escaneo con cámara física: NO EJECUTADO |
| N-08 | PASS | Instrumentado `logPortion_…snapshotSurvivesFoodEdit`, `recipe_versionedSnapshot…`, `providerCache_…` |
| N-09 | PASS | Unit `recipe_fixture_total_perServing_per100g` (365 → 182,5) y `recipe_withoutFinalWeight_hasNoGrams…`; captura 13 |
| N-10 | PASS parcial | Recreación con proceso matado (21–22). Cruce de medianoche y cambio de zona: NO EJECUTADO en dispositivo |
| N-11 | PASS | Capturas 11/12/23 (mismo objetivo en Nutrición y Perfil; «desde hoy»); unit `goals_noBackfill…`; sin meta no hay anillo (01) |
| N-12 | PASS | Unit `trend_fixture_twoOfSeven_mean1500`; captura 15 (2/7 días, huecos «Sin registros», sin déficit) |
| N-13 | FAIL parcial | Fuente, preparación y base visibles; atribución ODbL en la ficha (10). **Falta** un NOTICES/pantalla de licencias global |
| N-14 | PASS parcial | Estilo abierto, 48 dp, fuente 200 % (19–20; corregido «Tendencias» que se partía), atrás. TalkBack y orientación: NO EJECUTADO |
| N-15 | PASS | Instalación limpia sin datos precargados (01); ningún control de IA/foto/planificador |

## Corregido en esta sesión
- Pestañas de Nutrición con fuente 200 %: «Tendencias» se partía («Tendenci/as»); ahora una línea que se reduce solo si no cabe.
- Expectativas de pruebas antiguas al día con la base v6.

## Pendiente / requisitos externos
- **Correo de contacto en el User-Agent de Open Food Facts**: OFF lo pide; no se ha puesto ninguno. Decide qué contacto usar.
- **NOTICES / licencias** (N-13): falta pantalla o archivo global de atribución.
- Pruebas manuales en el móvil real (lista entregada al usuario el 27-11-2026): resultado pendiente de que las haga.
- Escáner con cámara física, TalkBack, orientación, API 28, medianoche/cambio de zona: NO EJECUTADO.
- Detalles menores vistos: el nombre de ración («Bol») no se muestra, aparece «Ración de 30 g»; en la ficha OFF «Ración 125 g · 125 g» repite el dato.
- El emulador con GPU por hardware daba capturas corruptas; se usó `-gpu swiftshader_indirect` (lento, la System UI dejó de responder dos veces).
