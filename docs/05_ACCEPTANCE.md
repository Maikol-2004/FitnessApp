# Criterios de aceptación ejecutables

Estos casos son requisitos; no se han ejecutado sobre una app en este paquete. En cada fase guardar resultado PASS/FAIL/NO EJECUTADO, dispositivo/API, versión de commit y evidencia. No confundir prueba documental con prueba de aplicación.

| ID | Fase | Dado / Cuando | Resultado esperado |
|---|---|---|---|
| AC00 | H0 | Checkout limpio, JDK/SDK compatibles; compilar con Wrapper | assembleDebug, unitarias y lint disponibles pasan o bloqueo preciso impide aceptación |
| AC01 | H0 | Primera apertura y reinicio tras preferencia | Sin crash; Titanium Champagne; preferencia conservada; sin permisos de salud |
| AC02 | H1 | Instalación sin datos; recorrer Inicio | Cabecera, resumen, seis tarjetas y próximo entrenamiento con CTA apropiados; ningún dato personal inventado |
| AC03 | H1 | Pulsar cada tarjeta y las cinco pestañas; usar Atrás | Destino correcto y regreso al origen, sin duplicados; pestañas con nombres aprobados |
| AC04 | H1 | Fuente 200 %, pantalla estrecha y TalkBack | Sin textos truncados funcionales, foco coherente y objetivos >=48 dp |
| AC05 | H2 | Crear rutina de dos ejercicios; editar y duplicar | Orden y valores persisten al cerrar/abrir; duplicado tiene nuevos IDs |
| AC06 | H2 | Iniciar con doble toque | Una sola sesión activa y snapshots completos |
| AC07 | H2 | Completar 2 series de 10 reps a 20 kg | Volumen 400 kg; al editar una a 8 reps, 360 kg; calentamiento excluido según regla |
| AC08 | H2 | Pausar, matar proceso, abrir y continuar | Series confirmadas y estado recuperados; tiempo sin valores negativos; descanso no reiniciado arbitrariamente |
| AC09 | H2 | Finalizar dos veces; después editar rutina | Una sesión histórica, volumen estable y nombre/series históricos sin alteración |
| AC10 | H2 | Volver atrás o cancelar descarte | No se pierde sesión; descarte explícito cambia estado y se excluye de progreso |
| AC11 | H3 | Proveedor ausente, denegado o parcial | Estado y acción apropiados por métrica; resto de la app utilizable |
| AC12 | H3 | Refrescar dos veces el mismo intervalo | Mismo resultado sin duplicados; origen y última actualización visibles |
| AC13 | H3 | Revocar permiso y volver a primer plano | Sin crash; métrica ya no se presenta como autorizada/actual; caché gestionada según política |
| AC14 | H3 | Proveedores superpuestos con registros conocidos | Agregado correcto conforme al proveedor; no suma cruda duplicada; ausencia se distingue de cero |
| AC15 | H4 | Alimento 100 g: 200 kcal, 10 g proteína; registrar 150 g | 300 kcal y 15 g proteína; cambiar a 50 g da 100 kcal y 5 g; eliminar resta entrada |
| AC16 | H4 | Añadir 250 + 500 ml, editar primero a 150 | Total 650 ml; borrar segundo deja 150; reinicio conserva |
| AC17 | H4 | Rechazar notificaciones y registrar agua | Registro funciona; recordatorios explican permiso; sin solicitudes repetitivas |
| AC18 | H5 | Dormir 23:00–07:00 sin cambio horario | 8 horas atribuidas al día del despertar; cambio DST usa instantes reales |
| AC19 | H5 | Registro manual superpuesto con importado | No suma ambas duraciones; fuente elegible y registro manual conservado |
| AC20 | H6 | Pasar kg a lb y volver | Misma cantidad normalizada; sin deriva por redondeos de presentación |
| AC21 | H6 | Semana con sesiones completas y descartadas | Solo completas contribuyen a volumen; PR coincide con mayor carga completada |
| AC22 | H7 | Exportar con selector y analizar JSON | schemaVersion, unidades, zona y registros propios consistentes; caché de salud excluida |
| AC23 | H7 | Cancelar borrado; después confirmar borrado | Cancelar conserva; confirmar limpia datos locales/tareas; Health Connect permanece intacto |
| AC24 | H7 | Instalar nueva versión sobre DB anterior | Migración conserva registros y relaciones; no recreación destructiva |
| AC25 | Todas | Sin conexión y ante error de guardado | Registro local utilizable; fallo no muestra éxito falso ni descarta formulario |

## Revisión visual
Comparar capturas en mismo tamaño, densidad y escala de fuente. Validar jerarquía, márgenes, colores, radio, orden de secciones y cinco pestañas. Las imágenes originales están ausentes: solo puede validarse la especificación escrita; fidelidad a originales queda NO EVALUABLE. Registrar diferencias de fallback fotográfico como limitación, no como cumplimiento visual perfecto.

## Rendimiento y entrega
Medir con build adecuada y dispositivo documentado: apertura, scroll de Inicio y sesión; corregir ANR, crashes y bloqueos de escritura. No fijar cifras de rendimiento sin medir. Al menos un dispositivo/emulador API 28 y uno Android 14+; Health Connect real requiere entorno compatible. No pasar por completadas pruebas dependientes de hardware que no se usó.
