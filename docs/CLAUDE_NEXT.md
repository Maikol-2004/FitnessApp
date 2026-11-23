# Encargo siguiente para Claude Code

## Recuperar cambios sin perder trabajo local
Primero comprueba carpeta, remoto, rama, estado y commits locales. El remoto esperado es https://github.com/Maikol-2004/FitnessApp.git. No uses reset --hard, clean ni push forzado. Conserva cambios propios y ajenos.
Si estás en main limpia y sin divergencia, recupera origin/main con pull --ff-only. Si trabajas en una rama H1, haz fetch origin y trae los cambios documentales de origin/main a tu rama con merge normal cuando sea compatible; no cambies de rama encima de modificaciones sin guardar. Si hay cambios sin commit, guárdalos de forma identificable antes de integrar; no hagas stash automático de trabajo ajeno ni descartes nada. Si hay conflicto, conserva ambas intenciones y resuélvelo; consulta únicamente cuando la elección afecte al trabajo del usuario y no pueda deducirse. No hagas pull ciego con archivos sueltos del ZIP que colisionen con las mismas rutas nuevas.

## Lectura y autorización de ejecución
Lee AGENTS.md, CLAUDE.md, docs/07_H1_H2_DELIVERY.md, docs/08_MOTION.md, design/assets-manifest.json y design/titanium-champagne/colors.json. Abre las tres referencias reales de design/references/. Los documentos 07/08 actualizan H1/H2 del paquete inicial y prevalecen sobre indicaciones antiguas de no usar descripciones o aplazar todo el movimiento. No vuelvas a ejecutar H0 desde cero.

Al recibir este encargo del usuario, implementa **H1 solamente** sobre lo ya existente; usa rama de fase si aún estás en main. La autorización para que el asistente suba documentación no autoriza por sí sola que publiques cambios de código. No hagas push ni avances a H2 sin indicación posterior.

Quiero Inicio con las fotografías locales a fondo completo, iconos champán, títulos mayores y las seis descripciones. Mantén cinco pestañas y Perfil funcional con nombre/unidades/reducir animaciones. No alteres los colores exactos ni introduzcas métricas DEMO en producción.
Esta vez el movimiento especificado forma parte de H1: M01–M05 de 08_MOTION.md, incluida mancuerna de carga durante esperas reales y alternativa accesible. No retrasar operaciones para mostrar animaciones. H2 tendrá su propio flujo completo y M06–M09 cuando la autorice.

Trabaja en pasos verticales: Inicio real y primera captura; navegación/Perfil; movimiento/estados; pruebas/cierre. No crear estructuras vacías ni actualizar herramientas que ya funcionan. Reutiliza tokens, AppContainer, DataStore y Navigation Compose existentes. No implementar CRUD de H2 ni Health Connect de H3 por anticipado.

Muestra una captura temprana al compilar Inicio, continúa dentro de H1 sin pedir aprobación entre pasos internos y entrega un informe corto con resultados H1-01 a H1-10, capturas/vídeo disponibles, comandos reales, pendientes y limitaciones. Corrige problemas de H1 antes de declararla finalizada. Mantén los pendientes API 28/TalkBack si no se han ejecutado. Detente y pide aprobación para H2.

## Encargo H2 reservado para aprobación posterior
Cuando el usuario autorice H2, seguir H2 A–E del documento 07 y M06–M09 del 08. Lista/editor/detalle/sesión/descanso/resumen/historial deben quedar utilizables, persistidos y con acabado visual. Room v1 se migra sin pérdida; no reescribir tema/Home de H1. Mostrar evidencia por paso y una sola entrega final H2. No comenzar H3 ni añadir servicios, alarmas exactas, backend o dependencias de animación sin requisito concreto.
