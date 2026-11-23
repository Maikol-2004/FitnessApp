# Reglas para agentes en FitnessApp

Ámbito: raíz del repositorio y descendientes salvo reglas más específicas válidas. No es necesario crear subagentes para usar este paquete.

## Fuente de verdad
Orden: instrucciones actuales del usuario; decisiones firmes documentadas; contratos de aceptación; especificaciones propuestas. Una imagen futura puede aclarar estética pero no autoriza cambios de funcionalidad o navegación. design/visual-manifest.json es autoridad sobre disponibilidad de assets; rutas previstas no son archivos existentes.

## Trabajo y calidad
Inspeccionar antes de cambiar. Cambios pequeños por fase. No tocar datos, credenciales ni configuración global del usuario. No sobrescribir trabajo ajeno ni regenerar proyecto encima de código válido. Mantener español en interfaz y documentación; nombres de código consistentes en inglés. Sin secretos ni datos personales en logs, fixtures o commits.

Usar repositorios como frontera de persistencia, estado observable y composables sin acceso directo a infraestructura. Room con migraciones verificables; DataStore para preferencias. Separar datos demo de datos del usuario. Evitar dependencias y abstracciones sin uso real.

Cada corrección debe incluir validación acorde al riesgo; priorizar cálculos, integridad, recuperación y permisos. No crear tests vacíos para alcanzar cobertura. Documentar comandos no disponibles y fallos sin ocultarlos. Capturas de demo deben decir Demo.

No publicar ni hacer push sin instrucción explícita. No avanzar H0→H1 ni entre fases sin aprobación. No cambiar estructura de cinco pestañas ni añadir Bienestar. No afirmar integración de reloj sin registros observables del proveedor.
