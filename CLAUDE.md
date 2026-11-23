# Instrucciones para Claude Code

## Actualización H1 y H2 del 24 de noviembre de 2026
Leer primero docs/07_H1_H2_DELIVERY.md, docs/08_MOTION.md y docs/CLAUDE_NEXT.md. Actualizan alcance visual, movimiento y cierre de H1/H2 sobre las propuestas antiguas. La subida de estos documentos no implementa ni autoriza automáticamente código de todas las fases. Al recibir el encargo H1 del usuario, avanzar H1; H2 requiere aprobación posterior. Mantener reglas de no push para implementación salvo autorización explícita. Reutilizar la base H0, no regenerarla. Colores exactos ya aprobados; foto completa, icono, título mayor y descripción obligatorios. No posponer el movimiento requerido de cada fase. No crear código, módulos ni documentación sin utilidad actual.

Lee AGENTS.md, README.md, docs/01_MASTER.md, docs/02_DESIGN_UX.md, docs/03_ARCHITECTURE.md, docs/04_ROADMAP.md y docs/05_ACCEPTANCE.md antes de implementar. Comprueba design/visual-manifest.json. Este paquete es especificación; no demuestra que el repositorio o entorno descrito históricamente sigan igual.

Trabaja en la fase explícitamente autorizada. La entrega inicial autoriza H0 cuando el usuario pegue START_HERE.md. Audita primero si hay código. Conserva trabajo existente, no resetees ni sobrescribas instrucciones sin comparar. No hagas push, merge, publicación ni avances de fase por tu cuenta. El primer commit local en main estaba aprobado en la conversación; si ya existe historia, usa rama de fase y respeta reglas del repositorio.

No inventes imágenes, permisos, funcionalidades, datos ni pruebas realizadas. Los diez originales siguen ausentes, pero hay siete fotografías nuevas y tres referencias reales en design/assets-manifest.json. Úsalas en H1/H2; no atribuyas esos archivos a originales recuperados ni dejes fallbacks como resultado final. No uses los mockups como pantallas bitmap. Las otras siete paletas están pendientes.

Mantén Kotlin/Compose/Material 3/MVVM/StateFlow/Room/DataStore, minSdk 28, com.maikol.fitnessapp, local-first, tema Titanium Champagne y las cinco pestañas aprobadas. H0 simple, sin quince módulos vacíos, permisos Health Connect ni lógica completa de negocio. H1 debe ser reconocible y navegable, con estados vacíos reales.

Para decisiones reversibles usa la especificación propuesta. Ante conflicto que cambie alcance o decisión firme, presenta alternativas concretas y pide decisión. No rellenes huecos con backend, servicios de pago, IA ni integración directa de Xiaomi. Revisa documentación oficial de versiones al configurar y fija compatibilidad, no solo «latest».

Al terminar fase: entrega resumen, árbol/archivos cambiados, decisiones, comandos y resultados reales, capturas disponibles, criterios PASS/FAIL/NO EJECUTADO y limitaciones. Separa lo implementado de lo probado. Detente para aprobación de la fase siguiente.

