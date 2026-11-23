# Informe H1 — Inicio, navegación, Perfil y movimiento

Fecha: 2026-11-24. Rama `fase/h1` (desde `main` a0fc8cc). Entorno: Windows 11, JBR de Android Studio, emulador `Medium_Phone_API_37.0` (API 37, 1080×2400, 420 dpi, ≈411 dp de ancho). Sin push.

## Implementado
- **Inicio**:
  - Saludo «Hola» o «Hola, nombre» y avatar que abre Perfil.
  - «Actividad de hoy» en estado vacío real: anillo «—», «Sin datos» y botón «Ver salud».
  - Seis tarjetas fotográficas a sangre (Entrenamiento, Salud, Nutrición, Hidratación, Sueño, Progreso), cada una con icono champán, título mayor y descripción.
  - Tarjeta «Tu próximo entrenamiento» con «Ver entrenamiento».
  - Dos columnas que pasan a una sola cuando el ancho o la fuente no dejan sitio al título más largo.
- **Navegación**:
  - Barra con las cinco pestañas aprobadas.
  - Entrenamiento, Salud y Progreso abren su raíz; Nutrición, Hidratación y Sueño son destinos secundarios dentro de Inicio, con flecha Atrás.
  - Volver a tocar la pestaña activa no apila copias.
- **Pantallas «Próximamente»**: foto, título, icono y texto concreto de lo que llegará. Entrenar incluye además «Volver a Inicio». No hay acciones falsas.
- **Perfil**:
  - Nombre opcional con Guardar y confirmación «Guardado», o un error que conserva el borrador.
  - Unidades kg·km / lb·mi.
  - «Reducir animaciones».
  - Apariencia Titanium Champagne en solo lectura.
  - Todo se guarda en DataStore.
- **Movimiento (doc 08)**:
  - M01 pulsación a 0,98.
  - M02 entrada de pantalla secundaria (8 dp + alpha, 220/140 ms) y entrada única de Inicio (180 ms).
  - M03 crossfade entre raíces (150 ms).
  - M04 cargador de mancuerna: aparece a partir de 200 ms, muestra «Está tardando…» a los 10 s y queda estático con movimiento reducido.
  - M05 aviso de estado (120 ms).
  - Con movimiento reducido (ajuste propio o animaciones del sistema desactivadas) solo quedan fundidos de 90 ms.
- **Recursos**:
  - Las siete fotos del manifiesto se copiaron a `drawable-nodpi` (876 KB, hashes verificados).
  - Iconos vectoriales propios.
  - Ningún mockup se usa como bitmap.
- **Eliminado de H0**: la pantalla técnica `foundation` y `AppMetadataRepository`, que ya no tenían uso. Room se mantiene sin cambios para H2.

## Comandos y resultados reales
Última ejecución sobre el código final:

`gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` → **BUILD SUCCESSFUL**.

| Tarea | Resultado |
|---|---|
| Unitarias | 22/22 OK: LoadingPhase 4, contraste 5, destinos 1, preferencias 4, categorías 1, ProfileViewModel 7 |
| Instrumentadas (API 37) | 9/9 OK: DB 2, LoadingRegion 2, Navegación 4, persistencia de Perfil 1 |
| Lint | 0 errores. Hay 3 avisos de versiones más nuevas, descartadas a propósito (igual que en H0) |
| Release | `app-release-unsigned.apk` generado. Solo declara el permiso interno de AndroidX para receptores no exportados; no hay permisos de salud |

## Criterios
| ID | Estado | Evidencia |
|---|---|---|
| H1-01 Instalación vacía | PASS | `pm clear` y apertura sin crash: saludo «Hola», actividad «Sin datos», próximo entrenamiento vacío. Sin datos DEMO (capturas 02 y 03) |
| H1-02 Tokens del tema | PASS | Colores exactos de H0 y test de contraste 5/5 |
| H1-03 6 tarjetas, 5 raíces y avatar | PASS | `NavigationTest` 4/4 y capturas 08–10 |
| H1-04 Persistencia y fallo de escritura | PASS | En el dispositivo: nombre y lb·mi guardados, `force-stop` (sin PID), reapertura con «Hola, Prueba» y lb·mi marcado (capturas 05–07). El fallo de escritura muestra el error y conserva el borrador (`ProfileViewModelTest`). `ProfilePersistenceTest` recrea la Activity |
| H1-05 Fotos sin conexión | PASS | Son recursos locales en el APK; no hay código de red |
| H1-06 Fuente 200 %, 320/360 dp y teclado | PASS (320 y 411 dp) / 360 dp NO EJECUTADO | Capturas 11–14 y 05 (teclado: los controles siguen accesibles). Se corrigieron dos defectos: etiquetas de pestaña partidas a 200 % (ahora autoajustadas en una línea) y el título «Entrenamiento» partido |
| H1-07 Carga a 50/800 ms, error y cancelación | PASS | `LoadingPhaseTest` (tiempo virtual) y `LoadingRegionTest` (reloj de Compose en el dispositivo). Error: `ProfileViewModelTest` |
| H1-08 Navegación rápida y toques repetidos | PASS | `rapidTabTaps_endOnLastDestination` y repetición de pestaña en `NavigationTest` |
| H1-09 Reducir movimiento y escala 0 | PASS | `animator_duration_scale 0`: navegación correcta (captura 15) y valor restaurado. El ajuste propio activo persiste tras `force-stop` (captura 16) |
| H1-10 Build de producción limpia | PASS | Ver la tabla de comandos. DEMO solo existe en `@Preview` |

## Capturas (`quality/evidence/h1/`)
| Archivo | Contenido |
|---|---|
| 01 | Captura temprana de Inicio, anterior a la corrección del título |
| 02, 03 | Inicio vacío: parte superior y final |
| 04 | Perfil |
| 05 | Perfil con el teclado abierto |
| 06 | Perfil tras guardar (nombre y lb·mi) |
| 07 | Inicio tras `force-stop` |
| 08 | Nutrición (destino secundario) |
| 09 | Entrenar |
| 10 | Salud |
| 11–13 | Fuente al 200 % |
| 14 | Inicio a 320 dp (una sola columna) |
| 15 | Escala de animación del sistema a 0 |
| 16 | «Reducir animaciones» activo |
| `h1-transiciones.mp4` | 16 s: arranque, tarjeta, Atrás y las cinco pestañas |

El nombre de prueba es «Prueba».

## Pendiente y limitaciones
- **API 28 NO EJECUTADO**: no hay imagen del sistema API 28 instalada.
- **TalkBack manual NO EJECUTADO**: las semánticas se han revisado en el código (encabezados, roles Tab/Button/Switch, región en vivo, foto decorativa), pero no con el lector de pantalla.
- **360 dp NO EJECUTADO** como tamaño exacto. Se probaron 320 dp y ≈411 dp.
- **Mancuerna (M04)**: en H1 no hay ninguna carga real lenta, así que se verifica con previews y pruebas sobre una carga simulada, no en uso real.
- **Vídeo**: no he revisado los fotogramas porque no tengo `ffmpeg`.
- **Arranque en frío**: en este emulador tarda 3–6 s (`TotalTime`) y las capturas tomadas antes salen negras. No se ha medido en un dispositivo físico.
- **APK de release**: unos 27 MB sin firmar y sin R8. La reducción de tamaño queda pendiente de una fase posterior.
- **H0 sin uso**: `EmptyState` y `ErrorState` siguen en el sistema de diseño sin uso en producción tras retirar `foundation`. Se mantienen para las pantallas de datos de H2.
