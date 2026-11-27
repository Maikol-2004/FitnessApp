# Nutrición N1.1 — informe (27-11-2026)

Sobre `8a44cc6`, rama `fase/f1`. Commit local **sin push** hasta revisión. Entrenar no se ha tocado. Datos y funciones
existentes conservados; sin cambio de esquema (Room sigue en v6).

## Cambios
| # | Problema | Corrección |
|---|---|---|
| 1 | Búsqueda OFF: un 200 con HTML/JSON inválido lanzaba excepción sin controlar, y un JSON sin `hits` se mostraba como «sin resultados». | Nuevo `RemoteResult.InvalidResponse`. `parseSearchResponse` solo da «0 resultados» con una lista `hits` real; cualquier otro 200 es error recuperable con «Reintentar». Producto con cuerpo inválido: el mismo error (antes `Failed(200)`). Red de seguridad en el ViewModel: un fallo inesperado del proveedor llega como error, no como cierre. |
| 2 | Editar un alimento: `updateItemQuantity` y `moveItem` iban por separado; si mover fallaba, la cantidad quedaba cambiada. | `NutritionRepository.updateItem` hace los dos pasos en una única transacción; `PortionViewModel` la usa. |
| 3 | `undo` borraba la referencia antes de restaurar: un fallo perdía lo borrado. | `UndoSlot` común: conserva lo borrado hasta que `restore` termina bien; si falla, aviso «No se pudo deshacer» con «Reintentar»; doble toque no restaura dos veces. Aplicado en `EntryViewModel` y también en `NutritionViewModel`, que tenía el mismo fallo. |
| 4a | El nombre propio de la ración («Bol») no se mostraba. | La opción de unidad muestra «Bol (30 g)»; la ficha, «Bol · 30 g». |
| 4b | Ficha OFF: «Ración 125 g · 125 g». | Un nombre que solo repite la cantidad no se muestra: «Ración · 125 g». |
| 4c | Registradas y restantes no sumaban el objetivo (548 + 1.453 = 2.001). | Un único redondeo (`roundKcal`); la diferencia se calcula sobre las kcal mostradas: 248 + 1.752 = 2.000. |
| 5 | Faltaban las atribuciones globales (N-13). | Perfil → «Fuentes y licencias» (Open Food Facts ODbL/DbCL, ML Kit, AndroidX/Kotlin Apache 2.0) y `NOTICES.md` con enlaces. **Correo de contacto de OFF: sin poner, pendiente de tu elección.** |
| extra | Encontrado al verificar: el campo numérico cortaba a 9 cifras, así que un EAN-13 no se podía escribir a mano (ni en «Introducir código» ni en «Crear alimento»). | Límite configurable; los campos de código admiten 14 cifras. |

## Comprobaciones añadidas
- Unitarias: `UndoSlotTest` (éxito, fallo y reintento, doble toque, descartar mientras restaura), `goalRemaining_usesDisplayedRounding`,
  `servingDisplayName_hidesQuantityOnlyLabels`.
- Instrumentadas (emulador): `OpenFoodFactsFailureTest` (HTML, vacío, truncado, sin `hits`, `[]`, `null`; lista vacía real;
  429/503; producto inválido; cliente sin red y ViewModel: error → reintento → resultados), `updateItem_moveFails_rollsBackQuantity_thenRetrySucceeds`,
  `undo_restoreFails_keepsReference_retryRestoresOnce`, `ManualCodeDialogTest` (EAN-13 con cero inicial completo).

## Verificación conjunta
| Comando | Resultado |
|---|---|
| `testDebugUnitTest` | **106 tests, 0 fallos** |
| `lintDebug` | **0 errores** (5 avisos y 1 nota, los mismos de antes) |
| `am instrument` completo en emulator-5554 | **OK (53 tests)**, incluida la regresión de Entrenar |
| Emulador, a mano | Ración «Bol (30 g)», 248 + 1.752 = 2.000, ficha OFF «Ración · 125 g» (2 peticiones reales), tarjeta de licencias |

Capturas: `quality/evidence/nutrition/n11-01…n11-04`.

## Pendiente
- Correo de contacto para el User-Agent de Open Food Facts (tu elección).
- NO EJECUTADO: escáner con cámara física, TalkBack, orientación, API 28, medianoche/cambio de zona. No se ha instalado en el móvil.
- `ManualCodeDialogTest` no se ejecutó contra el código anterior para verlo fallar; el corte a 9 cifras sí se reprodujo a mano en el emulador.
