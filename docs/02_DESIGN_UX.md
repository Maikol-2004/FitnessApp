# Sistema de diseño y contratos UX

## Autoridad visual
Dirección aprobada: premium oscuro, elegante, deportivo, sin neón; grafito, champán, blanco roto, fotografía sobria integrada y gráficos limpios. Las medidas y hex siguientes son una propuesta implementable, no valores extraídos de imágenes. Si aparecen originales, comparar y resolver diferencias con el usuario antes de cambiar estructura aprobada.

## Tokens propuestos
| Token | Valor | Uso |
|---|---|---|
| background | #121416 | Fondo |
| surface | #1D2023 | Tarjeta |
| surfaceRaised | #282C30 | Campo y panel |
| primary | #D8C3A5 | Acción y selección |
| onPrimary | #17191B | Texto sobre champán |
| onSurface | #F4F0EA | Texto principal |
| onSurfaceVariant | #BCB7AE | Texto secundario |
| outline | #76736D | Contorno funcional |
| error | #FFB4AB | Error con icono y texto |
| success | #A7C8B0 | Confirmación con texto |

Verificar contraste de combinaciones reales: texto normal 4.5:1, texto grande y límites funcionales 3:1. No usar texto champán sobre fotografías sin protección. No afirmar contraste por el nombre del token.

Tipografía inicial del sistema Android, sin descarga obligatoria: títulos 28/34 sp, sección 22/28, tarjeta 18/24, cuerpo 16/24, etiqueta 14/20; métricas 32/38 con cifras tabulares si disponibles. Pesos regular/medio/semibold; no convertir texto en bitmap.
Espaciado 4, 8, 12, 16, 24, 32 dp; margen horizontal 16 dp; separación de tarjetas 12 dp; radio tarjeta 20 dp, control 12 dp. Botón de altura mínima 48 dp. Inicio usa dos columnas en ancho normal; con texto ampliado o ancho insuficiente adaptar a una columna sin cortar títulos. Respetar barras del sistema, teclado e insets.

## Componentes
FitnessScaffold con barra inferior; MetricCard con valor/unidad/fuente/fecha; CategoryPhotoCard con título y subtítulo sobre degradado; PrimaryButton; EmptyState con CTA; ErrorState con reintento; PermissionCard; ChartCard con tabla alternativa; SetRow con campos y estado; DateSelector; RecordListItem con acciones accesibles. Estados pressed, focused, disabled, loading, empty y error forman parte del contrato.
Fotografías: pesas, reloj, comida, agua, dormitorio y progreso. No hay seis fotos sueltas disponibles tampoco. H1 debe usar un fallback honesto de superficie e icono lineal decorativo; anotar la desviación visual. No extraer fotos de supuestas pantallas ausentes ni introducir imágenes remotas sin procedencia/licencia. Las referencias futuras irán en design/reference/; los assets reales de la aplicación se gestionan aparte en recursos Android.

## Navegación
Cinco destinos raíz exactos: Inicio, Entrenar, Progreso, Salud, Perfil. Nutrición, Hidratación y Sueño se abren desde las tarjetas de Inicio y vuelven al origen con Atrás. Rutina y sesión se anidan en Entrenar. Restaurar estado de cada raíz; no duplicar destinos al tocar pestaña seleccionada. Pasar IDs, nunca entidades serializadas. Sesión activa ofrece acceso de continuación desde Inicio/Entrenar. Atrás durante sesión no descarta datos; finalizar y descartar son acciones diferentes.

## Movimiento propuesto
Feedback de pulsación breve, transiciones de estado 150–220 ms y actualización de progreso hasta 250 ms. Sin entradas escalonadas que retrasen uso, sin animaciones en cada pulsación de teclado ni contadores que oculten el valor real. Respetar escala de animaciones del sistema y accesibilidad. No depender del movimiento para comprender cambios.

## Estados de datos
Loading conserva estructura y no muestra ceros ficticios. Empty explica cómo registrar o conectar. Error conserva datos previos con marca de antigüedad y permite reintentar. Permiso ausente muestra conectar/gestionar, nunca bucle automático de solicitud. Valor cero solo se muestra cuando hay evidencia real de cero. Preview usa etiqueta Demo. No mostrar nombre personal, calorías, sueño o pasos de ejemplo en producción.

## Diez pantallas
### 01 Inicio
Ruta: `home`.

Contenido: Cabecera con saludo y avatar; resumen diario; seis tarjetas Entrenamiento, Salud, Nutrición, Hidratación, Sueño y Progreso; próximo entrenamiento.

Interacción: Avatar abre Perfil; cada tarjeta abre su módulo; iniciar abre sesión o invita a crear rutina.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 02 Entrenamiento
Ruta: `training`.

Contenido: Rutinas, acción crear, sesión pendiente e historial. Tarjetas con nombre, ejercicios y última sesión.

Interacción: Crear y editar rutina con formularios; tocar rutina abre detalle; archivar no borra historial.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 03 Detalle de rutina
Ruta: `routine/{routineId}`.

Contenido: Nombre, notas, lista ordenada de ejercicios con series/reps/carga/descanso y CTA iniciar.

Interacción: Editar, duplicar, archivar con confirmación; identificador inexistente muestra recuperación a Entrenar.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 04 Sesión activa
Ruta: `session/{sessionId}`.

Contenido: Tiempo, ejercicio actual, tabla de series, carga/reps editables, completar, descanso y finalizar.

Interacción: Guardar cada mutación; restaurar tras muerte de proceso; cancelar descanso no elimina series; finalizar abre resumen adicional sin referencia original.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 05 Salud
Ruta: `health`.

Contenido: Estado de conexión, pasos, distancia, actividad, calorías activas, frecuencia y enlace a sueño.

Interacción: Conectar y refrescar; permisos parciales por métrica; abrir ajustes de permisos; última actualización visible.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 06 Nutrición
Ruta: `nutrition`.

Contenido: Fecha, kcal/meta, macros, lista de comidas y añadir alimento.

Interacción: Cambiar día; crear alimento manual o favorito; editar/eliminar entrada; totales reactivos.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 07 Hidratación
Ruta: `hydration`.

Contenido: Cantidad/meta, indicador, botones rápidos, añadir y registros del día.

Interacción: Registrar, deshacer última alta, editar/eliminar y configurar recordatorios opcionales.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 08 Sueño
Ruta: `sleep`.

Contenido: Último descanso, intervalo, duración, fases disponibles e histórico semanal.

Interacción: Añadir manual, editar registro propio y filtrar periodo; importado no editable localmente como original.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 09 Progreso
Ruta: `progress`.

Contenido: Selector de periodo, peso, volumen semanal y PR por ejercicio.

Interacción: Añadir peso, cambiar periodo/ejercicio, consultar valores textuales y abrir historial.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.

### 10 Perfil y Apariencia
Ruta: `profile`.

Contenido: Nombre, unidades, metas, Titanium Champagne, estado de integración y gestión de datos.

Interacción: Guardar preferencias inmediatamente, mostrar otras paletas como futuras sin selector activo; exportar/borrar en H7.

Estados: carga, vacío, error y éxito según origen de datos; texto ampliado, TalkBack y vuelta con Atrás verificables. Referencia gráfica original: no disponible.
