# Nutrición N1–N3 — punto de parada (27-11-2026, 11:10)

Estado: **implementado y verificado; esperando revisión del usuario**. Commits locales en `fase/f1`, **sin push**.
Informe completo: `quality/NUTRITION_REPORT.md`. Capturas: `quality/evidence/nutrition/`.

Copia de seguridad del móvil (previa a instalar la v6) fuera del repo:
`C:\Users\giral\Documents\FitnessApp\backup-movil-2026-11-27\app-data.tar`.

## Hecho
- Lint 0 errores; 100 tests unitarios y 45 instrumentados (emulator-5554) en verde.
- Capturas reales de todos los estados pedidos (vacío, «Prueba», búsqueda local/remota, cantidad, receta, tendencias, objetivos,
  modo avión, cámara denegada, fuente 200 %, recreación).
- Corregido: pestañas que partían «Tendencias» con fuente 200 %.

## Pendiente (requiere al usuario o decisión)
1. Revisión del usuario → autorización de push.
2. Correo de contacto para el User-Agent de Open Food Facts (no se ha puesto ninguno).
3. Pantalla/archivo NOTICES global (N-13).
4. Con el móvil conectado: comprobar comidas antiguas en «Sin asignar»; instalar con `-r` solo si lo pide (lleva la corrección
   de pestañas, que no está en la versión instalada).
5. NO EJECUTADO: escáner con cámara física, TalkBack, orientación, API 28, medianoche/cambio de zona.
6. Menores: nombre de ración propio no se muestra («Ración de 30 g»); ficha OFF repite «125 g · 125 g».

Notas de entorno: emulador con `-gpu swiftshader_indirect` (con GPU por hardware las capturas salen corruptas); las capturas por
adb se guardan en `/data/local/tmp` (tras arrancar, `/sdcard` tarda en estar disponible).
