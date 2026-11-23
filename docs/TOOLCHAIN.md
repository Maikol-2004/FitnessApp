# Toolchain y compilación (H0)

Verificado el 2026-11-23 en Windows 11 con Android Studio 2026.2.1. Las versiones están fijadas en `gradle/libs.versions.toml` y en el Wrapper; no se usan versiones dinámicas.

## Matriz
| Componente | Versión | Motivo / fuente |
|---|---|---|
| Gradle (Wrapper) | 9.6.0, `distributionSha256Sum` oficial | Mínimo y versión por defecto de AGP 9.5 (notas de versión de AGP en developer.android.com) |
| Android Gradle Plugin | 9.5.2 | Último estable en Google Maven. AGP 9.5 admite hasta API 37 y requiere JDK 17 y Build Tools 36.0.0 |
| Kotlin | 2.4.21 | Kotlin integrado de AGP 9 (sin plugin `org.jetbrains.kotlin.android`, según la guía de migración a Kotlin integrado). La versión la fija el plugin del compilador de Compose |
| KSP | 2.3.14 | Procesador de Room (kapt es incompatible con Kotlin integrado) |
| JDK para ejecutar Gradle | JBR 25.0.3 de Android Studio | Bytecode con destino Java 17 (`compileOptions`) |
| compileSdk / targetSdk | 37 / 37 | Única plataforma instalada (android-37.0) |
| minSdk | 28 | Decisión firme |
| Compose BOM | 2026.09.00 (Compose 1.12.1, Material3 1.4.0) | La 2026.11.01 (Compose 1.13) exige compileSdk 37.1, que no está instalado |
| Room | 2.8.5 | Esquema exportado en `app/schemas/` |
| DataStore Preferences | 1.3.0 | |
| Navigation Compose | 2.10.2 | Rutas de texto; sin plugin de serialización |
| Activity 1.13.0 · Lifecycle 2.11.1 · Core 1.19.1 | | |
| kotlinx-coroutines-test | 1.9.0 | Coincide con el `kotlinx-coroutines-core` que resuelve la app. Con 1.11.0 se produce `NoSuchMethodError` en las pruebas instrumentadas |
| Espresso | 3.7.0 | Las versiones < 3.7 llaman a `InputManager.getInstance`, que ya no existe en API 37 |

No se usa desugaring: `java.time` existe de forma nativa desde API 26 y minSdk es 28.

Lint muestra 3 avisos informativos de «versión más nueva disponible» (Gradle 9.9.2, BOM 2026.11.01 y coroutines-test 1.11.0). Son descartes deliberados, explicados arriba.

## Preparación local
1. Crea `local.properties` en la raíz. No se versiona. Contenido: `sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk`. Lint exige escapar `\` y `:`.
2. Apunta `JAVA_HOME` a la ruta de Windows del JBR: `C:\Program Files\Android\Android Studio\jbr`. En Git Bash usa `C:/Program Files/...`, no `/c/...`.

## Comandos
```
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest   # requiere emulador o dispositivo
```

## Copias de seguridad
`allowBackup=false`. Además, `data_extraction_rules.xml` (Android 12+) y `backup_rules.xml` excluyen la base de datos, DataStore y sharedprefs de la copia en la nube y de la transferencia entre dispositivos. Efecto: si se desinstala la app o se cambia de dispositivo, se pierden los datos locales. La política se revisará en H7.
