# Encargo vigente N — Nutrición profesional

El usuario deja Entrenar como está y autoriza preparar Nutrición tomando Fitia y otras referencias como guía. Esta entrega contiene el plan, seis vistas conceptuales y criterios; todavía no implementa el módulo.

Lee docs/16_NUTRITION_MASTER.md y sus cuatro documentos en docs/nutrition/. Consulta design/nutrition-v1/README.md y manifest.json. La base revisada es cae2db1, Room v5; sincroniza origin/fase/f1 conservando trabajo local y todos los datos. No reconstruir G1/G2/V1/V2.

Implementa el lote N1–N3 completo: diario por comidas, alimentos y cantidades/raciones, registro rápido, favoritos/recientes, copiar, búsqueda externa y escáner real, recetas propias, objetivos coherentes y tendencias con cobertura. Usa los IDs/datos reales, snapshots y migración sin borrar comidas antiguas. No inferir ingredientes ni grupo de comidas históricas.

Diseño Titanium Champagne abierto, con Liquid Glass únicamente en controles, según las referencias. Las imágenes/cifras son conceptos: no copiar valores a producción ni usar fotos como información nutricional. No hace falta regenerar50imágenes de entrenamiento ni una biblioteca de fotos de alimentos.

Conserva null frente a0, base100g/100ml/ración, crudo/cocinado y origen. Los alimentos manuales guardados funcionan offline; los productos remotos se consultan realmente y se cachean al elegirlos. No hay un catálogo genérico precargado inventado. N2 debe respetar el contrato/licencias del proveedor y tener alternativa manual ante fallos. No publicar datos/fotos ni rellenar formularios externos por el usuario.

No detenerte a pedir permiso entre N1,N2,N3. Si existe un requisito externo no disponible, documenta el bloqueo exacto y completa lo independiente; no presentes una API simulada como funcional. N4/N5 (planificador, IA/foto/voz, servicios avanzados) quedan fuera. No desarrollar backend/cuentas ni tocar el entrenamiento por anticipación.

Una verificación conjunta proporcional al final, con migración, cálculos/fixtures, guardado, red/cámara, teclado, fuente grande y resultados reales N-01 a N-15. Entrega capturas y un único informe. No hacer push del código integrado hasta que el usuario lo revise.
