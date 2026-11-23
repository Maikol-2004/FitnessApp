# Informe H0 — Base técnica

Fecha: 2026-11-23. Entorno: Windows 11, Android Studio 2026.2.1, JBR 25.0.3, emulador `Medium_Phone_API_37.0` (API 37, 1080×2400, 420 dpi). La toolchain se detalla en [docs/TOOLCHAIN.md](../docs/TOOLCHAIN.md).

## Auditoría inicial
El repositorio no tenía código ni commits. Solo contenía el paquete de especificación. Las sumas de `SHA256SUMS.txt` se verificaron: 13/13 OK. Los archivos del paquete no se han modificado.

## Implementado
- Proyecto de un solo módulo `:app` con Wrapper, catálogo de versiones, `.gitignore` y `.gitattributes`.
- Paquetes `core/designsystem`, `core/navigation`, `core/database`, `core/preferences` y `feature/foundation`. DI manual mediante `AppContainer`.
- Tema Titanium Champagne oscuro único con los tokens de color, la tipografía, el espaciado y los radios propuestos. No usa color dinámico y las barras del sistema son siempre claras sobre oscuro.
- Componentes con previews: `FitnessCard`, `PrimaryButton` (normal, desactivado y cargando), `EmptyState` y `ErrorState`. Incluyen una preview con fuente al 200 % y dos previews de la pantalla base.
- DataStore: preferencia de unidades (kg·km / lb·mi) con una sola instancia. Si la lectura falla, se usa el valor por defecto; si la escritura falla, se informa sin fingir el cambio.
- Room v1 con una tabla técnica `app_metadata` (sin entidades de negocio). Esquema exportado en `app/schemas/` y sin migración destructiva.
- Navegación mínima: un único destino técnico `foundation`. El contrato `TopLevelDestination` fija exactamente Inicio, Entrenar, Progreso, Salud y Perfil, con un test. La barra inferior queda para H1.
- La copia de seguridad automática excluye DB y preferencias. No hay permisos de salud.

## Resultados reales
| Comando | Resultado |
|---|---|
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | 12/12 OK: contraste WCAG de tokens 5, preferencias 3, ViewModel 3, destinos 1 |
| `:app:lintDebug` | 0 errores, 3 avisos de versiones más nuevas descartadas a propósito |
| `:app:connectedDebugAndroidTest` (API 37) | 3/3 OK: DAO/callback 2, preferencia tras recrear Activity 1 |

El contraste se verificó con la fórmula WCAG en las combinaciones usadas. Texto ≥ 4.5:1 sobre fondo, tarjeta y superficie elevada; onPrimary sobre champán; primary, error y success sobre fondo y tarjeta. El contorno es ≥ 3:1. Todas pasan.

Verificación manual en el emulador API 37:
- La primera apertura no da crash y aplica el tema.
- Se seleccionó lb·mi, se mató el proceso (`am force-stop`, sin PID) y se reabrió la app: lb·mi sigue seleccionada según `uiautomator`.
- La DB `fitness.db` y `datastore/user_preferences.preferences_pb` se crearon.
- El único permiso del manifiesto fusionado es `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, propio de la app y añadido por androidx.core.
- Con fuente al 200 % no hay cortes y los objetivos táctiles miden ≥ 48 dp. La escala original (1.0) se restauró.

## Capturas (`quality/evidence/h0/`)
01 primera apertura · 02 tras elegir lb·mi · 03 tras matar el proceso y reabrir · 04 fuente al 200 %. Todas en el emulador API 37. Las capturas de previews no se exportaron: se ven en Android Studio.

## Criterios
| ID | Estado | Nota |
|---|---|---|
| AC00 | PASS | Wrapper; assemble, unitarias y lint en verde |
| AC01 | PASS en API 37 | Sin crash, tema aplicado, preferencia conservada tras matar el proceso, sin permisos de salud |
| AC01 en API 28 | NO EJECUTADO | No hay imagen de sistema API 28 instalada |
| Revisión visual frente a originales | NO EVALUABLE | Hay 0/10 imágenes originales |

## Limitaciones y pendientes
- Falta probar en API 28 y en dispositivo físico.
- TalkBack no se ha probado de forma manual. Los encabezados y las regiones dinámicas están anotados en código.
- Compose BOM 2026.11.01 requiere instalar SDK Platform 37.1. Si lo instalas, se puede subir la versión.
- El icono de la app es una mancuerna geométrica provisional y no procede de ninguna referencia.
- La pantalla `foundation` es técnica. En H1 se sustituye por Inicio con las cinco pestañas.
