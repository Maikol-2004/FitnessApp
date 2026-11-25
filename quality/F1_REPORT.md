# Informe F1 — lote funcional y primer Liquid Glass

Rama `fase/f1` (desde `fase/h2` integrada con `origin/main`). Fecha 25-11-2026. Commit local, **sin push**.

## Resumen

- Inicio conserva dos tarjetas por fila con las seis fotografías v2 (sujeto a la derecha, texto a la izquierda).
- Menú inferior «Liquid Glass»: cápsula grafito ahumada con lente champán que se desliza (240 ms; salto inmediato con «Reducir animaciones»). Cinco destinos.
- Registros manuales locales reales (Room v3) en Salud, Nutrición, Hidratación, Sueño y Peso. Metas opcionales en Perfil. Progreso con la semana de entrenamiento H2 y la tendencia de peso.
- Entrenamiento H2 intacto. Se eliminó la pantalla «próximamente» (`UpcomingFeatureScreen`).

## Archivos

**Nuevos**
- `core/navigation/LiquidGlassDock.kt`
- `core/database/TrackingEntities.kt`, `core/database/TrackingDao.kt`
- `core/tracking/TrackingCalc.kt`, `core/tracking/TrackingRepository.kt`
- `feature/tracking/TrackingUi.kt`
- `feature/{health,nutrition,hydration,sleep,progress}/*`
- `res/values/strings_f1.xml`
- `res/drawable-nodpi/category_*_right_v2.webp` (6)
- `app/schemas/…/3.json`
- Pruebas: `test/…/core/tracking/TrackingCalcTest.kt`, `androidTest/…/core/database/MigrationV3Test.kt`

**Modificados**
- `AppContainer`, `FitnessDatabase` (v3, `AutoMigration(2, 3)`), `UserPreferencesRepository` (metas)
- `FitnessNavHost` (dock superpuesto y rutas reales), `PrimaryButton` (borde de luz)
- Inicio (`HomeCategory`, `HomeComponents`, `HomeScreen`, `HomeViewModel`)
- Perfil (metas), pantallas H2 (margen del dock, `TrainingHero` con foto)
- `strings.xml`
- Pruebas H1/H2 adaptadas a F1: `NavigationTest`, `ProfilePersistenceTest`, `TrainingFlowTest`, `MigrationTest`

**Eliminados**
- `feature/upcoming/UpcomingFeatureScreen.kt`
- `res/drawable-nodpi/h1_training.webp`: Inicio usa ya la v2 y lint lo marcaba sin uso. El original sigue en `design/assets/photos/`.

## Decisiones

- **Fechas.** Actividad y peso usan la fecha local. Comidas y agua guardan instante UTC + zona + fecha local. El sueño cuenta en el día del despertar y su duración se calcula entre instantes, así que es correcta al cruzar medianoche y en el cambio de hora.
- **Vacío frente a cero.** Un campo vacío significa desconocido (null) y no cuenta como 0. Los totales de nutrición marcan «Incompleto» solo cuando unos registros indican el campo y otros no.
- **Sueños solapados.** Se rechazan dentro de una transacción. El formulario se conserva y muestra la noche en conflicto.
- **Unidades.** kg/lb y km/mi se convierten sin deriva: si el texto no se edita, se conserva el valor base.
- **Liquid Glass.** No hay desenfoque real del fondo. El fallback documentado es translúcido: grafito 0,74, brillo superior y borde champán. No se finge un blur.
- **Fuente del sistema muy grande.**
  - Las etiquetas del dock crecen hasta 1,15× y luego se mantienen, iguales en las cinco celdas, igual que hace `NavigationBar`.
  - Las cifras de los aros se ajustan dentro del círculo.

## Comandos y resultados reales

