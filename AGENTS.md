# Reglas para agentes en FitnessApp

## Actualización H1 y H2 del 24 de noviembre de 2026
Leer primero docs/07_H1_H2_DELIVERY.md, docs/08_MOTION.md y docs/CLAUDE_NEXT.md. Actualizan alcance visual, movimiento y cierre de H1/H2 sobre las propuestas antiguas. La subida de estos documentos no implementa ni autoriza automáticamente código de todas las fases. Al recibir el encargo H1 del usuario, avanzar H1; H2 requiere aprobación posterior. Mantener reglas de no push para implementación salvo autorización explícita. Reutilizar la base H0, no regenerarla. Colores exactos ya aprobados; foto completa, icono, título mayor y descripción obligatorios. No posponer el movimiento requerido de cada fase. No crear código, módulos ni documentación sin utilidad actual.

Ámbito: raíz del repositorio y descendientes salvo reglas más específicas válidas. No es necesario crear subagentes para usar este paquete.

## Fuente de verdad
Orden: instrucciones actuales del usuario; decisiones firmes documentadas; contratos de aceptación; especificaciones propuestas. Una imagen futura puede aclarar estética pero no autoriza cambios de funcionalidad o navegación. design/assets-manifest.json es autoridad actual sobre recursos nuevos. design/visual-manifest.json conserva únicamente el estado histórico de los diez originales ausentes; las rutas previstas no son archivos existentes.

## Trabajo y calidad
Inspeccionar antes de cambiar. Cambios pequeños por fase. No tocar datos, credenciales ni configuración global del usuario. No sobrescribir trabajo ajeno ni regenerar proyecto encima de código válido. Mantener español en interfaz y documentación; nombres de código consistentes en inglés. Sin secretos ni datos personales en logs, fixtures o commits.

Usar repositorios como frontera de persistencia, estado observable y composables sin acceso directo a infraestructura. Room con migraciones verificables; DataStore para preferencias. Separar datos demo de datos del usuario. Evitar dependencias y abstracciones sin uso real.

Cada corrección debe incluir validación acorde al riesgo; priorizar cálculos, integridad, recuperación y permisos. No crear tests vacíos para alcanzar cobertura. Documentar comandos no disponibles y fallos sin ocultarlos. Capturas de demo deben decir Demo.

No publicar ni hacer push sin instrucción explícita. No avanzar H0→H1 ni entre fases sin aprobación. No cambiar estructura de cinco pestañas ni añadir Bienestar. No afirmar integración de reloj sin registros observables del proveedor.

