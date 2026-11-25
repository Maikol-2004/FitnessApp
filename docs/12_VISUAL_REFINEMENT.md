# Refinamiento visual sobre F1.1

## Estado y alcance
F1 y F1.1 ya están implementadas. No reconstruir H0/H1/H2 ni repetir el lote F1. Integrar la entrega deliveries/2026-11-25-visual sobre la rama funcional actual, conservando cambios posteriores. La base revisada es c2c2f324e57fcc297126976970b28896da4f0231.

## Diseño aprobado
Mantener Titanium Champagne: fondo #121416, superficie #1D2023, elevada #282C30, acento #D8C3A5, texto #F4F0EA, secundario #BCB7AE. Dos tarjetas por fila en el móvil normal, objeto reconocible a la derecha, texto a la izquierda. Conservar cinco destinos y el primer Liquid Glass, sus estados y comportamiento. Adaptar a fuentes grandes sin recortes.

## Cambios concretos incluidos
1. Inicio: anillo de objetivo de pasos; cuadrícula de indicadores Pasos, Calorías activas, Distancia y Actividad con iconos propios. Valores derivados del estado real existente, sin datos inventados. Raya cuando no existe valor, cero cuando existe un cero real. No confundir calorías activas con calorías ingeridas. Hoy/Ver salud abre Salud existente; no promete un nuevo calendario. Fuente y estado vacío visibles.
2. Perfil: avatar/inicial, nombre guardado, cabeceras con iconos. Metas agrupadas en Actividad y agua y Nutrición diaria. Conservar guardado, validaciones, unidades, movimiento y tema actuales.
3. Salud, Nutrición, Hidratación, Sueño y Progreso: cabecera fotográfica consistente, distintivo e icono de categoría, filas de registros con icono y separación clara. Reutilizar fotos existentes. Entrenar conserva el flujo actual. Esta entrega mejora la estructura compartida, no rediseña por completo formularios ni gráficos específicos de cada categoría.
4. Tarjetas: borde champagne muy tenue para separar superficies.

## Integración
Revisar el diff y las ocho rutas en el manifiesto. Aplicar selectivamente los cambios si existe trabajo posterior, sin sobrescribir archivos completos a ciegas. Comprobar git apply --check antes de aplicar el parche; si falla por contexto o saltos de línea, integrar por diff. Los archivos de source/ son propuestas, no un segundo módulo compilable. No copiar nada a app/ que no corresponda a esas rutas. No cambiar applicationId, esquema Room, datos, versiones o dependencias por esta entrega.

## Movimiento y funcionamiento
Conservar el movimiento y las cargas reales ya implementadas: lente de selección Liquid Glass, feedback de pulsación, rellenos vinculados a datos y preferencia de movimiento reducido. No añadir esperas artificiales, GIF de carga permanente ni framework web/JavaScript. La mancuerna de carga solo corresponde a una operación realmente pendiente y debe desaparecer al resolver éxito/error. No rehacer infraestructura de animación ya funcional.

## Validación y aceptación
La copia aislada ha compilado, pasado 57 pruebas unitarias y lint sin errores (13 advertencias). Se comprobó manualmente navegación Inicio/Perfil/Salud y capturas vacías en emulador API37. No es una validación integral de CRUD, accesibilidad o móvil físico de esta nueva propuesta.

Tras integrar, ejecutar una secuencia final: build Debug, unitarias pertinentes, lint; recorrido por seis categorías y cinco destinos; guardar/cambiar metas y reiniciar; registrar actividad y comprobar resumen/anillo; cero frente a dato ausente; unidades; edición/borrado afectados por las filas; tamaño de fuente 200%, teclado, scroll y movimiento reducido. Pruebas intermedias solo para bloqueos o correcciones concretas. No añadir tests que solo repitan el layout ni nuevas capas sin uso real. Conservar datos del usuario y no ejecutar fixtures destructivas sobre su instalación.

Cierre: capturas Inicio con registros de prueba identificados, Perfil y categorías, resumen breve de comprobaciones y pendientes reales. No presentar los formularios/gráficos específicos como rediseñados si solo se aplicó la estructura común. El usuario autoriza esta subida documental/paquete; no equivale a autorización para un push futuro del código integrado de Claude.
