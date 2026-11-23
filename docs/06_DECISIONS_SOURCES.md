# Decisiones, procedencia y fuentes

## Evidencia recuperada
Conversación «Recopilación Apps Salud Fitness», id 6afc4290-3e98-83ed-8ec1-2b88253555b6. read_thread devolvió cinco turnos, sin más páginas y un único adjunto de texto. Se leyó el adjunto Texto pegado(3).txt: análisis histórico de H0 por Claude; no se considera prueba del entorno actual. No contiene imágenes. La carpeta actual no tenía archivos de proyecto antes de preparar esta entrega.

Decisiones firmes: identidad FitnessApp/com.maikol.fitnessapp, minSdk 28, tecnologías base, local-first, cinco pestañas, seis tarjetas, diez pantallas, tema y estilo oscuro, fases progresivas, H0 sencilla sin Health Connect, H1 visual, autorización por fase y sin push automático.

Especificación propuesta en este paquete: colores hex, espaciados, tipografía exacta, esquema de entidades, fórmulas de volumen/PR/progreso, DI manual, desglose H2–H7, exportación JSON y resolución de fuentes. Son decisiones de implementación iniciales revisables; no se atribuyen a una aprobación anterior. Siete paletas alternativas, fotografías finales y compatibilidad efectiva del Xiaomi S4 quedan pendientes. No hay promesa de fechas ni entorno validado.

## Fuentes técnicas oficiales consultadas el 23 de noviembre de 2026
- Arquitectura Android: https://developer.android.com/topic/architecture/recommendations
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Disponibilidad Health Connect: https://developer.android.com/health-and-fitness/health-connect/availability
- Inicio e integración: https://developer.android.com/health-and-fitness/health-connect/get-started
- Agregación y deduplicación: https://developer.android.com/health-and-fitness/health-connect/aggregate-data
- Lectura, paginación y permisos: https://developer.android.com/health-and-fitness/health-connect/read-data

Las recomendaciones de arquitectura sustentan el flujo UI/repositorio y la ubicación de DataStore. La documentación Health Connect sustenta detección de proveedor, permisos por tipo y lectura acotada. No se fija aquí una matriz de versiones que no se ha compilado; Claude debe verificarla al implementar.
