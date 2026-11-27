# Nutrición N1–N3 + N1.1 — estado (27-11-2026)

Estado: **implementado, verificado y publicado en `origin/fase/f1`** (sin merge a `main`). Informe: `quality/NUTRITION_REPORT.md`.
Capturas: `quality/evidence/nutrition/`.

Móvil (9LX495KRHMXO99QS): instalada la versión publicada con `install -r`; arranca sin cierre. Copias de datos fuera del repo en
`C:\Users\giral\Documents\FitnessApp\backup-movil-2026-11-27\` (`app-data.tar` antes de v6, `app-data-v6-antes-tendencias.tar`).
La base del móvil no tenía comidas antes de migrar; rutinas (2) y sesiones (2) conservadas.

## Hecho
- Lint 0 errores; 100 tests unitarios y 45 instrumentados (emulator-5554) en verde.
- Capturas reales de los estados pedidos; corregidas las pestañas que partían «Tendencias» con fuente 200 %.

## N1.1 (commit local, sin push hasta revisión)
Ver `quality/NUTRITION_N1_1_REPORT.md`: respuestas OFF inválidas, edición en una transacción, deshacer con reintento,
nombres de ración, redondeo de restantes, atribuciones globales (Perfil + `NOTICES.md`) y campo de código a 14 cifras.

## Pendiente
1. Resultado de las pruebas manuales del usuario en su móvil (sobre todo el escáner con cámara física).
2. **Correo de contacto para el User-Agent de Open Food Facts**: OFF lo pide; no se ha puesto ninguno y no debe inventarse.
   Lo decide el usuario.
3. Revisión de N1.1 y autorización de push.
4. NO EJECUTADO: escáner con cámara física, TalkBack, orientación, API 28, cruce de medianoche y cambio de zona, fallo de Room
   con borrador conservado.

Notas de entorno: emulador con `-gpu swiftshader_indirect` (con GPU por hardware las capturas salen corruptas); capturas por adb
en `/data/local/tmp`.