| Comando | Resultado |
|---|---|
| `gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Unitarias | 55/55 OK (11 nuevas en `TrackingCalcTest`) |
| Lint | 0 errores. 8 avisos anteriores a F1 (plurales de H2, versiones de AGP/dependencias, `screenHeightDp` en `SessionScreen`, «(c)»). Los 5 avisos introducidos por F1 se corrigieron. |
| `adb install -r` + `am instrument -w -r …AndroidJUnitRunner` (Medium_Phone_API_37.0) | **OK (22 tests)** |

- La primera ejecución instrumentada dio 6 fallos:
  - Textos de las tarjetas antiguas.
  - Pantallas «próximamente».
  - Varios campos de texto en Perfil.
  - `TrainingFlowTest` suponía una base vacía.
- Se corrigieron y la segunda ejecución pasó entera.
- `TrainingFlowTest` borra la base antes y después: solo debe ejecutarse en el emulador de pruebas, que contenía datos «Prueba».

## Recorrido manual (emulador, datos «Prueba»)

- **Migración en sitio.** Se instaló F1 sobre la app H2 con la rutina «Prueba torso», una sesión completada de 400 kg, una sesión pausada y el nombre «Prueba». Todo se conservó (captura 03).
- **Salud.**
  - Guardar sin valores muestra un aviso.
  - Se guardaron 6240 pasos, 4,2 km y 0 kcal, con los minutos vacíos.
  - Al editar, los pasos pasan a 9000.
  - Inicio muestra el registro con su fuente «Manual» (capturas 04, 06–08).
- **Metas.** Un valor fuera de rango («Entre 250 y 10000») no se guarda (captura 05).
- **Nutrición.** Total de 1.150 kcal → al editar, 1.050 → al eliminar una comida, 600. El campo parcial aparece marcado como «Incompleto» (capturas 09–10).
- **Hidratación.**
  - 250 + 500 = 750 ml. Añadir 150 y deshacer elimina solo ese registro.
  - Editar 250 → 150 deja 650. Eliminar 500 deja 150 (capturas 11–12).
- **Sueño.** Una noche de 8 h. El solape se rechaza y el formulario se conserva. El total de 7 días es 16 h y la media 8 h (capturas 13–15).
- **Progreso.**
  - 1 sesión, 400 kg.
  - Peso 73,1 → 72,4 kg, diferencia −0,7 kg, con tendencia y descripción accesible (captura 16).
  - En lb: 159,6 / 161,2 lb y 882 lb. Al volver a kg los valores son exactos: 72,4 / 73,1 (captura 18).
- **Reinicio.** Tras force-stop y reapertura, los datos siguen presentes (captura 17).
- **Fuente 200 % y animaciones del sistema a 0.**
  - Se detectaron dos fallos: la cifra salía del aro de pasos y las etiquetas del dock salían desiguales o cortadas.
  - Están corregidos y recapturados (capturas 19–20).

## Criterios

| ID | Criterio | Estado |
|---|---|---|
| F1-01 | Dos columnas y recursos v2 | PASS |
| F1-02 | Menú Liquid Glass y cinco rutas | PASS (desenfoque real sustituido por fallback translúcido documentado) |
| F1-03 | H2 conservada con historial y recuperación | PASS (migración en sitio + `TrainingFlowTest`) |
| F1-04 | Salud manual: guarda, edita, borra y actualiza Hoy | PASS |
| F1-05 | Nutrición real y totales | PASS |
| F1-06 | Agua real, meta, relleno y deshacer | PASS |
| F1-07 | Sueño con duración y fechas correctas | PASS (incluido el cambio de hora en unitarias) |
| F1-08 | Peso, historial y tendencia | PASS |
| F1-09 | Progreso de entrenamiento real | PASS |
| F1-10 | Perfil, metas y unidades persistentes | PASS |
| F1-11 | Todo offline tras reinicio | PASS (sin permiso de red; persistencia tras force-stop). Modo avión: NO EJECUTADO como paso aparte. |
| F1-12 | Migración sin pérdida | PASS (`MigrationV3Test` con el esquema v2 real + migración en sitio) |
| F1-13 | Sin demos ni botones falsos | PASS («DEMO» solo existe en `@Preview`) |
| F1-14 | Accesibilidad y movimiento | PASS parcial: fuente 200 %, animaciones a 0, «Reducir animaciones», `Role.Tab` y descripciones. TalkBack: NO EJECUTADO. |
| F1-15 | Pruebas finales registradas y fallos resueltos | PASS |

## Limitaciones

- **No ejecutado.** API 28 físico/emulador, TalkBack y modo avión como paso independiente.
- **Liquid Glass.** No hay desenfoque real del fondo (fallback translúcido).
- **Selectores de fecha y hora.** Siguen el idioma del dispositivo (inglés en el emulador).
- **Datos del emulador.** Las pruebas instrumentadas vacían la base: los datos «Prueba» de las capturas ya no están.
- **Reloj.** La integración del reloj queda para una fase posterior; todo es entrada manual.

APK Debug: `app/build/outputs/apk/debug/app-debug.apk`.
