# Verificación de la corrección visual propuesta

Fecha: 25 de noviembre de 2026. Inspección de ramas: main a0fc8cc y fase/h2 4953928.

Se leyeron los componentes de Inicio/fotos, barra inferior, botones, motion y componentes de entrenamiento de H2. Se comprobó que descripción 80 %, foto Crop, scrim fuerte y falta de indicador activo explican las capturas. La solución documentada preserva dos columnas, cinco destinos y datos de H2.

Seis variantes fotográficas reales disponibles y decodificables; hashes y dimensiones en design/polish-v2/manifest.json. Se inspeccionaron visualmente: cada sujeto es reconocible, menor y a la derecha. La generación de mockup de una columna se descartó y no se incluye. Los PNG de origen siguen fuera del runtime; WebP para el proyecto.

La entrega modifica únicamente planificación y recursos, ningún Kotlin, esquema de DB, archivo de build, test o estado del usuario. No se ha compilado o probado la app desde este cambio. Claude debe aplicar el encargo en la rama con H2 y verificar el resultado real con dos tarjetas por fila. Las pruebas recomendadas son proporcionales al cambio de UI, sin eliminar las pruebas existentes.
