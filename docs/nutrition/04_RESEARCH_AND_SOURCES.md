# Nutrición — referencias consultadas y decisiones

Consulta:26–27nov2026. Son fuentes primarias de producto/documentación; no un ranking de «la mejor app». No se descargaron ni copiaron recursos de los competidores.

## Referencias de producto

- **Fitia:** describe registro por texto/código/foto/voz, objetivos y recetas/planes. Tomamos facilidad para registrar y organizar comidas; la foto/voz y la planificación personalizada no se presuponen disponibles en FitnessApp. Sus datos revisados y recursos no son una base abierta que podamos importar. https://fitia.app/
- **MyFitnessPal:** su ayuda explica seleccionar comida, alimento y ración antes de registrar. Aplicamos ese control explícito del destino y cantidad. No copiamos funciones premium ni asumimos acceso a su API. https://support.myfitnesspal.com/hc/en-us/articles/360032274592-How-to-log-food-to-your-diary
- **Cronometer:** distingue fuentes y alimentos propios, y explica que no todos los nutrientes están informados. Aplicamos procedencia y cobertura por nutriente; no llamamos0 a un dato desconocido. https://support.cronometer.com/hc/en-us/articles/360018239472-Data-Sources y https://support.cronometer.com/hc/en-us/articles/360042550452-Data-Confidence-Scores

## Fuentes y herramientas posibles

- **Open Food Facts API:** lectura de productos, contrato, límites y licencias consultados; revalidar al implementar. No es garantía de exactitud. https://openfoodfacts.github.io/openfoodfacts-server/api/
- **Datos nutricionales OFF:** referencia de campos y bases, incluidos valores por100g/ración y producto preparado. No inferir100ml por el nombre. https://openfoodfacts.github.io/openfoodfacts-server/dev/explain-nutrition-data/ y https://openfoodfacts.github.io/documentation/docs/Product-Opener/schemas/schemas/product/
- **USDA FoodData Central:** datos públicos y API con clave; alternativa para catálogo genérico cuando exista integración/configuración real. https://fdc.nal.usda.gov/api-guide/
- **ML Kit Android:** escaneo local con modelo empaquetado o descargable; escoger implementación real compatible y verificar cámara. https://developers.google.com/ml-kit/vision/barcode-scanning/android

## Decisiones propias de FitnessApp

Diseño abierto con el material del menú/editor, diario offline, migración sin inventar ingredientes, snapshots de consumo, objetivos manuales y sin restar automáticamente ejercicio. N1–N3 son una decisión de producto propia, no una promesa de paridad completa con Fitia. Los objetivos médicos, déficits automáticos, alergias certificadas, reconocimiento fotográfico y planificador se separan del seguimiento básico.

Las referencias gráficas se generaron con la herramienta integrada de imágenes de OpenAI. Sus cifras y fotografías son ejemplos; no son un conjunto de alimentos verificados. Los prompts y el manifiesto visual acompañan los PNG reales.
