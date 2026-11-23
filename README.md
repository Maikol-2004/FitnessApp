# FitnessApp

Aplicación Android local-first con Kotlin, Jetpack Compose, Material 3, Room y DataStore. Tema oscuro Titanium Champagne, applicationId com.maikol.fitnessapp y minSdk 28. Repositorio con base H0; este cambio entrega planificación y recursos de H1/H2, no su código.

## Siguiente trabajo
1. [Plan maestro H1 y H2](docs/07_H1_H2_DELIVERY.md): composición, funcionalidad, pasos y aceptación.
2. [Movimiento e interacción](docs/08_MOTION.md): cargas, transiciones, relleno, serie, descanso y final.
3. [Encargo para Claude](docs/CLAUDE_NEXT.md): sincronización segura y ejecución de H1 solamente cuando el usuario lo solicite.
4. [Manifiesto de recursos](design/assets-manifest.json) y [colores exactos](design/titanium-champagne/colors.json).

## Referencias visuales
- [Tarjetas con icono y descripción, aportadas por el usuario](design/references/cards-layout-user-approved.webp).
- [Inicio, propuesta aprobada en colores y estructura](design/references/home-h1-approved.webp).
- [Entrenamiento H2, propuesta nueva para guiar la implementación](design/references/training-h2-proposal.webp).

Siete fotografías nuevas reutilizables en design/assets/photos/. Son archivos reales listados con dimensiones y hashes, no las diez imágenes originales que no se pudieron recuperar. Las tres referencias son documentación y no se empaquetan como pantallas de la aplicación.

## Construcción y estado
Usar el Wrapper y las versiones del proyecto. Preparación local y matriz en [TOOLCHAIN](docs/TOOLCHAIN.md). Resultados históricos y limitaciones H0 en [H0_REPORT](quality/H0_REPORT.md): API 28, móvil real y TalkBack manual pendientes hasta que se prueben. No reejecutados por este cambio documental.

Comandos desde la raíz en Windows, con SDK/JDK configurados:
```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```
El último requiere dispositivo/emulador. local.properties y credenciales no se versionan.

## Producto completo
[Producto](docs/01_MASTER.md), [diseño y UX](docs/02_DESIGN_UX.md), [arquitectura](docs/03_ARCHITECTURE.md), [roadmap](docs/04_ROADMAP.md), [aceptación](docs/05_ACCEPTANCE.md) y [decisiones](docs/06_DECISIONS_SOURCES.md). Para H1/H2 prevalecen los documentos 07/08 actuales. Revisar una fase antes de autorizar la siguiente. START_HERE.md conserva el encargo histórico de H0, no es el encargo actual.
