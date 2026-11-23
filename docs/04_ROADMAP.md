# Roadmap fase por fase

Cada fase se implementa tras autorización explícita del usuario. Entregar resultado comprobable y detenerse al finalizar; una fase bloqueada no se marca completada. No fijar fechas sin capacidad y revisión del repositorio.

## H0 Base técnica
Dependencia: Ninguna.

Alcance: Inspección del repositorio y entorno; proyecto compilable, Wrapper, gitignore, tema, componentes con previews, navegación base mínima, DataStore y Room mínima sin entidades de negocio anticipadas. Sin permisos de salud.

Puerta de aceptación: Compila y abre; tema aplicado; preferencia persiste; documentación de toolchain; no seis pantallas vacías de negocio.

Evidencia: Captura base y previews; salida de build/test/lint disponible y listado de pendientes.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.

## H1 Inicio reconocible
Dependencia: H0 aprobada.

Alcance: Cabecera, resumen vacío o real, cuadrícula de seis tarjetas, próximo entrenamiento y cinco destinos. Las secciones pendientes muestran un estado explicativo honesto hasta su fase. Fallback documentado para fotografías ausentes.

Puerta de aceptación: Todas las tarjetas navegan y Atrás funciona; dos columnas normales; fuente ampliada sin cortes; no números de demo en release.

Evidencia: Capturas de Inicio normal, vacío y texto grande; mapa de navegación.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.

## H2 Entrenamiento completo
Dependencia: H1 aprobada.

Alcance: CRUD de rutinas/ejercicios/series, orden, detalle, sesión activa, descanso, pausa, historial y resumen final.

Puerta de aceptación: Flujo crear → entrenar → finalizar persiste; recuperación tras muerte de proceso; doble toque no duplica sesión; editar rutina conserva histórico.

Evidencia: Pruebas de sesión/DAO y capturas de las cuatro vistas del módulo.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.

## H3 Salud y proveedor
Dependencia: H2 aprobada.

Alcance: Health Connect opcional, permisos mínimos, agregados y fuente/cobertura, actualizaciones y caché. Alimentar resumen de Inicio con datos reales.

Puerta de aceptación: Denegación parcial/revocación/sin proveedor/sin registros no bloquean app; importación repetida no duplica; no promesa de Xiaomi.

Evidencia: Matriz de dispositivos/proveedor y prueba con datos conocidos; permisos declarados.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.

## H4 Nutrición e hidratación
Dependencia: H3 aprobada.

Alcance: Alimentos manuales, favoritos, entradas y macros; agua, metas y recordatorios opcionales con permiso de notificaciones cuando aplique.

Puerta de aceptación: Editar/eliminar recalcula; registros sobreviven reinicio; recordar no obliga a aceptar permisos; sin alarmas exactas requeridas.

Evidencia: Pruebas de cantidades/fechas y capturas con datos reales de prueba etiquetados.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.

## H5 Sueño
Dependencia: H4 aprobada.

Alcance: Registro manual, integración del sueño disponible y vista semanal; resolución de solapamientos y fuente.

Puerta de aceptación: Noche que cruza medianoche y cambio horario correctamente calculadas; fases ausentes no se inventan; no suma duplicada.

Evidencia: Pruebas temporales y capturas manual/importado/sin datos.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.

## H6 Progreso y perfil
Dependencia: H5 aprobada.

Alcance: Peso, volumen, PR, filtros, gráficos accesibles; nombre, unidades y metas; estructura de temas sin siete paletas inventadas.

Puerta de aceptación: Totales coherentes con sesiones; conversión sin alterar almacenamiento; TalkBack accede a valores; preferencias sobreviven reinicio.

Evidencia: Fixtures conocidas y valores esperados; capturas con unidades alternativas.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.

## H7 Entrega y robustez
Dependencia: H6 aprobada.

Alcance: Exportación propia, borrado, auditoría de permisos/backups, migraciones, accesibilidad, rendimiento y guía de instalación final.

Puerta de aceptación: No pérdida tras actualización; exportación válida; cancelación de borrado intacta; instalación limpia y actualización verificadas.

Evidencia: Informe final con resultados reales, limitaciones, APK debug si se ha construido y matriz de pruebas.

Fuera de fase: cualquier función de fases posteriores. Corregir regresiones de lo ya entregado antes de avanzar.
