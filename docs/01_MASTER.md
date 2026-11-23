# Documento maestro de FitnessApp

## Propósito y decisiones firmes
Aplicación Android personal para registrar entrenamiento y hábitos y consultar su evolución con datos locales. Nombre provisional FitnessApp; applicationId com.maikol.fitnessapp; minSdk 28. Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, Room y DataStore. Health Connect se integra en H3. Sin servidor de pago obligatorio ni cuenta obligatoria. Interfaz inicial en español, modo oscuro Titanium Champagne. Entrenamiento es el primer módulo completo.

Se aprobó una dirección visual de grafito, champán suave, fotografía integrada y tarjetas redondeadas. Inicio contiene cabecera, resumen diario, seis categorías en dos columnas, próximo entrenamiento y navegación inferior Inicio, Entrenar, Progreso, Salud y Perfil. Las diez vistas de referencia se enumeran en el manifiesto. El historial textual habla de imágenes diseñadas, pero no proporciona sus binarios. El adjunto recuperado describe una propuesta de H0, no código ni pruebas ejecutadas.

## Alcance y prioridades
P0: persistencia fiable, recuperación de sesión, navegación clara, estados sin datos, consistencia entre vistas y diseño reconocible desde H1. P1: integración opcional de salud y registro manual de nutrición, agua y sueño. P2: tendencias, exportación y personalización. No incluir de oficio IA, suscripciones, publicidad, red social, chat, backend, GPS, integración Bluetooth directa con reloj ni base comercial de alimentos.

El usuario debe poder entrenar sin conexión, cerrar la app y recuperar registros. Una app recién instalada muestra vacíos útiles y acciones de empezar, nunca datos de salud inventados. Las previews y el modo demo pueden tener datos sintéticos, marcados como demostración y separados del almacenamiento personal.

## Requisitos de producto
FR01 Perfil local: nombre opcional, unidades kg/lb y km/mi, metas editables, apariencia y controles de datos. El saludo usa el nombre guardado o «Hola»; Michael no es un dato real de perfil.
FR02 Rutinas sin límite comercial: crear, editar, duplicar, ordenar y archivar. Ejercicios con nombre, notas, series objetivo, repeticiones, carga y descanso. No imponer un límite artificial; seguir sujeto al almacenamiento del dispositivo.
FR03 Sesión: iniciar desde rutina, completar series con valores reales, añadir serie, editar notas, descansar, pausar, reanudar y finalizar. Confirmar descartar una sesión con trabajo registrado. Una única sesión activa; ofrecer continuar antes de iniciar otra.
FR04 Historial: resumen final persistente con duración, ejercicios, series y volumen; acceso desde Entrenar y Progreso. Modificar una rutina no altera sesiones anteriores.
FR05 Salud: pasos, distancia, minutos de actividad, calorías activas, frecuencia cardíaca y sueño cuando el proveedor y los permisos los permitan. Cada métrica muestra fuente, intervalo y última actualización. Ausencia no equivale a cero.
FR06 Nutrición: alimentos manuales y favoritos, comidas del día y metas de energía/macros. Registrar cantidad y base nutricional por 100 g o por porción con gramos explícitos. Editar y eliminar recalcula los totales. Sin promesa de escáner o búsqueda remota.
FR07 Hidratación: registrar mililitros, accesos 150/250/500 ml propuestos, cantidad personalizada, objetivo y lista diaria editable. Recordatorios opcionales configurados por el usuario, nunca activados por defecto.
FR08 Sueño: registro manual de inicio/fin y calidad subjetiva opcional; importación si está disponible. Fases solo si la fuente las aporta. No generar puntuación clínica ni fases a partir de horas.
FR09 Progreso: peso manual, volumen por semana y marcas personales por ejercicio. Gráficos con periodo y tabla textual equivalente. No inferir composición corporal ni diagnósticos.
FR10 Apariencia: Titanium Champagne funcional; arquitectura de temas preparada para siete variantes aún sin nombres ni paletas aprobadas. No presentar siete temas ficticios como decisiones del usuario.
FR11 Datos: persistencia local, exportación manual versionada y borrado confirmado en H7. Sin telemetría ni servicios remotos por defecto. Definir claramente que borrar datos locales no borra el almacén Health Connect.

## Reglas de métricas propuestas
No hay fórmula aprobada para progreso diario: usar hasta H3 «Sin objetivo configurado»; después porcentaje de pasos/meta de pasos, etiquetado «Objetivo de pasos», limitado visualmente a 100 % y con valor real aparte. No mezclar salud y entrenamiento para inventar un índice general.
Volumen = suma de carga normalizada en kg × repeticiones de series completadas de resistencia; excluir calentamiento si está marcado y series sin carga externa. Mostrar alcance del cálculo. PR inicial: mayor carga completada por ejercicio, desempate por repeticiones; no estimar 1RM. Calorías activas importadas no se suman de nuevo a calorías estimadas de entrenamientos. No estimar gasto propio en el MVP.
Totales nutricionales = valor de base × cantidad/base. Permitir decimales y redondear solo al presentar; kcal introducidas se conservan, aunque difieran del cálculo 4/4/9. El peso usa kg internos y conversión al presentar. Agua en ml; energía en kcal; distancias en metros; duración en segundos. Metas son preferencias del usuario, no prescripciones automáticas.

## Calendario, accesibilidad y seguridad de uso
Instantes en UTC y zona/offset de origen conservados; agrupación por fecha en zona seleccionada del dispositivo, mostrando los límites del periodo. Sueño nocturno se atribuye al día del despertar. Intervalos inicio inclusivo y fin exclusivo. Probar medianoche y cambios de hora. Formatear números con configuración regional; no almacenar cadenas formateadas.
TalkBack, escala de fuente al 200 %, objetivos táctiles de al menos 48 dp, gráficos con resumen textual y estados que no dependan solo del color. Al registrar datos inválidos explicar el campo y conservar el resto. Los valores de salud se presentan como seguimiento personal sin diagnósticos. No impedir registros extremos válidos por umbrales inventados; exigir cantidades finitas y unidades claras.

## Flujos completos
Primera apertura → perfil opcional → Inicio vacío → Entrenar → crear rutina → detalle → iniciar sesión → completar series → finalizar → resumen → Progreso actualizado.
Inicio → Salud → explicación de conexión → permisos seleccionados → lectura → métricas; denegación permite seguir usando el resto.
Inicio → Nutrición/Hidratación/Sueño → añadir registro → confirmar → total actualizado → cerrar/reabrir → mismo dato.
Perfil → datos → exportar con selector del sistema o borrar con confirmación explícita. Cancelar deja datos intactos.
