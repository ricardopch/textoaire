# Texto en el aire

Letrero LED de puntos: cada letra ocupa toda la pantalla y se recorre girando el móvil, como si el texto estuviera fijo en el aire.

## Compilar el APK

**Opción A, sin instalar nada (GitHub):** sube esta carpeta a un repositorio nuevo. En la pestaña *Actions* se ejecuta "Compilar APK"; al terminar, descarga `texto-en-el-aire-apk` y dentro está `app-debug.apk`.

**Opción B, Android Studio:** abre la carpeta, espera a que sincronice y pulsa *Build > Build APK(s)*. Queda en `app/build/outputs/apk/debug/app-debug.apk`.

**Opción C, línea de comandos** (con Android SDK y Gradle 8.7+): `gradle assembleDebug`

Para instalarlo, copia el APK al móvil y permite "instalar apps de orígenes desconocidos".

## Sin APK

`app/src/main/assets/index.html` es toda la app. Servida por HTTPS, se abre en Chrome del móvil y se puede "Añadir a pantalla de inicio".
