# Instrucciones para Claude Code

## Encargo vigente V1 — editor abierto con Liquid Glass
Prevalecen docs/15_OPEN_GLASS_ROUTINE_EDITOR.md y docs/CLAUDE_NEXT.md. Referencia aprobada en design/routine-editor-open-glass/approved-reference.png: sin tarjeta exterior ni fondo por fila, campos y botones glass sobre fondo. Conservar G1/G2/G2.1, datos y corrección de volumen asistido. Este lote es del editor, no de la sesión activa. No push posterior de implementación sin autorización.

## Encargo vigente G2 — imágenes y ficha de cada ejercicio
Prevalecen docs/14_EXERCISE_MEDIA_AND_PROGRESS.md y docs/CLAUDE_NEXT.md. Medios por catalogId en design/exercises-v1/manifest.json; revisar VISUAL_REVIEW.md. G1 está completado: conservarlo junto a los datos. Los frames son posiciones estáticas, no vídeos fluidos. Implementar ficha y estadísticas reales sin rehacer fases previas. No push posterior del código integrado sin autorización.

## Encargo vigente G1 — biblioteca guiada de ejercicios
Prevalecen docs/13_EXERCISE_LIBRARY.md y docs/CLAUDE_NEXT.md para el nuevo encargo. Conservar el refinamiento visual ya terminado localmente y los datos. G1 añade catálogo y selección de ejercicios, fichas y guía de sesión sin regenerar Entrenar. Los encargos anteriores son antecedentes cuando contradigan el alcance actual. No push posterior de implementación sin autorización.

## Encargo vigente: refinamiento visual tras F1.1
Prevalecen docs/12_VISUAL_REFINEMENT.md y docs/CLAUDE_NEXT.md. F1/F1.1 están completadas; integrar la propuesta de deliveries/2026-11-25-visual sin reconstruir fases ni perder datos. Los encargos anteriores se conservan como historia cuando no contradigan este. Mantener dos columnas, fotos existentes y primer Liquid Glass. Esta entrega documental no modifica app/ ni autoriza un push futuro del código integrado por Claude.

## Encargo actual F1 y Liquid Glass
Prevalecen docs/10_FUNCTIONAL_MVP.md, docs/11_LIQUID_GLASS.md y docs/CLAUDE_NEXT.md. El usuario solicita conservar H2, mantener dos columnas y ejecutar un lote funcional completo: todas las tarjetas con registro local real, primer Liquid Glass y pruebas conjuntas al final. No parar entre antiguos H3–H6 para ese mínimo; no rehacer H0/H2. Las restricciones antiguas de solo ajuste visual o solo H1 son históricas para este encargo. Seguir los límites de F1, no inventar datos ni borrar trabajo; no push de implementación sin autorización posterior.

## Antecedente visual del 25 de noviembre de 2026
Para el encargo actual leer docs/09_VISUAL_POLISH.md y docs/CLAUDE_NEXT.md. H2 está en su rama y se conserva. Mantener DOS tarjetas por fila, imágenes v2 pequeñas/completas a la derecha y texto a la izquierda. Ajustar dock/botones usando componentes existentes. No comenzar H3, regenerar H0/H2 ni crear infraestructura sin uso. La entrega de estas instrucciones no implementa el cambio de Kotlin. Las reglas anteriores de fases quedan como contexto histórico cuando no contradigan este encargo.

## Actualización H1 y H2 del 24 de noviembre de 2026
Leer primero docs/07_H1_H2_DELIVERY.md, docs/08_MOTION.md y docs/CLAUDE_NEXT.md. Actualizan alcance visual, movimiento y cierre de H1/H2 sobre las propuestas antiguas. La subida de estos documentos no implementa ni autoriza automáticamente código de todas las fases. Al recibir el encargo H1 del usuario, avanzar H1; H2 requiere aprobación posterior. Mantener reglas de no push para implementación salvo autorización explícita. Reutilizar la base H0, no regenerarla. Colores exactos ya aprobados; foto completa, icono, título mayor y descripción obligatorios. No posponer el movimiento requerido de cada fase. No crear código, módulos ni documentación sin utilidad actual.

Lee AGENTS.md, README.md, docs/01_MASTER.md, docs/02_DESIGN_UX.md, docs/03_ARCHITECTURE.md, docs/04_ROADMAP.md y docs/05_ACCEPTANCE.md antes de implementar. Comprueba design/visual-manifest.json. Este paquete es especificación; no demuestra que el repositorio o entorno descrito históricamente sigan igual.

Trabaja en la fase explícitamente autorizada. La entrega inicial autoriza H0 cuando el usuario pegue START_HERE.md. Audita primero si hay código. Conserva trabajo existente, no resetees ni sobrescribas instrucciones sin comparar. No hagas push, merge, publicación ni avances de fase por tu cuenta. El primer commit local en main estaba aprobado en la conversación; si ya existe historia, usa rama de fase y respeta reglas del repositorio.

No inventes imágenes, permisos, funcionalidades, datos ni pruebas realizadas. Los diez originales siguen ausentes, pero hay siete fotografías nuevas y tres referencias reales en design/assets-manifest.json. Úsalas en H1/H2; no atribuyas esos archivos a originales recuperados ni dejes fallbacks como resultado final. No uses los mockups como pantallas bitmap. Las otras siete paletas están pendientes.

Mantén Kotlin/Compose/Material 3/MVVM/StateFlow/Room/DataStore, minSdk 28, com.maikol.fitnessapp, local-first, tema Titanium Champagne y las cinco pestañas aprobadas. H0 simple, sin quince módulos vacíos, permisos Health Connect ni lógica completa de negocio. H1 debe ser reconocible y navegable, con estados vacíos reales.

Para decisiones reversibles usa la especificación propuesta. Ante conflicto que cambie alcance o decisión firme, presenta alternativas concretas y pide decisión. No rellenes huecos con backend, servicios de pago, IA ni integración directa de Xiaomi. Revisa documentación oficial de versiones al configurar y fija compatibilidad, no solo «latest».

Al terminar fase: entrega resumen, árbol/archivos cambiados, decisiones, comandos y resultados reales, capturas disponibles, criterios PASS/FAIL/NO EJECUTADO y limitaciones. Separa lo implementado de lo probado. Detente para aprobación de la fase siguiente.


